package org.apache.log4j;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.RepositorySelector;

/**
 * Log4j 1.2 {@code LogManager} - signature-compatible replacement.
 *
 * <p>All lookups go through the {@link LoggerRepository} returned by the
 * configured {@link RepositorySelector}; the default repository is
 * {@link Hierarchy#getDefault()}.</p>
 */
public class LogManager {

    public static final String DEFAULT_CONFIGURATION_FILE = "log4j.properties";
    public static final String DEFAULT_XML_CONFIGURATION_FILE = "log4j.xml";
    public static final String DEFAULT_CONFIGURATION_KEY = "log4j.configuration";
    public static final String CONFIGURATOR_CLASS_KEY = "log4j.configuratorClass";
    public static final String DEFAULT_INIT_OVERRIDE_KEY = "log4j.defaultInitOverride";

    private static volatile RepositorySelector repositorySelector;

    public LogManager() {
    }

    /**
     * Set the repository selector. As in log4j 1.2, the selector can only be
     * installed once (further attempts with a different selector fail).
     */
    public static void setRepositorySelector(RepositorySelector selector, Object guard)
            throws IllegalArgumentException {
        if (guard == null) {
            throw new IllegalArgumentException("guard must not be null");
        }
        RepositorySelector current = repositorySelector;
        if (current != null && current != selector) {
            throw new IllegalArgumentException("The repository selector may not be reset.");
        }
        repositorySelector = selector;
    }

    public static LoggerRepository getLoggerRepository() {
        RepositorySelector selector = repositorySelector;
        return selector == null ? Hierarchy.getDefault() : selector.getLoggerRepository();
    }

    public static Logger getRootLogger() {
        return getLoggerRepository().getRootLogger();
    }

    public static Logger getLogger(String name) {
        return getLoggerRepository().getLogger(name);
    }

    public static Logger getLogger(Class clazz) {
        return getLogger(clazz == null ? "null" : clazz.getName());
    }

    public static Logger getLogger(String name, LoggerFactory factory) {
        return getLoggerRepository().getLogger(name, factory);
    }

    public static Logger exists(String name) {
        return getLoggerRepository().exists(name);
    }

    public static Enumeration getCurrentLoggers() {
        return getLoggerRepository().getCurrentLoggers();
    }

    public static void shutdown() {
        getLoggerRepository().shutdown();
    }

    public static void resetConfiguration() {
        getLoggerRepository().resetConfiguration();
    }
}
