# JsPlus-LogReplaced 專案品質評估報告

- 評估日期：2026-10-30（第二版；第一版 2026-10-29 針對 14 檔／470 行的初版替換層）
- 評估範圍：`D:\Working\JsPlus-LogReplaced`（日誌替換層）——構建機制、腳本、測試、CI、源碼、文件、版本控制
- 評估方式：靜態審查（結構盤點、`-Xlint:all` 嚴格編譯、源碼衛生掃描、腳本一致性核對）+ 構建/測試/接口比對實跑
- 樣本狀態：src 48 個 java（約 4,665 行），`jsplus-logreplaced-6.1.28.jar`（LibHome/jsplus/default），version.properties = 6.1.28

---

## 一、總評

**綜合評分：88 / 100** —— 五個日誌門面的**官方 interface 已全量實作並通過方法級閘門**（0 缺失），替換層從「能跑」升級為「簽名與類型可對外宣稱相容」；第二輪把替換層冒煙**按日誌廠商拆成 5 個獨立套件（29 用例）並改為對真實落盤斷言**，過程中還抓出並修掉 log4j1 `setLevel` 未下推引擎的真實缺陷；第三輪把 5 個廠商套件**併入 `TestAll.js`**，並把不屬於本層的套件**逐批清出**（`TestDatasource` 假陽性、`TestCache` / `TestFile` / `TestTool` 屬引擎套件），統一報告現為 **8 套件／50 用例**（2 引擎側 + 5 廠商 + 1 負載）：`TestAll` 每輪先清 `JsPlus\.tmp` 與 `logs\*`，報告歸檔成 `docs/reports/<version>.jar`（保留 10 個，與 Core 同佈局），並新增 `LoadTest.js`（6 例負載：突發吞吐、零丟行、8 KB 訊息完整性、異常負載、多 logger 交錯、級別閘門）；廠商套件只在 `JSPLUS_FACADE_EXIT=1` 時自持退出碼，負載套件用 `JSPLUS_LOADTEST_EXIT=1`；剩餘短板是 `TestAll` 的失敗上報、rawtypes 告警與 CI 未實跑。

| 維度 | 評分 | 一句話結論 |
| --- | --- | --- |
| 構建與版本機制 | 95 | 與 Core 逐項對齊（`jr.old.` 前綴、雙語 changelog、DiffScanner、javadoc 歸檔），實跑驗證；殘留 version.properties 髒狀態 |
| 腳本（bat/sh） | 92 | 自帶工具鏈、`ant.bat/sh`、`packJdk`、`release.bat/sh`、`JsPlus/bin` 啟動器；.sh 未在真實 bash 驗證 |
| 測試框架與套件 | 96 | 8 套件 50 用例全綠（2 引擎側 14 + 配置自檢 1 + 5 廠商 29 + 負載 6）；只保留與本層相關者——已移除假陽性 `TestDatasource` 與引擎套件 `TestCache`/`TestFile`/`TestTool`；`TestAll` 統一報告與 `TestFacades` / `LoadTest` 專用閘門口徑一致；斷言為「對真實落盤內容」而非只驗不拋；負載套件覆蓋吞吐、零丟行、長訊息、異常、多 logger 與級別閘門；仍缺 `TestAll` 的失敗上報 |
| CI 配置 | 80 | 流程完整、jar/version 一致性校驗；ant 版本未固定、未在 GitHub 實跑 |
| 源碼品質 | 84 | 官方 interface 全量（含 log4j2 384 方法，生成式）；rawtypes 告警仍在（官方原始簽名即 raw）、juli/slf4j 兩套適配器並存 |
| 文件 | 90 | README 中英/繁雙版已反映接口覆蓋表、行為映射與三道閘門；缺逐家族 API 使用範例 |
| 版本控制衛生 | 72 | 報告改放 `docs/latest/report/`（與 JsPlus-Core 一致，可納入版本控制），並由 `TestAll.bat/.sh` 每輪歸檔成 `docs/reports/<version>.jar`、保留最新 10 個（歸檔產物已列入 `.gitignore`）；`.gitignore` 已移除失效的 `/test_reports`，`hs_err_pid*.log` 已忽略；`version.properties` 每次建置遞增仍造成本地髒狀態 |

