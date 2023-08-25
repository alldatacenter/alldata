#!/bin/bash
#
# Copyright 2022-2022 The Koordinator Authors.
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

set -e

SHELL_FOLDER=$(cd "$(dirname "$0")";pwd)
LICENSE_HEADER_PATH="./hack/boilerplate/boilerplate.go.txt"

cd $GOPATH/src/github.com/koordinator-sh/koordinator

# generates gomock files
mockgen -source pkg/koordlet/statesinformer/states_informer.go \
  -destination pkg/koordlet/statesinformer/mockstatesinformer/mock.go \
  -copyright_file ${LICENSE_HEADER_PATH}
mockgen -source pkg/koordlet/metriccache/metric_cache.go \
  -destination pkg/koordlet/metriccache/mockmetriccache/mock.go \
  -copyright_file ${LICENSE_HEADER_PATH}
mockgen -source vendor/k8s.io/cri-api/pkg/apis/runtime/v1alpha2/api.pb.go \
  -destination pkg/runtime/handler/mockclient/mock.go \
  -imports github.com/koordinator-sh/koordinator/vendor/k8s.io/cri-api/pkg/apis/runtime/v1alpha2=k8s.io/cri-api/pkg/apis/runtime/v1alpha2 \
  -copyright_file ${LICENSE_HEADER_PATH} \
  -package mock_client RuntimeServiceClient
