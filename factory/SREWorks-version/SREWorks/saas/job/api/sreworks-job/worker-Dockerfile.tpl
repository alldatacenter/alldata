FROM {{ MAVEN_IMAGE }} AS build
COPY . /app
WORKDIR /app
RUN mkdir /root/.m2/ && curl {{ MAVEN_SETTINGS_XML }} -o /root/.m2/settings.xml
RUN mvn -Dmaven.test.skip=true clean package

FROM {{ JRE11_ALPINE_IMAGE }} AS release
USER root
WORKDIR /root
COPY --from=build /app/sreworks-job-worker/target/sreworks-job.jar /app/sreworks-job.jar
RUN sed -i 's/dl-cdn.alpinelinux.org/{{ APK_REPO_DOMAIN }}/g' /etc/apk/repositories && apk add build-base curl bash python3 python3-dev py3-pip
RUN pip3 config set global.index-url {{ PYTHON_PIP }} && pip3 config set global.trusted-host {{ PYTHON_PIP_DOMAIN }}
RUN pip3 install requests python-dateutil==1.4 --pre gql[aiohttp]
ENTRYPOINT ["java", "-Xmx1g", "-Xms1g", "-XX:ActiveProcessorCount=2", "-jar", "/app/sreworks-job.jar"]
