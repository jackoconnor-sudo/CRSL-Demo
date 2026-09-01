package com.northgate.ratings.report;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds the SQL for the desk's nightly reports.
 *
 * Every fragment of the statement is either a constant, a value of {@link Dimension} or
 * {@link Window}, or a column name that has been matched against {@link #ALLOWED_COLUMNS}
 * before it reaches the builder. Nothing supplied by a caller is interpolated as text.
 */
public final class ReportQueryBuilder {

    public enum Dimension {
        SECTOR("sector"),
        GRADE("grade"),
        OUTLOOK("outlook");

        private final String column;

        Dimension(String column) {
            this.column = column;
        }

        String column() {
            return column;
        }
    }

    public enum Window {
        LAST_30_DAYS("last_reviewed >= DATEADD('DAY', -30, CURRENT_DATE)"),
        LAST_QUARTER("last_reviewed >= DATEADD('MONTH', -3, CURRENT_DATE)"),
        ALL_TIME("1 = 1");

        private final String predicate;

        Window(String predicate) {
            this.predicate = predicate;
        }

        String predicate() {
            return predicate;
        }
    }

    private static final Set<String> ALLOWED_COLUMNS = new LinkedHashSet<>(
            Arrays.asList("issuer_id", "issuer_name", "grade", "outlook", "sector", "last_reviewed"));

    private ReportQueryBuilder() {
    }

    public static String rollup(Dimension dimension, Window window) {
        return "SELECT " + dimension.column() + ", COUNT(*) AS n FROM ratings WHERE "
                + window.predicate() + " GROUP BY " + dimension.column()
                + " ORDER BY n DESC";
    }

    public static String projection(String[] requestedColumns, Window window) {
        StringBuilder columns = new StringBuilder();
        for (String requested : requestedColumns) {
            String column = requested == null ? "" : requested.trim().toLowerCase();
            if (!ALLOWED_COLUMNS.contains(column)) {
                throw new IllegalArgumentException("column not available in reports: " + requested);
            }
            if (columns.length() > 0) {
                columns.append(", ");
            }
            columns.append(column);
        }
        if (columns.length() == 0) {
            columns.append("issuer_id");
        }
        return "SELECT " + columns + " FROM ratings WHERE " + window.predicate();
    }

    public static String coverage(Dimension dimension) {
        String column = dimension.column();
        return "SELECT " + column + ", COUNT(*) AS n, MAX(last_reviewed) AS newest FROM ratings"
                + " GROUP BY " + column;
    }
}
