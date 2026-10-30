package org.apache.logging.log4j;

import org.apache.logging.log4j.spi.StandardLevel;

/**
 * Log4j2 {@code Level} - signature-compatible replacement.
 *
 * <p>Implemented as an enum so that {@code values()}, {@code valueOf(String)},
 * {@code name()}, {@code toString()}, {@code compareTo(...)} and
 * {@code equals(...)} behave exactly like the official type for the well known
 * levels. Custom levels created through {@code forName(...)} collapse onto the
 * closest standard level, because the JsPlus engine only knows its own level
 * set.</p>
 */
public enum Level {

    OFF(Integer.MAX_VALUE),
    FATAL(50000),
    ERROR(40000),
    WARN(30000),
    INFO(20000),
    DEBUG(10000),
    TRACE(5000),
    ALL(Integer.MIN_VALUE);

    public static final String CATEGORY = "Log4j2Level";

    private final int intLevel;

    Level(int intLevel) {
        this.intLevel = intLevel;
    }

    public int intLevel() {
        return intLevel;
    }

    public StandardLevel getStandardLevel() {
        return StandardLevel.valueOf(name());
    }

    public boolean isInRange(Level low, Level high) {
        return intLevel >= low.intLevel() && intLevel <= high.intLevel();
    }

    public boolean isLessSpecificThan(Level other) {
        return other != null && intLevel >= other.intLevel();
    }

    public boolean isMoreSpecificThan(Level other) {
        return other != null && intLevel <= other.intLevel();
    }

    public static Level forName(String name, int intValue) {
        Level level = fromName(name);
        return level == null ? INFO : level;
    }

    public static Level getLevel(String name) {
        Level level = fromName(name);
        return level == null ? INFO : level;
    }

    public static Level toLevel(String name) {
        return getLevel(name);
    }

    public static Level toLevel(String name, Level defaultLevel) {
        Level level = fromName(name);
        return level == null ? defaultLevel : level;
    }

    private static Level fromName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toUpperCase();
        for (Level level : values()) {
            if (level.name().equals(normalized)) {
                return level;
            }
        }
        return null;
    }
}
