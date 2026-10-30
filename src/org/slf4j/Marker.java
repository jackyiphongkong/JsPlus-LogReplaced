package org.slf4j;

import java.io.Serializable;
import java.util.Iterator;

/**
 * SLF4J 1.7 marker interface - signature-compatible replacement.
 *
 * <p>Replaces {@code org.slf4j.Marker} so that code compiled against the
 * official SLF4J API can link against this jar. Markers carry a name and an
 * optional parent/child relation; the JsPlus-LogReplaced implementation uses
 * the marker name as a message prefix (see {@code LoggerClass}), because the
 * JsPlus logging engine has no marker filtering concept of its own.</p>
 */
public interface Marker extends Serializable {

    /** Marker name reserved for "any marker". */
    String ANY_MARKER = "*";

    /** Marker name reserved for "any non-null marker". */
    String ANY_NON_NULL_MARKER = "+";

    /** @return the marker name */
    String getName();

    /** Add a marker as a child (reference) of this marker. */
    void add(Marker reference);

    /** Remove a child marker; returns {@code true} when it was present. */
    boolean remove(Marker reference);

    /** @return {@code true} when this marker has at least one child */
    boolean hasChildren();

    /** @return {@code true} when this marker has at least one child */
    boolean hasReferences();

    /** @return an iterator over the child markers */
    Iterator<Marker> iterator();

    /** @return {@code true} when {@code other} is this marker or a descendant */
    boolean contains(Marker other);

    /** @return {@code true} when a marker with the given name is this marker or a descendant */
    boolean contains(String name);

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
}
