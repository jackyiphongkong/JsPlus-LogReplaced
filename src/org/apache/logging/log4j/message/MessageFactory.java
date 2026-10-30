package org.apache.logging.log4j.message;

/**
 * Log4j2 {@code MessageFactory} - signature-compatible replacement.
 */
public interface MessageFactory {

    Message newMessage(Object message);

    Message newMessage(String message);

    Message newMessage(String message, Object... params);
}
