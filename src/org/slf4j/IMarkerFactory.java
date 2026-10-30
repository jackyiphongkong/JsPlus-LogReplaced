package org.slf4j;

/**
 * SLF4J 1.7 marker factory interface - signature-compatible replacement.
 */
public interface IMarkerFactory {

    /** Return the marker with the given name, creating it when absent. */
    Marker getMarker(String name);

    /** @return {@code true} when a marker with this name has been created */
    boolean exists(String name);

    /** Detach (forget) the marker with the given name. */
    boolean detachMarker(String name);

    /** Return a marker that is not registered with this factory. */
    Marker getDetachedMarker(String name);
}
