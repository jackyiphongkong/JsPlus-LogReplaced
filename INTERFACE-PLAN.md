# JsPlus-LogReplaced 官方 Interface 補全方案

> 狀態：方案（未實作）。評估日期 2026-10-30。
> 目標：讓本項目成為「五種日誌門面的完整官方 API 替身」——框架/容器用官方 API 編譯、運行期零配置統一落到 JsPlus 日誌引擎（`com.killcoding.log.Logger`）。

---

## 0. 結論先行

| 家族 | 官方 interface | 現有 | 缺 | 結論 |
| --- | --- | --- | --- | --- |
| Apache JULI (`org.apache.juli.logging.Log`) | 18 | 18 | **0** | 已完整，僅需回歸測試 |
| Commons Logging (`org.apache.commons.logging.Log`) | 18 | 0 聲明（extends slf4j） | **3** | 缺 `fatal×2` + `isFatalEnabled()` |
| SLF4J 1.7 (`org.slf4j.Logger`) | 61 | 50 | **31** | 缺全部 Marker 變體（25）+ `getName()` + 5 個 `isXEnabled(Marker)`；**`org.slf4j.Marker` 類型本身不存在** |
| Log4j 1.2 (`Logger` + `Category`) | 55 | 0 聲明 | **42** | 只有構造器；Category 的級別/Appender/Repository 語義全缺 |
| Log4j2 2.20 (`org.apache.logging.log4j.Logger`) | **384** | 0 聲明 | **344** | 幾乎空白；`Marker/Level/Message/Supplier/EntryMessage` 等類型全缺 |

**根因**：現行設計是「一個 `org.slf4j.Logger` 超集接口承載所有家族」——接口裡有 20 個官方 slf4j 沒有的方法（`debug(Object)`、`debug(Exception)`、`debug(String,Throwable)` 等），各家族自己的類型體系（Marker / Level / Appender / Message / Supplier）完全沒有。所以「能跑」但「不是官方 interface」。

**建議口徑**：官方方法簽名全量（編譯期任何官方 API 調用都能解析）+ 關聯類型最小替身 + 統一委派到 JsPlus 引擎（行為語義按家族能力降級）。其中 log4j2 的 344 個方法 **用生成器產出**，不手寫。

---

## 1. 審計方法與可複現證據

- 工具：`build/iface-audit/compare.py`（javap 提取官方 jar 與我方 jar 的方法簽名，按 `name(params)` 歸一化後求差集）
- 官方基準 jar（`build/iface-audit/ref/`）：
  - `slf4j-api-1.7.36.jar`
  - `commons-logging-1.2.jar`
  - `tomcat-juli-9.0.85.jar`
  - `log4j-1.2.17.jar`
  - `log4j-api-2.20.0.jar`
- 完整缺口清單：`build/iface-audit/report.txt`、`report.json`；分類統計：`build/iface-audit/groups.txt`

### 缺口分類（摘要）

| 家族 | 缺口構成 |
| --- | --- |
| slf4j | Marker 日誌方法 25（5 級 × 5 形態）+ `isXEnabled(Marker)` 5 + `getName()` 1 |
| commons | `fatal(Object)`、`fatal(Object,Throwable)`、`isFatalEnabled()` |
| log4j1 | repository/static API 12、Appender API 10、Level/Priority API 7、misc 8、`log(...)` 3、`fatal` 2 |
| log4j2 | `log(Level,...)` 48、Marker×級別 84、多參數（3–10 個）49、Supplier 族 60、Message 族 14、CharSequence 20、`traceEntry/traceExit/entry/exit` 15、`atX()/always/catching/throwing/printf/logMessage` 14、`isEnabled(Level/Marker)` 8、`fatal(String/... )` 若干 |

### 官方 jar 的關聯類型（完整替身必須一併提供）

