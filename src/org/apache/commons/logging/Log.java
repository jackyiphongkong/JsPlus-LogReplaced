package org.apache.commons.logging;

/**
 * Commons Logging 1.2 {@code Log} interface - signature-compatible replacement.
 *
 * <p>The official interface declares 18 methods: the {@code debug/error/fatal/
 * info/trace/warn} family with {@code (Object)} and {@code (Object,Throwable)}
 * shapes plus the six {@code isXEnabled()} probes. This replacement derives
 * the whole {@code Object}/{@code String} surface from {@link org.slf4j.Logger}
 * (a JsPlus extension superset) and declares the three methods that are unique
 * to Commons Logging: {@code fatal(Object)}, {@code fatal(Object,Throwable)}
 * and {@code isFatalEnabled()}.</p>
 */
public interface Log extends org.slf4j.Logger {

    /** Log a fatal message (rendered at the engine's ERROR level). */
    void fatal(Object message);

    /** Log a fatal message with a cause (rendered at the engine's ERROR level). */
    void fatal(Object message, Throwable t);

    /** @return {@code true} when fatal logging is enabled */
    boolean isFatalEnabled();
}
