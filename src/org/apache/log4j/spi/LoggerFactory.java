package org.apache.log4j.spi;

import org.apache.log4j.Logger;

/**
 * Log4j 1.2 {@code LoggerFactory} - signature-compatible replacement.
 */
public interface LoggerFactory {

    /** Create a new logger instance for the given name. */
    Logger makeNewLoggerInstance(String name);
}
