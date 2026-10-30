package org.slf4j;

import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.LoggingEventBuilderClass;

/**
 * SLF4J 2.0 {@code Logger} interface - signature-compatible replacement for the
 * official API (aligned with slf4j-api 2.0.20), so that binaries compiled
 * against the official slf4j-api 2.x jar keep linking after it is removed from
 * the class path.
 *
 * <p>All 69 methods declared by slf4j-api 2.0.20 are present: the 60 abstract
 * level methods (including the 25 marker variants and the five
 * {@code isXEnabled(Marker)} probes), {@link #getName()}, the five fluent
 * {@code atX()} entry points and the {@code makeLoggingEventBuilder} /
 * {@code atLevel} / {@code isEnabledForLevel} defaults, plus the
 * {@link #ROOT_LOGGER_NAME} constant. The fluent defaults dispatch to
 * {@link LoggingEventBuilderClass}: an enabled level builds a live builder that
 * writes one event through the JsPlus engine, a disabled level yields the inert
 * {@link LoggingEventBuilderClass#NOOP} (same gate as the official
 * {@code NOPLoggingEventBuilder}).</p>
 *
 * <p>In addition this interface carries a JsPlus extension block - the
 * {@code (Object)}, {@code (Exception)} and {@code (String,Throwable)}
 * overloads - which the commons-logging, JULI and log4j stand-ins derive from;
 * those overloads are NOT part of the official SLF4J API and are documented as
 * extensions (decision D2: keep the superset).</p>
 */
public interface Logger {

    /* ------------------------------------------------------------------
     * JsPlus extensions (not part of the official SLF4J 2.0 API).
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

    /* ------------------------------------------------------------------
     * Official SLF4J 2.0 API.
     * ------------------------------------------------------------------ */

    /**
     * Case-insensitive String constant used to retrieve the name of the root
     * logger (official SLF4J constant).
     */
    String ROOT_LOGGER_NAME = "ROOT";

    /** Return the name of this logger instance. */
    String getName();

    /**
     * Make a new {@link LoggingEventBuilder} for this logger at the given
     * level. Unconditional - builds a live builder for every level (intended
     * for logging systems implementing the SLF4J API, not end users).
     *
     * @since 2.0
     */
    default LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        return new LoggingEventBuilderClass(this, level);
    }

    /**
     * Make a new {@link LoggingEventBuilder} for this logger at the desired
     * level. When this logger is disabled for that level an inert builder
     * ({@link LoggingEventBuilderClass#NOOP}) is returned.
     *
     * @since 2.0
     */
    default LoggingEventBuilder atLevel(Level level) {
        if (isEnabledForLevel(level)) {
            return makeLoggingEventBuilder(level);
        } else {
            return LoggingEventBuilderClass.NOOP;
        }
    }

    /** @return {@code true} when this logger is enabled for the given level */
    default boolean isEnabledForLevel(Level level) {
        // The engine probes are exact-level (LEVEL == LEVEL_<X>), i.e. only the
        // configured level reports true. Re-derive the enabled threshold from
        // whichever probe is on and compare with the official Level integers
        // (ERROR=40, WARN=30, INFO=20, DEBUG=10, TRACE=0), so this method
        // follows the official threshold semantics: every level at or above
        // the configured threshold is enabled.
        if (isErrorEnabled()) {
            return level.toInt() >= 40;
        }
        if (isWarnEnabled()) {
            return level.toInt() >= 30;
        }
        if (isInfoEnabled()) {
            return level.toInt() >= 20;
        }
        if (isDebugEnabled()) {
            return level.toInt() >= 10;
        }
        return level.toInt() >= 0;
    }

    boolean isTraceEnabled();

    boolean isTraceEnabled(Marker marker);

    /** Entry point for fluent logging at {@link Level#TRACE}. @since 2.0 */
    default LoggingEventBuilder atTrace() {
        if (isEnabledForLevel(Level.TRACE)) {
            return makeLoggingEventBuilder(Level.TRACE);
        } else {
            return LoggingEventBuilderClass.NOOP;
        }
    }

    void trace(Marker marker, String msg);

    void trace(Marker marker, String format, Object arg);

    void trace(Marker marker, String format, Object arg1, Object arg2);

    void trace(Marker marker, String format, Object... argArray);

    void trace(Marker marker, String msg, Throwable t);

    boolean isDebugEnabled();

    boolean isDebugEnabled(Marker marker);

    /** Entry point for fluent logging at {@link Level#DEBUG}. @since 2.0 */
    default LoggingEventBuilder atDebug() {
        if (isEnabledForLevel(Level.DEBUG)) {
            return makeLoggingEventBuilder(Level.DEBUG);
        } else {
            return LoggingEventBuilderClass.NOOP;
        }
    }

    void debug(Marker marker, String msg);

    void debug(Marker marker, String format, Object arg);

    void debug(Marker marker, String format, Object arg1, Object arg2);

    void debug(Marker marker, String format, Object... arguments);

    void debug(Marker marker, String msg, Throwable t);

    boolean isInfoEnabled();

    boolean isInfoEnabled(Marker marker);

    /** Entry point for fluent logging at {@link Level#INFO}. @since 2.0 */
    default LoggingEventBuilder atInfo() {
        if (isEnabledForLevel(Level.INFO)) {
            return makeLoggingEventBuilder(Level.INFO);
        } else {
            return LoggingEventBuilderClass.NOOP;
        }
    }

    void info(Marker marker, String msg);

    void info(Marker marker, String format, Object arg);

    void info(Marker marker, String format, Object arg1, Object arg2);

    void info(Marker marker, String format, Object... arguments);

    void info(Marker marker, String msg, Throwable t);

    boolean isWarnEnabled();

    boolean isWarnEnabled(Marker marker);

    /** Entry point for fluent logging at {@link Level#WARN}. @since 2.0 */
    default LoggingEventBuilder atWarn() {
        if (isEnabledForLevel(Level.WARN)) {
            return makeLoggingEventBuilder(Level.WARN);
        } else {
            return LoggingEventBuilderClass.NOOP;
        }
    }

    void warn(Marker marker, String msg);

    void warn(Marker marker, String format, Object arg);

    void warn(Marker marker, String format, Object arg1, Object arg2);

    void warn(Marker marker, String format, Object... arguments);

    void warn(Marker marker, String msg, Throwable t);

    boolean isErrorEnabled();

    boolean isErrorEnabled(Marker marker);

    /** Entry point for fluent logging at {@link Level#ERROR}. @since 2.0 */
    default LoggingEventBuilder atError() {
        if (isEnabledForLevel(Level.ERROR)) {
            return makeLoggingEventBuilder(Level.ERROR);
        } else {
            return LoggingEventBuilderClass.NOOP;
        }
    }

    void error(Marker marker, String msg);

    void error(Marker marker, String format, Object arg);

    void error(Marker marker, String format, Object arg1, Object arg2);

    void error(Marker marker, String format, Object... arguments);

    void error(Marker marker, String msg, Throwable t);
}