| jar | 公開頂層類型（節選，完整見 groups.txt） |
| --- | --- |
| slf4j-api | `Logger`、`LoggerFactory`、`ILoggerFactory`、`Marker`、`MarkerFactory`、`IMarkerFactory`、`MDC` |
| commons-logging | `Log`、`LogFactory`、`LogSource`、`LogConfigurationException` |
| tomcat-juli | `Log`、`LogFactory`、`DirectJDKLog`、`LogConfigurationException` |
| log4j 1.2 | `Logger`、`Category`、`Level`、`Priority`、`Appender`、`AppenderSkeleton`、`Layout`、`Hierarchy`、`LogManager`、`MDC`、`NDC`、`spi.LoggingEvent`、`spi.LoggerFactory` 等 37 個 |
| log4j-api 2.20 | `Logger`、`Level`、`Marker`、`MarkerManager`、`LogManager`、`LogBuilder`、`EventLogger`、`ThreadContext`、`CloseableThreadContext`、`message.Message`、`message.EntryMessage`、`util.Supplier`、`util.MessageSupplier` |

---

## 2. 「完整實現」的口徑定義（三層，方案同時滿足 1、2；3 按能力降級）

| 層 | 含義 | 達標標準 | 本方案 |
| --- | --- | --- | --- |
| L1 簽名完整 | 官方 interface 聲明的每個方法在替身類上存在且簽名一致 | `compare.py` 顯示 `missing = 0`（每個家族） | 必須 |
| L2 類型完整 | 方法簽名引用的類型（Marker/Level/Message/Supplier/Appender…）由本項目提供 | 用本項目 jar 單獨編譯官方 API 調用樣例通過（官方 jar 不參與 classpath） | 必須 |
| L3 語義完整 | Marker 過濾、Appender 掛載、Repository 層級、Supplier 惰性求值等行為與官方一致 | 行為測試 | **降級**（見 §5 決策） |

> 說明：L2 是關鍵。若 classpath 上仍有官方 jar，會出現 duplicate class，替身失效；若沒有官方 jar，缺 Marker/Level 類型則**編譯期直接報錯**（`logger.debug(marker, "x")` 無法解析）。

---

## 3. 分家族實施方案

### 3.1 SLF4J（缺 31 + Marker 類型）— 優先級 P0

新增/修改：

| 類 | 動作 |
| --- | --- |
| `org/slf4j/Marker.java` | 新增接口（`getName()`、`add/remove/contains/hasChildren/iterator` 等 slf4j 1.7 聲明） |
| `org/slf4j/Logger.java` | 補 25 個 Marker 方法 + `getName()` + 5 個 `isXEnabled(Marker)` |
| `org/slf4j/LoggerClass.java` | 實現上述方法；Marker 處理：**忽略**（或把 `marker.getName()` 作為消息前綴，見決策 D3） |

**保留決策**：現有 20 個非官方方法（`debug(Object)`、`debug(Exception)`、`debug(String,Throwable)`…）**保留**——它們是 commons/juli/log4j 家族所依賴的超集，刪除會破壞現有實現；在 `Logger.java` 註釋標明「JsPlus 擴展（非 slf4j 1.7 官方）」。

可選（按實際調用面）：`IMarkerFactory` / `MarkerFactory`（no-op Marker 工廠）、`MDC`、`ILoggerFactory`。

### 3.2 Commons Logging（缺 3）— P0

`org/apache/commons/logging/Log` 目前是空接口（`extends org.slf4j.Logger`）。補：

```java
void fatal(Object message);
void fatal(Object message, Throwable t);
boolean isFatalEnabled();
```

映射：`fatal` → JsPlus `Logger.error(...)`（或新增 FATAL 級別，見決策 D4）。

### 3.3 JULI（缺 0）— P0 已完成

`Log` 18 方法與官方 tomcat-juli 一致；`LogCustom` 全部實現。僅需回歸測試。

### 3.4 Log4j 1.2（缺 42 + 6 類）— P1

