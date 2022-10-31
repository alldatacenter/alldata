#!/bin/bash

source /etc/profile

./mvnw clean install -Prelease -DskipTests=TRUE
