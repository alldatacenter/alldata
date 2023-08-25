#!/usr/bin/env bash
#
# Copyright 2017 The Kubernetes Authors.
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

set -o errexit
set -o nounset
set -o pipefail

KOORDINATOR_ROOT=$(dirname "${BASH_SOURCE[0]}")/..
KOORDINATOR_RUNTIME_ROOT="${KOORDINATOR_ROOT}/apis/runtime"

runtime_versions=("v1alpha1")

function generate_code() {
  RUNTIME_API_VERSION="$1"
  KOORDINATOR_RUNTIME_PATH="${KOORDINATOR_RUNTIME_ROOT}/${RUNTIME_API_VERSION}"

  protoc \
  --proto_path="${KOORDINATOR_RUNTIME_PATH}" \
  --go_opt=paths=source_relative \
  --go_out="${KOORDINATOR_RUNTIME_PATH}" \
  --go-grpc_opt=paths=source_relative \
  --go-grpc_out="${KOORDINATOR_RUNTIME_PATH}" \
  "api.proto"
}

for v in "${runtime_versions[@]}"; do
  generate_code "${v}"
done