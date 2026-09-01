package com.northgate.ratings.repository;

import java.util.List;

import com.northgate.ratings.domain.Rating;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RatingsRepository {

    private final JdbcTemplate jdbc;

    public RatingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Rating> MAPPER = (rs, rowNum) -> new Rating(
            rs.getString("issuer_id"),
            rs.getString("issuer_name"),
            rs.getString("grade"),
            rs.getString("outlook"),
            rs.getString("sector"),
            rs.getString("last_reviewed"));

    public Rating findByIssuerId(String issuerId) {
        String sql = "SELECT * FROM ratings WHERE issuer_id = '" + issuerId + "'";
        List<Rating> rows = jdbc.query(sql, MAPPER);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Rating> search(String namePattern, String sector) {
        String sql = "SELECT * FROM ratings WHERE issuer_name LIKE '%" + namePattern + "%'";
        if (sector != null && !sector.isEmpty()) {
            sql = sql + " AND sector = '" + sector + "'";
        }
        return jdbc.query(sql + " ORDER BY issuer_name", MAPPER);
    }

    public List<Rating> findByGrades(String gradeCsv) {
        String sql = "SELECT * FROM ratings WHERE grade IN (" + quoteCsv(gradeCsv) + ")";
        return jdbc.query(sql, MAPPER);
    }

    public int updateGrade(String issuerId, String grade, String outlook) {
        String sql = "UPDATE ratings SET grade = '" + grade + "', outlook = '" + outlook
                + "', last_reviewed = CURRENT_DATE WHERE issuer_id = '" + issuerId + "'";
        return jdbc.update(sql);
    }

    public int countBySector(String sector) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ratings WHERE sector = '" + sector + "'",
                Integer.class);
    }

    private static String quoteCsv(String csv) {
        StringBuilder sb = new StringBuilder();
        for (String part : csv.split(",")) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append('\'').append(part.trim()).append('\'');
        }
        return sb.toString();
    }
}
