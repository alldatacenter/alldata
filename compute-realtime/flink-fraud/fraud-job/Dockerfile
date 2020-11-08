FROM openjdk:8 AS job-build
WORKDIR /home/gradle/app/

COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle gradle
# To cause download and cache of verifyGoogleJavaFormat dependency
RUN echo "class Dummy {}" > Dummy.java
# download dependencies
RUN ./gradlew build
COPY . .
RUN rm Dummy.java
RUN ./gradlew build
RUN mv /home/gradle/app/build/libs/fraud-app*-deploy.jar /home/gradle/app/build/libs/fraud-app-deploy.jar

# ---

FROM flink:1.8.2

COPY --from=job-build /home/gradle/app/build/libs/fraud-app-deploy.jar lib/job.jar
COPY docker-entrypoint.sh /

USER flink
EXPOSE 8081 6123
ENTRYPOINT ["/docker-entrypoint.sh"]
