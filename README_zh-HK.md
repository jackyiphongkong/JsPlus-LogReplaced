# JsPlus-LogReplaced

JsPlus-LogReplaced 是 **JsPlus 的日誌替換層**——為五種日誌門面（`org.apache.commons.logging`、`org.apache.juli.logging`、`org.apache.log4j`、`org.apache.logging.log4j`、`org.slf4j`）提供相容的替代實作，讓框架程式碼照舊呼叫熟悉的 API，同時把每一次日誌呼叫轉入 JsPlus 日誌引擎（`com.killcoding.log.Logger` / `LoggerFactory`，由 `jsplus-core-<version>.jar` 提供）。

不需第三方日誌 jar，也不需日誌設定檔：依賴 Apache Commons Logging / SLF4J / JULI 的容器或框架，會透明地寫入 JsPlus 應用日誌（`logs/app/yyyyMMdd.log`，由 `JsPlus/config/Logger.properties` 控制）。

## 快速開始（三步）

工具鏈隨專案內建（免安裝 Java / Ant，無需 `JAVA_HOME`）：

| 步驟 | 指令 | 產物 |
| --- | --- | --- |
| 1. 建置 | `ant.bat`（Linux/macOS：`ant.sh`） | `LibHome\jsplus\default\jsplus-logreplaced-<version>.jar` |
| 2. 測試 | `test\TestAll.bat`（Linux/macOS：`test\TestAll.sh`） | `docs/latest/report\*`（HTML / MD / CI 摘要），每次執行歸檔至 `docs/reports\<version>.jar` |
| 3. 精簡執行環境 | `packJdk.bat`（Linux/macOS：`packJdk.sh`） | `Java\runtime`（jlink 裁減） |

基線：`ant build` → BUILD SUCCESSFUL；`TestAll.bat` → **50 passed / 0 failed，exit 0**（8 個 JS 套件：2 個引擎側——TestConsole（測試環境前置）與 TestLog（`com.killcoding.log`）——加 5 個替換層廠商套件 TestSlf4j / TestCommonsLogging / TestJuli / TestLog4j / TestLog4j2，再加持續負載套件 LoadTest）。`test\TestFacades.bat` 單獨跑 5 個廠商套件（**29/29，exit 0**），作為專用的門面閘門；`test\LoadTest.bat` 單獨跑負載套件（**6/6，exit 0**）。

## 工程標準（對齊 JsPlus-Core）

| 項目 | JsPlus-LogReplaced 現狀 |
| --- | --- |
| `build.xml` | 目標結構與 JsPlus-Core 相同：`version-init` → `version-flags` → bump 各分支 → `write-changelog` → `changelog-diff` → `replace-version` → `clear` → `compile` → `package` → `build`（= `package,javadoc`） |
| 版本機制 | `version.properties`（`jr.major` / `jr.minor` / `jr.patch`）每次 `ant build` 自動 **patch +1**（999 進位、semver）；bump 前的值以 `jr.old.` 前綴重載，確保 jar 名與 `version.properties` 永不漂移；jar = `jsplus-logreplaced-${jr.version}.jar`；打包前刪除舊的 `jsplus-logreplaced-*.jar` |
| 版本戳記 | `Version.VERSION` 使用佔位符 `${BuildVersion.VERSION}(${BuildVersion.BUILD_DATE})`，編譯前替換、編譯後還原 |
| 變更日誌 | 每次建置向 **兩個**檔案各追加一條（UTF-8）：`changes.log`（英）與 `changes_zh-HK.log`（繁中），標題 `## <version> (<builddate> <buildtime>)`；各語言優先級：`-Dchange.en` / `-Dchange.zh` → `build\changes-pending-en.txt` / `build\changes-pending-zh-HK.txt`（消費後刪除）→ 預設 `- build: automatic version bump` / `- build: 自動版本遞增` |
| 原始碼差異 | 每次建置用 `build/tools/DiffScanner.java` 對比 `src` 與鏡像（`jr.diffroot` = `D:\GitHubRepos\JsPlus-LogReplaced\src`，不存在則靜默略過），並向兩份日誌寫入 `- found diffed: <path> (A\|M\|D)` |
| Javadoc | 每次建置重新生成 **`docs/latest/api/`**（視窗標題帶版本、來源連結、`-Xdoclint:none`），並歸檔為 **`docs/apis/<version>.jar`**；可用 `-Djavadoc.skip=true` 略過 |
| CI | `.github/workflows/ci.yml`：`ant build` → jar 與 `version.properties` 一致性檢查 → `TestAll.sh` → CI 摘要 → 上傳產物（測試報告、建置 jar、javadoc 站與歸檔 jar） |

