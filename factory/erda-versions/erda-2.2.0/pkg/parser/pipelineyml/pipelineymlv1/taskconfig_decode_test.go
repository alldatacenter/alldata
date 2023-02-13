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

func TestTaskConfig_decodeAggregateStepTasks(t *testing.T) {
	y := New(getBytePipelineYml())
	err := y.Parse()
	require.NoError(t, err)
	aggTC := y.obj.Stages[0].TaskConfigs[0]
	require.Equal(t, len(aggTC), 1)
	steps, err := aggTC.decodeAggregateStepTasks()
	require.NoError(t, err)
	require.Equal(t, len(steps), 2)
	get, ok := steps[0].(*GetTask)
	require.True(t, ok)
	require.Equal(t, get.Get, "repo")
	_, ok = steps[1].(*CustomTask)
	require.True(t, ok)
}

func TestTaskConfig_decodeStepTask(t *testing.T) {
	getTC := TaskConfig{
		FieldGet.String():     "repo",
		FieldDisable.String(): false,
		FieldEnvs.String(): map[string]string{
			"e1": "v1",
		},
	}
	getStep, err := getTC.decodeSingleStepTask(steptasktype.GET)
	require.NoError(t, err)
	step, ok := getStep.(StepTask)
	require.True(t, ok)
	require.False(t, step.IsDisable())
	require.Equal(t, len(step.GetEnvs()), 1)
	require.Equal(t, step.GetEnvs()["e1"], "v1")
	require.Equal(t, step.Name(), "repo")
	_, ok = getStep.(*PutTask)
	require.False(t, ok)
	get, ok := getStep.(*GetTask)
	require.True(t, ok)
	require.False(t, *get.Disable)
	require.Equal(t, len(get.Envs), 1)
	require.Equal(t, get.Envs["e1"], "v1")
	require.Equal(t, get.Get, "repo")
}

func TestTaskConfig_decodeStepTaskWithValidate(t *testing.T) {
	y := New(getBytePipelineYml())
	y.SetPipelineID(123)
	err := y.Parse(WithBranch("develop"))
	require.NoError(t, err)
	aggTC := y.obj.Stages[0].TaskConfigs[0]
	steps, err := aggTC.decodeAggregateStepTasks()
	require.NoError(t, err)
	getTC := steps[0].GetTaskConfig()
	getStep, err := getTC.decodeStepTaskWithValidate(steptasktype.GET, y)
	require.NoError(t, err)
	get, ok := getStep.(*GetTask)
	require.True(t, ok)
	require.Equal(t, get.Branch, "develop")
}

func TestConvertObjectToTaskConfig(t *testing.T) {
	tc, err := convertObjectToTaskConfig(GetTask{Get: "repo"})
	require.NoError(t, err)
	require.Equal(t, tc[FieldGet.String()], "repo")
}
