package com.northgate.ratings;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Request values written to the log must come out byte for byte as they went in. A log4j2
 * that still expands {@code ${...}} lookups in messages renders this differently.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RatingsLookupLoggingTest {

    private static final String LOGGER = "com.northgate.ratings.controller.RatingsController";

    @Autowired
    private MockMvc mvc;

    private CapturingAppender appender;
    private LoggerConfig loggerConfig;

    @BeforeEach
    void attachAppender() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        appender = new CapturingAppender();
        appender.start();
        loggerConfig = context.getConfiguration().getLoggerConfig(LOGGER);
        loggerConfig.addAppender(appender, null, null);
        context.updateLoggers();
    }

    @AfterEach
    void detachAppender() {
        loggerConfig.removeAppender(appender.getName());
        appender.stop();
    }

    @Test
    void requestValuesAreLoggedLiterallyWithoutLookupExpansion() throws Exception {
        String payload = "${java:version}";

        mvc.perform(get("/api/ratings/{issuerId}", payload).param("requestedBy", payload))
                .andExpect(status().isNotFound());

        assertThat(appender.lines).contains("rating lookup issuer=" + payload + " requestedBy=" + payload);
    }

    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> lines = new CopyOnWriteArrayList<>();

        CapturingAppender() {
            super("lookup-capture", null,
                    PatternLayout.newBuilder().withPattern("%m").build(),
                    true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            lines.add(new String(getLayout().toByteArray(event), StandardCharsets.UTF_8));
        }
    }
}
