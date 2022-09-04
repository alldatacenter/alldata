FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/maven:3.8.3-adoptopenjdk-11 AS build

COPY . /app
COPY settings.xml /root/.m2/settings.xml
RUN cd /app && mvn -f pom.xml -Dmaven.test.skip=true clean package

FROM sreworks-registry.cn-beijing.cr.aliyuncs.com/mirror/openjdk8:alpine-jre AS release
ARG START_MODULE=warehouse-start
ARG TARGET_DIRECTORY=warehouse
ARG JAR_NAME=warehouse.jar
ARG BUILD_JAR=/app/${START_MODULE}/target/warehouse-start.jar

COPY --from=build ${BUILD_JAR} /app/${JAR_NAME}
COPY ./sbin/ /app/sbin/
COPY ./${START_MODULE}/src/main/resources/application.properties /app/application.properties
COPY ./skywalking-agent/ /app/skywalking-agent/

RUN apk add --update --no-cache gettext
RUN chmod +x /app/sbin/run.sh
ENTRYPOINT ["/app/sbin/run.sh"]