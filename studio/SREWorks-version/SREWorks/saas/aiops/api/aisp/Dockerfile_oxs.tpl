FROM {{ MAVEN_IMAGE }} AS build
COPY . /app
RUN cd /app && mvn -f pom.xml -Dmaven.test.skip=true clean package

FROM {{ JRE8_IMAGE }} AS release
ARG START_MODULE=tdata-aisp-start
ARG JAR_NAME=tdata-aisp.jar
ARG BUILD_JAR=/app/${START_MODULE}/target/tdata-aisp.jar

COPY --from=build ${BUILD_JAR} /app/${JAR_NAME}
COPY ./sbin/ /app/sbin/
COPY ./${START_MODULE}/src/main/resources/application-oxs.properties /app/application.properties

ENTRYPOINT ["/app/sbin/run.sh"]