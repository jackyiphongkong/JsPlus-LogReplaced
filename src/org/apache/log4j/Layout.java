package org.apache.log4j;

import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;

/**
 * Log4j 1.2 {@code Layout} - signature-compatible replacement.
 */
public abstract class Layout implements OptionHandler {

    public static final String LINE_SEP = System.getProperty("line.separator");

    public static final int LINE_SEP_LEN = LINE_SEP.length();

    public Layout() {
    }

    /** Format one event. */
    public abstract String format(LoggingEvent event);

    public String getContentType() {
        return "text/plain";
    }

    public String getHeader() {
        return "";
    }

    public String getFooter() {
        return "";
    }

    /** @return {@code true} when the layout already renders throwables */
    public abstract boolean ignoresThrowable();
}
