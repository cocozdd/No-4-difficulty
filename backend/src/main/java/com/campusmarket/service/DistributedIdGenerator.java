package com.campusmarket.service;

/**
 * Generates globally unique identifiers that are safe to use across service instances.
 */
public interface DistributedIdGenerator {

    /**
     * Generates the next numeric identifier for the given business tag.
     *
     * @param businessTag logical namespace for the ID (orders, payments, etc.)
     * @return a positive 64-bit identifier
     */
    long nextId(String businessTag);

    /**
     * Generates a human friendly Base36 string identifier for the given business tag.
     *
     * @param businessTag logical namespace for the ID
     * @return upper-case Base36 representation of the generated ID
     */
    default String nextIdAsString(String businessTag) {
        return Long.toString(nextId(businessTag), 36).toUpperCase();
    }
}
