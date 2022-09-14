FROM {{ MAVEN_IMAGE }} AS build

COPY . /app
RUN mkdir /root/.m2/ && curl {{ MAVEN_SETTINGS_XML }} -o /root/.m2/settings.xml
RUN cd /app && mvn -Dmaven.test.skip=true clean package -U

FROM {{ JRE8_IMAGE }} AS release
ARG START_MODULE=tkg-one-start
ARG TARGET_DIRECTORY=tkg-one
ARG DEPENDENCY=/app/${START_MODULE}/target/${TARGET_DIRECTORY}

COPY --from=build /app/${START_MODULE}/target/${TARGET_DIRECTORY}.jar /app/${TARGET_DIRECTORY}.jar

COPY sbin/start.sh /app/start.sh
COPY ./${START_MODULE}/src/main/resources/application.properties /app/application.properties
RUN chmod +x /app/start.sh

ENTRYPOINT ["/app/start.sh"]