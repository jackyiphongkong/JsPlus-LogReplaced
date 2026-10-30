package org.apache.log4j.spi;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;

/**
 * Log4j 1.2 {@code ErrorHandler} - signature-compatible replacement.
 */
public interface ErrorHandler extends OptionHandler {

    void setLogger(Logger logger);

    void error(String message, Exception e, int errorCode);

    void error(String message);

    void error(String message, Exception e, int errorCode, LoggingEvent event);

    void setAppender(Appender appender);

    void setBackupAppender(Appender appender);
}
