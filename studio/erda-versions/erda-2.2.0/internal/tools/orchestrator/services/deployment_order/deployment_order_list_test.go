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
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
)

func TestListDeploymentOrder(t *testing.T) {
	bdl := bundle.New()
	order := New()

	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetMyApps", func(*bundle.Bundle, string, uint64) (*apistructs.ApplicationListResponseData, error) {
		return &apistructs.ApplicationListResponseData{}, nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "ListDeploymentOrder", func(*dbclient.DBClient, *apistructs.DeploymentOrderListConditions, *apistructs.PageInfo) (int, []dbclient.DeploymentOrder, error) {
		return 0, []dbclient.DeploymentOrder{}, nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "ListReleases", func(*dbclient.DBClient, []string) ([]*dbclient.Release, error) {
		return []*dbclient.Release{}, nil
	})

	got, err := order.List("1", 1, &apistructs.DeploymentOrderListConditions{}, &apistructs.PageInfo{})
	assert.NoError(t, err)
	assert.Equal(t, got, &apistructs.DeploymentOrderListData{
		Total: 0,
		List:  []*apistructs.DeploymentOrderItem{},
	})

}

func TestConvertDeploymentOrderToResponseItem(t *testing.T) {
	order := New()

	defer monkey.UnpatchAll()

	modes := map[string]apistructs.ReleaseDeployMode{
		"default": {
			Expose:                 true,
			ApplicationReleaseList: [][]string{{"2deb000b57bfac9d72c14d4ed967b572"}, {"523af537946b79c4f8369ed39ba78605"}},
		},
	}
	modesData, err := json.Marshal(modes)
	if err != nil {
		t.Fatal(err)
	}

	monkey.PatchInstanceMethod(reflect.TypeOf(order.db), "ListReleases", func(*dbclient.DBClient, []string) ([]*dbclient.Release, error) {
		return []*dbclient.Release{
			{
				ReleaseId:        "68a6df7529914c89b632fb18450d0055",
				IsProjectRelease: true,
				Modes:            string(modesData),
			},
		}, nil
	})

	statusMapJson, err := getFakeStatusMap()
	assert.NoError(t, err)

	deployList := [][]string{{"id1"}, {"id2"}}
	deployData, err := json.Marshal(deployList)
	if err != nil {
		t.Fatal(err)
	}
	data := []dbclient.DeploymentOrder{
		{
			Type:         apistructs.TypeProjectRelease,
			ReleaseId:    "68a6df7529914c89b632fb18450d0055",
			Operator:     "1",
			StatusDetail: string(statusMapJson),
			IsOutdated:   0,
			DeployList:   string(deployData),
		},
	}

	res, err := order.convertDeploymentOrderToResponseItem(data)
	assert.NoError(t, err)
	assert.Equal(t, len(res), 1)
	assert.Equal(t, res[0].Status, apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusDeploying))
	assert.Equal(t, res[0].ApplicationStatus, "1/2")
}

func TestParseApplicationStatus(t *testing.T) {
	type args struct {
		DeploymentStatus apistructs.DeploymentOrderStatusMap
	}
	tests := []struct {
		name string
		args args
		want int
	}{
		{
			name: "apps",
			args: args{
				DeploymentStatus: apistructs.DeploymentOrderStatusMap{
					"app-1": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusWaiting,
					},
					"app-2": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusOK,
					},
					"app-3": apistructs.DeploymentOrderStatusItem{
						DeploymentStatus: apistructs.DeploymentStatusOK,
					},
				},
			},
			want: 2,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := parseApplicationStatus(tt.args.DeploymentStatus)

			if tt.want != got {
				t.Errorf("parseApplicationStatus got = %v, want %v", got, tt.want)
			}
		})
	}
}

func getFakeStatusMap() ([]byte, error) {
	statusMap := apistructs.DeploymentOrderStatusMap{
		"app-1": {DeploymentStatus: apistructs.DeploymentStatusDeploying},
		"app-2": {DeploymentStatus: apistructs.DeploymentStatusOK},
	}

	return json.Marshal(statusMap)
}
