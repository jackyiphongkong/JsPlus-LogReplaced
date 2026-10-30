package org.apache.log4j.spi;

/**
 * Log4j 1.2 {@code OptionHandler} - signature-compatible replacement.
 */
public interface OptionHandler {

    /** Activate the options set on this component. */
    void activateOptions();
}
