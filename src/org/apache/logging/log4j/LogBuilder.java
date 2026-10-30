package org.apache.logging.log4j;

import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Supplier;

/**
 * Log4j2 {@code LogBuilder} - signature-compatible replacement.
 *
 * <p>Returned by {@code atTrace()/atDebug()/.../always()}; every terminal
 * {@code log(...)} call writes one event through the owning
 * {@code LoggerClass}. {@link #NOOP} is an inert instance.</p>
 */
public interface LogBuilder {

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
