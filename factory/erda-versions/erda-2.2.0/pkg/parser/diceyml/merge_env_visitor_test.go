// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package diceyml

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

var mergeYml = `version: "2.0"
envs: {}
services:
    v-admin-center:
      ports:
      - 8080
      resources:
        cpu: 0.1
        max_cpu: 0.1
        mem: 256
      deployments:
        replicas: 1
      expose:
      - 80
      - 443
addons:
  monitor:
    plan: monitor:dev
environments:
  development: {}
  production:
    envs:
      DIFF_KEY: aaa
      TERMINUS_TRACE_ENABLE: "true"
    services:
      blog-web:
        envs:
          KEY: value
          KEY2: value2
          TERMINUS_APP_NAME: blog-web-jar
        resources:
          cpu: 0.1
          max_cpu: 0.1
          mem: 256
        deployments:
          replicas: 1
        health_check:
          http:
            port: 80
    addons:
      mysql:
        plan: rds
      redis:
        plan: redis:medium
  staging: {}
  test: {}
`

func TestMergeAddons(t *testing.T) {
	d, err := New([]byte(mergeYml), false)
	assert.Nil(t, err)
	MergeEnv(d.obj, "production")
	_, ok := d.obj.AddOns["mysql"]

	assert.True(t, ok)
	_, ok = d.obj.AddOns["redis"]
	assert.True(t, ok)
	_, ok = d.obj.AddOns["monitor"]
	assert.False(t, ok)
}
