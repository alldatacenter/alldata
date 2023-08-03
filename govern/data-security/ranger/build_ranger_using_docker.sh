#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License

#This script creates the Docker image (if not already created) and runs maven in the container
#1. Install Docker
#2. Checkout Ranger source and go to the root directory
#3. Run this script. If host is linux, then run this script as "sudo $0 ..."
#4. If you are running on Mac, then you don't need to use "sudo"
#5. To delete the image, run "[sudo] docker rmi ranger_dev"

#Usage: [sudo] ./build_ranger_using_docker.sh [-build_image] mvn  <build params>
#Example 1 (default no param): (mvn -Pall -DskipTests=true clean compile package install)
#Example 2 (Regular build): ./build_ranger_using_docker.sh mvn -Pall clean install -DskipTests=true
#Example 3 (Recreate Docker image): ./build_ranger_using_docker.sh mvn -Pall -build_image clean install -DskipTests=true 
#Notes: To remove build image manually, run "docker rmi ranger_dev" or "sudo docker rmi ranger_dev"

default_command="mvn -Pall -DskipTests=true clean compile package install"
build_image=0
if [ "$1" = "-build_image" ]; then
    build_image=1
    shift
fi

params=$*
if [ $# -eq 0 ]; then
    params=$default_command
fi

image_name="ranger_dev"
remote_home=
container_name="--name ranger_build"

if [ ! -d security-admin ]; then
    echo "ERROR: Run the script from root folder of source. e.g. $HOME/git/ranger"
    exit 1
fi

images=`docker images | cut -f 1 -d " "`
[[ $images =~ $image_name ]] && found_image=1 || build_image=1

if [ $build_image -eq 1 ]; then
    echo "Creating image $image_name ..."
    docker rmi -f $image_name

docker build -t $image_name - <<Dockerfile
FROM centos

RUN mkdir /tools
WORKDIR /tools

#Install default services
#RUN yum clean all
RUN yum install -y wget
RUN yum install -y git
RUN yum install -y gcc
RUN yum install -y bzip2 fontconfig

#Download and install JDK8 from AWS s3's docker-assets 
RUN wget https://s3.eu-central-1.amazonaws.com/docker-assets/dist/jdk-8u101-linux-x64.rpm
RUN rpm -i jdk-8u101-linux-x64.rpm

ENV JAVA_HOME /usr/java/latest
ENV  PATH $JAVA_HOME/bin:$PATH


ADD https://www.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz.sha512 /tools
ADD http://www-us.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz /tools
RUN sha512sum  apache-maven-3.5.4-bin.tar.gz | cut -f 1 -d " " > tmp.sha1

RUN diff -w tmp.sha1 apache-maven-3.5.4-bin.tar.gz.sha512

RUN tar xfz apache-maven-3.5.4-bin.tar.gz
RUN ln -sf /tools/apache-maven-3.5.4 /tools/maven

ENV  PATH /tools/maven/bin:$PATH
ENV MAVEN_OPTS "-Xmx2048m -XX:MaxPermSize=512m"

# Setup gosu for easier command execution
RUN gpg --keyserver pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
    && curl -o /usr/local/bin/gosu -SL "https://github.com/tianon/gosu/releases/download/1.10/gosu-amd64" \
    && curl -o /usr/local/bin/gosu.asc -SL "https://github.com/tianon/gosu/releases/download/1.10/gosu-amd64.asc" \
    && gpg --verify /usr/local/bin/gosu.asc \
    && rm /usr/local/bin/gosu.asc \
    && rm -r /root/.gnupg/ \
    && chmod +x /usr/local/bin/gosu

RUN useradd -ms /bin/bash builder
RUN usermod -g root builder
RUN mkdir -p /scripts

RUN echo "#!/bin/bash" > /scripts/mvn.sh
RUN echo 'set -x; if [ "\$1" = "mvn" ]; then usermod -u \$(stat -c "%u" pom.xml) builder; gosu builder bash -c '"'"'ln -sf /.m2 \$HOME'"'"'; exec gosu builder "\$@"; fi; exec "\$@" ' >> /scripts/mvn.sh

RUN chmod -R 777 /scripts
RUN chmod -R 777 /tools

ENTRYPOINT ["/scripts/mvn.sh"]
Dockerfile

fi

src_folder=`pwd`

LOCAL_M2="$HOME/.m2"
mkdir -p $LOCAL_M2
set -x
docker run --rm  -v "${src_folder}:/ranger" -w "/ranger" -v "${LOCAL_M2}:${remote_home}/.m2" $container_name $image_name $params
