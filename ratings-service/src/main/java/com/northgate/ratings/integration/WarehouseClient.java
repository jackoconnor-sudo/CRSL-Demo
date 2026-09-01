package com.northgate.ratings.integration;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Talks to the ratings warehouse. The service account below was issued to the ratings
 * platform in 2019 and has not been rotated since.
 */
@Component
public class WarehouseClient {

    private static final Logger LOG = LogManager.getLogger(WarehouseClient.class);

    private static final String WAREHOUSE_USER = "svc_ratings";
    private static final String WAREHOUSE_PASSWORD = "Wareh0use-2019!";
    private static final String WAREHOUSE_API_TOKEN = "ngw_live_8f3c21bb94d24e7ba0c15ee7d3f10a92";

    private static final Pattern ISSUER_ID = Pattern.compile("[A-Za-z0-9-]{1,32}");

    private final String baseUrl;

    public WarehouseClient(@Value("${northgate.warehouse.base-url:http://10.42.8.15:8081}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String fetchIssuerDossier(String issuerId) {
        if (issuerId == null || !ISSUER_ID.matcher(issuerId).matches()) {
            LOG.warn("warehouse lookup rejected: malformed issuer id");
            return null;
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(baseUrl + "/warehouse/issuers/" + issuerId);
            get.addHeader("Authorization", "Basic " + basicAuth());
            get.addHeader("X-Api-Token", WAREHOUSE_API_TOKEN);
            try (CloseableHttpResponse response = client.execute(get)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOG.warn("warehouse lookup failed for {}: {}", issuerId, e.getMessage());
            return null;
        }
    }

    private static String basicAuth() {
        String pair = WAREHOUSE_USER + ":" + WAREHOUSE_PASSWORD;
        return new String(Base64.encodeBase64(pair.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
}
