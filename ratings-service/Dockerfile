FROM eclipse-temurin:8-jre-jammy

ENV APP_HOME=/opt/northgate \
    JAVA_OPTS="-Xms256m -Xmx1024m"

RUN groupadd --system --gid 10001 northgate \
    && useradd --system --uid 10001 --gid northgate --no-create-home northgate \
    && mkdir -p /var/northgate/exports /opt/northgate/bin \
    && chown -R northgate:northgate /var/northgate /opt/northgate

COPY ratings-service/target/ratings-service.jar $APP_HOME/ratings-service.jar
COPY ratings-service/docker/export.sh /opt/northgate/bin/export.sh
RUN chmod +x /opt/northgate/bin/export.sh

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/northgate/ratings-service.jar"]
