package org.slf4j;

/**
 * SLF4J 2.0 {@code ILoggerFactory} - signature-compatible replacement for the
 * official interface (aligned with slf4j-api 2.0.20). Returned by
 * {@link LoggerFactory#getILoggerFactory()}; each call builds a per-name
 * {@link LoggerClass} backed by the JsPlus logging engine.
 */
public interface ILoggerFactory {

    /** @return a logger bound to the given name */
    Logger getLogger(String name);
}
