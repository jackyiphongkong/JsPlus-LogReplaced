package org.apache.logging.log4j;

import java.io.Serializable;

/**
 * Log4j2 {@code Marker} - signature-compatible replacement.
 */
public interface Marker extends Serializable {

    Marker addParents(Marker... parents);

    @Override
    boolean equals(Object obj);

    String getName();

    Marker[] getParents();

    @Override
    int hashCode();

    boolean hasParents();

    boolean isInstanceOf(Marker marker);

    boolean isInstanceOf(String name);

    boolean remove(Marker parent);

    Marker setParents(Marker... parents);
}
