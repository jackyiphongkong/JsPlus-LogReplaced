package org.apache.log4j.spi;

/**
 * Log4j 1.2 {@code RepositorySelector} - signature-compatible replacement.
 */
public interface RepositorySelector {

    LoggerRepository getLoggerRepository();
}
