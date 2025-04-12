package com.github.xhea1.party.app.util;

import com.github.xhea1.partytools.model.CreatorRecord;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for formatting tables.
 */
public class TableFormatter {

    private TableFormatter() {
    }

    /**
     * Formats a list of CreatorRecords into a table string.
     * <p>
     * If the input list is empty, returns "No creators to display."
     *
     * @param creators List of CreatorRecord objects
     * @return Formatted table string
     */
    public static String formatCreators(Collection<CreatorRecord> creators) {
        if (creators.isEmpty()) return "No creators to display.";

        // headers
        List<String> headers = List.of("ID", "Name", "Service", "Updated");

        // formatters
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // rows as string lists
        List<List<String>> rows = creators.stream()
                .map(c -> List.of(c.id(), c.name(), c.service(),
                                  dtf.format(LocalDateTime.ofInstant(c.updated(), ZoneOffset.UTC))))
                .collect(Collectors.toList());

        // add headers
        rows.addFirst(headers);

        // calculate column widths
        int[] colWidths = new int[headers.size()];
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                colWidths[i] = Math.max(colWidths[i], row.get(i)
                        .length());
            }
        }

        // separator line
        String sep = "+" + Arrays.stream(colWidths)
                .mapToObj(w -> "-".repeat(w + 2))
                .collect(Collectors.joining("+")) + "+";

        // build table
        StringBuilder sb = new StringBuilder();
        sb.append(sep)
                .append("\n");

        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            sb.append("|");
            for (int i = 0; i < row.size(); i++) {
                sb.append(" ")
                        .append(padRight(row.get(i), colWidths[i]))
                        .append(" |");
            }
            sb.append("\n");
            if (r == 0 || r == rows.size() - 1) sb.append(sep)
                    .append("\n");
        }

        return sb.toString();
    }

    /**
     * Pads a string to the right with spaces, up to a maximum length.
     *
     * @param s Input string
     * @param n Maximum length
     * @return Padded string
     */
    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
}


