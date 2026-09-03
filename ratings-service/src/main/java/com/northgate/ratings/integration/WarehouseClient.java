package com.northgate.ratings.integration;

import java.nio.charset.StandardCharsets;

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
 * Talks to the ratings warehouse. Credentials come from the ratings-warehouse Secret via
 * NORTHGATE_WAREHOUSE_USER / _PASSWORD / _API_TOKEN; none are held in source.
 */
@Component
public class WarehouseClient {

    private static final Logger LOG = LogManager.getLogger(WarehouseClient.class);

    private final String baseUrl;
    private final String user;
    private final String password;
    private final String apiToken;

    public WarehouseClient(@Value("${northgate.warehouse.base-url:http://10.42.8.15:8081}") String baseUrl,
                           @Value("${northgate.warehouse.user:}") String user,
                           @Value("${northgate.warehouse.password:}") String password,
                           @Value("${northgate.warehouse.api-token:}") String apiToken) {
        this.baseUrl = baseUrl;
        this.user = user;
        this.password = password;
        this.apiToken = apiToken;
        if (user.isEmpty() || password.isEmpty() || apiToken.isEmpty()) {
            LOG.warn("warehouse credentials are not fully configured; lookups will be unauthenticated");
        }
    }

    public String fetchIssuerDossier(String issuerId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(baseUrl + "/warehouse/issuers/" + issuerId);
            if (!user.isEmpty() && !password.isEmpty()) {
                get.addHeader("Authorization", "Basic " + basicAuth());
            }
            if (!apiToken.isEmpty()) {
                get.addHeader("X-Api-Token", apiToken);
            }
            try (CloseableHttpResponse response = client.execute(get)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOG.warn("warehouse lookup failed for " + issuerId + ": " + e.getMessage());
            return null;
        }
    }

    private String basicAuth() {
        String pair = user + ":" + password;
        return new String(Base64.encodeBase64(pair.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
}
