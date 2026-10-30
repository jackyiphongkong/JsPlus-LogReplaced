// ============================================================================
// JsPlus-LogReplaced StartH2 - start the H2 TCP server (same shape as
// JsPlus-Core script/StartH2.js)
// ----------------------------------------------------------------------------
// Run      : JsPlus/bin/StartH2.bat (foreground, blocks) or
//            JsPlus/bin/BG_StartH2.bat (hidden window / nohup)
//            <java> -cp <LibHome> com.killcoding.jsplus.Main
//                   -Env script -GhostName H2Ghost -Task <JS_PLUS_PATH>script/StartH2.js
// Config   : JsPlus/config/Appenv.properties  (H2_PORT / H2_BASE_DIR)
//            JsPlus/config/DataSource.properties (jdbc:h2:tcp://localhost:<port>/logdb)
// Stop     : JsPlus/bin/StopH2.bat / .sh (Stop.bat StartH2.bat) - kills the
//            JVM whose command line carries "StartH2".
// NOTE     : -GhostName H2Ghost keeps this JVM alive after the task returns
//            (the ghost console is the process handle StopH2 looks for);
//            without it the server would die with the console.
// ============================================================================
var Logger = Java.type("com.killcoding.log.Logger");
var LoggerFactory = Java.type("com.killcoding.log.LoggerFactory");
var Thread = Java.type("java.lang.Thread");
var System = Java.type("java.lang.System");
var GhostConsole = Java.type("com.killcoding.jsplus.GhostConsole");

var log = LoggerFactory.getLogger("StartH2.js");
Logger.systemInfo("StartH2.js","Starting...");
var jsPlusPath = System.getProperty("JS_PLUS_PATH");

var H2StartConsole = Console.newBuilder();
H2StartConsole.getContext().put("OriginConsole",H2StartConsole);
H2StartConsole.eval(jsPlusPath + '/script/H2Server.js');

var H2StopConsole = Console.newBuilder();
H2StopConsole.getContext().put("OriginConsole",H2StartConsole);
H2StopConsole.eval(jsPlusPath + '/script/H2Server.js');

var H2ShutdownConsole = Console.newBuilder();
H2ShutdownConsole.getContext().put("OriginConsole",H2StartConsole);
H2ShutdownConsole.eval(jsPlusPath + '/script/H2Server.js');

function start(){
    var msg = H2StartConsole.getContext().get('server').start();
    return msg;
}

function stop(){
    H2StopConsole.getContext().put("server",H2StartConsole.getContext().get("server"));
    var msg = H2StopConsole.getContext().get('server').stop();
    return msg;
}

function shutdown(){
    H2ShutdownConsole.getContext().put("server",H2StartConsole.getContext().get("server"));
    var msg = H2ShutdownConsole.getContext().get('server').shutdown();
    return msg;
}

function restart(){
    var msg = stop();
    Thread.sleep(1000);
    msg += start();
    return msg;
}

start();

var ghostName = Console.getConsoleVariables().getString("GhostName",null);

if(ghostName !== null){
    print('Starting ' + ghostName + '...');
    var ghostConsole = new GhostConsole(Console,ghostName);
    ghostConsole.start(true);
    print('Started ' + ghostName);
}
