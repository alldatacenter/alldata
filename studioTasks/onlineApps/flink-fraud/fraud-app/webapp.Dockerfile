# --- UI Build
FROM node:10 as ui-build
WORKDIR /home/node/app

COPY package.json package-lock.json tsconfig.json ./
COPY scripts scripts
RUN npm ci --unsafe-perm
COPY public public
COPY src/app src/app
COPY src/index.tsx src
COPY src/react-app-env.d.ts src
RUN npm run build

# --- Maven Build
FROM maven:3.6.2-jdk-8-openj9 as maven-build
WORKDIR /home/maven/work

COPY pom.xml .
RUN mvn -B -e -C -T 1C org.apache.maven.plugins:maven-dependency-plugin:3.1.1:go-offline
COPY . .
COPY --from=ui-build /home/node/app/build /home/maven/work/target/classes/static/
RUN mvn -B -e -o -T 1C verify
RUN mv target/fraud-app*.jar target/fraud-app.jar

# --- Main container
FROM openjdk:8-jdk-alpine as main

COPY --from=maven-build /home/maven/work/target/fraud-app.jar .
EXPOSE 5656

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dspring.profiles.active=dev","-jar","fraud-app.jar"]
