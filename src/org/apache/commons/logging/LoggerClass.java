package org.apache.commons.logging;

/**
 * Commons Logging {@code Log} implementation backed by the JsPlus engine.
 *
 * <p>{@code fatal} has no dedicated engine level, so it is rendered at the
 * engine's ERROR level (design decision D4).</p>
 */
public class LoggerClass extends org.slf4j.LoggerClass implements org.apache.commons.logging.Log {

    public LoggerClass(Class clazz) {
        super(clazz);
    }

    public LoggerClass(String clazzName) {
        super(clazzName);
    }

    @Override
    public void fatal(Object message) {
        error(message);
    }

    @Override
    public void fatal(Object message, Throwable t) {
        error(message, t);
    }

    @Override
    public boolean isFatalEnabled() {
        return isErrorEnabled();
    }
}
