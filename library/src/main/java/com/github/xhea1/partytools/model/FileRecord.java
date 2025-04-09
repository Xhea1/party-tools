package com.github.xhea1.partytools.model;

import org.jspecify.annotations.NullMarked;

/**
 * Saves data about a file.
 *
 * @param name file name
 * @param path path to the file on the server
 */
@NullMarked
public record FileRecord(String name, String path) {
}