新增類型：`Level`、`Priority`、`Appender`（no-op）、`LoggerRepository`（簡化）、`Hierarchy`（簡化，或 `LoggerRepository` 的子類）、`spi.LoggingEvent`、`spi.LoggerFactory`、`LogManager`（靜態入口）。

需補的 42 方法分兩組：

- **靜態/倉庫族（12）**：`getLogger(Class/String/String,spi.LoggerFactory)`、`getRootLogger()`、`getRoot()`、`getInstance(...)`×2、`exists(String)`、`getCurrentCategories()`、`getDefaultHierarchy()`、`getHierarchy()`、`getLoggerRepository()`
- **實例族（30）**：`fatal×2`、`getName()`、`getParent()`、`getLevel()/setLevel(Level)/getPriority()/setPriority(Priority)`、`getEffectiveLevel()`、`getChainedPriority()`、`isEnabledFor(Priority)`、`log×4`、`l7dlog×2`、`addAppender/removeAppender×2/removeAllAppenders/getAppender/getAllAppenders/isAttached/setAdditivity/getAdditivity/callAppenders`、`assertLog`、`getResourceBundle/setResourceBundle`、`shutdown()`

語義降級：`Appender` 全部 no-op（統一收口到 JsPlus 文件 + 控制台，不接 appender）；`Hierarchy/Repository` 返回本類單例（`getRootLogger()` 返回 root 實例）；`Level/Priority` 為常量映射。

### 3.5 Log4j2（缺 344 + 8 類）— P1/P2（生成式）

新增類型：`Level`（含 `getLevel/…` 常量）、`Marker`、`message.Message`、`message.EntryMessage`、`util.Supplier`、`util.MessageSupplier`、`LogBuilder`、`MarkerManager`（可選）、`ThreadContext`/`CloseableThreadContext`（可選）。

方法產出策略：**寫一個 `build/tools/genLog4j2Logger.py`**（模板 + 笛卡爾積）生成 `Logger` 接口 384 方法：

```
級別：trace/debug/info/warn/error/fatal
形態：
  (String)
  (String,Object...)
  (Object)
  (Object,Throwable)          # 官方有 Marker+Object 等
  (CharSequence) / (CharSequence,Throwable)
  (Message) / (Message,Throwable)
  (Supplier) / (Supplier,Throwable)
  (MessageSupplier) / (MessageSupplier,Throwable)
  (String,Object) … (String,Object×2..10)
  (String,Throwable)
  (String,Supplier...)
  (Marker, …全套鏡像)
  (Level, …全套鏡像)
  其他：log(Level, …)、atX()、always()、catching/throwing、printf、logMessage、
        traceEntry/traceExit/entry/exit、isEnabled(Level/Marker)、isXEnabled(Marker)、getName/getLevel/getMessageFactory/getFlowMessageFactory
```

語義降級：`Supplier/MessageSupplier` **立即求值**（破壞惰性，但行為正確）；`EntryMessage/Entry/Exit` 返回 no-op 對象；`LogBuilder` 為簡化 builder（終結時寫日誌）。

工作量：生成器 ~150 行 + 生成後的實現骨架（用「統一 `log(Level, Marker, Message, Throwable)` 出口 + 一個私有方法分派」可把 300+ 方法壓成 ~80 行實現）。

### 3.6 統一出口（避免 5 套重複）

現狀：`slf4j.LoggerClass`（261 行）是核心，`commons/log4j/log4j2` 繼承它，`juli.LogCustom` 另寫一套（90 行，重複）。

建議：抽 `com.killcoding.logreplaced.adapt` 包：

| 類 | 職責 |
| --- | --- |
| `LogCore` | 持有 `com.killcoding.log.Logger`；`emit(level, markerName, message, args, throwable)` 唯一出口 |
| `LevelMapper` | slf4j/commons/juli/log4j1/log4j2 級別 ↔ JsPlus 級別（INFO/WARN/ERROR/DEBUG/TRACE/MARK） |
| `ArgFormatter` | `{}` / `%s` / `Message` / `Supplier` 的統一格式化 |

