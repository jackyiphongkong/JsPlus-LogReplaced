package org.apache.log4j;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;

import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.LoggerRepository;

/**
 * Log4j 1.2 {@code Category} - signature-compatible replacement.
 *
 * <p>Level methods delegate to the JsPlus engine through the inherited
 * {@link org.slf4j.LoggerClass} surface; appenders are remembered but never
 * invoked (the engine is the single sink). Hierarchy handling is reduced to a
 * level field plus an optional repository reference.</p>
 */
public class Category extends org.slf4j.LoggerClass implements AppenderAttachable {

    protected String name;
    protected volatile Level level;
    protected volatile Category parent;
    protected ResourceBundle resourceBundle;
    protected LoggerRepository repository;
    protected boolean additive = true;

    private final List<Appender> appenders = new ArrayList<Appender>();

    protected Category(String name) {
        super(name);
        this.name = name;
    }

    protected Category(Class clazz) {
        super(clazz);
        this.name = clazz == null ? "null" : clazz.getName();
    }

    /* ------------------------------------------------------------------
     * Appender API - accepted and remembered, never invoked.
     * ------------------------------------------------------------------ */

    @Override
    public synchronized void addAppender(Appender newAppender) {
        if (newAppender != null) {
            appenders.add(newAppender);
        }
    }

    @Override
    public synchronized Enumeration getAllAppenders() {
        return new Vector<Appender>(appenders).elements();
    }

    @Override
    public synchronized Appender getAppender(String appenderName) {
        for (Appender appender : appenders) {
            if (appender != null && appender.getName() != null
                    && appender.getName().equals(appenderName)) {
                return appender;
            }
        }
        return null;
    }

    @Override
    public boolean isAttached(Appender appender) {
        return appender != null && appenders.contains(appender);
    }

    @Override
    public synchronized void removeAllAppenders() {
        appenders.clear();
    }

    @Override
    public synchronized void removeAppender(Appender appender) {
        appenders.remove(appender);
    }

    @Override
    public synchronized void removeAppender(String appenderName) {
        for (int i = appenders.size() - 1; i >= 0; i--) {
            Appender appender = appenders.get(i);
            if (appender != null && appender.getName() != null
                    && appender.getName().equals(appenderName)) {
                appenders.remove(i);
            }
        }
    }

    public boolean getAdditivity() {
        return additive;
    }

    public void setAdditivity(boolean additive) {
        this.additive = additive;
    }

    /* ------------------------------------------------------------------
     * Logging API - the engine is the only sink.
     * ------------------------------------------------------------------ */

    public void assertLog(boolean assertion, String msg) {
        if (!assertion) {
            error(msg);
        }
    }

    /** Accepted for API compatibility; the engine already received the event. */
    public void callAppenders(LoggingEvent event) {
    }

    public void fatal(Object message) {
        error(message);
    }

    public void fatal(Object message, Throwable t) {
        error(message, t);
    }

    public void log(Priority priority, Object message) {
        if (priority == null) {
            return;
        }
        int p = priority.toInt();
        if (p <= Level.TRACE_INT) {
            trace(message);
        } else if (p <= Level.DEBUG_INT) {
            debug(message);
        } else if (p <= Level.INFO_INT) {
            info(message);
        } else if (p <= Level.WARN_INT) {
            warn(message);
        } else {
            error(message);
        }
    }

    public void log(Priority priority, Object message, Throwable t) {
        if (priority == null) {
            return;
        }
        int p = priority.toInt();
        if (p <= Level.TRACE_INT) {
            trace(message, t);
        } else if (p <= Level.DEBUG_INT) {
            debug(message, t);
        } else if (p <= Level.INFO_INT) {
            info(message, t);
        } else if (p <= Level.WARN_INT) {
            warn(message, t);
        } else {
            error(message, t);
        }
    }

    public void log(String fqnOfCaller, Priority priority, Object message, Throwable t) {
        log(priority, message, t);
    }

    public void l7dlog(Priority priority, String key, Throwable t) {
        l7dlog(priority, key, null, t);
    }

