# Java 8 runtime on a supported LTS distribution. The service only needs a JRE at runtime.
FROM eclipse-temurin:8-jre-noble

ENV APP_HOME=/opt/northgate \
    JAVA_OPTS="-Xms256m -Xmx1024m" \
    NORTHGATE_WAREHOUSE_API_TOKEN=ngw_live_8f3c21bb94d24e7ba0c15ee7d3f10a92

# Fixed IDs so deploy/k8s/deployment.yaml can reference them in its securityContext.
RUN groupadd --gid 10001 northgate \
    && useradd --uid 10001 --gid northgate --no-create-home --shell /usr/sbin/nologin northgate \
    && mkdir -p /var/northgate/exports /opt/northgate/bin \
    && chown -R northgate:northgate /var/northgate /opt/northgate

COPY ratings-service/target/ratings-service.jar $APP_HOME/ratings-service.jar
COPY ratings-service/docker/export.sh /opt/northgate/bin/export.sh
RUN chmod +x /opt/northgate/bin/export.sh

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/northgate/ratings-service.jar"]
