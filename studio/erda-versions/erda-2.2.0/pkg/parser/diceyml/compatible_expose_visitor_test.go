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

var compatible_expose_yml = `version: 2.0
services:
  web:
    ports:
      - 8080
      - 20880
      - port: 1234
        protocol: "UDP"
      - port: 12345
        protocol: "TCP"
        expose: true
    health_check:
      exec:
        cmd: "echo 1"
    deployments:
      replicas: 1
    resources:
      cpu: 0.1
      mem: 512
      disk: 0
    expose:
      - 20880
      - 1234
    volumes:
      - storage: "nfs"
        path: "/data/file/resource"`

func TestCompatibleExpose(t *testing.T) {
	d, err := New([]byte(compatible_expose_yml), false) // new的时候已经执行了CompatibleExpose
	assert.Nil(t, err)
	assert.True(t, d.obj.Services["web"].Ports[0].Expose)
}
