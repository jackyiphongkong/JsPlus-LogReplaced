package org.apache.logging.log4j.util;

/**
 * Log4j2 {@code Supplier} - signature-compatible replacement.
 *
 * <p>Providers are evaluated eagerly by this replacement layer (decision D6):
 * the value is read at call time instead of being deferred to an enabled
 * level check.</p>
 */
public interface Supplier<T> {

    T get();
}
