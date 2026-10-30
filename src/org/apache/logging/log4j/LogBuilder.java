package org.apache.logging.log4j;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;

/**
 * Log4j2 {@code LogBuilder} - signature-compatible replacement.
 *
 * <p>Returned by {@code atTrace()/atDebug()/.../always()}; every terminal
 * {@code log(...)} call writes one event through the owning
 * {@code LoggerClass}. {@link #NOOP} is an inert instance.</p>
 *
 * <p>This interface also extends {@code org.slf4j.spi.LoggingEventBuilder}:
 * {@code org.apache.logging.log4j.Logger} extends {@code org.slf4j.Logger}, and
 * both APIs declare the fluent {@code atX()} entry points, so the log4j2
 * builder must be a covariant subtype of the SLF4J 2.0 builder for the two
 * interfaces to coexist (the log4j2 {@code atInfo()} returns this subtype).
 * The inherited fluent methods ({@code setMessage}/{@code addArgument}/...)
 * are implemented by the log4j2 builder and write through the same engine;
 * their supplier arguments are the {@code java.util.function.Supplier} of the
 * SLF4J API, distinct from the log4j2 {@code org.apache.logging.log4j.util.Supplier}
 * used by the log4j2 overloads.</p>
 */
public interface LogBuilder extends org.slf4j.spi.LoggingEventBuilder {

    /** Inert builder that drops every call. */
    LogBuilder NOOP = new NoopLogBuilder();

    LogBuilder withMarker(Marker marker);

    LogBuilder withThrowable(Throwable throwable);

    LogBuilder withLocation();

    LogBuilder withLocation(StackTraceElement location);

    void log(CharSequence message);

    void log(String message);

    void log(String message, Object... params);

    void log(String message, Supplier<?>... paramSuppliers);

    void log(Message message);

    void log(Supplier<Message> messageSupplier);

    Message logAndGet(Supplier<Message> messageSupplier);

    void log(Object message);

    void log(String message, Object p0);

    void log(String message, Object p0, Object p1);

    void log(String message, Object p0, Object p1, Object p2);

    void log(String message, Object p0, Object p1, Object p2, Object p3);

    void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4);

    void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

    void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

    void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7);

    void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8);

    void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9);

    void log();

    /** Inert implementation backing {@link #NOOP}. */
    final class NoopLogBuilder implements LogBuilder {

        @Override
        public LogBuilder withMarker(Marker marker) {
            return this;
        }

        @Override
        public LogBuilder withThrowable(Throwable throwable) {
            return this;
        }

        @Override
        public LogBuilder withLocation() {
            return this;
        }

        @Override
        public LogBuilder withLocation(StackTraceElement location) {
            return this;
        }

        @Override
        public LogBuilder setCause(Throwable cause) {
            return this;
        }

        @Override
        public LogBuilder addMarker(org.slf4j.Marker marker) {
            return this;
        }

        @Override
        public LogBuilder addArgument(Object p) {
            return this;
        }

        @Override
        public LogBuilder addArgument(java.util.function.Supplier<?> objectSupplier) {
            return this;
        }

        @Override
        public LogBuilder addKeyValue(String key, Object value) {
            return this;
        }

        @Override
        public LogBuilder addKeyValue(String key, java.util.function.Supplier<Object> valueSupplier) {
            return this;
        }

        @Override
        public LogBuilder setMessage(String message) {
            return this;
        }

        @Override
        public LogBuilder setMessage(java.util.function.Supplier<String> messageSupplier) {
            return this;
        }

        @Override
        public void log(java.util.function.Supplier<String> messageSupplier) {
        }

        @Override
        public void log(CharSequence message) {
        }

        @Override
        public void log(String message) {
        }

        @Override
        public void log(String message, Object... params) {
        }

        @Override
        public void log(String message, Supplier<?>... paramSuppliers) {
        }

        @Override
        public void log(Message message) {
        }

        @Override
        public void log(Supplier<Message> messageSupplier) {
        }

        @Override
        public Message logAndGet(Supplier<Message> messageSupplier) {
            return messageSupplier == null ? null : messageSupplier.get();
        }

        @Override
        public void log(Object message) {
        }

        @Override
        public void log(String message, Object p0) {
        }

        @Override
        public void log(String message, Object p0, Object p1) {
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2) {
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3) {
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        }

        @Override
        public void log(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        }

        @Override
        public void log() {
        }
    }
}
