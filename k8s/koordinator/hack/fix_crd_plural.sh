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

# replace corrected plural/singular name of nodeslo crd.
sed -in-place -e 's/  name: nodesloes.slo.koordinator.sh/  name: nodeslos.slo.koordinator.sh/g' config/crd/bases/slo.koordinator.sh_nodesloes.yaml
sed -in-place -e 's/plural: nodesloes$/plural: nodeslos/g' config/crd/bases/slo.koordinator.sh_nodesloes.yaml

rm -f config/crd/bases/slo.koordinator.sh_nodesloes.yamln-place
mv config/crd/bases/slo.koordinator.sh_nodesloes.yaml config/crd/bases/slo.koordinator.sh_nodeslos.yaml