## 目錄結構

| 路徑 | 用途 |
| --- | --- |
| `src/` | 替換層原始碼——48 個 `.java`（約 4,665 行）：五個門面包，以及它們所需的官方伴生型別（`Marker`、`Level`/`Priority`、`LogBuilder`、`Message`、`Appender`、`spi` 介面） |
| `src/com/killcoding/logreplaced/Version.java` | 建置版本戳記（`Version.VERSION`） |
| `src/org/...` | 五種日誌門面及其伴生型別（見「替換層覆蓋範圍」） |
| `LibHome/` | 執行期依賴（`graalvm\default` 腳本引擎、`h2\default` JDBC 驅動、`nashorn\default`），以及 `jsplus\default`（放 `jsplus-core-<version>.jar` 引擎與建置出的 `jsplus-logreplaced-<version>.jar`） |
| `Java/default` | 內建 JDK（Oracle GraalVM 25.0.3） |
| `Java/runtime` | `packJdk` 生成的精簡執行環境（可刪除重建） |
| `ant/default` | 內建 Apache Ant |
| `JsPlus/bin` | 主控台啟動器：`StartLogRp.bat` / `.sh`（classpath 自動由 `LibHome` 組出，**含 JsPlus-Core 引擎**）、`BG_StartLogRp`、`Stop`、`StopLogRp`；H2 啟動器：`StartH2`、`BG_StartH2`、`StopH2`、`StartH2Console`（Shell／`init` 重建）；`StopAll` 一次停掉本檢出目錄下全部 JsPlus JVM |
| `JsPlus/script/main.js` | `StartLogRp` 載入的最小啟動任務 |
| `JsPlus/script/StartH2.js` + `H2Server.js` | H2 TCP 伺服器啟動（start / stop / shutdown 三個子主控台）——與 JsPlus-Core 同型 |
| `JsPlus/config/Logger.properties` | 執行期日誌設定錨點，主控台啟動時載入 |
| `JsPlus/config/Appenv.properties` | `H2_PORT=${JsPlus_H2_PORT:3308}`、`H2_BASE_DIR=${JS_PLUS_PATH}../Database/` |
| `JsPlus/config/DataSource.properties` | H2 資料源錨點：`jdbc:h2:tcp://localhost:${H2_PORT}/logdb`（`sample_user` / `sample_password`）＋連線池上限 |
| `Database/` | H2 資料目錄（`H2_BASE_DIR`），由 `StartH2` 建立，存放 `logdb.mv.db`，已列入 `.gitignore` |
| `JsPlus/config/GhostDataSource.properties` | ghost 主控台資料源（`GhostTimer=500ms`、H2 連線）。保留 Core 同構，但本項目 schema 不含 `ghost_remote_command` 佇列，ghost 主控台會空轉重試、H2 服務本身不受影響 |
| `dbscript/change.sql` | `logdb` 的可重複執行 DDL：`sample_user` ＋ `app_logger_config`（逐 host 遠程覆蓋：`level` / `class_name_enable_regex` / `class_name_disable_regex`）＋ `app_logger_msg`（日誌記錄） |
| `dbscript/fallback.sql` | 重置腳本（只 drop 測試表，`ghost_remote_command` 永不 drop）——`StartH2Console init` 的第一段 |
| `JsPlus/.tmp` | 主控台工作目錄 |
| `test/` | `TestBase.js` 框架、`TestAll.js` 入口、`TestAll.bat` / `TestAll.sh`、2 個引擎側套件（`TestConsole.js` / `TestLog.js`）、**每個日誌廠商一個套件**（`TestSlf4j.js` / `TestCommonsLogging.js` / `TestJuli.js` / `TestLog4j.js` / `TestLog4j2.js`，同時註冊進 `TestAll.js`，並可由 `TestFacades.bat` / `TestFacades.sh` 單獨執行）、負載套件（`LoadTest.js` + `LoadTest.bat` / `LoadTest.sh`），另有 `iface/refapi/`（雙向編譯樣例） |
| `docs/latest/report/` | 最近一次執行的報告（`TestAll-Report.html` / `.md`、`TestAll-CI-Summary.md`、各套件報告） |
| `docs/reports/<version>.jar` | 每次執行的測試報告歸檔 jar（一版本一 jar，保留最新 10 個） |
| `docs/latest/api/` | 最新版本的 javadoc 站——開啟 `docs/latest/api/index.html` |
| `docs/apis/<version>.jar` | 每個版本歸檔一份 javadoc jar |
| `build/classes/` | 中間編譯輸出（僅建置期） |
| `build/tools/DiffScanner.java` | `changelog-diff` 使用的原始碼差異工具 |
| `version.properties` | 建置版本狀態（自動遞增） |
| `changes.log` / `changes_zh-HK.log` | 中英雙語建置變更日誌（英 / 繁中） |

