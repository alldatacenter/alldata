#!/bin/bash

for  m in solr-webapp tis-assemble tis-collection-info-collect tis-console tis-index-builder tis-scala-compiler-dependencies tis-web-start
do
  cd $m
  echo "start deploy $m"
  mvn com.qlangtech.tis:tisasm-maven-plugin:1.0.2:put
done