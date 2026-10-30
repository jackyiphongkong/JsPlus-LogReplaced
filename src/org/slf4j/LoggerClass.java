package org.slf4j;

/**
 * SLF4J 2.0 {@code Logger} implementation backed by the JsPlus logging engine
 * ({@code com.killcoding.log.Logger}), aligned with slf4j-api 2.0.20.
 *
 * <p>All five official levels map to the engine as follows:</p>
 * <ul>
 *   <li>{@code trace} -&gt; {@code mark} (the engine's TRACE equivalent)</li>
 *   <li>{@code debug} -&gt; {@code debug}</li>
 *   <li>{@code info}  -&gt; {@code info}</li>
 *   <li>{@code warn}  -&gt; {@code warn}</li>
 *   <li>{@code error} -&gt; {@code error}</li>
 * </ul>
 *
 * <p>Markers are rendered as a message prefix ({@code "<marker> - <message>"}):
 * the engine has no marker filtering, so the marker name is preserved in the
 * log line instead of being dropped (design decision D3: "message prefix").
 * The SLF4J 2.0 fluent entry points ({@code atTrace()/atDebug()/atInfo()/
 * atWarn()/atError()}, {@code atLevel(Level)}) are interface default methods
 * that dispatch to {@code org.slf4j.spi.LoggingEventBuilderClass}.</p>
 */
public class LoggerClass implements org.slf4j.Logger {

    private com.killcoding.log.Logger logger = null;

    public LoggerClass(Class clazz) {
        super();
        logger = new com.killcoding.log.Logger(clazz);
    }

    public LoggerClass(String clazzName) {
        super();
        logger = new com.killcoding.log.Logger(clazzName);
    }

    /**
     * Expose the underlying JsPlus engine logger to subclasses that need to
     * emit at a level not covered by the slf4j surface (log4j1 {@code log(Priority,...)}).
     */
    protected com.killcoding.log.Logger engine() {
        return logger;
    }

    /** Render a marker as the message prefix used by every marker overload. */
    private String markerPrefix(Marker marker) {
        return (marker == null) ? "" : marker.getName() + " - ";
    }

    /* ------------------------------------------------------------------
     * JsPlus extensions (Object / Exception / Throwable overloads).
     * ------------------------------------------------------------------ */

    @Override
    public void debug(Object message) {
        logger.debug(message + "");
    }

    @Override
    public void info(Object message) {
        logger.info(message + "");
    }

    @Override
    public void warn(Object message) {
        logger.warn(message + "");
    }

    @Override
    public void error(Object message) {
        logger.error(message + "");
    }

    @Override
    public void trace(Object message) {
        logger.mark(message + "");
    }

    @Override
    public void debug(Object message, Throwable throwable) {
        logger.debug(message + "", throwable);
    }

    @Override
    public void info(Object message, Throwable throwable) {
        logger.info(message + "", throwable);
    }

    @Override
    public void warn(Object message, Throwable throwable) {
        logger.warn(message + "", throwable);
    }

    @Override
    public void error(Object message, Throwable throwable) {
        logger.error(message + "", throwable);
    }

    @Override
    public void trace(Object message, Throwable throwable) {
        logger.mark(message + "", throwable);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }

    @Override
    public void debug(String message, Object o) {
        logger.debug(message, o);
    }

    @Override
    public void debug(String message, Object o1, Object o2) {
        logger.debug(message, o1, o2);
    }

    @Override
    public void debug(String message, Object... params) {
        logger.debug(message, params);
    }

