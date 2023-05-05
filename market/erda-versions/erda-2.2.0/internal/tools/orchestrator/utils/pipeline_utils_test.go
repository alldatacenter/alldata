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

package utils

import (
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func TestGenRedeployPipelineYaml(t *testing.T) {
	expectOutPut := "${dice-deploy-redeploy:OUTPUT:deployment_id}"

	yml := GenRedeployPipelineYaml(uint64(1))
	assert.Equal(t, yml.Version, "1.1")
	assert.Equal(t, len(yml.Stages), 4)
	assert.Equal(t, len(yml.Stages[0]), 1)
	assert.Equal(t, yml.Stages[0][0].Type, "dice-deploy-redeploy")
	assert.Equal(t, yml.Stages[0][0].Params["runtime_id"], "1")
	assert.Equal(t, yml.Stages[1][0].Type, "dice-deploy-addon")
	assert.Equal(t, yml.Stages[1][0].Params["deployment_id"], expectOutPut)
	assert.Equal(t, yml.Stages[2][0].Type, "dice-deploy-service")
	assert.Equal(t, yml.Stages[2][0].Params["deployment_id"], expectOutPut)
	assert.Equal(t, yml.Stages[3][0].Type, "dice-deploy-domain")
	assert.Equal(t, yml.Stages[3][0].Params["deployment_id"], expectOutPut)
}

func TestGenCreateByReleasePipelineYaml(t *testing.T) {
	expectOutPut := "${dice-deploy-release-DEV:OUTPUT:deployment_id}"

	yml := GenCreateByReleasePipelineYaml("1111111f1a1k1e11111111111111111", []string{"DEV"})
	assert.Equal(t, yml.Version, "1.1")
	assert.Equal(t, len(yml.Stages), 4)
	assert.Equal(t, yml.Stages[0][0].Type, "dice-deploy-release")
	assert.Equal(t, yml.Stages[0][0].Alias, "dice-deploy-release-DEV")
	assert.Equal(t, yml.Stages[0][0].Params["release_id"], "1111111f1a1k1e11111111111111111")
	assert.Equal(t, yml.Stages[0][0].Params["workspace"], "DEV")
	assert.Equal(t, yml.Stages[1][0].Type, "dice-deploy-addon")
	assert.Equal(t, yml.Stages[1][0].Alias, "dice-deploy-addon-DEV")
	assert.Equal(t, yml.Stages[1][0].Params["deployment_id"], expectOutPut)
	assert.Equal(t, yml.Stages[2][0].Type, "dice-deploy-service")
	assert.Equal(t, yml.Stages[2][0].Alias, "dice-deploy-service-DEV")
	assert.Equal(t, yml.Stages[2][0].Params["deployment_id"], expectOutPut)
	assert.Equal(t, yml.Stages[3][0].Type, "dice-deploy-domain")
	assert.Equal(t, yml.Stages[3][0].Alias, "dice-deploy-domain-DEV")
	assert.Equal(t, yml.Stages[3][0].Params["deployment_id"], expectOutPut)
}

func TestFindCRBRRunningPipeline(t *testing.T) {
	var bdl *bundle.Bundle
	now := time.Now()
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "PageListPipeline",
		func(_ *bundle.Bundle, req apistructs.PipelinePageListRequest) (*apistructs.PipelinePageListData, error) {
			resp := &apistructs.PipelinePageListData{
				Pipelines: []apistructs.PagePipeline{
					apistructs.PagePipeline{
						ID:      12580,
						YmlName: "dice-deploy-release-develop",
						Extra: apistructs.PipelineExtra{
							DiceWorkspace: "test",
							RunUser: &apistructs.PipelineUser{
								ID: "2",
							},
						},
						FilterLabels: map[string]string{"appID": "1", "branch": "develop"},
						TimeBegin:    &now,
					},
					apistructs.PagePipeline{
						ID:      12581,
						YmlName: "dice-deploy-release-feature/test",
						Extra: apistructs.PipelineExtra{
							DiceWorkspace: "DEV",
							RunUser: &apistructs.PipelineUser{
								ID: "2",
							},
						},
						FilterLabels: map[string]string{"appID": "1", "branch": "feature/xxx"},
						TimeBegin:    &now,
					},
				},
			}
			return resp, nil
		},
	)
	defer monkey.UnpatchAll()

	result, err := FindCRBRRunningPipeline(uint64(1), "test", "", bdl)
	assert.NoError(t, err)
	assert.Equal(t, len(result), 1)
	assert.Equal(t, result[0].ID, uint64(12580))
}