## 替換層覆蓋範圍

所有家族最終都指向同一個引擎——經 `org.slf4j.LoggerClass`（JULI 則為專用的 `LogCustom`）轉入 `com.killcoding.log.Logger`。**每個家族的官方 interface 均已完整實作**，並逐方法對照官方 jar 驗證（見「介面完整性閘門」）：

| 門面 | 已對齊的官方 API | 本層宣告方法數 | 入口 | 實作 |
| --- | --- | --- | --- | --- |
| SLF4J 1.7 | `org.slf4j.Logger`（61）+ `Marker` / `IMarkerFactory` / `MarkerFactory` | 81 = 61 官方 + 20 個 JsPlus 擴充 | `org.slf4j.LoggerFactory.getLogger(Class/String)` | `org.slf4j.LoggerClass` |
| Apache Commons Logging 1.2 | `org.apache.commons.logging.Log`（18） | 84（18 全覆蓋，其餘為繼承） | `org.apache.commons.logging.LogFactory.getLog(Class/String)` | `org.apache.commons.logging.LoggerClass` |
| Apache Log4j 1.2 | `Logger`（7）+ `Category`（48）= 55，另有 `Priority` / `Level` / `Appender` / `Layout` / `spi.*` | 122 | `org.apache.log4j.Logger.getLogger(...)` / `new Logger(...)` | `org.apache.log4j.Category`（基底）+ `Logger` |
| Apache Log4j 2.20 | `Logger`（384）+ `Level` / `Marker` / `LogBuilder` / `LogManager` / `message.*` / `util.Supplier` | 424 | `org.apache.logging.log4j.LogManager.getLogger(Class/String)` | `org.apache.logging.log4j.LoggerClass`（生成式） |
| Apache JULI 9.0 | `org.apache.juli.logging.Log`（18） | 18 | `org.apache.juli.logging.LogFactory.getLog(Class/String)` | `org.apache.juli.logging.LogCustom` |

`LogFactory` / `LoggerFactory` / `LogManager` 是唯一的建構入口，各自回傳以類別或名稱為單位的 logger；`release(...)` 方法為空實作（無資源可釋放）。

### 行為映射（刻意設計，已記錄）

- **Marker** → 訊息前置詞 `"<markerName> - " + message`，不做過濾（`Marker` 仍保留 `add/remove/hasChildren/iterator` 語義）。
- **`fatal`** → 引擎 `error`（引擎無 FATAL 級別）；log4j2 `FATAL` 同理。
- **`trace`**（log4j2）與 **`TRACE`**（log4j1）→ 引擎 `mark`。
- **log4j1 Appender / Hierarchy** 僅在 logger 上記錄、從不派發（`LoggingEvent#callAppenders` 為空操作）；級別由 `Priority.toInt()` 決定。
- **log4j1 `setLevel` / `setPriority`** 會下推到引擎（`Category.engine().setLevel(...)`），因此 `setLevel(Level.DEBUG)` 確實能讓 `debug()` 落盤；引擎缺少的兩個級別就近折疊：`FATAL` → `ERROR`、`TRACE` → `DEBUG`、`ALL` → `DEBUG`、`OFF` → `ERROR`。
- **log4j2 `Supplier` / `MessageSupplier`** 一律即時求值，確保呼叫不被靜默丟棄。
- **log4j2 `Level`** 以 enum 實作（保留 `values()`/`valueOf()/name()` 語義）；`LogManager.getContext()/setFactory()/getFactory()` 刻意不提供——本層沒有可插拔的 LoggerContext。
- log4j2 `Logger` 與其實作（384 個方法）由 `build/tools/genLog4j2.py` 依官方 `javap` 簽名**生成**；維護請改生成器，不要手改生成檔（`src/org/apache/logging/log4j/Logger.java`、`LoggerClass.java`）。

