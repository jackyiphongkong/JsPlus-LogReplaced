package org.apache.log4j.spi;

/**
 * Log4j 1.2 {@code Filter} - signature-compatible replacement.
 *
 * <p>Filters are accepted but not evaluated: the replacement layer sends every
 * event straight to the JsPlus engine, so {@link #decide(LoggingEvent)} is
 * abstract for API compatibility only.</p>
 */
public abstract class Filter implements OptionHandler {

    public Filter next;

    public static final int DENY = -1;
    public static final int NEUTRAL = 0;
    public static final int ACCEPT = 1;

    public Filter() {
    }

    @Override
    public void activateOptions() {
    }

    /** Decide what to do with the event. */
    public abstract int decide(LoggingEvent event);

    public void setNext(Filter next) {
        this.next = next;
    }

    public Filter getNext() {
        return next;
    }
}
