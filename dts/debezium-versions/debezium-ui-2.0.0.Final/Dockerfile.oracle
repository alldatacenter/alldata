FROM registry.access.redhat.com/ubi8/ubi-minimal:8.3 AS builder

ARG JAVA_PACKAGE=java-11-openjdk-headless

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ENV JAVA_HOME="/usr/lib/jvm/jre-11"
ENV NPM_CONFIG_CACHE="/.cache/npm"

RUN microdnf install ca-certificates ${JAVA_PACKAGE} java-11-openjdk-devel git \
    && microdnf update \
    && microdnf clean all \
    && mkdir -p /javabuild/backend \
    && mkdir -p /javabuild/ui \
    && chown -R 1001 /javabuild \
    && chmod -R "g+rwX" /javabuild \
    && chown -R 1001:root /javabuild \
    && mkdir -p /.cache \
    && chown -R 1001 /.cache \
    && chmod -R "g+rwX" /.cache \
    && chown -R 1001:root /.cache \
    && echo "securerandom.source=file:/dev/urandom" >> /etc/alternatives/jre/lib/security/java.security

USER 1001

COPY --chown=1001:root mvnw /javabuild/mvnw
COPY --chown=1001:root .mvn/ /javabuild/.mvn/
COPY --chown=1001:root pom.xml /javabuild/
COPY --chown=1001:root backend/pom.xml /javabuild/backend/pom.xml
COPY --chown=1001:root ui/pom.xml /javabuild/ui/pom.xml

WORKDIR /javabuild

RUN ./mvnw clean dependency:go-offline -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

COPY --chown=1001:root . /javabuild/

RUN ./mvnw package -am -pl backend -Dquarkus.package.type=fast-jar -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120

FROM registry.access.redhat.com/ubi8/ubi-minimal:8.3

ARG JAVA_PACKAGE=java-11-openjdk-headless
ARG RUN_JAVA_VERSION=1.3.8

ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en'
ENV JAVA_HOME="/usr/lib/jvm/jre-11"

# Install java and the run-java script
# Also set up permissions for user `1001`
RUN microdnf install curl ca-certificates ${JAVA_PACKAGE} \
    && microdnf update \
    && microdnf clean all \
    && mkdir /deployments \
    && chown 1001 /deployments \
    && chmod "g+rwX" /deployments \
    && chown 1001:root /deployments \
    && curl https://repo1.maven.org/maven2/io/fabric8/run-java-sh/${RUN_JAVA_VERSION}/run-java-sh-${RUN_JAVA_VERSION}-sh.sh -o /deployments/run-java.sh \
    && chown 1001 /deployments/run-java.sh \
    && chmod 540 /deployments/run-java.sh \
    && echo "securerandom.source=file:/dev/urandom" >> /etc/alternatives/jre/lib/security/java.security

# Configure the JAVA_OPTIONS, you can add -XshowSettings:vm to also display the heap size.
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"

# We make four distinct layers so if there are application changes the library layers can be re-used
COPY --from=builder --chown=1001 /javabuild/backend/target/quarkus-app/lib/ /deployments/lib/
COPY --from=builder --chown=1001 /javabuild/backend/target/quarkus-app/*.jar /deployments/
COPY --from=builder --chown=1001 /javabuild/backend/target/quarkus-app/app/ /deployments/app/
COPY --from=builder --chown=1001 /javabuild/backend/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 1001

ENTRYPOINT [ "/deployments/run-java.sh" ]