### 介面完整性閘門

三項本地可執行的檢查：

| 檢查 | 指令 | 最近結果 |
| --- | --- | --- |
| 逐方法比對五個官方 API jar | `python build/iface-audit/compare.py` | 五家族全部 0 缺失（61/81、18/84、18/18、55/122、384/424） |
| 同一份官方 API 樣例雙向編譯 | `test\iface\refapi\CompileApiSample.bat` | `ApiUsageSample.java` 對官方 jar 與對 `jsplus-logreplaced` 均編譯通過 |
| 五門面執行期冒煙 | `test\TestFacades.bat` | 5 個廠商套件共 29/29（7 + 5 + 4 + 6 + 7），exit 0——每個門面呼叫都落到 `logs/app/<yyyyMMdd>.log` |

對照 jar 置於 `build/iface-audit/ref/`（slf4j-api 1.7.36、commons-logging 1.2、tomcat-juli 9.0.85、log4j 1.2.17、log4j-api 2.20.0）；方案與決策記錄見 `INTERFACE-PLAN.md`。替換層不可與官方日誌 jar 同 classpath 共存（套件同名）。

## 測試套件

### 框架特徵：用 JavaScript 驗證 Java

每個套件都是在內建 JsPlus 主控台引擎中執行的 JavaScript 檔——與生產環境嵌入的是同一個引擎。引擎側套件（`TestConsole` / `TestLog`）分別覆蓋測試框架自身所站的主控台環境與 `com.killcoding.log` 核心；廠商套件直接呼叫真實的替換類（`LogFactory.getLog(...)` / `LoggerFactory.getLogger(...)` / `LogManager.getLogger(...)`），對實際結果斷言，兩類都輸出 GitHub 風格 HTML/MD 報告。沒有 JUnit，也沒有獨立測試框架：`test/*.js` + `test/TestBase.js` 就是全部。

### 如何執行

| 需求 | 指令 |
| --- | --- |
| 全量：8 個套件 + 彙總報告 | `test\TestAll.bat`（Linux/macOS：`test\TestAll.sh`） |
| 只跑 5 個廠商套件（退出碼可作 CI 閘門） | `test\TestFacades.bat`（Linux/macOS：`test\TestFacades.sh`） |
| 只跑負載套件（6 個負載用例，退出碼可作 CI 閘門） | `test\LoadTest.bat`（Linux/macOS：`test\LoadTest.sh`） |
| 單一廠商套件 | `TestFacades.bat` 內含五個獨立主控台；只跑一個時沿用同一啟動行，改 `-Task test/TestLog4j.js` 等 |
| 雙向編譯合規樣例 | `test\iface\refapi\CompileApiSample.bat` |
| 任一套件 | 從 `TestAll.bat` 複製 `-Task test/TestXxx.js` 那一行；每個套件都是普通主控台任務，可獨立執行 |

