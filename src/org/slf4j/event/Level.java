package org.slf4j.event;

/**
 * SLF4J 2.0 {@code Level} enum - signature-compatible replacement for the
 * official {@code org.slf4j.event.Level} (aligned with slf4j-api 2.0.20).
 *
 * <p>The integer values match the official {@code EventConstants}:
 * {@code ERROR=40, WARN=30, INFO=20, DEBUG=10, TRACE=0}. The JsPlus engine has
 * no TRACE level; the mapping of SLF4J levels onto engine levels is documented
 * in {@code LoggerClass} (TRACE -&gt; MARK).</p>
 */
public enum Level {

    ERROR(40, "ERROR"),
    WARN(30, "WARN"),
    INFO(20, "INFO"),
    DEBUG(10, "DEBUG"),
    TRACE(0, "TRACE");

    private final int levelInt;
    private final String levelStr;

    Level(int i, String s) {
        levelInt = i;
        levelStr = s;
    }

    /** @return the official integer value of this level */
    public int toInt() {
        return levelInt;
    }

    /** @return the {@code Level} whose integer value is {@code levelInt} */
    public static Level intToLevel(int levelInt) {
        switch (levelInt) {
        case (0):
            return TRACE;
        case (10):
            return DEBUG;
        case (20):
            return INFO;
        case (30):
            return WARN;
        case (40):
            return ERROR;
        default:
            throw new IllegalArgumentException("Level integer [" + levelInt + "] not recognized.");
        }
    }

    /** @return the level name, e.g. {@code "ERROR"} */
    @Override
    public String toString() {
        return levelStr;
    }
}