之後 `slf4j.LoggerClass`、`juli.LogCustom`、log4j1/2 全部薄封裝到 `LogCore`，消除重複（也順手解決 QUALITY-REVIEW P2#6「兩套實作並存」）。

---

## 4. 驗證方案（方案的關鍵組成，不止「能編譯」）

1. **簽名校驗 gate**：`compare.py` 從「審計腳本」升級為 CI 步驟——5 個家族 `missing` 必須為 0，否則 CI 失敗（輸出 `report.txt` 作為 artifact）。
2. **雙向編譯樣例**：`test/iface/refapi/` 下按 5 個家族各寫一份「用遍官方 API」的 Java 樣例（覆蓋全部方法與類型），分兩次編譯：
   - `javac -cp <官方 jar>`（證明樣例本身合法）
   - `javac -cp <本項目 jar>`（證明替身可完全替代官方 jar）
   兩次都通過才算 L1+L2 達標。
3. **運行期冒煙**：新增 `test/TestReplacement.js`（在 TestAll 註冊，需你確認——現有 TestAll 已全綠）：
   - 5 個家族 `getLogger` 返回非 null；
   - 各級別、Marker 變體、多參數、Throwable 調用不拋異常；
   - 日誌落到 `logs/app/<yyyyMMdd>.log`（含 marker 前綴/參數展開內容）；
   - `isXEnabled` 與級別聯動。

   > 實施結果（見 §10.5）：最終**未**採用單檔 `TestReplacement.js`。實際做法是按日誌廠商拆成 5 個獨立套件（`TestSlf4j` / `TestCommonsLogging` / `TestJuli` / `TestLog4j` / `TestLog4j2`）；第三輪再把它們**註冊進 `TestAll.js`**（統一報告 87 用例 = 58 主控台/自檢 + 29 廠商，報告與實際套件對版），同時由 `TestFacades.bat` 單獨驅動、各自持有退出碼；`TestReplacement.js` 與後續判定為假陽性的 `TestDatasource.js` 均已刪除。相較原計畫，斷言從「不拋異常」升級為「對真實落盤內容斷言」。
4. **回歸**：原 63 用例保持全綠（第三輪併入廠商套件、移除 `TestDatasource` 後合計 87 用例全綠）；`ant build` + javadoc 正常。

---

## 5. 需要你拍板的決策點

| # | 決策 | 可選項 | 建議 |
| --- | --- | --- | --- |
| D1 | 完整度口徑 | A：5 家族全量（含 log4j2 384）；B：slf4j/commons/juli/log4j1 全量 + log4j2 按調用面；C：只 slf4j/commons/juli | **A**（生成器可把 log4j2 成本壓到可接受） |
| D2 | 現有非官方超集方法（slf4j 20 / commons 35 / log4j1 37 / log4j2 10） | 保留 / 清理 | **保留**（家族實現依賴它們；標註為擴展） |
| D3 | Marker 語義 | 忽略 / 消息前綴 / 支持過濾 | **消息前綴**（可追溯，成本低）；過濾留待後續 |
| D4 | fatal 映射 | ERROR / 新增 FATAL 級別 / MARK | **ERROR**（不動引擎級別體系）；如需區分用 MARK |
| D5 | Appender / Hierarchy / Repository | no-op / 簡化實現 / 完整 | **no-op + 簡化**（統一收口原則） |
| D6 | log4j2 Supplier 惰性 | 立即求值 / 真惰性 | **立即求值**（正確性優先，性能影響可忽略） |
| D7 | 是否也提供 MDC / ThreadContext / NDC | 提供 / 不提供 | 先**不提供**，按實際調用面再加 |

---

## 6. 工作量與里程碑

