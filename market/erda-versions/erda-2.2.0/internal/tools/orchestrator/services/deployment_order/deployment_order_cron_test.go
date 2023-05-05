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
	"encoding/json"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"

	releasepb "github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

func TestInspectDeploymentStatusDetail(t *testing.T) {
	type args struct {
		DeploymentOrder *dbclient.DeploymentOrder
		StatusMap       apistructs.DeploymentOrderStatusMap
	}

	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "first-batch",
			args: args{
				DeploymentOrder: &dbclient.DeploymentOrder{},
				StatusMap: apistructs.DeploymentOrderStatusMap{
					"java-demo": apistructs.DeploymentOrderStatusItem{
						AppID:            1,
						DeploymentID:     1,
						DeploymentStatus: apistructs.DeploymentStatusInit,
						RuntimeID:        1,
					},
				},
			},
			want: "{\"java-demo\":{\"appId\":1,\"deploymentId\":1,\"deploymentStatus\":\"INIT\",\"runtimeId\":1}}",
		},
		{
			name: "status-appending",
			args: args{
				DeploymentOrder: &dbclient.DeploymentOrder{
					StatusDetail: "{\"go-demo\":{\"appId\":0,\"deploymentId\":0,\"deploymentStatus\":\"INIT\",\"runtimeId\":0}}",
				},
				StatusMap: apistructs.DeploymentOrderStatusMap{
					"java-demo": apistructs.DeploymentOrderStatusItem{
						AppID:            1,
						DeploymentID:     1,
						DeploymentStatus: apistructs.DeploymentStatusDeploying,
						RuntimeID:        1,
					},
				},
			},
			want: "{\"go-demo\":{\"appId\":0,\"deploymentId\":0,\"deploymentStatus\":\"INIT\",\"runtimeId\":0},\"" +
				"java-demo\":{\"appId\":1,\"deploymentId\":1,\"deploymentStatus\":\"DEPLOYING\",\"runtimeId\":1}}",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := inspectDeploymentStatusDetail(tt.args.DeploymentOrder, tt.args.StatusMap)
			assert.NoError(t, err)
			assert.Equal(t, tt.want, tt.args.DeploymentOrder.StatusDetail)
		})
	}
}

////go:generate mockgen -destination=./deployment_order_release_test.go -package deployment_order github.com/erda-project/erda-proto-go/core/dicehub/release/pb ReleaseServiceServer
func TestPushOnDeploymentOrderPolling(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	rss := NewMockReleaseServiceServer(ctrl)

	rss.EXPECT().GetRelease(gomock.Any(), gomock.Any()).AnyTimes().Return(&releasepb.ReleaseGetResponse{
		Data: &releasepb.ReleaseGetResponseData{
			ReleaseID: "202cb962ac59075b964b07152d234b70",
			Modes: map[string]*releasepb.ModeSummary{
				"default": {
					ApplicationReleaseList: []*releasepb.ReleaseSummaryArray{
						{
							List: []*releasepb.ApplicationReleaseSummary{
								{
									ReleaseID:       "202cb962ac59075b964b07152d234b70",
									ApplicationName: "app-demo",
								},
							},
						},
					},
				},
			},
		},
	}, nil)

	deployList := [][]string{{"id1"}, {"id2"}}
	data, err := json.Marshal(deployList)
	if err != nil {
		t.Fatal(err)
	}
	order := New(WithReleaseSvc(rss))
	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "FindUnfinishedDeploymentOrders", func(*dbclient.DBClient) ([]dbclient.DeploymentOrder, error) {
		return []dbclient.DeploymentOrder{
			{
				ReleaseId:    "202cb962ac59075b964b07152d234b70",
				CurrentBatch: 1,
				BatchSize:    3,
				Workspace:    apistructs.WORKSPACE_PROD,
				Status:       string(apistructs.DeploymentStatusDeploying),
				DeployList:   string(data),
			},
		}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "GetRuntimeByAppName", func(*dbclient.DBClient, string, uint64, string) (*dbclient.Runtime, error) {
		return &dbclient.Runtime{}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "FindLastDeployment", func(*dbclient.DBClient, uint64) (*dbclient.Deployment, error) {
		// if record not found, will return nil
		return nil, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "ListReleases", func(*dbclient.DBClient, []string) ([]*dbclient.Release, error) {
		return []*dbclient.Release{
			{ReleaseId: "id1"},
			{ReleaseId: "id2"},
		}, nil
	})

	defer monkey.UnpatchAll()

	_, err = order.PushOnDeploymentOrderPolling()
	assert.NoError(t, err)
}
