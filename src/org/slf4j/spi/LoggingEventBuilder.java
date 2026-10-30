package org.slf4j.spi;

import java.util.function.Supplier;

import org.slf4j.Marker;

/**
 * SLF4J 2.0 fluent-logging {@code LoggingEventBuilder} - signature-compatible
 * replacement for the official {@code org.slf4j.spi.LoggingEventBuilder}
 * (aligned with slf4j-api 2.0.20).
 *
 * <p>Returned by {@code Logger.atTrace()/atDebug()/atInfo()/atWarn()/atError()},
 * {@code atLevel(Level)} and {@code makeLoggingEventBuilder(Level)}. Every
 * terminal {@code log(...)} call writes one event through the owning
 * {@code org.slf4j.Logger}; {@link LoggingEventBuilderClass#NOOP} is the inert
 * instance returned for a disabled level (same gate as the official
 * {@code NOPLoggingEventBuilder}).</p>
 */
public interface LoggingEventBuilder {

    /** Set the cause (throwable) carried by the event being built. */
    LoggingEventBuilder setCause(Throwable cause);

    /** Add a marker to the event being built. */
    LoggingEventBuilder addMarker(Marker marker);

    /** Add an argument used to expand {@code {}} placeholders of the message. */
    LoggingEventBuilder addArgument(Object p);

    /** Add an argument supplier (evaluated eagerly by this layer). */
    LoggingEventBuilder addArgument(Supplier<?> objectSupplier);

    /** Add a key-value pair rendered as a {@code key=value } prefix. */
    LoggingEventBuilder addKeyValue(String key, Object value);

    /** Add a key-value pair whose value comes from a supplier (evaluated eagerly). */
    LoggingEventBuilder addKeyValue(String key, Supplier<Object> valueSupplier);

    /** Set the message of the event being built. */
    LoggingEventBuilder setMessage(String message);

    /** Set the message via a supplier (evaluated eagerly by this layer). */
    LoggingEventBuilder setMessage(Supplier<String> messageSupplier);

    /** Perform the actual logging. Without this call nothing is written. */
    void log();

    /** Equivalent to {@code setMessage(message)} followed by {@code log()}. */
    void log(String message);

    /** Equivalent to {@code setMessage(message)} + one argument + {@code log()}. */
    void log(String message, Object arg);

    /** Equivalent to {@code setMessage(message)} + two arguments + {@code log()}. */
    void log(String message, Object arg0, Object arg1);

    /** Equivalent to {@code setMessage(message)} + arguments + {@code log()}. */
    void log(String message, Object... args);

    /** Equivalent to {@code setMessage(supplier)} followed by {@code log()}. */
    void log(Supplier<String> messageSupplier);
}
