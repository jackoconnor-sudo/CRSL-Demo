package com.northgate.ratings;

import java.util.List;

import com.northgate.ratings.domain.Rating;
import com.northgate.ratings.feed.LegacyFeedParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyFeedParserTest {

    private static final String FEED = "<feed><issuer><id>NG-2001</id><name>Ivelet Rail</name>"
            + "<grade>BBB</grade><outlook>stable</outlook><sector>Transport</sector>"
            + "<reviewed>2025-06-02</reviewed></issuer></feed>";

    @Test
    void parsesTheOvernightFeed() {
        List<Rating> ratings = new LegacyFeedParser().parse(FEED);
        assertEquals(1, ratings.size());
        assertEquals("NG-2001", ratings.get(0).getIssuerId());
        assertEquals("Ivelet Rail", ratings.get(0).getIssuerName());
    }
}
