package org.apache.log4j;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.spi.HierarchyEventListener;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;

/**
 * Log4j 1.2 {@code Hierarchy} - simplified replacement.
 *
 * <p>Implements the repository contract with in-memory, non-persistent state:
 * loggers are created on demand and nothing is cached, because the JsPlus
 * engine owns the real logging state.</p>
 */
public class Hierarchy implements LoggerRepository {

    private static final Hierarchy DEFAULT = new Hierarchy(Logger.getRootLogger());

    private final Logger root;
    private volatile Level threshold = Level.ALL;
    private volatile boolean resetVersion;

    public Hierarchy(Logger root) {
        this.root = root;
    }

    /** The repository used when no selector has been configured. */
    public static Hierarchy getDefault() {
        return DEFAULT;
    }

    @Override
    public void addHierarchyEventListener(HierarchyEventListener listener) {
    }

    @Override
    public boolean isDisabled(int level) {
        return false;
    }

    @Override
    public void setThreshold(Level level) {
        this.threshold = level;
        this.resetVersion = !resetVersion;
    }

    @Override
    public void setThreshold(String level) {
        setThreshold(Level.toLevel(level));
    }

    @Override
    public void emitNoAppenderWarning(Category category) {
    }

    @Override
    public Level getThreshold() {
        return threshold;
    }

    @Override
    public Logger getLogger(String name) {
        return Logger.getLogger(name);
    }

    @Override
    public Logger getLogger(String name, LoggerFactory factory) {
        return Logger.getLogger(name);
    }

    @Override
    public Logger getRootLogger() {
        return root;
    }

    @Override
    public Logger exists(String name) {
        return null;
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Enumeration getCurrentLoggers() {
        return new Vector<Logger>().elements();
    }

    @Override
    public Enumeration getCurrentCategories() {
        return getCurrentLoggers();
    }

    @Override
    public void fireAddAppenderEvent(Category category, Appender appender) {
    }

    @Override
    public void resetConfiguration() {
        threshold = Level.ALL;
    }

    /** @return a counter bumped on every threshold change (diagnostic only) */
    public boolean isResetVersion() {
        return resetVersion;
    }
}
