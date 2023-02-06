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

package pipelineymlv1

import (
	"testing"

	"github.com/stretchr/testify/require"

	. "github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/pipelineymlvars"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/steptasktype"
)

func getBytePipelineYml() []byte {
	return []byte(`version: '1.0'
stages:
- name: repo
  tasks:
  - aggregate:
    - get: repo
    - task: echo
      config:
        image_resource:
          type: docker-image
          source:
            repository: centos
        run:
          path: echo
          args: ["hello", "world"]

- name: build
  tasks:
  - put: bp-backend

resources:
- name: repo
  type: git
- name: bp-backend
  type: buildpack
`)
}

func TestPipelineYml_UpdatePipelineOnTask(t *testing.T) {
	y := New(getBytePipelineYml())
	y.SetPipelineID(123)
	err := y.Parse()
	require.NoError(t, err)

	// update get: repo
	err = y.UpdatePipelineOnTask(GenerateTaskUUID(0, "repo", 0, "repo", y.metadata.instanceID),
		TaskUpdateParams{Envs: &map[string]string{"e1": "v1"}, Disable: &[]bool{true}[0], ForceBuildpack: func() *bool { b := true; return &b }(), Pause: &[]bool{true}[0]})
	require.NoError(t, err)
	get, ok := y.obj.Stages[0].Tasks[0].(*GetTask)
	require.True(t, ok)
	require.Equal(t, len(get.Envs), 1)
	require.True(t, *get.Disable)
	// get 不支持修改 params.force_buildpack
	require.Nil(t, get.Params)
	require.True(t, *get.Pause)

	// update put: bp-backend
	err = y.UpdatePipelineOnTask(GenerateTaskUUID(1, "build", 0, "bp-backend", y.metadata.instanceID),
		TaskUpdateParams{Envs: &map[string]string{"e1": "v1", "e2": "v2"}, Disable: &[]bool{true}[0], ForceBuildpack: func() *bool { b := false; return &b }()})
	require.NoError(t, err)
	put, ok := y.obj.Stages[1].Tasks[0].(*PutTask)
	require.True(t, ok)
	require.Equal(t, len(put.Envs), 2)
	require.True(t, *put.Disable)
	// put 支持修改 params.force_buildpack
	require.False(t, put.Params[FieldParamForceBuildpack.String()].(bool))
	require.Nil(t, put.Pause)
}

func TestTaskConfig_foundTaskConfigSnippetAndUpdate(t *testing.T) {
	aggTC := TaskConfig{
		FieldAggregate.String(): []TaskConfig{
			{
				FieldPut.String():     "bp-backend",
				FieldDisable.String(): true,
				FieldEnvs.String(): map[string]string{
					"e1": "v1",
				},
				FieldParams.String(): map[string]interface{}{
					FieldParamForceBuildpack.String(): false,
				},
			},
			{
				FieldGet.String():     "repo",
				FieldDisable.String(): false,
				FieldEnvs.String(): map[string]string{
					"e1": "v1",
				},
			},
			{
				FieldTask.String():    "makefile",
				FieldDisable.String(): false,
				FieldTaskConfig.String(): map[string]interface{}{
					FieldTaskConfigEnvs.String(): map[string]string{
						"e1": "v1",
					},
				},
			},
		},
	}
	_, err := aggTC.foundTaskConfigSnippetAndUpdate(true, 3, steptasktype.GET, TaskUpdateParams{})
	require.Error(t, err)

	newAggTC, err := aggTC.foundTaskConfigSnippetAndUpdate(true, 1, steptasktype.GET,
		TaskUpdateParams{
			Envs:    &map[string]string{"e1": "v11", "e2": "v2"},
			Disable: &[]bool{true}[0],
		})
	require.NoError(t, err)

	steps, err := newAggTC.decodeAggregateStepTasks()
	require.NoError(t, err)

	get, ok := steps[1].(*GetTask)
	require.True(t, ok)
	require.True(t, *get.Disable)
	require.Equal(t, len(get.Envs), 2)
	require.Equal(t, get.Envs["e1"], "v11")
	require.Equal(t, get.Envs["e2"], "v2")
}