---

## 二、問題清單（依嚴重度）

### P0 — 應立即修正

目前無 P0：`ant build` 成功（6.1.28，jar 與 `docs/apis/6.1.28.jar` 版本一致）、`TestAll.bat` 50 passed / 0 failed（8 套件）、`TestFacades.bat` 29 passed / 0 failed（exit 0）、`LoadTest.bat` 6 passed / 0 failed（exit 0）、雙向編譯通過、接口閘門 0 缺失。

### P1 — 短中期改善

| # | 問題 | 證據 | 影響 |
| --- | --- | --- | --- |
| 1 | **替換層本體零專用測試（已關閉，第二輪加強）** | 第一版：14 個替換類無直接斷言。第二輪改為**按日誌廠商拆分**：`TestSlf4j.js`(7) / `TestCommonsLogging.js`(5) / `TestJuli.js`(4) / `TestLog4j.js`(6) / `TestLog4j2.js`(7)，共 29 用例，由 `TestFacades.bat/.sh` 統一驅動、各自帶 CI 可用退出碼；原單檔 `TestReplacement.js/.bat` 已刪除。斷言從「不拋異常」升級為「對 `logs/app/<yyyyMMdd>.log` 的實際內容」（Marker 前綴、`{}` 展開、Throwable、級別映射）。**該輪直接抓出並修正 log4j1 `Category.setLevel` 未下推引擎的缺陷** | 已消除「門面 API 漂移無法被發現」與「只冒煙不斷言」的風險；第三輪已把 5 個廠商套件註冊進 `TestAll.js`（彙總 92 用例，報告對版），同時它們只在 `JSPLUS_FACADE_EXIT=1` 時自持退出碼，`TestFacades` 仍作獨立閘門 |
| 2 | **TestAll 恆以 exit 0 結束** | `test/TestAll.js:211` `System.exit(0)` 無條件執行；實測失敗時 `TestAll.bat` 退出碼仍為 0 | CI 漏報測試失敗（全綠假象），與 Core 的 P1 新發現同源 |
| 3 | **TestDatasource 假陽性（已移除）** | `test/TestDatasource.js` 的 SCOPE 宣稱 JDBC 相關類「none is bundled in LibHome/Core/default/lib」，實際 `LibHome/h2/default/h2-2.4.240.jar` 存在且 `LibHome/Core` 目錄不存在；C5 scope check 恆 `ok: true`，其餘 4 例只驗 `Clock` getter，與本替換層無關 | 已刪除該套件與其報告，`TestAll` 同步移除；另按同一標準移除與本層無關的引擎套件 `TestCache`（cache）、`TestFile`（file）、`TestTool`（tool），統一報告現為 50 用例（含負載 6 例）。消除「JDBC 零覆蓋卻宣稱已界定範圍」的假象與一條恆真斷言 |
| 4 | **專案名字樣殘留（已修正）** | 第一版 41 處 "JsPlus-Core" 已統一為 "JsPlus-LogReplaced"，`TestAll.bat/.sh` 的 jar 名描述同步為 `jsplus-logreplaced-` | 已修正並回歸 63/0 |

### P2 — 長期 / 重構

| # | 問題 | 影響 |
| --- | --- | --- |
| 5 | `-Xlint:all` 下 rawtypes 告警（`getLog(Class)` / `getLogger(Class)` / `getInstance(Class)` 等） | 本輪為**簽名相容**而保留官方原始 raw 簽名（官方 commons/slf4j/log4j1 本身即 raw）；若要消警需改為 `Class<?>`，會與官方二進位簽名語義偏離，需相容評估 |
| 6 | `org.apache.juli.logging.LogCustom` 與 `org.slf4j.LoggerClass` 兩套轉接實作並存 | 轉接邏輯重複、行為易漂移；JULI 為既有完整實作，本輪未動以降低回歸風險 |
| 7 | `.gitignore` 與報告目錄（已處理） | 報告已改放 `docs/latest/report/`（與 JsPlus-Core 一致並納入版本控制）；`.gitignore` 移除失效的 `/test_reports`；`hs_err_pid*.log` 既已忽略 |
| 8 | `version.properties` 每次構建遞增 | 本地 git status 永久髒；需明確策略（CI 構建後 checkout 或納入忽略） |
| 9 | CI 的 ant 由 apt 安裝，版本未固定 | 跨環境行為漂移 |
| 10 | `build/tools/genLog4j2.py` 是 log4j2 `Logger`/`LoggerClass` 的唯一生成來源，但 `/build` 被 .gitignore 排除 | 乾淨 checkout 後無法重新生成 384 方法層；需將生成器移出 `build/` 或加反忽略 |

