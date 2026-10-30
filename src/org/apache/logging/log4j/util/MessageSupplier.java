package org.apache.logging.log4j.util;

import org.apache.logging.log4j.message.Message;

/**
 * Log4j2 {@code MessageSupplier} - signature-compatible replacement.
 */
public interface MessageSupplier {

    Message get();
}
