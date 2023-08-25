#!/usr/bin/env bash
#
# Copyright 2022 The Koordinator Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

PROJECT=$(cd $(dirname $0)/..; pwd)

LICENSEHEADERCHECKER_VERSION=v1.4.0

GOBIN=${PROJECT}/bin go install github.com/lluissm/license-header-checker/cmd/license-header-checker@${LICENSEHEADERCHECKER_VERSION}

LICENSEIGNORE=$(cat ${PROJECT}/.licenseignore | tr '\n' ',')

${PROJECT}/bin/license-header-checker -r -a -v -i ${LICENSEIGNORE} ${PROJECT}/hack/boilerplate/boilerplate.go.txt . go
