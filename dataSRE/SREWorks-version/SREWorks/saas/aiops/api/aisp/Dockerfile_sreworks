FROM registry.cn-hangzhou.aliyuncs.com/alisre/sreworks-base AS build
COPY . /app
COPY settings.xml /root/.m2/settings.xml
RUN cd /app && mvn -f pom.xml -Dmaven.test.skip=true clean package

FROM registry.cn-hangzhou.aliyuncs.com/alisre/openjdk:11.0.10-jre AS release
ARG START_MODULE=tdata-aisp-start-private
ARG JAR_NAME=tdata-aisp.jar
ARG BUILD_JAR=/app/${START_MODULE}/target/tdata-aisp.jar

COPY --from=build ${BUILD_JAR} /app/${JAR_NAME}
COPY ./sbin/ /app/sbin/
COPY ./${START_MODULE}/src/main/resources/application-sreworks.properties /app/application.properties

ENTRYPOINT ["/app/sbin/run.sh"]