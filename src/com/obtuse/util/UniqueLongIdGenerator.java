/*
 Copyright © 2014 Daniel Boulet
 */

package com.obtuse.util;

/**
 * Generate unique long ids within some id-space.
 */

public interface UniqueLongIdGenerator {

    /**
     * Generate a long id which is different than any other ids generated by separate invocations of this method on
     * this instance.
     * <p/>Implementations of this method should be thread safe.
     * @return a long id which is unique from the perspective of this instance.
     */

    long getUniqueId();

}
