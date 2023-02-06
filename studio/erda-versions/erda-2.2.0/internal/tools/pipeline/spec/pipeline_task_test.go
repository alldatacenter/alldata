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

package spec

import (
	"encoding/json"
	"fmt"
	"testing"
	"time"

	"github.com/magiconair/properties/assert"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/pkg/metadata"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func TestRuntimeID(t *testing.T) {
	s := `{"metadata":[{"name":"runtimeID","value":"9","type":"link"},{"name":"operatorID","value":"2"}]}`
	r := taskresult.Result{}
	if err := json.Unmarshal([]byte(s), &r); err != nil {
		logrus.Fatal(err)
	}
	pt := PipelineTask{Result: &r}
	assert.Equal(t, pt.RuntimeID(), "9")
}

func TestTaskContextDedup(t *testing.T) {
	ctx := PipelineTaskContext{
		InStorages: metadata.Metadata{
			{Name: "in1", Value: "v1"},
			{Name: "in2", Value: "v2"},
			{Name: "in1", Value: "v1_2"},
		},
		OutStorages: metadata.Metadata{
			{Name: "out1", Value: "v1"},
			{Name: "out2", Value: "v2"},
			{Name: "out1", Value: "v1_2"},
		},
	}
	assert.Equal(t, len(ctx.InStorages), 3)
	assert.Equal(t, len(ctx.OutStorages), 3)

	ctx.Dedup()
	assert.Equal(t, len(ctx.InStorages), 2)
	assert.Equal(t, len(ctx.OutStorages), 2)
}

