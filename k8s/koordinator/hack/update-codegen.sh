#!/bin/bash
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

# This shell is used to auto generate some useful tools for k8s, such as lister,
# informer, client and so on.

set -o errexit
set -o nounset
set -o pipefail

GOPATH=`go env GOPATH`
export GOPATH

SCRIPT_ROOT=$(dirname "${BASH_SOURCE[0]}")/..
ROOT_PKG=github.com/koordinator-sh/koordinator

# code-generator does work with go.mod but makes assumptions about
# the project living in `$GOPATH/src`. To work around this and support
# any location; create a temporary directory, use this as an output
# base, and copy everything back once generated.
TEMP_DIR=$(mktemp -d)
cleanup() {
    echo ">> Removing ${TEMP_DIR}"
    rm -rf ${TEMP_DIR}
}
trap "cleanup" EXIT SIGINT

echo ">> Temporary output directory ${TEMP_DIR}"

# generate the code with:
# --output-base    because this script should also be able to run inside the vendor dir of
#                  k8s.io/kubernetes. The output-base is needed for the generators to output into the vendor dir
#                  instead of the $GOPATH directly. For normal projects this can be dropped.
$SCRIPT_ROOT/hack/generate-groups.sh "client,informer,lister" \
 github.com/koordinator-sh/koordinator/pkg/client github.com/koordinator-sh/koordinator/apis \
 "config:v1alpha1 slo:v1alpha1 scheduling:v1alpha1" \
 --output-base "${TEMP_DIR}" \
 --go-header-file hack/boilerplate/boilerplate.go.txt

${SCRIPT_ROOT}/hack/generate-internal-groups.sh \
  "deepcopy,conversion,defaulter" \
  github.com/koordinator-sh/koordinator/pkg/scheduler/apis/generated \
  github.com/koordinator-sh/koordinator/pkg/scheduler/apis \
  github.com/koordinator-sh/koordinator/pkg/scheduler/apis \
  "config:v1beta2" \
  --output-base "${TEMP_DIR}" \
  --go-header-file hack/boilerplate/boilerplate.go.txt

${SCRIPT_ROOT}/hack/generate-internal-groups.sh \
  "deepcopy,conversion,defaulter" \
  github.com/koordinator-sh/koordinator/pkg/descheduler/apis/generated \
  github.com/koordinator-sh/koordinator/pkg/descheduler/apis \
  github.com/koordinator-sh/koordinator/pkg/descheduler/apis \
  "config:v1alpha2" \
  --output-base "${TEMP_DIR}" \
  --go-header-file hack/boilerplate/boilerplate.go.txt

# Copy everything back.
cp -a "${TEMP_DIR}/${ROOT_PKG}/." "${SCRIPT_ROOT}/"

function custom_sed(){
    perl -i -pe $@
}

custom_sed 's#\"config\"#\"config.koordinator.sh\"#g' ./pkg/client/clientset/versioned/typed/config/v1alpha1/fake/fake_*.go
custom_sed 's#\"slo\"#\"slo.koordinator.sh\"#g' ./pkg/client/clientset/versioned/typed/slo/v1alpha1/fake/fake_*.go
custom_sed 's#\"scheduling\"#\"scheduling.koordinator.sh\"#g' ./pkg/client/clientset/versioned/typed/scheduling/v1alpha1/fake/fake_*.go
