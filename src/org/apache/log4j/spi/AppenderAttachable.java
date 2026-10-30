package org.apache.log4j.spi;

import java.util.Enumeration;

import org.apache.log4j.Appender;

/**
 * Log4j 1.2 {@code AppenderAttachable} - signature-compatible replacement.
 */
public interface AppenderAttachable {

    void addAppender(Appender newAppender);

    Enumeration getAllAppenders();

    Appender getAppender(String name);

    boolean isAttached(Appender appender);

    void removeAllAppenders();

    void removeAppender(Appender appender);

    void removeAppender(String name);
}
