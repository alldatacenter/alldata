FROM ${MAVEN_IMAGE} AS build
COPY . /app
WORKDIR /app
RUN mkdir /root/.m2/ && curl ${MAVEN_SETTINGS_XML} -o /root/.m2/settings.xml
RUN mvn -Dmaven.test.skip=true clean package -U

# Release
FROM ${MAVEN_IMAGE} AS release
COPY ./sbin /app/sbin
USER root
WORKDIR /root
# Copy Jars
COPY --from=build /app/tesla-appmanager-start-standalone/target/tesla-appmanager.jar /app/tesla-appmanager-standalone.jar
COPY --from=build /app/tesla-appmanager-start-standalone/target/tesla-appmanager/BOOT-INF/classes/application-docker.properties /app/config/application.properties
# Copy Resources
COPY --from=build /app/tesla-appmanager-start-standalone/target/tesla-appmanager/BOOT-INF/classes/dynamicscripts /app/dynamicscripts
COPY --from=build /app/tesla-appmanager-start-standalone/target/tesla-appmanager/BOOT-INF/classes/jinja /app/jinja
RUN curl -o /app/helm "${HELM_BIN_URL}" \
    && chmod +x /app/helm \
    && curl -o /app/kustomize "${KUSTOMIZE_BIN_URL}"  \
    && chmod +x /app/kustomize

WORKDIR /app
ENTRYPOINT ["/app/sbin/run_sreworks.sh"]