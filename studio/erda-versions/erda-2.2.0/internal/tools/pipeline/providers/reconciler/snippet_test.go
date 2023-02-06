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

package reconciler

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func Test_fulfillParentSnippetTask(t *testing.T) {
	var (
		dbClient *dbclient.Client
	)
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "UpdatePipelineTaskStatus", func(_ *dbclient.Client, id uint64, status apistructs.PipelineStatus, ops ...dbclient.SessionOption) error {
		return nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "UpdatePipelineTaskTime", func(_ *dbclient.Client, p *spec.Pipeline, ops ...dbclient.SessionOption) error {
		return nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "UpdatePipelineExtraSnapshot", func(_ *dbclient.Client, pipelineID uint64, snapshot spec.Snapshot, ops ...dbclient.SessionOption) error {
		return nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "GetPipelineTask", func(_ *dbclient.Client, id interface{}) (spec.PipelineTask, error) {
		return spec.PipelineTask{ID: 1}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "UpdatePipelineTaskSnippetDetail", func(_ *dbclient.Client, id uint64, snippetDetail apistructs.PipelineTaskSnippetDetail, ops ...dbclient.SessionOption) error {
		return nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "UpdatePipelineTaskMetadata", func(_ *dbclient.Client, id uint64, result *taskresult.Result) error {
		return nil
	})
	defer monkey.UnpatchAll()
	tests := []struct {
		name           string
		p              *spec.Pipeline
		task           *spec.PipelineTask
		wantTaskStatus apistructs.PipelineStatus
	}{
		{
			name: "success snippet pipeline",
			p: &spec.Pipeline{
				PipelineBase: spec.PipelineBase{
					IsSnippet:    true,
					ParentTaskID: &[]uint64{1}[0],
					Status:       apistructs.PipelineStatusSuccess,
				},
			},
			task:           &spec.PipelineTask{},
			wantTaskStatus: apistructs.PipelineStatusSuccess,
		},
		{
			name: "success snippet pipeline",
			p: &spec.Pipeline{
				PipelineBase: spec.PipelineBase{
					IsSnippet:    true,
					ParentTaskID: &[]uint64{2}[0],
					Status:       apistructs.PipelineStatusFailed,
				},
			},
			task:           &spec.PipelineTask{},
			wantTaskStatus: apistructs.PipelineStatusFailed,
		},
	}
	tr := &defaultTaskReconciler{dbClient: dbClient}
	//monkey.PatchInstanceMethod(reflect.TypeOf(tr), "calculateAndUpdatePipelineOutputValues", func(_ *defaultTaskReconciler, p *spec.Pipeline, tasks []*spec.PipelineTask) ([]apistructs.PipelineOutputWithValue, error) {
	//	return []apistructs.PipelineOutputWithValue{}, nil
	//})
	//monkey.PatchInstanceMethod(reflect.TypeOf(tr), "handleParentSnippetTaskOutputs", func(_ *defaultTaskReconciler, snippetPipeline *spec.Pipeline, outputValues []apistructs.PipelineOutputWithValue) error {
	//	return nil
	//})
	for _, tt := range tests {
		monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "GetPipelineWithTasks",
			func(_ *dbclient.Client, id uint64) (*spec.PipelineWithTasks, error) {
				return &spec.PipelineWithTasks{
					Pipeline: &spec.Pipeline{
						PipelineBase: spec.PipelineBase{
							ID:           1,
							Status:       tt.p.Status,
							ParentTaskID: &[]uint64{1}[0],
						},
					},
					Tasks: []*spec.PipelineTask{
						{
							ID: 1,
						},
					},
				}, nil
			})
		t.Run(tt.name, func(t *testing.T) {
			err := tr.fulfillParentSnippetTask(tt.p, tt.task)
			if err != nil {
				t.Errorf("fulfillParentSnippetTask() error = %v", err)
			}
			if tt.task.Status != tt.wantTaskStatus {
				t.Errorf("fulfillParentSnippetTask() task.Status = %v, want %v", tt.task.Status, tt.wantTaskStatus)
			}
		})
	}
}
