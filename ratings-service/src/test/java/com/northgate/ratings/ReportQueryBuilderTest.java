package com.northgate.ratings;

import com.northgate.ratings.report.ReportQueryBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportQueryBuilderTest {

    @Test
    void rollupUsesTheEnumColumn() {
        String sql = ReportQueryBuilder.rollup(ReportQueryBuilder.Dimension.SECTOR,
                ReportQueryBuilder.Window.ALL_TIME);
        assertTrue(sql.startsWith("SELECT sector, COUNT(*)"));
    }

    @Test
    void projectionAcceptsWhitelistedColumns() {
        String sql = ReportQueryBuilder.projection(new String[] {"issuer_id", " GRADE "},
                ReportQueryBuilder.Window.ALL_TIME);
        assertEquals("SELECT issuer_id, grade FROM ratings WHERE 1 = 1", sql);
    }

    @Test
    void projectionRejectsAnythingElse() {
        assertThrows(IllegalArgumentException.class, () -> ReportQueryBuilder.projection(
                new String[] {"issuer_id) UNION SELECT password FROM users --"},
                ReportQueryBuilder.Window.ALL_TIME));
    }
}
