package org.apache.log4j.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;

/**
 * Log4j 1.2 {@code LoggingEvent} - signature-compatible replacement.
 *
 * <p>Carries the data a log4j appender would consume. The replacement layer
 * writes to the JsPlus engine directly, so this type mainly exists so that
 * {@code Appender#doAppend(LoggingEvent)} and
 * {@code Category#callAppenders(LoggingEvent)} keep their official signatures.</p>
 */
public class LoggingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public final transient String fqnOfCategoryClass;
    public final String categoryName;
    public transient Priority level;
    public final long timeStamp;

    private final Object message;
    private final Throwable throwable;
    private final String threadName;
    private final Map<String, String> properties = new HashMap<String, String>();

    public LoggingEvent(String fqnOfCategoryClass, Category logger, Priority level,
                        Object message, Throwable throwable) {
        this(fqnOfCategoryClass, logger, System.currentTimeMillis(), level, message, throwable);
    }

    public LoggingEvent(String fqnOfCategoryClass, Category logger, long timeStamp,
                        Priority level, Object message, Throwable throwable) {
        this.fqnOfCategoryClass = fqnOfCategoryClass;
        this.categoryName = logger == null ? "root" : logger.getName();
        this.timeStamp = timeStamp;
        this.level = level;
        this.message = message;
        this.throwable = throwable;
        this.threadName = Thread.currentThread().getName();
    }

    public LoggingEvent(String fqnOfCategoryClass, Category logger, long timeStamp, Level level,
                        Object message, String threadName, ThrowableInformation throwableInformation,
                        String ndc, LocationInfo info, Map properties) {
        this.fqnOfCategoryClass = fqnOfCategoryClass;
        this.categoryName = logger == null ? "root" : logger.getName();
        this.timeStamp = timeStamp;
        this.level = level;
        this.message = message;
        this.throwable = throwableInformation == null ? null : throwableInformation.getThrowable();
        this.threadName = threadName;
        if (properties != null) {
            for (Object key : properties.keySet()) {
                Object value = properties.get(key);
                if (value != null) {
                    this.properties.put(String.valueOf(key), String.valueOf(value));
                }
            }
        }
    }

    public LocationInfo getLocationInformation() {
        return LocationInfo.NA_LOCATION_INFO;
    }

    public Level getLevel() {
        return level instanceof Level ? (Level) level : Level.toLevel(level.toInt());
    }

    public String getLoggerName() {
        return categoryName;
    }

    public Category getLogger() {
        return null;
    }

    public Object getMessage() {
        return message;
    }

    public String getNDC() {
        return null;
    }

    public Object getMDC(String key) {
        return properties.get(key);
    }

    public void getMDCCopy() {
    }

    public String getRenderedMessage() {
        return String.valueOf(message);
    }

    public static long getStartTime() {
        return 0L;
    }

    public String getThreadName() {
        return threadName;
    }

    public ThrowableInformation getThrowableInformation() {
        return throwable == null ? null : new ThrowableInformation(throwable);
    }

    public String[] getThrowableStrRep() {
        ThrowableInformation info = getThrowableInformation();
        return info == null ? null : info.getThrowableStrRep();
    }

    public final void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public final String getProperty(String key) {
        return properties.get(key);
    }

    public final boolean locationInformationExists() {
        return false;
    }

    public final long getTimeStamp() {
        return timeStamp;
    }

    public Set getPropertyKeySet() {
        return properties.keySet();
    }

    public Map getProperties() {
        return properties;
    }

    public String getFQNOfLoggerClass() {
        return fqnOfCategoryClass;
    }

    public Object removeProperty(String key) {
        return properties.remove(key);
    }
}
