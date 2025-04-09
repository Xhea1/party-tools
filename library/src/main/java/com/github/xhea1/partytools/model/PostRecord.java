package com.github.xhea1.partytools.model;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

/**
 * Record class to represent a single post-entry.
 * This class contains only top-level fields from the `posts` array.
 */
@NullMarked
public record PostRecord(
        int fileId,
        String id,
        String user,
        String service,
        String title,
        String published,
        String substring,
        Optional<FileRecord> file,
        List<FileRecord> attachments
) {}

