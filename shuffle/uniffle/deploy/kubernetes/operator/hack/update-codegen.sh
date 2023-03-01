#!/bin/bash

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -o errexit
set -o nounset
set -o pipefail

SCRIPT_ROOT=$(dirname "${BASH_SOURCE}")/..

go mod vendor

MODULE="github.com/apache/incubator-uniffle/deploy/kubernetes/operator"
GENERATED_BASE="pkg"
OUTPUT_PACKAGE="${MODULE}/pkg/generated"
APIS_PACKAGE="${MODULE}/api"
CODEGEN_PKG="./vendor/k8s.io/code-generator"

trap EXIT SIGINT SIGTERM

GENERATED_TMP_DIR=$(mktemp -d)

chmod +x ${CODEGEN_PKG}/generate-groups.sh
${CODEGEN_PKG}/generate-groups.sh all \
  ${OUTPUT_PACKAGE} ${APIS_PACKAGE} \
  "uniffle:v1alpha1" \
  --go-header-file "${SCRIPT_ROOT}"/hack/headers/header.go.txt \
  --output-base "${GENERATED_TMP_DIR}"

cp -rf "${GENERATED_TMP_DIR}"/${OUTPUT_PACKAGE} ${GENERATED_BASE}

rm -r "${GENERATED_TMP_DIR}"

rm -r ./vendor