func TestPipelineTaskExecutorName_Check(t *testing.T) {
	tests := []struct {
		name string
		that PipelineTaskExecutorName
		want bool
	}{
		{
			name: "PipelineTaskExecutorNameEmpty",
			that: PipelineTaskExecutorNameEmpty,
			want: true,
		},
		{
			name: "PipelineTaskExecutorNameSchedulerDefault",
			that: PipelineTaskExecutorNameSchedulerDefault,
			want: true,
		},
		{
			name: "PipelineTaskExecutorNameAPITestDefault",
			that: PipelineTaskExecutorNameAPITestDefault,
			want: true,
		},
		{
			name: "other",
			that: "other",
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.that.Check(); got != tt.want {
				t.Errorf("Check() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestPipelineTaskExecutorKind_Check(t *testing.T) {
	tests := []struct {
		name string
		that PipelineTaskExecutorKind
		want bool
	}{
		{
			name: "PipelineTaskExecutorKindScheduler",
			that: PipelineTaskExecutorKindScheduler,
			want: true,
		},
		{
			name: "PipelineTaskExecutorKindMemory",
			that: PipelineTaskExecutorKindMemory,
			want: true,
		},
		{
			name: "PipelineTaskExecutorKindAPITest",
			that: PipelineTaskExecutorKindAPITest,
			want: true,
		},
		{
			name: "other",
			that: "other",
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.that.Check(); got != tt.want {
				t.Errorf("Check() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestMakeTaskExecutorCtxKey(t *testing.T) {
	task := &PipelineTask{ID: 1}
	ctxKey := MakeTaskExecutorCtxKey(task)
	assert.Equal(t, ctxKey, "executor-done-chan-1")
}

func TestPipelineTaskAppendError(t *testing.T) {
	task := PipelineTask{}
	task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{Msg: "a"})
	task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{Msg: "a"})
	assert.Equal(t, 1, len(task.Inspect.Errors))
	task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{Msg: "b"})
	assert.Equal(t, 2, len(task.Inspect.Errors))
	startA := time.Date(2021, 8, 19, 10, 10, 0, 0, time.Local)
	endA := time.Date(2021, 8, 19, 10, 30, 0, 0, time.Local)
	task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{Msg: "a", Ctx: taskerror.ErrorContext{StartTime: startA, EndTime: endA}})
	assert.Equal(t, 3, len(task.Inspect.Errors))
	start := time.Date(2021, 8, 19, 10, 9, 0, 0, time.Local)
	end := time.Date(2021, 8, 19, 10, 29, 0, 0, time.Local)
	task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{Msg: "a", Ctx: taskerror.ErrorContext{StartTime: start, EndTime: end}})
	taskDto := task.Convert2DTO()
	assert.Equal(t, uint64(2), taskDto.Result.Errors[2].Ctx.Count)
	assert.Equal(t, 3, len(taskDto.Result.Errors))
	assert.Equal(t, start.Unix(), taskDto.Result.Errors[2].Ctx.StartTime.Unix())
	assert.Equal(t, endA.Unix(), taskDto.Result.Errors[2].Ctx.EndTime.Unix())
}

func TestConvertErrors(t *testing.T) {
	task := PipelineTask{}
	start := time.Date(2021, 8, 24, 9, 45, 1, 1, time.Local)
	end := time.Date(2021, 8, 24, 9, 46, 1, 1, time.Local)
	task.Inspect.Errors = task.Inspect.Errors.AppendError(&taskerror.Error{Msg: "err", Ctx: taskerror.ErrorContext{
		StartTime: start,
		EndTime:   end,
		Count:     2,
	}})
	taskDto := task.Convert2DTO()
	assert.Equal(t, fmt.Sprintf("err\nstartTime: %s\nendTime: %s\ncount: %d", start.Format("2006-01-02 15:04:05"), end.Format("2006-01-02 15:04:05"), 2), taskDto.Result.Errors[0].Msg)
}

func TestGenerateExecutorVersion(t *testing.T) {
	normalTask := PipelineTask{ID: 1, Extra: PipelineTaskExtra{}}
	loopTask := PipelineTask{ID: 1, Extra: PipelineTaskExtra{
		LoopOptions: &apistructs.PipelineTaskLoopOptions{
			LoopedTimes: 100,
		},
	}}
	assert.Equal(t, normalTask.GenerateExecutorDoneChanDataVersion(), "executor-done-chan-data-version-1")
	assert.Equal(t, loopTask.GenerateExecutorDoneChanDataVersion(), "executor-done-chan-data-version-1-loop-100")
}

func TestCheckExecutorVersion(t *testing.T) {
	loopTask := PipelineTask{ID: 1, Extra: PipelineTaskExtra{
		LoopOptions: &apistructs.PipelineTaskLoopOptions{
			LoopedTimes: 100,
		},
	}}
	actualVersion := "executor-done-chan-data-version-1-loop-100"
	errVersion := "executor-done-chan-data-version-1-loop-99"
	assert.Equal(t, loopTask.CheckExecutorDoneChanDataVersion(actualVersion), nil)
	assert.Equal(t, loopTask.CheckExecutorDoneChanDataVersion(errVersion).Error(), "executor data expected version: executor-done-chan-data-version-1-loop-100, actual version: executor-done-chan-data-version-1-loop-99")
}

func TestGetExecutorName(t *testing.T) {
	testCases := []struct {
		name     string
		task     *PipelineTask
		expected string
	}{
		{
			name: "old scheduler normal task",
			task: &PipelineTask{
				ID: 1,
				Extra: PipelineTaskExtra{
					ExecutorName: PipelineTaskExecutorNameSchedulerDefault,
					ClusterName:  "erda-op",
				},
				ExecutorKind: PipelineTaskExecutorKindScheduler,
			},
			expected: "k8s-job-erda-op",
		},
		{
			name: "old scheduler flink task",
			task: &PipelineTask{
				ID: 1,
				Extra: PipelineTaskExtra{
					ExecutorName: PipelineTaskExecutorNameSchedulerDefault,
					ClusterName:  "erda-op",
					Action: pipelineyml.Action{
						Params: map[string]interface{}{
							"bigDataConf": "{\n    \"flinkConf\": {\"kind\": \"job\"}\n}",
						},
					},
				},
				ExecutorKind: PipelineTaskExecutorKindScheduler,
			},
			expected: "k8s-flink-erda-op",
		},
		{
			name: "old scheduler spark task",
			task: &PipelineTask{
				ID: 1,
				Extra: PipelineTaskExtra{
					ExecutorName: PipelineTaskExecutorNameSchedulerDefault,
					ClusterName:  "erda-op",
					Action: pipelineyml.Action{
						Params: map[string]interface{}{
							"bigDataConf": "{\n    \"sparkConf\": {\"kind\": \"job\"}\n}",
						},
					},
				},
				ExecutorKind: PipelineTaskExecutorKindScheduler,
			},
			expected: "k8s-spark-erda-op",
		},
		{
			name: "normal job task",
			task: &PipelineTask{
				ID: 1,
				Extra: PipelineTaskExtra{
					ExecutorName: PipelineTaskExecutorName("k8s-job-erda-op"),
					ClusterName:  "erda-op",
				},
				ExecutorKind: PipelineTaskExecutorKindK8sJob,
			},
			expected: "k8s-job-erda-op",
		},
	}
	for _, tt := range testCases {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, string(tt.task.GetExecutorName()), tt.expected)
		})
	}
}
