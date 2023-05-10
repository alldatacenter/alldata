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

package deployment_order

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
)

func TestComposeApplicationsInfo(t *testing.T) {
	type args struct {
		Releases   [][]*dbclient.Release
		Params     map[string]apistructs.DeploymentOrderParam
		AppsStatus apistructs.DeploymentOrderStatusMap
	}

	appStatus := apistructs.DeploymentOrderStatusMap{
		"app1": {
			DeploymentID:     10,
			DeploymentStatus: apistructs.DeploymentStatusDeploying,
		},
	}

	params := map[string]apistructs.DeploymentOrderParam{
		"app1": []*apistructs.DeploymentOrderParamData{
			{Key: "key1", Value: "value1", Type: "ENV", Encrypt: true, Comment: "test1"},
			{Key: "key2", Value: "value2", Type: "FILE", Comment: "test2"},
		},
	}

	tests := []struct {
		name string
		args args
		want [][]*apistructs.ApplicationInfo
	}{
		{
			name: "pipeline",
			args: args{
				Releases: [][]*dbclient.Release{
					{
						{
							ReleaseId:       "8d2385a088df415decdf6357147ed4a2",
							Labels:          "{\n    \"gitCommitId\": \"27504bb7cb788bee08a50612b97faea201c0efed\",\n    \"gitBranch\": \"master\"\n}",
							ApplicationName: "app1",
						},
					},
				},
				Params:     params,
				AppsStatus: appStatus,
			},
			want: [][]*apistructs.ApplicationInfo{
				{
					{
						Name:         "app1",
						DeploymentId: 10,
						ReleaseId:    "8d2385a088df415decdf6357147ed4a2",
						Params: &apistructs.DeploymentOrderParam{
							{
								Key:     "key1",
								Value:   "",
								Encrypt: true,
								Type:    "kv",
								Comment: "test1",
							},
							{
								Key:     "key2",
								Value:   "value2",
								Type:    "dice-file",
								Comment: "test2",
							},
						},
						Branch:   "master",
						CommitId: "27504bb7cb788bee08a50612b97faea201c0efed",
						Status:   apistructs.DeploymentStatusDeploying,
					},
				},
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := composeApplicationsInfo(tt.args.Releases, tt.args.Params, tt.args.AppsStatus)
			assert.NoError(t, err)
			assert.Equal(t, got, tt.want)
		})
	}
}

func TestGetDeploymentOrderAccessDenied(t *testing.T) {
	order := New()

	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "GetDeploymentOrder", func(*dbclient.DBClient, string) (*dbclient.DeploymentOrder, error) {
		return &dbclient.DeploymentOrder{}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(order.bdl), "CheckPermission", func(*bundle.Bundle, *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
		return &apistructs.PermissionCheckResponseData{
			Access: false,
		}, nil
	})

	_, err := order.Get("100000", "789418c6-0bd4-4186-bd41-45372984621f")
	assert.Equal(t, err, apierrors.ErrListDeploymentOrder.AccessDenied())
}
