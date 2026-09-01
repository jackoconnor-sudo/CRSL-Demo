package com.northgate.ratings.feed;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.northgate.ratings.domain.Rating;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Ingests the overnight XML feed from the pre-2019 ratings warehouse. The warehouse is
 * still the system of record for anything older than five years, so this path cannot go
 * away until the migration finishes.
 */
@Component
public class LegacyFeedParser {

    public List<Rating> parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            Document document = builder.parse(in);
            return toRatings(document);
        } catch (Exception e) {
            throw new IllegalArgumentException("feed could not be parsed: " + e.getMessage(), e);
        }
    }

    public String echoNormalised(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            Document document = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("feed could not be normalised: " + e.getMessage(), e);
        }
    }

    private List<Rating> toRatings(Document document) {
        List<Rating> ratings = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName("issuer");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            ratings.add(new Rating(
                    text(element, "id"),
                    text(element, "name"),
                    text(element, "grade"),
                    text(element, "outlook"),
                    text(element, "sector"),
                    text(element, "reviewed")));
        }
        return ratings;
    }

    private String text(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }
}