| 階段 | 內容 | 估時 |
| --- | --- | --- |
| M1 | slf4j 31 方法 + `Marker` 類型；commons 3 方法；juli 回歸 | 0.5 天 |
| M2 | log4j1：6 個類型 + 42 方法 + 級別映射 | 1.5 天 |
| M3 | log4j2：8 個類型 + 生成器 + 344 方法實現 | 2–3 天 |
| M4 | `LogCore/LevelMapper/ArgFormatter` 收口重構（可與 M1–M3 合併做） | 0.5 天 |
| M5 | 簽名校驗 gate 進 CI + 雙向編譯樣例 + TestReplacement 套件 | 1 天 |
| 合計 | | **約 5–6 人天** |

里程碑驗收：每個 M 完成後 `compare.py` 對應家族 `missing=0`，且 `ant build` + `TestAll` 全綠。

---

## 7. 風險

| 風險 | 說明 | 緩解 |
| --- | --- | --- |
| 類衝突 | classpath 若同時有官方 jar（slf4j-api / log4j-api…）→ duplicate class，行為不可預測 | 項目文檔明示「不可與官方 jar 共存」；CI 檢查 LibHome 不含官方 logger jar |
| 版本漂移 | slf4j 2.x、log4j2 2.x 後期版本的接口有增減 | 審計基準固定版本（1.7.36 / 2.20.0），升級時重跑 `compare.py` |
| 語義降級被誤用 | Marker 不進過濾、Appender no-op、Supplier 立即求值 | 在 README/QUALITY-REVIEW 明示降級清單；測試覆蓋降級行為 |
| javadoc 體量 | log4j2 384 方法 + 多類型，javadoc 生成耗時增加 | javadoc 已有 `-Djavadoc.skip` 開關 |
| 生成器可維護性 | 生成代碼不利於手改 | 生成器 + 模板入庫，生成結果不入庫（或入庫但標註 generated） |

---

## 8. 交付物預覽（實作階段）

```
src/com/killcoding/logreplaced/adapt/     LogCore / LevelMapper / ArgFormatter
src/org/slf4j/Marker.java                 新增
src/org/slf4j/Logger.java                 補 31
src/org/apache/commons/logging/Log.java   補 3
src/org/apache/log4j/                     Level / Priority / Appender / LoggerRepository /
                                          Hierarchy / LogManager / spi.LoggingEvent / spi.LoggerFactory / Category
src/org/apache/logging/log4j/            Level / Marker / message.Message / message.EntryMessage /
                                          util.Supplier / util.MessageSupplier / LogBuilder
build/tools/genLog4j2Logger.py            方法生成器
build/iface-audit/compare.py              CI 簽名校驗 gate（升級）
test/iface/refapi/                        5 份官方 API 全量調用樣例
test/TestReplacement.js                   運行期冒煙套件（待確認後註冊到 TestAll）
```

---

## 9. 下一步

拍板 D1–D7（尤其 **D1 完整度口徑**與 **D3 Marker 語義**），我即可按 M1 → M2 → M3 順序實作；M1 完成即可先交 slf4j/commons 的完整對齊與校驗報告。

---

## 10. 實施結果（2026-10-30，D1=A + 其餘建議默認）

### 10.1 各家族落地情形

