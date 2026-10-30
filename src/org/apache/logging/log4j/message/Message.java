package org.apache.logging.log4j.message;

import java.io.Serializable;

/**
 * Log4j2 {@code Message} - signature-compatible replacement.
 */
public interface Message extends Serializable {

    String getFormattedMessage();

    String getFormat();

    Object[] getParameters();

    Throwable getThrowable();
}
