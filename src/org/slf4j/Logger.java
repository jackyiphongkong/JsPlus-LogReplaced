package org.slf4j;

/**
 * SLF4J 1.7 {@code Logger} interface - signature-compatible replacement for
 * the official API, so that existing binaries keep linking after the official
 * slf4j-api jar is removed from the class path.
 *
 * <p>The 61 methods declared by slf4j-api 1.7.36 are all present (including
 * the 25 marker variants, the five {@code isXEnabled(Marker)} probes and
 * {@link #getName()}). In addition this interface carries a JsPlus extension
 * block - the {@code (Object)}, {@code (Exception)} and
 * {@code (String,Throwable)} overloads - which the commons-logging, JULI and
 * log4j stand-ins derive from; those overloads are NOT part of the official
 * SLF4J API and are documented as extensions.</p>
 */
public interface Logger {

    /* ------------------------------------------------------------------
     * JsPlus extensions (not part of the official SLF4J 1.7 API).
     * ------------------------------------------------------------------ */
    void debug(Object message);

    void info(Object message);

    void warn(Object message);

    void error(Object message);

    void trace(Object message);

    void debug(String message);

    void debug(String message, Object o);

    void debug(String message, Object o1, Object o2);

    void debug(String message, Object... params);

    void debug(String message, Exception exception);

    void debug(Exception exception);

    void debug(String message, Throwable throwable);

    void debug(Object message, Throwable throwable);

    void info(String message);

    void info(String message, Object o);

    void info(String message, Object o1, Object o2);

    void info(String message, Object... params);

    void info(String message, Exception exception);

    void info(Exception exception);

    void info(String message, Throwable throwable);

    void info(Object message, Throwable throwable);

    void warn(String message);

    void warn(String message, Object o);

    void warn(String message, Object o1, Object o2);

    void warn(String message, Object... params);

    void warn(String message, Exception exception);

    void warn(Exception exception);

    void warn(String message, Throwable throwable);

    void warn(Object message, Throwable throwable);

    void error(String message);

    void error(String message, Object o);

    void error(String message, Object o1, Object o2);

    void error(String message, Object... params);

    void error(String message, Exception exception);

    void error(Exception exception);

    void error(String message, Throwable throwable);

    void error(Object message, Throwable throwable);

    void trace(String message);

    void trace(String message, Object o);

    void trace(String message, Object o1, Object o2);

    void trace(String message, Object... params);

    void trace(String message, Exception exception);

    void trace(Exception exception);

    void trace(String message, Throwable throwable);

    void trace(Object message, Throwable throwable);

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isErrorEnabled();

    /* ------------------------------------------------------------------
     * Official SLF4J 1.7 API - marker variants (25 methods).
     * ------------------------------------------------------------------ */
    void trace(Marker marker, String message);

    void trace(Marker marker, String message, Object o);

    void trace(Marker marker, String message, Object o1, Object o2);

    void trace(Marker marker, String message, Object... params);

    void trace(Marker marker, String message, Throwable throwable);

    void debug(Marker marker, String message);

    void debug(Marker marker, String message, Object o);

    void debug(Marker marker, String message, Object o1, Object o2);

    void debug(Marker marker, String message, Object... params);

    void debug(Marker marker, String message, Throwable throwable);

    void info(Marker marker, String message);

    void info(Marker marker, String message, Object o);

    void info(Marker marker, String message, Object o1, Object o2);

    void info(Marker marker, String message, Object... params);

    void info(Marker marker, String message, Throwable throwable);

    void warn(Marker marker, String message);

    void warn(Marker marker, String message, Object o);

    void warn(Marker marker, String message, Object o1, Object o2);

    void warn(Marker marker, String message, Object... params);

    void warn(Marker marker, String message, Throwable throwable);

    void error(Marker marker, String message);

    void error(Marker marker, String message, Object o);

    void error(Marker marker, String message, Object o1, Object o2);

    void error(Marker marker, String message, Object... params);

    void error(Marker marker, String message, Throwable throwable);

    /* ------------------------------------------------------------------
     * Official SLF4J 1.7 API - marker probes + name.
     * ------------------------------------------------------------------ */
    boolean isTraceEnabled(Marker marker);

    boolean isDebugEnabled(Marker marker);

    boolean isInfoEnabled(Marker marker);

    boolean isWarnEnabled(Marker marker);

    boolean isErrorEnabled(Marker marker);

    String getName();
}
