package org.slf4j.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * JsPlus implementation of {@link LoggingEventBuilder} (SLF4J 2.0 fluent API).
 *
 * <p>The builder accumulates markers, arguments, key-value pairs, a message and
 * a cause, then writes exactly one event on the terminal {@code log(...)} call.
 * Rendering follows the project's documented behavioural mapping:</p>
 * <ul>
 *   <li>markers become the message prefix {@code "<markerName> - "} (decision
 *       D3, same as every Marker overload of {@code LoggerClass});</li>
 *   <li>key-value pairs become the official prefix {@code "key=value "}
 *       (same rendering as the official {@code DefaultLoggingEventBuilder});</li>
 *   <li>{@code {}} placeholders are expanded eagerly with the accumulated
 *       arguments (same {@code format} semantics as the log4j2 builder);</li>
 *   <li>suppliers ({@code addArgument(Supplier)}, {@code setMessage(Supplier)},
 *       {@code addKeyValue(String, Supplier)}) are evaluated eagerly so no call
 *       is silently dropped (decision D6).</li>
 * </ul>
 *
 * <p>{@link #NOOP} is the inert singleton returned by {@code atLevel(...)} /
 * {@code atX()} when the owning logger is disabled for that level, mirroring the
 * official {@code NOPLoggingEventBuilder}: calls on it build nothing and log
 * nothing.</p>
 */
public class LoggingEventBuilderClass implements LoggingEventBuilder {

    /** Inert builder that drops every call (disabled-level gate). */
    public static final LoggingEventBuilder NOOP = new NoopLoggingEventBuilder();

    private final org.slf4j.Logger logger;
    private final Level level;

    private String message;
    private Throwable throwable;

    private final List<Object> arguments = new ArrayList<Object>();
    private final List<Marker> markers = new ArrayList<Marker>();
    private final List<String[]> keyValues = new ArrayList<String[]>();

    public LoggingEventBuilderClass(org.slf4j.Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public LoggingEventBuilder setCause(Throwable cause) {
        this.throwable = cause;
        return this;
    }

    @Override
    public LoggingEventBuilder addMarker(Marker marker) {
        if (marker != null) {
            markers.add(marker);
        }
        return this;
    }

    @Override
    public LoggingEventBuilder addArgument(Object p) {
        arguments.add(p);
        return this;
    }

    @Override
    public LoggingEventBuilder addArgument(Supplier<?> objectSupplier) {
        arguments.add(objectSupplier == null ? null : objectSupplier.get());
        return this;
    }

    @Override
    public LoggingEventBuilder addKeyValue(String key, Object value) {
        keyValues.add(new String[] { key, text(value) });
        return this;
    }

    @Override
    public LoggingEventBuilder addKeyValue(String key, Supplier<Object> valueSupplier) {
        keyValues.add(new String[] { key, text(valueSupplier == null ? null : valueSupplier.get()) });
        return this;
    }

    @Override
    public LoggingEventBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public LoggingEventBuilder setMessage(Supplier<String> messageSupplier) {
        this.message = messageSupplier == null ? null : messageSupplier.get();
        return this;
    }

    @Override
    public void log() {
        emit();
    }

    @Override
    public void log(String message) {
        this.message = message;
        emit();
    }

    @Override
    public void log(String message, Object arg) {
        this.message = message;
        arguments.add(arg);
        emit();
    }

    @Override
    public void log(String message, Object arg0, Object arg1) {
        this.message = message;
        arguments.add(arg0);
        arguments.add(arg1);
        emit();
    }

    @Override
    public void log(String message, Object... args) {
        this.message = message;
        if (args != null) {
            for (Object a : args) {
                arguments.add(a);
            }
        }
        emit();
    }

    @Override
    public void log(Supplier<String> messageSupplier) {
        this.message = messageSupplier == null ? null : messageSupplier.get();
        emit();
    }

    /* ------------------------------------------------------------------
     * Internal rendering + dispatch.
     * ------------------------------------------------------------------ */

    private void emit() {
        String merged = mergePrefixes();
        String formatted = arguments.isEmpty() ? merged : format(merged, arguments.toArray());
        Exception ex = asException(throwable);
        switch (level) {
        case TRACE:
            if (ex != null) {
                logger.trace(formatted, ex);
            } else {
                logger.trace(formatted);
            }
            break;
        case DEBUG:
            if (ex != null) {
                logger.debug(formatted, ex);
            } else {
                logger.debug(formatted);
            }
            break;
        case INFO:
            if (ex != null) {
                logger.info(formatted, ex);
            } else {
                logger.info(formatted);
            }
            break;
        case WARN:
            if (ex != null) {
                logger.warn(formatted, ex);
            } else {
                logger.warn(formatted);
            }
            break;
        case ERROR:
            if (ex != null) {
                logger.error(formatted, ex);
            } else {
                logger.error(formatted);
            }
            break;
        default:
            throw new IllegalArgumentException("Level [" + level + "] not recognized.");
        }
    }

    /** Markers ({@code "<name> - "}) then key-value pairs ({@code "k=v "}) then the message. */
    private String mergePrefixes() {
        if (markers.isEmpty() && keyValues.isEmpty()) {
            return message == null ? "" : message;
        }
        StringBuilder sb = new StringBuilder(64);
        for (Marker m : markers) {
            sb.append(m.getName()).append(" - ");
        }
        for (String[] kv : keyValues) {
            sb.append(kv[0]).append('=').append(kv[1]).append(' ');
        }
        sb.append(message == null ? "" : message);
        return sb.toString();
    }

    /** Expand {@code {}} placeholders with the given arguments (project rule). */
    private static String format(String pattern, Object[] args) {
        if (pattern == null) {
            return "null";
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        StringBuilder sb = new StringBuilder(pattern.length() + 32);
        int argIndex = 0;
        int cursor = 0;
        while (cursor < pattern.length()) {
            int hit = pattern.indexOf("{}", cursor);
            if (hit < 0 || argIndex >= args.length) {
                sb.append(pattern.substring(cursor));
                break;
            }
            sb.append(pattern, cursor, hit);
            sb.append(text(args[argIndex++]));
            cursor = hit + 2;
        }
        return sb.toString();
    }

    private static String text(Object value) {
        return value == null ? "null" : value.toString();
    }

    private static Exception asException(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof Exception) {
            return (Exception) throwable;
        }
        return new RuntimeException(throwable);
    }

    /** Inert implementation backing {@link #NOOP}. */
    private static final class NoopLoggingEventBuilder implements LoggingEventBuilder {

        @Override
        public LoggingEventBuilder setCause(Throwable cause) {
            return this;
        }

        @Override
        public LoggingEventBuilder addMarker(Marker marker) {
            return this;
        }

        @Override
        public LoggingEventBuilder addArgument(Object p) {
            return this;
        }

        @Override
        public LoggingEventBuilder addArgument(Supplier<?> objectSupplier) {
            return this;
        }

        @Override
        public LoggingEventBuilder addKeyValue(String key, Object value) {
            return this;
        }

        @Override
        public LoggingEventBuilder addKeyValue(String key, Supplier<Object> valueSupplier) {
            return this;
        }

        @Override
        public LoggingEventBuilder setMessage(String message) {
            return this;
        }

        @Override
        public LoggingEventBuilder setMessage(Supplier<String> messageSupplier) {
            return this;
        }

        @Override
        public void log() {
        }

        @Override
        public void log(String message) {
        }

        @Override
        public void log(String message, Object arg) {
        }

        @Override
        public void log(String message, Object arg0, Object arg1) {
        }

        @Override
        public void log(String message, Object... args) {
        }

        @Override
        public void log(Supplier<String> messageSupplier) {
        }
    }
}
