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

package container_provider

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/actionagent"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func TestConstructContainerProvider(t *testing.T) {
	validLabel := map[string]string{
		apistructs.ContainerInstanceLabelType:     string(apistructs.ContainerInstanceECI),
		apistructs.ContainerInstanceLabelCPU:      "1.0",
		apistructs.ContainerInstanceLabelMemoryMB: "1024",
	}
	validProvider := ConstructContainerProvider(WithLabels(validLabel))
	assert.Equal(t, apistructs.ContainerInstanceECI, validProvider.ContainerInstanceType)

	invalidLabel := map[string]string{
		apistructs.ContainerInstanceLabelType:     "xxx",
		apistructs.ContainerInstanceLabelCPU:      "1.0",
		apistructs.ContainerInstanceLabelMemoryMB: "1024",
	}
	invalidProvider := ConstructContainerProvider(WithLabels(invalidLabel))
	assert.Equal(t, false, invalidProvider.IsHitted)

	validTypeButInvalidCPU := map[string]string{
		apistructs.ContainerInstanceLabelType:     string(apistructs.ContainerInstanceECI),
		apistructs.ContainerInstanceLabelCPU:      "xxx",
		apistructs.ContainerInstanceLabelMemoryMB: "1024",
	}
	validTypeButInvalidCPUProvider := ConstructContainerProvider(WithLabels(validTypeButInvalidCPU))
	assert.Equal(t, true, validTypeButInvalidCPUProvider.IsHitted)
	assert.Equal(t, float64(0), validTypeButInvalidCPUProvider.CPU)

	validTypeButInvalidMem := map[string]string{
		apistructs.ContainerInstanceLabelType:     string(apistructs.ContainerInstanceECI),
		apistructs.ContainerInstanceLabelCPU:      "1.0",
		apistructs.ContainerInstanceLabelMemoryMB: "xxx",
	}
	validTypeButInvalidMemProvider := ConstructContainerProvider(WithLabels(validTypeButInvalidMem))
	assert.Equal(t, true, validTypeButInvalidMemProvider.IsHitted)
	assert.Equal(t, float64(0), validTypeButInvalidMemProvider.MemoryMB)
}

func TestDealPipelineProviderBeforeRun(t *testing.T) {
	p := &spec.Pipeline{
		PipelineBase: spec.PipelineBase{
			ID: 1,
		},
		PipelineExtra: spec.PipelineExtra{
			Extra: spec.PipelineExtraInfo{
				ContainerInstanceProvider: nil,
			},
		},
	}
	clusterInfo := apistructs.ClusterInfoData{
		apistructs.ECIEnable:  "true",
		apistructs.ECIHitRate: "100",
	}
	DealPipelineProviderBeforeRun(p, clusterInfo)
	assert.Equal(t, true, p.Extra.ContainerInstanceProvider.IsHitted)
	assert.Equal(t, apistructs.ContainerInstanceECI, p.Extra.ContainerInstanceProvider.ContainerInstanceType)
}

func TestDealJobAndClusterInfo(t *testing.T) {
	job := &apistructs.JobFromUser{
		ContainerInstanceProvider: &apistructs.ContainerInstanceProvider{
			IsHitted:              true,
			ContainerInstanceType: apistructs.ContainerInstanceECI,
		},
		Env: map[string]string{},
	}
	clusterInfo := apistructs.ClusterInfoData{
		apistructs.ECIEnable:  "false",
		apistructs.ECIHitRate: "0",
	}
	DealJobAndClusterInfo(job, clusterInfo)
	assert.Equal(t, "true", clusterInfo[apistructs.BuildkitEnable])
	assert.Equal(t, "100", clusterInfo[apistructs.BuildkitHitRate])
	assert.Equal(t, "true", job.Env[actionagent.EnvEnablePushLog2Collector])
}
