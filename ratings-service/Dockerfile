# Base image last touched in the 1.2 release, February 2021, when the tag was pinned so
# the nightly build stopped moving underneath us. Nothing has moved it since.
FROM openjdk:8u282-jdk-buster

ENV APP_HOME=/opt/northgate \
    JAVA_OPTS="-Xms256m -Xmx1024m" \
    NORTHGATE_WAREHOUSE_API_TOKEN=ngw_live_8f3c21bb94d24e7ba0c15ee7d3f10a92

RUN mkdir -p /var/northgate/exports /opt/northgate/bin

COPY ratings-service/target/ratings-service.jar $APP_HOME/ratings-service.jar
COPY ratings-service/docker/export.sh /opt/northgate/bin/export.sh
RUN chmod +x /opt/northgate/bin/export.sh

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/northgate/ratings-service.jar"]
