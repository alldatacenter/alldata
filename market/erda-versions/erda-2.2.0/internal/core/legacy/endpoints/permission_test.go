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
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/services/application"
	"github.com/erda-project/erda/internal/core/legacy/services/project"
)

func TestEndpoints_buildScopeInfo(t *testing.T) {
	type args struct {
		accessReq  apistructs.ScopeRoleAccessRequest
		permission apistructs.PermissionList
	}
	tests := []struct {
		name    string
		args    args
		want    apistructs.PermissionList
		wantErr bool
	}{
		{
			name: "test_app_name",
			args: args{
				accessReq: apistructs.ScopeRoleAccessRequest{
					Scope: apistructs.Scope{
						Type: "app",
						ID:   "1",
					},
				},
				permission: apistructs.PermissionList{},
			},
			want: apistructs.PermissionList{
				ScopeInfo: &apistructs.ScopeInfo{
					ProjectName: "test",
					AppName:     "test",
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			e := &Endpoints{}

			var app = &application.Application{}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(app), "Get", func(app *application.Application, applicationID int64) (*model.Application, error) {
				return &model.Application{
					Name:      "test",
					ProjectID: 1,
				}, nil
			})
			defer patch1.Unpatch()

			var pj = &project.Project{}
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(pj), "GetModelProject", func(project *project.Project, projectID int64) (*model.Project, error) {
				return &model.Project{
					DisplayName: "test",
				}, nil
			})
			defer patch2.Unpatch()

			e.app = app
			e.project = pj

			got, err := e.buildScopeInfo(tt.args.accessReq, tt.args.permission)
			if (err != nil) != tt.wantErr {
				t.Errorf("buildScopeInfo() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("buildScopeInfo() got = %v, want %v", got, tt.want)
			}
		})
	}
}
