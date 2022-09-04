FROM {{ MAVEN_IMAGE }} AS build
COPY . /app
RUN mkdir /root/.m2/ && curl {{ MAVEN_SETTINGS_XML }} -o /root/.m2/settings.xml
RUN cd /app && mvn -Dmaven.test.skip=true clean package

FROM {{ JRE8_IMAGE }} AS release
ARG START_MODULE=tesla-authproxy-start
ARG TARGET_DIRECTORY=tesla-authproxy-service
ARG JAR_NAME=tesla-authproxy.jar
ARG BUILD_JAR=/app/${START_MODULE}/target/tesla-authproxy.jar

COPY --from=build ${BUILD_JAR} /app/${JAR_NAME}
COPY ./sbin/ /app/sbin/
COPY ./${START_MODULE}/src/main/resources/application-docker.properties /app/application.properties
RUN sed -i 's/dl-cdn.alpinelinux.org/{{ APK_REPO_DOMAIN }}/g' /etc/apk/repositories
RUN apk add --update --no-cache gettext
ENTRYPOINT ["/app/sbin/run.sh"]
