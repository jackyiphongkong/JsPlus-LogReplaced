package org.apache.log4j.spi;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;

/**
 * Log4j 1.2 {@code HierarchyEventListener} - signature-compatible replacement.
 */
public interface HierarchyEventListener {

    void addAppenderEvent(Category category, Appender appender);

    void removeAppenderEvent(Category category, Appender appender);
}
