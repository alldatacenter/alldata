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

package taskop

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskrun"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func TestCalculateNextLoopTimeDuration(t *testing.T) {
	tt := []struct {
		loopedTimes uint64
		want        string
	}{
		{
			loopedTimes: 0,
			want:        "1s",
		},
		{
			loopedTimes: 1,
			want:        "1.5s",
		},
		{
			loopedTimes: 2,
			want:        "2.25s",
		},
		{
			loopedTimes: 3,
			want:        "3.375s",
		},
		{
			loopedTimes: 4,
			want:        "5.0625s",
		},
		{
			loopedTimes: 5,
			want:        "7.59375s",
		},
		{
			loopedTimes: 6,
			want:        "10s",
		},
		{
			loopedTimes: 7,
			want:        "10s",
		},
		{
			loopedTimes: 8,
			want:        "10s",
		},
		{
			loopedTimes: 9,
			want:        "10s",
		},
	}

	w := NewWait(&taskrun.TaskRun{})
	for i := range tt {
		assert.Equal(t, tt[i].want, w.calculateNextLoopTimeDuration(tt[i].loopedTimes).String())
	}
}

func Test_waitWhenDone(t *testing.T) {
	taskRun := &taskrun.TaskRun{
		Task: &spec.PipelineTask{},
		P: &spec.Pipeline{
			PipelineBase: spec.PipelineBase{
				ID: 1,
			},
		},
	}
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(taskRun), "AppendLastMsg", func(tr *taskrun.TaskRun, msg string) error {
		t.Logf("task AppendLastMsg taskID: %d, msg: %s", tr.Task.ID, msg)
		return nil
	})
	defer pm1.Unpatch()
	w := NewWait(taskRun)
	statusWithDesc := apistructs.PipelineStatusDesc{
		Status: apistructs.PipelineStatusSuccess,
		Desc:   "success",
	}
	err := w.WhenDone(statusWithDesc)
	assert.NoError(t, err)
}