---

## 三、優化建議（可執行）

### 立刻（P1）
1. **CI 增加替換層閘門步驟**：`TestFacades.bat` / `.sh` 已自帶 CI 可用退出碼（任一廠商套件失敗即 `exit 1`），建議在 `ci.yml` 的 `TestAll.sh` 之後串接一步。
2. **TestAll 失敗上報**：`TestAll.bat/.sh` 在偵測到 FAIL 時以非 0 退出碼結束（或在 CI 步驟加退出碼判定）。
3. ~~修 TestDatasource.js~~ **已完成**：該套件（連同 `TestCache` / `TestFile` / `TestTool`）已移除，`TestAll` 現為 8 套件 / 50 用例（含 `LoadTest` 6 例負載）。

### 短中期（P2）
4. **生成器入版本控制**：`.gitignore` 加 `!/build/tools/genLog4j2.py`（或遷至 `tools/genLog4j2.py`），並在 CI 加 `python tools/genLog4j2.py && git diff --exit-code` 防漂移。
5. **rawtypes 治理**：僅對自訂擴充方法加泛型，官方同名方法簽名保持原樣。
6. **合併兩套轉接實作**：`LogCustom` 改為委派 `org.slf4j.LoggerClass`（或反之），消除重複。
7. ~~`.gitignore` 增補~~ **已完成**：報告目錄已由 `test_reports/` 遷至 `docs/latest/report/`（與 Core 對齊），`.gitignore` 移除失效的 `/test_reports`；`hs_err_pid*.log` 既已忽略。
8. **CI 固定 ant 版本**（如 apache/ant 官方 binary 或 1.10.x）。

---

## 四、驗證方式與覆蓋範圍

- **已實跑驗證**：
  - `ant build`（BUILD SUCCESSFUL；48 個源文件；產物 `jsplus-logreplaced-6.1.28.jar` + `docs/apis/6.1.28.jar` + `docs/latest/api`，三者版本一致）。
  - `python build/iface-audit/compare.py`：五家族 **0 missing**（SLF4J 61/81、Commons 18/84、JULI 18/18、Log4j1 55/122、Log4j2 384/424）。
  - `test\iface\refapi\CompileApiSample.bat`：**BOTH COMPILES OK**（同一 `ApiUsageSample.java` 對官方 jar 與本層 jar 均編譯）。
  - `test\TestFacades.bat`：**29 passed / 0 failed，exit 0**（TestSlf4j 7 / TestCommonsLogging 5 / TestJuli 4 / TestLog4j 6 / TestLog4j2 7），每一條斷言均核對 `logs/app/20261030.log` 的實際行／事件區塊。
  - `test\TestAll.bat`：**50 passed / 0 failed，exit 0**（8 套件：TestConsole 9 + TestLog 5 + 配置自檢 1 + 5 廠商 29 + LoadTest 6；jar 6.1.28 參與運行，`Category.setLevel` 改動後無回歸）；`LoadTest` 單獨跑 6/0、exit 0，實測突發入列約 5000 行/秒、10000 行零丟行。
- **靜態審查**：`-Xlint:all` 嚴格編譯、源碼衛生掃描（48 檔逐檔盤點）、`Test*.js` 頭部註解與 TestAll.js 流程核對、CI 與腳本一致性核對。
- **未覆蓋**：`.sh` 腳本真實 bash 執行（本機無 bash，含新增 `TestFacades.sh`）；GitHub Actions 實際跑通（需 push 驗證）；`release.bat/sh` 對實際兄弟專案的複製執行（僅靜態核對）；log4j2 `Level` 為 enum 而官方為 final class，`Level` 級別的 enum 自動方法（`valueOf(Class,String)`/`clone()`/`readResolve()`）未逐方法比對（閘門僅比對 `Logger` 的 384 方法與各家族入口類）。