    public void l7dlog(Priority priority, String key, Object[] params, Throwable t) {
        String message = getResourceBundleString(key);
        if (message == null) {
            message = key;
        }
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(params[i]));
            }
        }
        log(priority, message, t);
    }

    /* ------------------------------------------------------------------
     * Level / hierarchy accessors.
     * ------------------------------------------------------------------ */

    public final String getName() {
        return name;
    }

    public final Category getParent() {
        return parent;
    }

    public final Level getLevel() {
        if (level != null) {
            return level;
        }
        LoggerRepository repo = getLoggerRepository();
        return repo == null ? null : repo.getThreshold();
    }

    public void setLevel(Level level) {
        this.level = level;
        String engineLevel = engineLevelName(level);
        if (engineLevel != null) {
            engine().setLevel(engineLevel);
        }
    }

    /**
     * Map a log4j 1.2 priority onto the JsPlus engine level vocabulary.
     *
     * <p>The engine knows only DEBUG/INFO/WARN/ERROR/MARK, so the two priorities
     * it lacks are folded onto the nearest one: FATAL -> ERROR, TRACE -> DEBUG
     * (trace events themselves still reach the engine as MARK, see the trace
     * overloads), ALL -> DEBUG and OFF -> ERROR (the engine cannot silence a
     * logger completely).</p>
     */
    private static String engineLevelName(Level level) {
        if (level == null) {
            return null;
        }
        int value = level.toInt();
        if (value == Level.FATAL.toInt()) {
            return "ERROR";
        }
        if (value == Level.TRACE.toInt() || value == Level.ALL.toInt()) {
            return "DEBUG";
        }
        if (value == Level.OFF.toInt()) {
            return "ERROR";
        }
        return level.toString();
    }

    public final Level getPriority() {
        return getLevel();
    }

    public void setPriority(Priority priority) {
        setLevel(priority == null ? null : Level.toLevel(priority.toInt()));
    }

    public Level getEffectiveLevel() {
        for (Category c = this; c != null; c = c.parent) {
            if (c.level != null) {
                return c.level;
            }
        }
        LoggerRepository repo = getLoggerRepository();
        return repo == null ? Level.DEBUG : repo.getThreshold();
    }

    public Priority getChainedPriority() {
        return getEffectiveLevel();
    }

    public boolean isEnabledFor(Priority p) {
        if (p == null) {
            return false;
        }
        int value = p.toInt();
        if (value <= Level.TRACE_INT) {
            return isTraceEnabled();
        }
        if (value <= Level.DEBUG_INT) {
            return isDebugEnabled();
        }
        if (value <= Level.INFO_INT) {
            return isInfoEnabled();
        }
        if (value <= Level.WARN_INT) {
            return isWarnEnabled();
        }
        return isErrorEnabled();
    }

    /* ------------------------------------------------------------------
     * Resource bundles.
     * ------------------------------------------------------------------ */

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public void setResourceBundle(ResourceBundle bundle) {
        this.resourceBundle = bundle;
    }

    protected String getResourceBundleString(String key) {
        if (resourceBundle == null) {
            return null;
        }
        try {
            return resourceBundle.getString(key);
        } catch (java.util.MissingResourceException e) {
            return null;
        }
    }

    /* ------------------------------------------------------------------
     * Repository.
     * ------------------------------------------------------------------ */

    public LoggerRepository getLoggerRepository() {
        return repository == null ? Hierarchy.getDefault() : repository;
    }

    public LoggerRepository getHierarchy() {
        return getLoggerRepository();
    }

    protected void setHierarchy(LoggerRepository repository) {
        this.repository = repository;
    }

    /* ------------------------------------------------------------------
     * Static API.
     * ------------------------------------------------------------------ */

    public static Logger exists(String name) {
        return null;
    }

    public static Enumeration getCurrentCategories() {
        return new Vector<Category>().elements();
    }

    public static LoggerRepository getDefaultHierarchy() {
        return Hierarchy.getDefault();
    }

    public static Category getInstance(String name) {
        return Logger.getLogger(name);
    }

    public static Category getInstance(Class clazz) {
        return Logger.getLogger(clazz);
    }

    public static final Category getRoot() {
        return Logger.getRootLogger();
    }

    public static void shutdown() {
    }
}
