<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Griffin Development Environment Building Guide
We have pre-built Apache Griffin docker images for Apache Griffin developers. You can use those images directly, which set up a ready development environment for you much faster than building the environment locally.

## Set Up with Docker Images
Here are step-by-step instructions of how to [pull Docker images](../docker/griffin-docker-guide.md#environment-preparation) from the repository and run containers using the images.

## Run or Debug locally
### For service module
If you need to develop the service module, you need to modify some configuration in the following files.
Docker host is your machine running the docker containers, which means if you install docker and run docker containers on 192.168.100.100, then the `<docker host ip>` is 192.168.100.100.

In service/src/main/resources/application.properties
```
spring.datasource.url = jdbc:postgresql://<docker host ip>:35432/quartz?autoReconnect=true&useSSL=false

hive.metastore.uris = thrift://<docker host ip>:39083

elasticsearch.host = <docker host ip>
elasticsearch.port = 39200
```

In service/src/main/resources/sparkJob.properties
```
livy.uri=http://<docker host ip>:38998/batches

yarn.uri=http://<docker host ip>:38088
```

Now you can start the service module in your local IDE, running or debugging org.apache.griffin.core.GriffinWebApplication.

### For UI module
If you only wanna develop the UI module, you just need to modify some configuration.

In ui/angular/src/app/service/service.service.ts
```
// public BACKEND_SERVER = "";
public BACKEND_SERVER = 'http://<docker host ip>:38080';
```
Making the change above, you can test your UI module by using remote service.

However, on the most of conditions, you need to develop the UI module with some modification in service module.
Then you need to follow the steps above for service module first, and
in ui/angular/src/app/service/service.service.ts
```
// public BACKEND_SERVER = "";
public BACKEND_SERVER = 'http://localhost:8080';
```
Making the change, you can start service module locally, and test your UI module using local service.

After that, you can run local server using following command:
```
cd ui/angular 
../angular/node_modules/.bin/ng serve --port 8080
```

### For measure module
If you only wanna develop the measure module, you can ignore both of service or UI module.
You can test your measure JAR built in the docker container, using the existed spark environment.

For debug purpose, you'd better install hadoop, spark, hive locally, so you can test your program more quickly.

Note: If you run Hadoop in a pseudo-distributed mode on MacOS you have to update hdfs-site.xml, namely, comment out the parameters **dfs.namenode.servicerpc-address** and **dfs.namenode.rpc-address**, otherwise you can get the error like: "java.net.ConnectException: Call From mycomputer/127.0.1.1 to localhost:9000 failed on connection exception: java.net.ConnectException: Connection refused; For more details see:  http://wiki.apache.org/hadoop/ConnectionRefused"

## Deploy on docker container
Firstly, in the griffin directory, build your packages at once.
```
mvn clean install
```

### For service module and ui module
1. Login to docker container, and stop running Apache Griffin service.
```
docker exec -it <griffin docker container id> bash
cd ~/service
ps -ef | grep service.jar
kill -9 <pid of service.jar>
```
2. Service and ui module are both packaged in `service/target/service-<version>.jar`, copy it into your docker container.
```
docker cp service-<version>.jar <griffin docker container id>:/root/service/service.jar
```
3. In docker container, start the new service.
```
cd ~/service
nohup java -jar service.jar > service.log &
```
Now you can follow the service log by `tail -f service.log`.

### For measure module
1. Measure module is packaged in `measure/target/measure-<version>.jar`, copy it into your docker container.
```
docker cp measure-<version>.jar <griffin docker container id>:/root/measure/griffin-measure.jar
```
2. Login to docker container, and overwrite griffin-measure.jar onto hdfs inside.
```
docker exec -it <griffin docker container id> bash
hadoop fs -rm /griffin/griffin-measure.jar
hadoop fs -put /root/measure/griffin-measure.jar /griffin/griffin-measure.jar
```
Now the Apache Griffin service will submit jobs by using this new griffin-measure.jar.

## Build new Apache Griffin docker image
For end2end test, you will need to build a new Apache Griffin docker image, for more convenient test.
1. Pull the docker build repo on your docker host.
```
git clone https://github.com/bhlx3lyx7/griffin-docker.git
```
2. Copy your measure and service JAR into griffin_spark2 directory.
```
cp service-<version>.jar <path to>/griffin-docker/griffin_spark2/prep/service/service.jar
cp measure-<version>.jar <path to>/griffin-docker/griffin_spark2/prep/measure/griffin-measure.jar
```
3. Build your new Apache Griffin docker image.
In griffin_spark2 directory.
```
cd <path to>/griffin-docker/griffin_spark2
docker build -t <image name>[:<image version>] .
```
4. If you are using another image name (or version), you need also modify the docker-compose file you're using.
```
griffin:
  image: <image name>[:<image version>]
```
5. Now you can run your new Apache Griffin docker image.
```
docker-compose -f <docker-compose file> up -d
```