| 家族 | 落地內容 | 官方方法覆蓋 |
| --- | --- | --- |
| SLF4J 1.7 | 新增 `Marker`（`ANY_MARKER`/`ANY_NON_NULL_MARKER`、add/remove/hasChildren/hasReferences/iterator/contains）、`IMarkerFactory`、`MarkerFactory`（內建 `SimpleMarkerFactory`/`SimpleMarker` 註冊表）；`Logger` 保留 20 個 JsPlus 擴充並補齊 31 個官方方法；`LoggerClass` 加 `markerPrefix()`、5 個 `isXEnabled(Marker)`、`getName()` | 61 / 61 |
| Commons Logging 1.2 | `Log` 補 `fatal(Object)`、`fatal(Object,Throwable)`、`isFatalEnabled()`；`LoggerClass` 三方法實現（fatal → error） | 18 / 18 |
| JULI 9.0 | 未改動（原即 18/18） | 18 / 18 |
| Log4j 1.2 | 新增 `Priority`、`Level`、`Appender`、`Layout`、`Category`、`Hierarchy`、`LogManager` 與 `spi`（`OptionHandler`/`Filter`/`ErrorHandler`/`LoggerFactory`/`ThrowableInformation`/`LocationInfo`/`AppenderAttachable`/`HierarchyEventListener`/`LoggerRepository`/`RepositorySelector`/`LoggingEvent`）；`Logger` 改為 `extends Category`（官方層級）；`Category.log(Priority,...)` 按 `toInt()` 映射引擎級別，`l7dlog` 以 `ResourceBundle` 展開 `{i}` | 55 / 55（`Logger` 7 + `Category` 48） |
| Log4j 2.20 | `build/tools/genLog4j2.py` 由官方 `javap` 生成 `Logger` 384 方法與 `LoggerClass` 實現；新增 `spi/StandardLevel`、`Level`（enum）、`Marker`、`MarkerManager`、`util/Supplier`、`util/MessageSupplier`、`message/Message`、`FlowMessage`、`EntryMessage`、`ExitMessage`、`MessageFactory`、`FlowMessageFactory`、`LogBuilder`（含 `NOOP` 與 `NoopLogBuilder`）；`LogManager` 提供 12 個 `getLogger`/`getFormatterLogger` 形態 + `exists` + `shutdown`×3 | 384 / 384 |

### 10.2 閘門實測

```
python build/iface-audit/compare.py         ->  0 missing / 五家族
     SLF4J   official 61  / ours 81   (61 官方 + 20 擴充)
     Commons official 18  / ours 84
     JULI    official 18  / ours 18
     Log4j1  official 55  / ours 122
     Log4j2  official 384 / ours 424

test\iface\refapi\CompileApiSample.bat      ->  BOTH COMPILES OK
     同一份 ApiUsageSample.java：官方 5 jar 編譯通過；本層 jar 編譯通過

test\TestFacades.bat                        ->  29 passed / 0 failed, exit 0
     五個廠商套件（TestSlf4j 7 / TestCommonsLogging 5 / TestJuli 4 / TestLog4j 6 / TestLog4j2 7）
     每個門面呼叫實際落入 logs/app/20261030.log

test\TestAll.bat                            ->  87 passed / 0 failed, exit 0（10 套件）
ant build                                   ->  jsplus-logreplaced-6.1.28.jar + docs/apis/6.1.28.jar
```

### 10.3 與方案的三處偏離（如實記錄）

1. **M4 統一出口（`LogCore`/`LevelMapper`/`ArgFormatter`）未實施**：各家族仍各自持有映射邏輯（`slf4j.LoggerClass` 為核心，commons/log4j1/log4j2 繼承它，JULI 仍是獨立 `LogCustom`）。原因：優先完成介面全量與可驗證性，重構會動到已全綠的測試路徑（現為 87 用例）；此項保留為 P2（QUALITY-REVIEW #6）。
2. **`org.apache.logging.log4j.LogManager` 未提供 `getContext()`/`setFactory()`/`getFactory()`**：本層沒有可插拔的 `LoggerContext`，補這些入口需再造 `spi.LoggerContext` + `LoggerContextFactory` 類型體系；目前口徑是「調用面齊備，context 管理屬實作層不提供」。
3. **log4j2 `Level` 以 enum 實作**（官方為 final class）：保留 `values()/valueOf()/name()`；`Level.valueOf(Class,String)`、`readResolve()` 等 enum 自動形態與官方不逐方法一致，閘門未把 `Level` 納入方法級比對（只比對 `Logger` 的 384 與各家族入口類）。

### 10.4 維護入口

