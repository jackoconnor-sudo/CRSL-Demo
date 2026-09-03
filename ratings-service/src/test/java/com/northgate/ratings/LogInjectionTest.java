package com.northgate.ratings;

import java.io.StringWriter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LogInjectionTest {

    @Test
    void lookupsInLoggedRequestValuesAreNotResolved() {
        StringWriter out = new StringWriter();
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        WriterAppender appender = WriterAppender.newBuilder()
                .setName("capture")
                .setTarget(out)
                .setLayout(PatternLayout.newBuilder().withPattern("%m%n").build())
                .build();
        appender.start();
        Logger logger = context.getLogger("com.northgate.test.injection");
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        try {
            String hostile = "${java:version}";
            logger.info("rating lookup issuer={} requestedBy={}", hostile, "x");
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
        assertTrue(out.toString().contains("${java:version}"),
                "logged value was substituted: " + out);
    }
}
