package com.northgate.ratings.controller;

import java.util.List;

import com.northgate.ratings.domain.Rating;
import com.northgate.ratings.feed.LegacyFeedParser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final LegacyFeedParser parser;

    public FeedController(LegacyFeedParser parser) {
        this.parser = parser;
    }

    @PostMapping(value = "/xml", consumes = MediaType.APPLICATION_XML_VALUE)
    public List<Rating> ingest(@RequestBody String xml) {
        return parser.parse(xml);
    }

    @PostMapping(value = "/xml/normalise", consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public String normalise(@RequestBody String xml) {
        return parser.echoNormalised(xml);
    }
}
