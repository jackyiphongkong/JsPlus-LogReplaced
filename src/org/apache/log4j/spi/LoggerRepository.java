package org.apache.log4j.spi;

import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Log4j 1.2 {@code LoggerRepository} - signature-compatible replacement.
 */
public interface LoggerRepository {

    void addHierarchyEventListener(HierarchyEventListener listener);

    boolean isDisabled(int level);

    void setThreshold(Level level);

    void setThreshold(String level);

    void emitNoAppenderWarning(Category category);

    Level getThreshold();

    Logger getLogger(String name);

    Logger getLogger(String name, LoggerFactory factory);

    Logger getRootLogger();

    Logger exists(String name);

    void shutdown();

    Enumeration getCurrentLoggers();

    Enumeration getCurrentCategories();

    void fireAddAppenderEvent(Category category, Appender appender);

    void resetConfiguration();
}
