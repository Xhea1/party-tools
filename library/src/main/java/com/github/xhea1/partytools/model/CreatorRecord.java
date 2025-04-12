package com.github.xhea1.partytools.model;

import java.time.Instant;

/**
 * Saves data about a creator.
 *
 * @param id ID of the creator
 * @param name name of the creator
 * @param service service used by the creator
 * @param updated when the creator was last updated
 */
public record CreatorRecord(String id, String name, String service, Instant updated) {
}
