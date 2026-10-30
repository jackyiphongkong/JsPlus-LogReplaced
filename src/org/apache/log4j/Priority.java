package org.apache.log4j;

/**
 * Log4j 1.2 {@code Priority} - signature-compatible replacement.
 *
 * <p>Priorities are plain value objects: the numeric level is the only data
 * that matters, because the replacement layer maps it onto the JsPlus engine
 * level set (DEBUG / INFO / WARN / ERROR / MARK).</p>
 */
public class Priority {

    public static final int OFF_INT = Integer.MAX_VALUE;
    public static final int FATAL_INT = 50000;
    public static final int ERROR_INT = 40000;
    public static final int WARN_INT = 30000;
    public static final int INFO_INT = 20000;
    public static final int DEBUG_INT = 10000;
    public static final int ALL_INT = Integer.MIN_VALUE;

    public static final Priority FATAL = new Priority(FATAL_INT, "FATAL", 0);
    public static final Priority ERROR = new Priority(ERROR_INT, "ERROR", 3);
    public static final Priority WARN = new Priority(WARN_INT, "WARN", 4);
    public static final Priority INFO = new Priority(INFO_INT, "INFO", 6);
    public static final Priority DEBUG = new Priority(DEBUG_INT, "DEBUG", 7);

    transient int level;
    transient String levelStr;
    transient int syslogEquivalent;

    protected Priority() {
        this(DEBUG_INT, "DEBUG", 7);
    }

    protected Priority(int level, String levelStr, int syslogEquivalent) {
        this.level = level;
        this.levelStr = levelStr;
        this.syslogEquivalent = syslogEquivalent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Priority)) {
            return false;
        }
        return level == ((Priority) o).level;
    }

    @Override
    public int hashCode() {
        return level;
    }

    public final int getSyslogEquivalent() {
        return syslogEquivalent;
    }

    public boolean isGreaterOrEqual(Priority p) {
        return p != null && level >= p.level;
    }

    public static Priority[] getAllPossiblePriorities() {
        return new Priority[] { FATAL, ERROR, WARN, INFO, DEBUG };
    }

    @Override
    public final String toString() {
        return levelStr;
    }

    public final int toInt() {
        return level;
    }

    public static Priority toPriority(String sArg) {
        return toPriority(sArg, DEBUG);
    }

    public static Priority toPriority(int val) {
        return toPriority(val, DEBUG);
    }

    public static Priority toPriority(int val, Priority defaultPriority) {
        if (val == FATAL_INT) {
            return FATAL;
        }
        if (val == ERROR_INT) {
            return ERROR;
        }
        if (val == WARN_INT) {
            return WARN;
        }
        if (val == INFO_INT) {
            return INFO;
        }
        if (val == DEBUG_INT) {
            return DEBUG;
        }
        return defaultPriority;
    }

    public static Priority toPriority(String sArg, Priority defaultPriority) {
        if (sArg == null) {
            return defaultPriority;
        }
        String s = sArg.trim().toUpperCase();
        if ("FATAL".equals(s)) {
            return FATAL;
        }
        if ("ERROR".equals(s)) {
            return ERROR;
        }
        if ("WARN".equals(s)) {
            return WARN;
        }
        if ("INFO".equals(s)) {
            return INFO;
        }
        if ("DEBUG".equals(s)) {
            return DEBUG;
        }
        if ("ALL".equals(s)) {
            return new Priority(ALL_INT, "ALL", 7);
        }
        if ("OFF".equals(s)) {
            return new Priority(OFF_INT, "OFF", 0);
        }
        try {
            return toPriority(Integer.parseInt(s), defaultPriority);
        } catch (NumberFormatException e) {
            return defaultPriority;
        }
    }
}