報告寫入 `docs/latest/report\`（HTML + MD + CI 摘要），並於每次執行後歸檔至 `docs/reports\<version>.jar`——版本取自專案 jar 檔名，保留最新 10 個，歸檔失敗僅 WARN、不影響退出碼（與 JsPlus-Core 同一佈局）。

任何套件開跑前，`TestAll.bat` / `TestAll.sh` 會先清理 `JsPlus\.tmp`（`evalJavaSource` / `evalJavaFile` 的暫存樹，同 JsPlus-Core Step 0b）與 `logs\*`（引擎輸出樹 `logs\app\<yyyyMMdd>.log`）。因此每輪都從空日誌開始，套件只會看到本輪寫入的行——上一輪遺留的行不可能再造成誤判通過。

### 套件

| 套件 | 用例數 | 關注點 |
| --- | --- | --- |
| TestConsole | 9 | 其他套件依賴的主控台環境：`JS_PLUS_PATH`、全域 `Console`、`evalScript` / `load` / `print`、Java 互操作（`Java.type` / `ArrayList` / `HashMap`）、`DiskFile` 讀寫往返 |
| TestLog | 5 | `com.killcoding.log`：`LoggerFactory.getLogger`、由 `Logger.properties` 取得的級別狀態、info/warn/error/mark/debug 不拋例外、`FileOut` 寫入 `logs/app/<yyyyMMdd>.log`、`setLevel("DEBUG")` 切換 `isDebugEnabled` |
| LoadTest | 6 | 對 `com.killcoding.log` 的持續負載：10000 行突發吞吐（呼叫方不得被 I/O 阻塞）、異步 flush 後零丟行、8 KB 訊息完整性、200 個異常、5 個 logger × 1000 行交錯、負載下的級別過濾。排在**最後**執行，其約 16500 行不會干擾上游套件 |

> 已移除（它們測的是本替換層不發佈的引擎套件）：`TestDatasource.js`（假陽性——範圍註記恆為通過、只驗 Clock getter）、`TestCache.js`（`com.killcoding.cache`）、`TestFile.js`（`com.killcoding.file`）、`TestTool.js`（`com.killcoding.tool`）。對應報告已一併刪除。

**廠商套件**——每個日誌家族一個檔案，各自可獨立執行，並且對**真實落盤**（`logs/app/<yyyyMMdd>.log`）斷言，而不是只驗「不拋例外」。它們像引擎側套件一樣註冊進 `TestAll.js`（彙總報告因此涵蓋全部 8 個套件），同時 `TestFacades.bat` / `.sh` 也能單獨驅動它們並取得逐廠商退出碼：

| 套件 | 用例數 | 關注點 |
| --- | --- | --- |
| TestSlf4j | 7 | `org.slf4j`：`getName()`、`isXEnabled()` 與設定級別一致、25 個 Marker 重載 + 5 個 `isXEnabled(Marker)`、Marker → `"<marker> - "` 前綴、`{}` 展開、Throwable 落盤、`trace()` → `MARK` |
| TestCommonsLogging | 5 | `org.apache.commons.logging`：`getLog()`、`fatal()` → `ERROR`、`fatal(Object, Throwable)` 保留原因、`isFatalEnabled()` == `isErrorEnabled()`、五個共用級別 |
| TestJuli | 4 | `org.apache.juli.logging`：`getLog()` → `LogCustom`、18 個官方方法全部可呼叫、`info()` 落盤、`fatal()` → `ERROR` |
| TestLog4j | 6 | `org.apache.log4j`（1.2）：`getName()`、`Level=INFO` 下 debug 被過濾、`setLevel(DEBUG)` 後 debug 落盤（並還原級別）、`fatal()` → `ERROR`、`log(Priority, ...)` 映射級別、`l7dlog` / appender 僅記錄為安全 no-op |
| TestLog4j2 | 7 | `org.apache.logging.log4j`（2.20）：`getName()`、`atInfo().withMarker().withThrowable().log()` 同時帶標記與異常、`fatal()` → `ERROR`、`printf()` 展開、`trace()` → `MARK`、`throwing(T)` 回傳同一實例、`Level` 序／範圍／`forName` 與 `isXEnabled(Marker)` 一致 |

### 強制測試約定

- 執行期設定錨點 `JsPlus/config/Logger.properties` 必須存在；`TestAll.js` 先做設定自檢並記入彙總報告；
- 套件 **絕不呼叫 `System.exit()`**——由 `TestAll.js` 擁有 JVM 結束權（`JSPLUS_TEST_EXIT=1`，由 `TestAll.bat` / `TestAll.sh` 設定），讓互動式 `Console.eval(...)` 執行完不會殺掉主控台；
- 每個套件呼叫 `beginTest()` 並自行統計計數；`TestAll.js` 在每個套件結束後立即擷取，彙總為 `TestAll-Report.md` / `.html` + `TestAll-CI-Summary.md`；
- **例外——廠商套件**（`TestSlf4j` … `TestLog4j2`）：它們在 `TestAll.js` 裡是普通套件（該處不退出），同時也可由 `TestFacades.bat` 單獨驅動，退出權只在 `JSPLUS_FACADE_EXIT=1` 時才動用 `System.exit(failCount > 0 ? 1 : 0)`。因此：`TestAll` 執行時彙總它們的計數、`TestFacades` 執行時取得可作 CI 閘門的退出碼、互動式 `Console.eval(...)` 兩者都不會發生；
- **`LoadTest` 同型**，用獨立開關 `JSPLUS_LOADTEST_EXIT=1`（由 `test\LoadTest.bat` / `.sh` 設定）：單獨執行時可作 CI 閘門，在 `TestAll` 內則不退出；
- **負載套件只能單執行緒**：此 GraalVM 版本不允許從其他執行緒進入 JS context（`Multi threaded access requested ... not allowed for language(s) js`），也無法從 JS 實例化 Java 介面（`new Runnable({run: fn})` → `Message not supported`）。因此負載在 JS 執行緒上直擊引擎真正的瓶頸——異步寫入佇列：實測約 5000 行/秒的入列速率、10000 行零丟行；
- 引擎把 Throwable 寫在事件行**下方**（`**message** ` 之後接堆疊），所以關於異常的斷言必須看整個事件區塊（見 `TestLog4j2.js` 的 `appLogEventBlock`）——只查單行會漏掉；
- **落盤文字一律走 `JsString`**：所有套件以 `com.killcoding.jsplus.JsString`（`JsString.valueOf(file.readAllString("UTF-8"))` + Java `indexOf` / `split` / `startsWith`）讀取日誌。內建 GraalVM **沒有 TRegex**，JS 正則字面量會報 `No language for id regex found`；JsString 把正則運算留在 `java.util.regex`，也讓 `java.lang.String` 的方法能跨語言邊界呼叫；
- 測試夾具放在 `test/.core-test-tmp/`，執行結束後清除；
- 拋出例外的套件記為失敗套件，不中斷其餘套件。

### 如何新增套件

1. 照 `test/TestLog.js` 的形態複製。
2. 載入框架——`load(System.getProperty("JS_PLUS_PATH") + "../test/TestBase.js")`——並設定 `REPORT_TITLE` / `REPORT_NAME`。
3. 呼叫 `beginTest()` 重置計數，然後每個斷言一個 `check({caseId, name, ok, detail, objective, objectiveZh, command})`。
4. 以 `writeReport()` 收尾；**絕不呼叫 `System.exit()`**（廠商套件屬例外：只在 `JSPLUS_FACADE_EXIT=1` 時退出，該變數由 `TestFacades.bat` 設定；`LoadTest` 同理，開關為 `JSPLUS_LOADTEST_EXIT=1`）。
5. 在 `test/TestAll.js` 註冊套件（`suites` 陣列 **與** 單行套件清單兩處）；廠商套件另外還要在 `TestFacades.bat` / `.sh` 的清單內新增一個名稱，以保留獨立閘門。

## 建置與版本

- `version.properties`（`jr.major` / `jr.minor` / `jr.patch`）每次 `ant build` 自動 **patch +1**（999 進位、semver）。
- 版本經佔位符 `${BuildVersion.VERSION}(${BuildVersion.BUILD_DATE})` 寫入 `Version.VERSION`，編譯前替換、編譯後還原。
- 每次建置刪除舊的 `jsplus-logreplaced-*.jar`，輸出帶版本的 jar 至 `LibHome/jsplus/default/`。
- 每次建置向 `changes.log`（英）與 `changes_zh-HK.log`（繁中）各追加一條歷史記錄；條目優先級見上表。
- 每次建置重新生成 API 站至 `docs/latest/api/`，並歸檔為 `docs/apis/<version>.jar`。
- 虛擬記憶體過小的主機上，fork 出的 javac 可能無法掛接 JVMCI：請匯出 `ANT_OPTS=-Xms16m -Xmx128m -XX:+UseSerialGC -XX:-UseJVMCICompiler`，並在 `JAVA_TOOL_OPTIONS` 設相同旗標（fork 會繼承）。

## 環境變數覆寫（命名慣例與伺服器版一致）

| 變數 | 預設 | 用途 |
| --- | --- | --- |
| `JsPlus_JavaDir_JsPlus-LogReplaced` | `default` | `Java\` 下的 JDK 目錄名 |
| `JsPlus_JvmParams_JsPlus-LogReplaced` | `-Xms256m -Xmx1024m -Xss256k` | JVM 參數 |
| `SCRIPT_ENGINE`（腳本參數 %1） | `graalvm.js` | 腳本引擎 |

## 發布輔助腳本

`release.bat`（Linux/macOS：`release.sh`）把最新建置的 `jsplus-logreplaced-*.jar` 複製到掃描根目錄（預設為本專案的上一層資料夾，可用 `JS_PLUS_RELEASE_ROOT` 覆寫）下每個含有 `LibHome\jsplus\default` 的兄弟專案。該處原有的 `jsplus-logreplaced-*.jar` 改名為 `*.jar.bak`（不刪除）；其他 jar 不動；來源專案自身跳過。退出碼：`0` = 成功，`1` = 失敗。

## 執行

```bat
:: 建置 + 全量測試
ant.bat build
test\TestAll.bat
```

直接啟動主控台：使用內建啟動器 `JsPlus\bin\StartLogRp.bat`（Linux/macOS：`JsPlus/bin/StartLogRp.sh`）——它會自動從 `LibHome` 下所有 JAR（含 JsPlus-Core 引擎與本專案 jar）組出 classpath，並執行 `com.killcoding.jsplus.Main -Env script -ScriptEngine graalvm.js -Task script/main.js`。

## H2 資料庫（本地錨點）

替換層本身不需要資料庫，但執行環境錨點與 JsPlus-Core 一樣附帶本地 H2 TCP 伺服器，讓任何期待 `jdbc:h2:tcp://localhost:3308/logdb` 的程式都能接上。

