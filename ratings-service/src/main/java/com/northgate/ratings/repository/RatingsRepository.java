package com.northgate.ratings.repository;

import java.util.ArrayList;
import java.util.Collections;
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
        List<Rating> rows = jdbc.query("SELECT * FROM ratings WHERE issuer_id = ?", MAPPER, issuerId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Rating> search(String namePattern, String sector) {
        if (sector != null && !sector.isEmpty()) {
            return jdbc.query(
                    "SELECT * FROM ratings WHERE issuer_name LIKE ? AND sector = ? ORDER BY issuer_name",
                    MAPPER, "%" + namePattern + "%", sector);
        }
        return jdbc.query("SELECT * FROM ratings WHERE issuer_name LIKE ? ORDER BY issuer_name",
                MAPPER, "%" + namePattern + "%");
    }

    public List<Rating> findByGrades(String gradeCsv) {
        List<Object> grades = new ArrayList<>();
        for (String part : gradeCsv.split(",")) {
            if (!part.trim().isEmpty()) {
                grades.add(part.trim());
            }
        }
        if (grades.isEmpty()) {
            return Collections.emptyList();
        }
        String placeholders = String.join(",", Collections.nCopies(grades.size(), "?"));
        return jdbc.query(String.format("SELECT * FROM ratings WHERE grade IN (%s)", placeholders),
                MAPPER, grades.toArray());
    }

    public int updateGrade(String issuerId, String grade, String outlook) {
        return jdbc.update(
                "UPDATE ratings SET grade = ?, outlook = ?, last_reviewed = CURRENT_DATE WHERE issuer_id = ?",
                grade, outlook, issuerId);
    }

    public int countBySector(String sector) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ratings WHERE sector = ?", Integer.class, sector);
    }
}