- log4j2 的 `Logger`/`LoggerClass` 為生成檔：改 **`build/tools/genLog4j2.py`** 後執行 `python build/tools/genLog4j2.py` 重新生成；官方簽名來源 `build/iface-audit/log4j2_Logger_official.txt`。
- 接口閘門的比對類清單在 `build/iface-audit/compare.py` 的 `FAMILIES`（log4j1 需同時比對 `Logger` 與 `Category`，log4j2 以 `Logger` 介面為準）。
- `/build` 被 `.gitignore` 排除，生成器與審計腳本目前不在版本控制內（列為 QUALITY-REVIEW P2#10）。

### 10.5 測試按日誌廠商拆分（同日第二輪，2026-10-30）

原 `test/TestReplacement.js`（五家族塞在一個檔案的冒煙）已被拆分並刪除，改為**每個廠商一個可獨立執行的套件**，由 `test/TestFacades.bat` / `.sh` 逐個起獨立主控台執行、聚合退出碼：

| 套件 | 用例 | 斷言的實際語義 |
| --- | --- | --- |
| `TestSlf4j.js` | 7 | `getName`、級別探測、25 個 Marker 重載 + 5 個 `isXEnabled(Marker)`、Marker 前綴落盤、`{}` 展開、Throwable 落盤、`trace()` → `MARK` 行 |
| `TestCommonsLogging.js` | 5 | `getLog`、`fatal` → `ERROR`、`fatal(Object,Throwable)` 保留原因、`isFatalEnabled()` == `isErrorEnabled()`、五級別可用且 `info` 落盤 |
| `TestJuli.js` | 4 | `getLog` → `LogCustom`、18 方法全可呼叫、`info` 落盤、`fatal` → `ERROR` |
| `TestLog4j.js` | 6 | `getName`、`Level=INFO` 下 debug 被過濾、`setLevel(DEBUG)` 後 debug 落盤、`fatal` → `ERROR`、`log(Priority,...)` 映射、`l7dlog`/appender 安全 no-op |
| `TestLog4j2.js` | 7 | `getName`、`atInfo().withMarker().withThrowable().log()` 同時帶標記與異常、`fatal` → `ERROR`、`printf` 展開、`trace` → `MARK`、`throwing(T)` 回傳同一實例、`Level` 序／範圍／`forName` |

**拆分過程中由新斷言暴露並修正的一處實現缺陷**：log4j1 `Category.setLevel(Level)` 原本只寫入欄位、未下推引擎，導致 `setLevel(Level.DEBUG)` 後 `debug()` 仍被引擎 INFO 過濾。已修為 `setLevel`/`setPriority` 一併呼叫 `engine().setLevel(...)`，並把引擎缺少的級別就近折疊（`FATAL`→`ERROR`、`TRACE`/`ALL`→`DEBUG`、`OFF`→`ERROR`）。

**兩處斷言口徑修正（非實現缺陷，已如實記錄）**：
1. 引擎把 Throwable 寫在事件行**下方**（`**msg**` 之後另起堆疊行），故異常類斷言改為比對「事件區塊」（`appLogEventBlock`），而非單行；
2. 內建 GraalVM **缺 TRegex**，JS 正則字面量會拋 `No language for id regex found`；所有落盤讀取改用 `com.killcoding.jsplus.JsString`（Java `indexOf`/`split`/`startsWith`）。
3. `printf` 用例曾用固定字串斷言而命中上一輪的歷史行（假通過），已改為帶 `System.nanoTime()` 的唯一 tag。

### 10.6 統一彙總與無關套件清理（同日第三～五輪，2026-10-30）

**問題**：拆分後 `docs/latest/report/TestAll-Report.html` 仍只列 6 個套件／63 用例，與實際測試格局對不上（廠商套件只由 `TestFacades` 驅動）。已做三件事把口徑對齊：

