#!/bin/bash

mvn clean package
pname="system-devops"
mkdir -p target/${pname}
mkdir -p target/${pname}/bin
mkdir -p target/${pname}/lib
mkdir -p target/${pname}/lib-update
cp -r script/* target/${pname}/bin
cp -r target/system-devops-automain-1.0-SNAPSHOT-jar-with-dependencies.jar target/${pname}/lib-update
cd target
tar -zcvf ${pname}.tar.gz ${pname}

cd ../
scp -r target/system-devops.tar.gz root@120.77.155.220:/usr/local/BigDataPlatform/system-devops/system-devops.tar.gz
