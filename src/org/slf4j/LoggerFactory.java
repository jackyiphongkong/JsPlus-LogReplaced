package org.slf4j;

/**
 * SLF4J 2.0 {@code LoggerFactory} - signature-compatible replacement for the
 * official entry class (aligned with slf4j-api 2.0.20).
 *
 * <p>{@code getLogger(Class/String)} are the construction points of the slf4j
 * family; each returns a per-class or per-name {@link LoggerClass} routed into
 * the JsPlus logging engine. {@link #getILoggerFactory()} returns a shared
 * factory whose {@code getLogger(String)} builds the same per-name logger.
 * There is no pluggable service provider: this layer has no
 * {@code SLF4JServiceProvider} / SPI mechanism (same stance as the log4j2
 * {@code LogManager.getContext()} decision - context management is not
 * provided).</p>
 */
public class LoggerFactory {

    private static volatile ILoggerFactory I_LOGGER_FACTORY = new LoggerFactoryClass();

    public LoggerFactory() {
        super();
    }

    /** @return a logger bound to the given class name */
    public static org.slf4j.Logger getLogger(Class clazz) {
        org.slf4j.Logger logger = new org.slf4j.LoggerClass(clazz);
        return logger;
    }

    /** @return a logger bound to the given name */
    public static Logger getLogger(String clazzName) {
        org.slf4j.Logger logger = new org.slf4j.LoggerClass(clazzName);
        return logger;
    }

    /** @return the shared factory backing the static {@code getLogger} methods */
    public static ILoggerFactory getILoggerFactory() {
        return I_LOGGER_FACTORY;
    }

    private static final class LoggerFactoryClass implements ILoggerFactory {

        @Override
        public Logger getLogger(String name) {
            return new LoggerClass(name);
        }
    }
}
