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

package orchestrator

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
)

func Test_GenScaleMessage(t *testing.T) {
	oldService := &apistructs.RuntimeInspectServiceDTO{
		Resources: apistructs.RuntimeServiceResourceDTO{
			CPU: 1,
			Mem: 1024,
		},
		Deployments: apistructs.RuntimeServiceDeploymentsDTO{
			Replicas: 1,
		},
	}

	zh, en := genScaleMessage(oldService, oldService)
	assert.Equal(t, zh, "CPU: 未变, 内存: 未变, 实例数: 未变")
	assert.Equal(t, en, "CPU: no change, Mem: no change, Replicas: no change")

	newService := &apistructs.RuntimeInspectServiceDTO{}
	newService.Resources.CPU = 2
	newService.Resources.Mem = 1025
	newService.Deployments.Replicas = 2
	zh, en = genScaleMessage(oldService, newService)
	assert.Equal(t, zh, "CPU: 从 1核 变为 2核, 内存: 从 1024MB 变为 1025MB, 实例数: 从 1 变为 2")
	assert.Equal(t, en, "CPU: update from 1core to 2core, Mem: update from 1024MB to 1025MB, Replicas: update from 1 to 2")

	newService.Deployments.Replicas = 1
	zh, en = genScaleMessage(oldService, newService)
	assert.Equal(t, zh, "CPU: 从 1核 变为 2核, 内存: 从 1024MB 变为 1025MB, 实例数: 未变")
	assert.Equal(t, en, "CPU: update from 1core to 2core, Mem: update from 1024MB to 1025MB, Replicas: no change")
}
