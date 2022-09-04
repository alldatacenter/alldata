FROM ${MAVEN_IMAGE} AS build
COPY . /app
RUN cd /app && mvn -Dmaven.test.skip=true clean package -U

FROM ${JRE8_IMAGE} AS release
ARG START_MODULE=tesla-gateway-start-private
ARG TARGET_DIRECTORY=tesla-gateway
ARG DEPENDENCY=/app/${START_MODULE}/target/${TARGET_DIRECTORY}

COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
COPY build/start.sh /app/sbin/start.sh
RUN mv /app/application-docker.yml /app/application.yml
ENTRYPOINT ["/app/sbin/start.sh"]