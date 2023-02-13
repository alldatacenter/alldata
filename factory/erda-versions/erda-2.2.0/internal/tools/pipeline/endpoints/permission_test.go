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

package endpoints

import (
	"testing"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func Test_checkTaskOperatesBelongToTasks(t *testing.T) {
	type args struct {
		taskOperates []apistructs.PipelineTaskOperateRequest
		tasks        []spec.PipelineTask
	}

	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "empty tasks",
			args: args{
				tasks: nil,
				taskOperates: []apistructs.PipelineTaskOperateRequest{
					{
						TaskAlias: "git-checkout",
						Disable:   &[]bool{true}[0],
					},
				},
			},
			wantErr: true,
		},
		{
			name: "empty taskOperates",
			args: args{
				tasks: []spec.PipelineTask{
					{
						Name: "git-checkout",
					},
				},
				taskOperates: nil,
			},
			wantErr: false,
		},
		{
			name: "taskOperates not belong to tasks",
			args: args{
				tasks: []spec.PipelineTask{
					{
						Name: "git-checkout",
					},
					{
						Name: "release",
					},
				},
				taskOperates: []apistructs.PipelineTaskOperateRequest{
					{
						TaskAlias: "dice",
					},
				},
			},
			wantErr: true,
		},
		{
			name: "taskOperates belong to tasks",
			args: args{
				tasks: []spec.PipelineTask{
					{
						Name: "git-checkout",
					},
					{
						Name: "release",
					},
				},
				taskOperates: []apistructs.PipelineTaskOperateRequest{
					{
						TaskAlias: "release",
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := checkTaskOperatesBelongToTasks(tt.args.taskOperates, tt.args.tasks); (err != nil) != tt.wantErr {
				t.Errorf("checkTaskOperatesBelongToTasks() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
