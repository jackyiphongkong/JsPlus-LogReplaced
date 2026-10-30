package org.apache.logging.log4j.spi;

/**
 * Log4j2 {@code StandardLevel} - signature-compatible replacement.
 */
public enum StandardLevel {

    OFF(Integer.MAX_VALUE),
    FATAL(50000),
    ERROR(40000),
    WARN(30000),
    INFO(20000),
    DEBUG(10000),
    TRACE(5000),
    ALL(Integer.MIN_VALUE);

    private final int intLevel;

    StandardLevel(int intLevel) {
        this.intLevel = intLevel;
    }

    public int intLevel() {
        return intLevel;
    }

    public static StandardLevel getStandardLevel(int intLevel) {
        for (StandardLevel level : values()) {
            if (level.intLevel == intLevel) {
                return level;
            }
        }
        return INFO;
    }
}
