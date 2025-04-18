package com.github.xhea1.partytools.model;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Record class to represent a single post-entry.
 * This class contains only top-level fields from the `posts` array.
 * Some fields are optional, as they may not be present in all posts.
 */
@NullMarked
public record PostRecord(
        OptionalInt fileId,
        String id,
        String user,
        String service,
        String title,
        String published,
        Optional<String> substring,
        Optional<FileRecord> file,
        List<FileRecord> attachments
) {}

