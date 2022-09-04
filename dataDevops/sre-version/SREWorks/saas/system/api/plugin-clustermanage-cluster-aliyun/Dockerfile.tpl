FROM {{ MAVEN_IMAGE }} AS build
COPY . /app
WORKDIR /app

RUN mkdir /root/.m2/ && curl {{ MAVEN_SETTINGS_XML }} -o /root/.m2/settings.xml
RUN mvn -Dmaven.test.skip=true clean package


FROM {{ JRE11_IMAGE }} AS release
USER root
WORKDIR /root
COPY --from=build /app/plugin-clustermanage-cluster-aliyun-start/target/plugin-clustermanage-cluster-aliyun.jar /app/plugin-clustermanage-cluster-aliyun.jar
ENTRYPOINT ["java", "-Xmx100m", "-Xms20m", "-XX:ActiveProcessorCount=2", "-jar", "/app/plugin-clustermanage-cluster-aliyun.jar"]
