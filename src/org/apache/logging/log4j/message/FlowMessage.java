package org.apache.logging.log4j.message;

/**
 * Log4j2 {@code FlowMessage} - signature-compatible replacement.
 */
public interface FlowMessage extends Message {

    String getText();

    Message getMessage();
}