    @Override
    public void debug(String message, Exception exception) {
        logger.debug(message, exception);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    @Override
    public void debug(Exception exception) {
        logger.debug(exception);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void info(String message, Object o) {
        logger.info(message, o);
    }

    @Override
    public void info(String message, Object o1, Object o2) {
        logger.info(message, o1, o2);
    }

    @Override
    public void info(String message, Object... params) {
        logger.info(message, params);
    }

    @Override
    public void info(String message, Exception exception) {
        logger.info(message, exception);
    }

    @Override
    public void info(Exception exception) {
        logger.info(exception);
    }

    @Override
    public void info(String message, Throwable throwable) {
        logger.info(message, throwable);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void warn(String message, Object o) {
        logger.warn(message, o);
    }

    @Override
    public void warn(String message, Object o1, Object o2) {
        logger.warn(message, o1, o2);
    }

    @Override
    public void warn(String message, Object... params) {
        logger.warn(message, params);
    }

    @Override
    public void warn(String message, Exception exception) {
        logger.warn(message, exception);
    }

    @Override
    public void warn(Exception exception) {
        logger.warn(exception);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        logger.warn(message, throwable);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Object o) {
        logger.error(message, o);
    }

    @Override
    public void error(String message, Object o1, Object o2) {
        logger.error(message, o1, o2);
    }

    @Override
    public void error(String message, Object... params) {
        logger.error(message, params);
    }

    @Override
    public void error(String message, Exception exception) {
        logger.error(message, exception);
    }

    @Override
    public void error(Exception exception) {
        logger.error(exception);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public void trace(String message) {
        logger.mark(message);
    }

    @Override
    public void trace(String message, Object o) {
        logger.mark(message, o);
    }

    @Override
    public void trace(String message, Object o1, Object o2) {
        logger.mark(message, o1, o2);
    }

    @Override
    public void trace(String message, Object... params) {
        logger.mark(message, params);
    }

    @Override
    public void trace(String message, Exception exception) {
        logger.mark(message, exception);
    }

    @Override
    public void trace(Exception exception) {
        logger.mark(exception.getMessage(), exception);
    }

    @Override
    public void trace(String message, Throwable throwable) {
        logger.mark(message, throwable);
    }

    /* ------------------------------------------------------------------
     * Official SLF4J 1.7 API - marker variants.
     * ------------------------------------------------------------------ */

    @Override
    public void trace(Marker marker, String message) {
        logger.mark(markerPrefix(marker) + message);
    }

    @Override
    public void trace(Marker marker, String message, Object o) {
        logger.mark(markerPrefix(marker) + message, o);
    }

    @Override
    public void trace(Marker marker, String message, Object o1, Object o2) {
        logger.mark(markerPrefix(marker) + message, o1, o2);
    }

    @Override
    public void trace(Marker marker, String message, Object... params) {
        logger.mark(markerPrefix(marker) + message, params);
    }

    @Override
    public void trace(Marker marker, String message, Throwable throwable) {
        logger.mark(markerPrefix(marker) + message, throwable);
    }

    @Override
    public void debug(Marker marker, String message) {
        logger.debug(markerPrefix(marker) + message);
    }

    @Override
    public void debug(Marker marker, String message, Object o) {
        logger.debug(markerPrefix(marker) + message, o);
    }

    @Override
    public void debug(Marker marker, String message, Object o1, Object o2) {
        logger.debug(markerPrefix(marker) + message, o1, o2);
    }

    @Override
    public void debug(Marker marker, String message, Object... params) {
        logger.debug(markerPrefix(marker) + message, params);
    }

    @Override
    public void debug(Marker marker, String message, Throwable throwable) {
        logger.debug(markerPrefix(marker) + message, throwable);
    }

    @Override
    public void info(Marker marker, String message) {
        logger.info(markerPrefix(marker) + message);
    }

    @Override
    public void info(Marker marker, String message, Object o) {
        logger.info(markerPrefix(marker) + message, o);
    }

    @Override
    public void info(Marker marker, String message, Object o1, Object o2) {
        logger.info(markerPrefix(marker) + message, o1, o2);
    }

    @Override
    public void info(Marker marker, String message, Object... params) {
        logger.info(markerPrefix(marker) + message, params);
    }

    @Override
    public void info(Marker marker, String message, Throwable throwable) {
        logger.info(markerPrefix(marker) + message, throwable);
    }

    @Override
    public void warn(Marker marker, String message) {
        logger.warn(markerPrefix(marker) + message);
    }

    @Override
    public void warn(Marker marker, String message, Object o) {
        logger.warn(markerPrefix(marker) + message, o);
    }

    @Override
    public void warn(Marker marker, String message, Object o1, Object o2) {
        logger.warn(markerPrefix(marker) + message, o1, o2);
    }

    @Override
    public void warn(Marker marker, String message, Object... params) {
        logger.warn(markerPrefix(marker) + message, params);
    }

    @Override
    public void warn(Marker marker, String message, Throwable throwable) {
        logger.warn(markerPrefix(marker) + message, throwable);
    }

    @Override
    public void error(Marker marker, String message) {
        logger.error(markerPrefix(marker) + message);
    }

    @Override
    public void error(Marker marker, String message, Object o) {
        logger.error(markerPrefix(marker) + message, o);
    }

    @Override
    public void error(Marker marker, String message, Object o1, Object o2) {
        logger.error(markerPrefix(marker) + message, o1, o2);
    }

    @Override
    public void error(Marker marker, String message, Object... params) {
        logger.error(markerPrefix(marker) + message, params);
    }

    @Override
    public void error(Marker marker, String message, Throwable throwable) {
        logger.error(markerPrefix(marker) + message, throwable);
    }

    /* ------------------------------------------------------------------
     * Level probes + name.
     * ------------------------------------------------------------------ */

    @Override
    public boolean isTraceEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public String getName() {
        return logger.getClazzName();
    }
}
