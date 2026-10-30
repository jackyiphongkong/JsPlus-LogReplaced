package org.apache.logging.log4j.message;

/**
 * Log4j2 {@code FlowMessageFactory} - signature-compatible replacement.
 */
public interface FlowMessageFactory {

    EntryMessage newEntryMessage(String message, Object... params);

    EntryMessage newEntryMessage(Message message);

    ExitMessage newExitMessage(String message, Object result);

    ExitMessage newExitMessage(Message message);

    ExitMessage newExitMessage(Object result, Message message);

    ExitMessage newExitMessage(EntryMessage message);

    ExitMessage newExitMessage(Object result, EntryMessage message);
}