| 需求 | 指令 |
| --- | --- |
| 啟動 H2（前景） | `JsPlus\bin\StartH2.bat`（Linux/macOS：`JsPlus/bin/StartH2.sh`） |
| 啟動 H2（背景） | `JsPlus\bin\BG_StartH2.bat`（Linux/macOS：`JsPlus/bin/BG_StartH2.sh`） |
| 停止 H2 | `JsPlus\bin\StopH2.bat`（Linux/macOS：`JsPlus/bin/StopH2.sh`） |
| 開 H2 Shell／重建 schema | `JsPlus\bin\StartH2Console.bat`（`init` → fallback.sql → change.sql；執行前先 `StopH2`，因為 init 直開資料檔） |
| 一次停掉本檢出的全部 JsPlus JVM | `JsPlus\bin\StopAll.bat`（Linux/macOS：`JsPlus/bin/StopAll.sh`） |

- 埠：`H2_PORT=${JsPlus_H2_PORT:3308}`——設定環境變數 `JsPlus_H2_PORT` 即可改埠；
- 資料檔：`H2_BASE_DIR=${JS_PLUS_PATH}../Database/`（`Database/logdb.mv.db`，已列入 `.gitignore`，由 `StartH2` 以 `-ifNotExists` 在首次連線時建立）；
- 連線：`jdbc:h2:tcp://localhost:3308/logdb`，帳號 `sample_user`、密碼 `sample_password`（見 `JsPlus/config/DataSource.properties`），驅動 `org.h2.Driver` 來自 `LibHome/h2/default/h2-2.4.240.jar`；
- `-GhostName H2Ghost` 讓伺服器 JVM 在任務結束後繼續存活——這個幽靈就是 `StopH2` 的匹配對象（`Stop.bat StartH2.bat` → 關鍵字 `StartH2`）。

