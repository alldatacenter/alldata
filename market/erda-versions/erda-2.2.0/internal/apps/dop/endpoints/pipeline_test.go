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
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/permission"
)

func Test_shouldCheckPermission(t *testing.T) {
	type args struct {
		isInternalClient       bool
		isInternalActionClient bool
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "isInternalClient",
			args: args{
				isInternalClient:       true,
				isInternalActionClient: false,
			},
			want: false,
		},
		{
			name: "isInternalActionClient",
			args: args{
				isInternalClient:       false,
				isInternalActionClient: true,
			},
			want: true,
		},
		{
			name: "isInternalClient_and_isInternalActionClient",
			args: args{
				isInternalClient:       true,
				isInternalActionClient: true,
			},
			want: true,
		},
		{
			name: "otherClient",
			args: args{
				isInternalClient:       false,
				isInternalActionClient: false,
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := shouldCheckPermission(tt.args.isInternalClient, tt.args.isInternalActionClient); got != tt.want {
				t.Errorf("shouldCheckPermission() = %v, want %v", got, tt.want)
			}
		})
	}
}

// func TestUpdateCmsNsConfigsWhenUserNotExist(t *testing.T) {
// 	var bdl *bundle.Bundle
// 	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetMemberByUserAndScope",
// 		func(*bundle.Bundle, apistructs.ScopeType, string, uint64) ([]apistructs.Member, error) {
// 			return nil, nil
// 		})
// 	defer monkey.UnpatchAll()
// 	e := New()
// 	assert.Equal(t, "the member is not exist", e.UpdateCmsNsConfigs("1", 1).Error())
// }

func TestEndpoints_pipelineDetail(t *testing.T) {

}

func Test_getPipelineDetailAndCheckPermission(t *testing.T) {
	type args struct {
		req          apistructs.CICDPipelineDetailRequest
		identityInfo apistructs.IdentityInfo
		result       apistructs.PipelineDetailDTO
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test",
			args: args{
				req: apistructs.CICDPipelineDetailRequest{
					PipelineID:               1,
					SimplePipelineBaseResult: false,
				},
				result: apistructs.PipelineDetailDTO{
					PipelineDTO: apistructs.PipelineDTO{
						ID:            1,
						ApplicationID: 1,
						Branch:        "master",
					},
				},
				identityInfo: apistructs.IdentityInfo{
					UserID: "1",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			var bdl = &bundle.Bundle{}
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetPipelineV2", func(bdl *bundle.Bundle, req apistructs.PipelineDetailRequest) (*apistructs.PipelineDetailDTO, error) {
				assert.Equal(t, req.PipelineID, tt.args.req.PipelineID)
				assert.Equal(t, req.SimplePipelineBaseResult, tt.args.req.SimplePipelineBaseResult)

				return &tt.args.result, nil
			})

			defer patch.Unpatch()

			var permissionChecker = &permission.Permission{}
			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(permissionChecker), "CheckRuntimeBranch", func(p *permission.Permission, identityInfo apistructs.IdentityInfo, appID uint64, branch string, action string) error {
				assert.Equal(t, tt.args.identityInfo, identityInfo)
				assert.Equal(t, tt.args.result.ApplicationID, appID)
				assert.Equal(t, tt.args.result.Branch, branch)
				return nil
			})
			defer patch1.Unpatch()

			got, err := getPipelineDetailAndCheckPermission(bdl, permissionChecker, tt.args.req, tt.args.identityInfo)
			if (err != nil) != tt.wantErr {
				t.Errorf("getPipelineDetailAndCheckPermission() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			assert.NotNil(t, got)
			assert.Equal(t, got.ID, tt.args.result.ID)
		})
	}
}
