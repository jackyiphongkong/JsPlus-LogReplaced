package org.apache.logging.log4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Log4j2 {@code MarkerManager} - signature-compatible replacement.
 *
 * <p>Markers are cached by name. Parents are recorded for {@code hasParents()}
 * and {@code isInstanceOf(...)}, but the JsPlus engine only uses the marker
 * name as a message prefix.</p>
 */
public final class MarkerManager {

    private static final Map<String, Marker> MARKERS = new ConcurrentHashMap<String, Marker>();

    private MarkerManager() {
    }

    public static void clear() {
        MARKERS.clear();
    }

    public static boolean exists(String name) {
        return MARKERS.containsKey(name);
    }

    public static Marker getMarker(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Marker name cannot be null");
        }
        Marker marker = MARKERS.get(name);
        if (marker == null) {
            marker = new SimpleMarker(name);
            Marker existing = MARKERS.putIfAbsent(name, marker);
            if (existing != null) {
                marker = existing;
            }
        }
        return marker;
    }

    public static Marker getMarker(String name, String... parentNames) {
        Marker marker = getMarker(name);
        if (parentNames != null) {
            List<Marker> parents = new ArrayList<Marker>();
            for (String parentName : parentNames) {
                parents.add(getMarker(parentName));
            }
            marker.setParents(parents.toArray(new Marker[parents.size()]));
        }
        return marker;
    }

    public static Marker getMarker(String name, Marker parent) {
        return getMarker(name).addParents(parent);
    }

    private static final class SimpleMarker implements Marker {

        private static final long serialVersionUID = 1L;

        private final String name;
        private volatile Marker[] parents = new Marker[0];

        SimpleMarker(String name) {
            this.name = name;
        }

        @Override
        public Marker addParents(Marker... newParents) {
            if (newParents == null || newParents.length == 0) {
                return this;
            }
            List<Marker> merged = new ArrayList<Marker>();
            for (Marker m : parents) {
                merged.add(m);
            }
            for (Marker m : newParents) {
                if (m != null && !merged.contains(m)) {
                    merged.add(m);
                }
            }
            parents = merged.toArray(new Marker[merged.size()]);
            return this;
        }

        @Override
        public Marker setParents(Marker... newParents) {
            parents = newParents == null ? new Marker[0] : newParents.clone();
            return this;
        }

        @Override
        public Marker[] getParents() {
            return parents.clone();
        }

        @Override
        public boolean hasParents() {
            return parents.length > 0;
        }

        @Override
        public boolean isInstanceOf(Marker marker) {
            if (marker == null) {
                return false;
            }
            if (this.equals(marker)) {
                return true;
            }
            for (Marker parent : parents) {
                if (parent.isInstanceOf(marker)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isInstanceOf(String markerName) {
            if (name.equals(markerName)) {
                return true;
            }
            for (Marker parent : parents) {
                if (parent.isInstanceOf(markerName)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean remove(Marker parent) {
            List<Marker> kept = new ArrayList<Marker>();
            for (Marker m : parents) {
                if (!m.equals(parent)) {
                    kept.add(m);
                }
            }
            boolean removed = kept.size() != parents.length;
            parents = kept.toArray(new Marker[kept.size()]);
            return removed;
        }

        @Override
        public String getName() {
            return name;
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
