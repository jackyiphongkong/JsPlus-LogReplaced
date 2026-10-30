package org.apache.log4j;

import org.apache.log4j.spi.LoggerFactory;

/**
 * Log4j 1.2 {@code Logger} - signature-compatible replacement.
 *
 * <p>Matches the official hierarchy ({@code Logger extends Category}) while
 * still carrying the JsPlus slf4j surface inherited from
 * {@link org.slf4j.LoggerClass} through {@link Category}.</p>
 */
public class Logger extends Category {

    private static final Logger ROOT = new Logger("root");

    public Logger(String name) {
        super(name);
    }

    protected Logger(String name, LoggerFactory factory) {
        super(name);
    }

    /** JsPlus extension: class-based constructor kept from the original stand-in. */
    public Logger(Class clazz) {
        super(clazz);
    }

    public static Logger getLogger(String name) {
        return new Logger(name);
    }

    public static Logger getLogger(Class clazz) {
        return new Logger(clazz);
    }

    public static Logger getLogger(String name, LoggerFactory factory) {
        return new Logger(name, factory);
    }

    public static Logger getRootLogger() {
        return ROOT;
    }
}
