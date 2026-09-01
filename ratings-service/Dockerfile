FROM eclipse-temurin:8-jre-jammy

ENV APP_HOME=/opt/northgate \
    JAVA_OPTS="-Xms256m -Xmx1024m"

RUN groupadd --system northgate && useradd --system --gid northgate --no-create-home northgate \
    && mkdir -p /var/northgate/exports /opt/northgate/bin \
    && chown -R northgate:northgate /var/northgate /opt/northgate

COPY ratings-service/target/ratings-service.jar $APP_HOME/ratings-service.jar
COPY ratings-service/docker/export.sh /opt/northgate/bin/export.sh
RUN chmod +x /opt/northgate/bin/export.sh

USER northgate

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/northgate/ratings-service.jar"]
