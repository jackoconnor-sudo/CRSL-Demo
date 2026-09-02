package com.northgate.ratings.feed;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
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
            DocumentBuilder builder = secureDocumentBuilderFactory().newDocumentBuilder();
            InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
            Document document = builder.parse(in);
            return toRatings(document);
        } catch (Exception e) {
            throw new IllegalArgumentException("feed could not be parsed: " + e.getMessage(), e);
        }
    }

    public String echoNormalised(String xml) {
        try {
            Document document = secureDocumentBuilderFactory().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            Transformer transformer = secureTransformerFactory().newTransformer();
            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("feed could not be normalised: " + e.getMessage(), e);
        }
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static TransformerFactory secureTransformerFactory() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
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