func TestTaskConfig_updateSingleTaskConfig(t *testing.T) {

	// get: envs / disable / pause
	getTC := TaskConfig{
		FieldGet.String():     "repo",
		FieldDisable.String(): false,
		FieldEnvs.String(): map[string]string{
			"e1": "v1",
		},
	}
	newGetTC, err := getTC.updateSingleTaskConfig(steptasktype.GET,
		TaskUpdateParams{
			Envs:    &map[string]string{"e1": "v11", "e2": "v2"},
			Disable: &[]bool{true}[0],
			Pause:   &[]bool{true}[0],
		})
	require.NoError(t, err)
	getStep, err := newGetTC.decodeSingleStepTask(steptasktype.GET)
	require.NoError(t, err)
	get, ok := getStep.(*GetTask)
	require.True(t, ok)
	require.True(t, *get.Disable)
	require.Equal(t, len(get.Envs), 2)
	require.Equal(t, get.Envs["e1"], "v11")
	require.Equal(t, get.Envs["e2"], "v2")
	require.True(t, *get.Disable)

	// put: envs / disable / forceBuildpack
	putTC := TaskConfig{
		FieldPut.String():     "bp-backend",
		FieldDisable.String(): true,
		FieldEnvs.String(): map[string]string{
			"e1": "v1",
		},
		FieldParams.String(): Params{
			FieldParamForceBuildpack.String(): false,
		},
	}
	newPutTC, err := putTC.updateSingleTaskConfig(steptasktype.PUT,
		TaskUpdateParams{
			Envs:           &map[string]string{"e1": "v11", "e2": "v2"},
			Disable:        &[]bool{false}[0],
			ForceBuildpack: &[]bool{true}[0],
			Pause:          &[]bool{false}[0],
		})
	require.NoError(t, err)
	putStep, err := newPutTC.decodeSingleStepTask(steptasktype.PUT)
	require.NoError(t, err)
	put, ok := putStep.(*PutTask)
	require.True(t, ok)
	require.False(t, *put.Disable)
	require.Equal(t, len(put.Envs), 2)
	require.Equal(t, put.Envs["e1"], "v11")
	require.Equal(t, put.Envs["e2"], "v2")
	require.True(t, put.Params["force_buildpack"].(bool))
	require.False(t, *put.Pause)

	// put: envs / disable
	taskTC := TaskConfig{
		FieldTask.String():    "makefile",
		FieldDisable.String(): false,
		FieldTaskConfig.String(): map[string]interface{}{
			FieldTaskConfigEnvs.String(): map[string]string{
				"e1": "v1",
			},
		},
	}
	newTaskTC, err := taskTC.updateSingleTaskConfig(steptasktype.TASK,
		TaskUpdateParams{
			Envs:    &map[string]string{"e1": "v11", "e2": "v2"},
			Disable: &[]bool{true}[0],
		})
	require.NoError(t, err)
	taskStep, err := newTaskTC.decodeSingleStepTask(steptasktype.TASK)
	require.NoError(t, err)
	task, ok := taskStep.(*CustomTask)
	require.True(t, ok)
	require.True(t, *task.Disable)
	require.Equal(t, len(task.Config.Envs), 2)
	require.Equal(t, task.Config.Envs["e1"], "v11")
	require.Equal(t, task.Config.Envs["e2"], "v2")
	// 修改：disable 为 false，envs 不更新
	newTaskTC, err = newTaskTC.updateSingleTaskConfig(steptasktype.TASK,
		TaskUpdateParams{
			Envs:    nil,
			Disable: func() *bool { b := false; return &b }(),
		})
	require.NoError(t, err)
	taskStep, err = newTaskTC.decodeSingleStepTask(steptasktype.TASK)
	require.NoError(t, err)
	task, ok = taskStep.(*CustomTask)
	require.True(t, ok)
	require.False(t, *task.Disable)
	require.Equal(t, len(task.Config.Envs), 2)
	require.Nil(t, task.Pause)
}
