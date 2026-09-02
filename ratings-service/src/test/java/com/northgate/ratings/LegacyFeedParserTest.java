package com.northgate.ratings;

import java.util.List;

import com.northgate.ratings.domain.Rating;
import com.northgate.ratings.feed.LegacyFeedParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacyFeedParserTest {

    private static final String FEED = "<feed><issuer><id>NG-2001</id><name>Ivelet Rail</name>"
            + "<grade>BBB</grade><outlook>stable</outlook><sector>Transport</sector>"
            + "<reviewed>2025-06-02</reviewed></issuer></feed>";

    private static final String XXE_FEED = "<!DOCTYPE feed [<!ENTITY xxe SYSTEM \"file:///etc/hostname\">]>"
            + "<feed><issuer><id>NG-2001</id><name>&xxe;</name></issuer></feed>";

    @Test
    void parsesTheOvernightFeed() {
        List<Rating> ratings = new LegacyFeedParser().parse(FEED);
        assertEquals(1, ratings.size());
        assertEquals("NG-2001", ratings.get(0).getIssuerId());
        assertEquals("Ivelet Rail", ratings.get(0).getIssuerName());
    }

    @Test
    void rejectsExternalEntitiesWhenParsing() {
        assertThrows(IllegalArgumentException.class, () -> new LegacyFeedParser().parse(XXE_FEED));
    }

    @Test
    void rejectsExternalEntitiesWhenNormalising() {
        assertThrows(IllegalArgumentException.class, () -> new LegacyFeedParser().echoNormalised(XXE_FEED));
    }

    @Test
    void normalisesTheOvernightFeed() {
        String normalised = new LegacyFeedParser().echoNormalised(FEED);
        assertFalse(normalised.isEmpty());
        assertEquals(1, new LegacyFeedParser().parse(normalised).size());
    }
}
