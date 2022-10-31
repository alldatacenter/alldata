#!/bin/bash

source /etc/profile

 mvn -U clean package assembly:assembly -DskipTests=TRUE
