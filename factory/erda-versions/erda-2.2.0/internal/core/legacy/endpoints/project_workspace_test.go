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
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/services/permission"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func generateProjectWorkspaceCreateRequest() apistructs.ProjectWorkSpaceAbility {

	return apistructs.ProjectWorkSpaceAbility{
		ProjectID: 1,
		Workspace: "DEV",
		Abilities: "{\"ECI\":\"enable\"}",
	}
}

func TestEndpoints_CreateProjectWorkSpace(t *testing.T) {
	type fields struct {
		db         *dao.DBClient
		permission *permission.Permission
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}

	req := generateProjectWorkspaceCreateRequest()

	reqStr, _ := json.Marshal(req)
	httpReq, _ := http.NewRequest(http.MethodPost, "/api/project-workspace-abilitie", bytes.NewBuffer(reqStr))

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		{
			name: "Test_01",
			fields: fields{
				db:         &dao.DBClient{},
				permission: &permission.Permission{},
			},
			args: args{
				r: httpReq,
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: httpserver.Resp{
					Success: true,
					Data:    nil,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			e := &Endpoints{
				db:         tt.fields.db,
				permission: tt.fields.permission,
			}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(e.permission), "CheckPermission", func(_ *permission.Permission, req *apistructs.PermissionCheckRequest) (bool, error) {
				return true, nil
			})

			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(e.db), "CreateProjectWorkspaceAbilities", func(_ *dao.DBClient, ability apistructs.ProjectWorkSpaceAbility) error {
				return nil
			})

			patch3 := monkey.Patch(user.GetUserID, func(r *http.Request) (user.ID, error) {
				return "2", nil
			})

			patch4 := monkey.Patch(user.GetOrgID, func(r *http.Request) (uint64, error) {

				return 1, nil
			})

			defer patch4.Unpatch()
			defer patch3.Unpatch()
			defer patch2.Unpatch()
			defer patch1.Unpatch()

			got, err := e.CreateProjectWorkSpace(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("CreateProjectWorkSpace() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("CreateProjectWorkSpace() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestEndpoints_GetProjectWorkSpace(t *testing.T) {
	type fields struct {
		db         *dao.DBClient
		permission *permission.Permission
	}
	type args struct {
		ctx  context.Context
		r    *http.Request
		vars map[string]string
	}

	httpReq, _ := http.NewRequest(http.MethodGet, "/api/project-workspace-abilities/{projectID}/{workspace}", nil)

	tests := []struct {
		name    string
		fields  fields
		args    args
		want    httpserver.Responser
		wantErr bool
	}{
		// TODO: Add test cases.
		{
			name: "Test_01",
			fields: fields{
				db:         &dao.DBClient{},
				permission: &permission.Permission{},
			},
			args: args{
				r: httpReq,
				vars: map[string]string{
					"projectID": "1",
					"workspace": "DEV",
				},
			},
			want: httpserver.HTTPResponse{
				Status: http.StatusOK,
				Content: httpserver.Resp{
					Success: true,
					Data:    generateProjectWorkspaceCreateRequest(),
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			e := &Endpoints{
				db:         tt.fields.db,
				permission: tt.fields.permission,
			}

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(e.permission), "CheckPermission", func(_ *permission.Permission, req *apistructs.PermissionCheckRequest) (bool, error) {
				return true, nil
			})

			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(e.db), "GetProjectWorkspaceAbilities", func(_ *dao.DBClient, projectID uint64, workspace string) (apistructs.ProjectWorkSpaceAbility, error) {
				return generateProjectWorkspaceCreateRequest(), nil
			})

			patch3 := monkey.Patch(user.GetUserID, func(r *http.Request) (user.ID, error) {
				return "2", nil
			})

			patch4 := monkey.Patch(user.GetOrgID, func(r *http.Request) (uint64, error) {

				return 1, nil
			})

			defer patch4.Unpatch()
			defer patch3.Unpatch()
			defer patch2.Unpatch()
			defer patch1.Unpatch()

			got, err := e.GetProjectWorkSpace(tt.args.ctx, tt.args.r, tt.args.vars)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetProjectWorkSpace() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetProjectWorkSpace() got = %v, want %v", got, tt.want)
			}
		})
	}
}
