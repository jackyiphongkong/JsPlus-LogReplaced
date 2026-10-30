package org.slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * SLF4J 1.7 marker factory - signature-compatible replacement.
 *
 * <p>Keeps a small in-memory registry so that {@link #getMarker(String)} is
 * stable for a given name. Markers returned by this factory are lightweight
 * (name plus parent/child links); the JsPlus logging engine only consumes the
 * name as a message prefix.</p>
 */
public class MarkerFactory {

    private static volatile IMarkerFactory MARKER_FACTORY = new SimpleMarkerFactory();

    private MarkerFactory() {
    }

    /** Return the (shared) marker with the given name. */
    public static Marker getMarker(String name) {
        return MARKER_FACTORY.getMarker(name);
    }

    /** Return a marker that is not registered with the shared factory. */
    public static Marker getDetachedMarker(String name) {
        return MARKER_FACTORY.getDetachedMarker(name);
    }

    /** Return the marker factory backing the static helper methods. */
    public static IMarkerFactory getIMarkerFactory() {
        return MARKER_FACTORY;
    }

    /** Replace the marker factory (kept for API compatibility). */
    public static void setIMarkerFactory(IMarkerFactory factory) {
        MARKER_FACTORY = factory;
    }

    private static final class SimpleMarkerFactory implements IMarkerFactory {
        private final Map<String, Marker> markers = new HashMap<String, Marker>();

        @Override
        public synchronized Marker getMarker(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Marker name cannot be null");
            }
            Marker marker = markers.get(name);
            if (marker == null) {
                marker = new SimpleMarker(name);
                markers.put(name, marker);
            }
            return marker;
        }

        @Override
        public synchronized boolean exists(String name) {
            return markers.containsKey(name);
        }

        @Override
        public synchronized boolean detachMarker(String name) {
            return markers.remove(name) != null;
        }

        @Override
        public Marker getDetachedMarker(String name) {
            return new SimpleMarker(name);
        }
    }

    private static final class SimpleMarker implements Marker {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final List<Marker> references = new ArrayList<Marker>();

        SimpleMarker(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public synchronized void add(Marker reference) {
            if (reference == null) {
                return;
            }
            for (Marker m : references) {
                if (m.equals(reference)) {
                    return;
                }
            }
            references.add(reference);
        }

        @Override
        public synchronized boolean remove(Marker reference) {
            return references.remove(reference);
        }

        @Override
        public synchronized boolean hasChildren() {
            return !references.isEmpty();
        }

        @Override
        public synchronized boolean hasReferences() {
            return !references.isEmpty();
        }

        @Override
        public synchronized Iterator<Marker> iterator() {
            return new ArrayList<Marker>(references).iterator();
        }

        @Override
        public synchronized boolean contains(Marker other) {
            if (other == null) {
                return false;
            }
            if (this.equals(other)) {
                return true;
            }
            for (Marker m : references) {
                if (m.contains(other)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized boolean contains(String name) {
            if (this.name.equals(name)) {
                return true;
            }
            for (Marker m : references) {
                if (m.contains(name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Marker)) {
                return false;
            }
            return name.equals(((Marker) obj).getName());
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