### 日誌表（`app_logger_config` / `app_logger_msg`）

`dbscript/change.sql` 建立兩張表，讓每一行日誌都能像資料一樣查詢：

| 表 | 粒度 | 欄位（對應 `Logger.properties` / `MessageFormat`） |
| --- | --- | --- |
| `app_logger_config` | 每個 host 一行 —— `DatabaseOutConfigFrom` 讀的就是它 | `hostname`（`:hostname` 查詢鍵）、`level`、`class_name_enable_regex`、`class_name_disable_regex` —— 引擎 `LogRecord.writeLogBatchToDatabase` 只認這三個鍵 |
| `app_logger_msg` | 每個日誌事件一行 | `app_name`、`logger_name`（`:log_class_name`）、`log_level`（`:level`）、`log_time`（`:log_time_format`）、`hostname`、`thread_name`（`:log_thread_name`）、`thread_id`（`:log_thread_id`）、`message`（CLOB，`:message`）、`stack_trace`（CLOB，`:stack_trace`） |

兩張表都沿用項目慣例：`id CHAR(16) PRIMARY KEY` ＋ `created_at` / `updated_at` / `deleted_at` / `deleted`（軟刪除）。`message` 與 `stack_trace` 刻意用 `CLOB`——負載套件會寫 8000 字元訊息與多 Frame 堆疊。

Core 繼承下來的 `ghost_remote_command` 佇列**不在此建立**：本層職責是統一口徑的日誌路徑，不是遠程命令派發。`JsPlus/config/GhostDataSource.properties` 保留 Core 同構以備日後啟用，屆時 `-GhostName H2Ghost` 主控台會因缺表空轉重試（H2 服務本身不受影響）。