1. **廠商套件併入 `TestAll.js`**：`suites` 陣列加入 5 個廠商套件，統一報告涵蓋全部套件。為避免套件被 `load()` 時 `System.exit` 殺掉彙總執行，退出條件由 `JSPLUS_TEST_EXIT` 改為專用的 `JSPLUS_FACADE_EXIT`（只由 `TestFacades.bat` / `.sh` 與 `build/run-suite.bat` 設定）。結果：`TestAll` = 彙總計數（不含退出碼），`TestFacades` = 逐廠商 CI 閘門。
2. **移除假陽性的 `test/TestDatasource.js`**（及其 `docs/latest/report/TestDatasource-Report.*`）：其 5 個用例中 4 個只驗 `com.killcoding.datasource.Clock` getter，第 5 個是恆 `ok: true` 的 scope note，且註解宣稱 JDBC 相關類「none is bundled in LibHome/Core/default/lib」——實際 `LibHome/h2/default/h2-2.4.240.jar` 存在、`LibHome/Core` 目錄不存在。該套件與本替換層無關，又製造「JDBC 零覆蓋卻已界定範圍」的假象。
3. **移除與本層無關的引擎套件**：`test/TestCache.js`（`com.killcoding.cache`，12 例）、`test/TestFile.js`（`com.killcoding.file`，13 例）、`test/TestTool.js`（`com.killcoding.tool`，18 例）及其報告；保留 `TestConsole`（其餘套件的執行環境前置：`JS_PLUS_PATH` / `load` / `print` / Java 互操作）與 `TestLog`（`com.killcoding.log` 引擎日誌核心，與本層直接相關）。

### 10.7 報告佈局、歸檔與負載套件（同日晚間，2026-10-30）

按 JsPlus-Core 的工程標準，把測試產物與執行方式對齊：

1. **報告目錄**：由 `test_reports/` 遷至 **`docs/latest/report/`**（Core `TestBase.js` 的同名約定），`TestBase.js` / `TestAll.js`、各套件頭部註解、README、CI 產物路徑與 `.gitignore` 同步。
2. **報告歸檔**：`TestAll.bat` / `.sh` 每輪把 `docs/latest/report/` 打包成 **`docs/reports/<version>.jar`**（版本取自 `jsplus-logreplaced-<version>.jar` 檔名），並保留最新 10 個；打包失敗僅 WARN、不改退出碼——archive 由 TestAll 自持，ant 只管 `docs/apis`（與 Core 一致）。
3. **每輪先清理**：`Step 0` 刪除 `JsPlus/.tmp`（`evalJavaSource` / `evalJavaFile` 暫存，同 Core Step 0b）與 `logs/*`，讓每輪從空日誌開始，杜絕歷史行誤判通過（實測 620687 bytes 舊日誌被清空，本輪自寫 39817 bytes）。
4. **負載套件 `test/LoadTest.js`（6 例）**：突發寫入吞吐（10000 行，呼叫方不得被 I/O 阻塞，實測約 5000 行/秒）、異步 flush 後零丟行、8 KB 訊息完整性、200 個 Throwable、5 個 logger × 1000 行交錯、負載下的級別閘門。註冊為 `TestAll.js` 的**最後**一個套件；獨立入口 `test\LoadTest.bat` / `.sh` 以 `JSPLUS_LOADTEST_EXIT=1` 自持退出碼。
   - **限制（實測）**：此 GraalVM 版本禁止跨執行緒進入 JS context（`Multi threaded access requested ... not allowed for language(s) js`），且無法從 JS 實例化 Java 介面（`new Runnable({run: fn})` → `Message not supported`），故負載只能單執行緒驅動，但這仍直擊引擎真正的瓶頸——異步寫入佇列。

**最終口徑（實跑，6.1.28 jar）**：

| 入口 | 套件 | 用例 | 退出碼 |
| --- | --- | --- | --- |
| `test\TestAll.bat` | 8（TestConsole + TestLog + 5 廠商 + LoadTest）＋ 配置自檢 | 50 passed / 0 failed | 0 |
| `test\TestFacades.bat` | 5（廠商） | 29 passed / 0 failed | 0（任一失敗即 1） |
| `test\LoadTest.bat` | 1（負載，6 例） | 6 passed / 0 failed | 0（任一失敗即 1） |
