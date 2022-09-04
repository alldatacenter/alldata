FROM {{ MAVEN_IMAGE }} AS build

COPY . /app
RUN mkdir /root/.m2/ && curl {{ MAVEN_SETTINGS_XML }} -o /root/.m2/settings.xml
RUN cd /app && mvn -f pom.xml -Dmaven.test.skip=true clean package

FROM {{ JRE8_IMAGE }} AS release
ARG START_MODULE=dataset-start
ARG TARGET_DIRECTORY=dataset
ARG JAR_NAME=dataset.jar
ARG BUILD_JAR=/app/${START_MODULE}/target/dataset-start.jar

COPY --from=build ${BUILD_JAR} /app/${JAR_NAME}
COPY ./sbin/ /app/sbin/
COPY ./${START_MODULE}/src/main/resources/application.properties /app/application.properties
COPY ./skywalking-agent/ /app/skywalking-agent/

RUN sed -i 's/dl-cdn.alpinelinux.org/{{ APK_REPO_DOMAIN }}/g' /etc/apk/repositories
RUN apk add --update --no-cache gettext
RUN chmod +x /app/sbin/run.sh
ENTRYPOINT ["/app/sbin/run.sh"]