func TestFindCreatingRuntimesByRelease(t *testing.T) {
	var bdl *bundle.Bundle
	now := time.Now()
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "PageListPipeline",
		func(_ *bundle.Bundle, req apistructs.PipelinePageListRequest) (*apistructs.PipelinePageListData, error) {
			resp := &apistructs.PipelinePageListData{
				Pipelines: []apistructs.PagePipeline{
					apistructs.PagePipeline{
						ID:      12580,
						YmlName: "dice-deploy-release-develop",
						Extra: apistructs.PipelineExtra{
							DiceWorkspace: "test",
							RunUser: &apistructs.PipelineUser{
								ID: "2",
							},
						},
						FilterLabels: map[string]string{"appID": "1", "branch": "develop"},
						TimeBegin:    &now,
					},
					apistructs.PagePipeline{
						ID:      12581,
						YmlName: "dice-deploy-release-feature/test-ttt",
						Extra: apistructs.PipelineExtra{
							DiceWorkspace: "DEV",
							RunUser: &apistructs.PipelineUser{
								ID: "2",
							},
						},
						FilterLabels: map[string]string{"appID": "1", "branch": "feature/xxx"},
						TimeBegin:    &now,
					},
					apistructs.PagePipeline{
						ID:      12581,
						YmlName: "dice-deploy-release-feature/test",
						Extra: apistructs.PipelineExtra{
							DiceWorkspace: "DEV",
							RunUser: &apistructs.PipelineUser{
								ID: "2",
							},
						},
						FilterLabels: map[string]string{"appID": "1", "branch": "feature/xxx"},
						TimeBegin:    &now,
					},
				},
			}
			return resp, nil
		},
	)
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetPipeline",
		func(_ *bundle.Bundle, pipelineID uint64) (*apistructs.PipelineDetailDTO, error) {
			resp := &apistructs.PipelineDetailDTO{
				PipelineStages: []apistructs.PipelineStageDetailDTO{
					apistructs.PipelineStageDetailDTO{
						PipelineTasks: []apistructs.PipelineTaskDTO{apistructs.PipelineTaskDTO{Type: "dice-deploy-release",
							Status: apistructs.PipelineStatusRunning}},
					},
				},
			}
			return resp, nil
		},
	)
	defer monkey.UnpatchAll()

	result, err := FindCreatingRuntimesByRelease(uint64(1), map[string][]string{"test": []string{"develop/fake"}}, "", bdl)
	assert.NoError(t, err)
	assert.Equal(t, len(result), 1)
	assert.Equal(t, result[0].Name, "develop")
	assert.Equal(t, result[0].Source, apistructs.RELEASE)
	assert.Equal(t, result[0].Status, "Init")
	assert.Equal(t, result[0].DeployStatus, apistructs.DeploymentStatusDeploying)
	assert.Equal(t, result[0].Extra["buildId"], uint64(12580))
	assert.Equal(t, result[0].Extra["fakeRuntime"], true)
	assert.Equal(t, result[0].LastOperator, "2")
	assert.Equal(t, result[0].LastOperateTime, now)
}

func TestIsUndoneDeployByReleaseTask(t *testing.T) {
	pipelineDetailDTO := &apistructs.PipelineDetailDTO{
		PipelineStages: []apistructs.PipelineStageDetailDTO{
			apistructs.PipelineStageDetailDTO{
				PipelineTasks: []apistructs.PipelineTaskDTO{apistructs.PipelineTaskDTO{Type: "dice-deploy-release",
					Status: apistructs.PipelineStatusRunning}},
			},
		},
	}
	assert.True(t, isUndoneTaskOFDeployByRelease(pipelineDetailDTO))

	pipelineDetailDTO.PipelineStages[0].PipelineTasks[0].Type = "fsdsasfs"
	assert.False(t, isUndoneTaskOFDeployByRelease(pipelineDetailDTO))

	pipelineDetailDTO.PipelineStages[0].PipelineTasks[0].Status = "Failed"
	assert.False(t, isUndoneTaskOFDeployByRelease(pipelineDetailDTO))
}
