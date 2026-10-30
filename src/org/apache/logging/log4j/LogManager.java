package org.apache.logging.log4j;

import org.apache.logging.log4j.message.MessageFactory;

/**
 * Log4j2 {@code LogManager} - signature-compatible replacement.
 *
 * <p>Covered: every logger lookup shape used by applications (name, class,
 * object, message-factory variants, root, formatter loggers) plus
 * {@code exists}/{@code shutdown}. Context and factory management
 * ({@code getContext()}, {@code setFactory(...)}) are intentionally not
 * provided: this replacement layer has no pluggable LoggerContext, and those
 * entry points belong to the log4j2 implementation, not to the API surface a
 * caller needs in order to log.</p>
 */
public class LogManager {

    public static final String FACTORY_PROPERTY_NAME = "log4j2.loggerContextFactory";

    public static final String ROOT_LOGGER_NAME = "";

    protected LogManager() {
    }

    public static boolean exists(String name) {
        return false;
    }

    public static Logger getLogger() {
        return getRootLogger();
    }

    public static Logger getLogger(Class<?> clazz) {
        return new LoggerClass(clazz == null ? "null" : clazz.getName());
    }

    public static Logger getLogger(Class<?> clazz, MessageFactory messageFactory) {
        return getLogger(clazz);
    }

    public static Logger getLogger(MessageFactory messageFactory) {
        return getRootLogger();
    }

    public static Logger getLogger(Object object) {
        return getLogger(object == null ? "null" : object.toString());
    }

    public static Logger getLogger(Object object, MessageFactory messageFactory) {
        return getLogger(object);
    }

    public static Logger getLogger(String name) {
        return new LoggerClass(name);
    }

    public static Logger getLogger(String name, MessageFactory messageFactory) {
        return getLogger(name);
    }

    public static Logger getRootLogger() {
        return new LoggerClass(ROOT_LOGGER_NAME);
    }

    public static Logger getFormatterLogger() {
        return getRootLogger();
    }

    public static Logger getFormatterLogger(Class<?> clazz) {
        return getLogger(clazz);
    }

    public static Logger getFormatterLogger(Object object) {
        return getLogger(object);
    }

    public static Logger getFormatterLogger(String name) {
        return getLogger(name);
    }

    public static void shutdown() {
    }

    public static void shutdown(boolean currentContext) {
    }

    public static void shutdown(boolean currentContext, boolean allContexts) {
    }
}
