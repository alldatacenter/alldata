#!/usr/bin/env bash
set -e

mvnProfile=flink-embedded

echo "mvn profile = ${mvnProfile}"
mvn clean package -pl bitsail-dist -am -Dmaven.test.skip=true -U -P${mvnProfile}
