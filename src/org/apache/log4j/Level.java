package org.apache.log4j;

import java.io.Serializable;

/**
 * Log4j 1.2 {@code Level} - signature-compatible replacement.
 *
 * <p>Adds {@code TRACE} and the {@code toLevel(...)} converters on top of
 * {@link Priority}. Level values are consumed by {@code Category#log} to pick
 * the matching JsPlus engine level.</p>
 */
public class Level extends Priority implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TRACE_INT = 5000;

    public static final Level OFF = new Level(OFF_INT, "OFF", 0);
    public static final Level FATAL = new Level(FATAL_INT, "FATAL", 0);
    public static final Level ERROR = new Level(ERROR_INT, "ERROR", 3);
    public static final Level WARN = new Level(WARN_INT, "WARN", 4);
    public static final Level INFO = new Level(INFO_INT, "INFO", 6);
    public static final Level DEBUG = new Level(DEBUG_INT, "DEBUG", 7);
    public static final Level TRACE = new Level(TRACE_INT, "TRACE", 7);
    public static final Level ALL = new Level(ALL_INT, "ALL", 7);

    protected Level(int level, String levelStr, int syslogEquivalent) {
        super(level, levelStr, syslogEquivalent);
    }

    public static Level toLevel(String sArg) {
        return toLevel(sArg, DEBUG);
    }

    public static Level toLevel(int val) {
        return toLevel(val, DEBUG);
    }

    public static Level toLevel(int val, Level defaultLevel) {
        if (val == TRACE_INT) {
            return TRACE;
        }
        if (val == DEBUG_INT) {
            return DEBUG;
        }
        if (val == INFO_INT) {
            return INFO;
        }
        if (val == WARN_INT) {
            return WARN;
        }
        if (val == ERROR_INT) {
            return ERROR;
        }
        if (val == FATAL_INT) {
            return FATAL;
        }
        if (val == OFF_INT) {
            return OFF;
        }
        if (val == ALL_INT) {
            return ALL;
        }
        return defaultLevel;
    }

    public static Level toLevel(String sArg, Level defaultLevel) {
        if (sArg == null) {
            return defaultLevel;
        }
        String s = sArg.trim().toUpperCase();
        if ("TRACE".equals(s)) {
            return TRACE;
        }
        if ("DEBUG".equals(s)) {
            return DEBUG;
        }
        if ("INFO".equals(s)) {
            return INFO;
        }
        if ("WARN".equals(s)) {
            return WARN;
        }
        if ("ERROR".equals(s)) {
            return ERROR;
        }
        if ("FATAL".equals(s)) {
            return FATAL;
        }
        if ("OFF".equals(s)) {
            return OFF;
        }
        if ("ALL".equals(s)) {
            return ALL;
        }
        try {
            return toLevel(Integer.parseInt(s), defaultLevel);
        } catch (NumberFormatException e) {
            return defaultLevel;
        }
    }
}
