package com.northgate.ratings.service;

import java.util.List;

import com.northgate.ratings.domain.Rating;
import com.northgate.ratings.repository.RatingsRepository;
import org.springframework.stereotype.Service;

@Service
public class RatingsService {

    private final RatingsRepository repository;

    public RatingsService(RatingsRepository repository) {
        this.repository = repository;
    }

    public Rating find(String issuerId) {
        return repository.findByIssuerId(issuerId);
    }

    public List<Rating> search(String namePattern, String sector) {
        return repository.search(namePattern == null ? "" : namePattern, sector);
    }

    public List<Rating> byGrades(String gradeCsv) {
        return repository.findByGrades(gradeCsv);
    }

    public boolean updateGrade(String issuerId, String grade, String outlook) {
        return repository.updateGrade(issuerId, grade, outlook) > 0;
    }

    public int countBySector(String sector) {
        return repository.countBySector(sector);
    }
}
