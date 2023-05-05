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

package autotestv2

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func TestGetStepMapByGroupID(t *testing.T) {
	tt := []struct {
		steps            []*apistructs.TestPlanV2Step
		wantGroupIDs     []uint64
		wantStepGroupMap map[uint64][]*apistructs.TestPlanV2Step
	}{
		{
			steps: []*apistructs.TestPlanV2Step{
				{ID: 1, PreID: 0, GroupID: 0, SceneSetID: 1},
				{ID: 2, PreID: 1, GroupID: 2, SceneSetID: 2},
				{ID: 3, PreID: 2, GroupID: 2, SceneSetID: 3},
				{ID: 4, PreID: 3, GroupID: 4, SceneSetID: 4},
				{ID: 5, PreID: 4, GroupID: 4, SceneSetID: 5},
				{ID: 6, PreID: 5, GroupID: 4, SceneSetID: 6},
				{ID: 7, PreID: 6, GroupID: 7, SceneSetID: 7},
			},
			wantGroupIDs: []uint64{1, 2, 4, 7},
			wantStepGroupMap: map[uint64][]*apistructs.TestPlanV2Step{
				1: {{ID: 1, PreID: 0, GroupID: 0, SceneSetID: 1}},
				2: {{ID: 2, PreID: 1, GroupID: 2, SceneSetID: 2}, {ID: 3, PreID: 2, GroupID: 2, SceneSetID: 3}},
				4: {{ID: 4, PreID: 3, GroupID: 4, SceneSetID: 4}, {ID: 5, PreID: 4, GroupID: 4, SceneSetID: 5}, {ID: 6, PreID: 5, GroupID: 4, SceneSetID: 6}},
				7: {{ID: 7, PreID: 6, GroupID: 7, SceneSetID: 7}},
			},
		},
		{
			steps: []*apistructs.TestPlanV2Step{
				{ID: 5, PreID: 0, GroupID: 0, SceneSetID: 1},
				{ID: 3, PreID: 5, GroupID: 3, SceneSetID: 2},
				{ID: 7, PreID: 3, GroupID: 4, SceneSetID: 3},
				{ID: 4, PreID: 7, GroupID: 4, SceneSetID: 4},
				{ID: 1, PreID: 4, GroupID: 1, SceneSetID: 5},
				{ID: 2, PreID: 1, GroupID: 1, SceneSetID: 6},
				{ID: 6, PreID: 2, GroupID: 6, SceneSetID: 7},
			},
			wantGroupIDs: []uint64{5, 3, 4, 1, 6},
			wantStepGroupMap: map[uint64][]*apistructs.TestPlanV2Step{
				5: {{ID: 5, PreID: 0, GroupID: 0, SceneSetID: 1}},
				3: {{ID: 3, PreID: 5, GroupID: 3, SceneSetID: 2}},
				4: {{ID: 7, PreID: 3, GroupID: 4, SceneSetID: 3}, {ID: 4, PreID: 7, GroupID: 4, SceneSetID: 4}},
				1: {{ID: 1, PreID: 4, GroupID: 1, SceneSetID: 5}, {ID: 2, PreID: 1, GroupID: 1, SceneSetID: 6}},
				6: {{ID: 6, PreID: 2, GroupID: 6, SceneSetID: 7}},
			},
		},
	}

	for _, v := range tt {
		stepGroupMap, groupIDs := getStepMapByGroupID(v.steps)
		if !reflect.DeepEqual(v.wantGroupIDs, groupIDs) {
			t.Error("fail")
		}
		for k, v1 := range stepGroupMap {
			for i, v2 := range v1 {
				if v.wantStepGroupMap[k][i].ID != v2.ID {
					t.Error("fail")
				}
			}
		}
	}
}

func TestListExecHistorySceneSetByParentPID(t *testing.T) {
	var DB *dao.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(DB), "ListExecHistorySceneSetByParentPID", func(*dao.DBClient, uint64) (history []dao.AutoTestExecHistory, err error) {
		return []dao.AutoTestExecHistory{
			{
				BaseModel: dbengine.BaseModel{
					ID: 1,
				},
			},
		}, nil
	})
	defer monkey.UnpatchAll()

	svc := Service{db: DB}
	list, err := svc.ListExecHistorySceneSetByParentPID(1)
	if err != nil {
		t.Fatal(err)
	}
	if len(list) != 1 {
		t.Fatal("fail")
	}
}

func TestExecHistoryApiStatusCount(t *testing.T) {
	var DB *dao.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(DB), "ExecHistoryApiStatusCount", func(*dao.DBClient, apistructs.StatisticsExecHistoryRequest) (list []apistructs.ExecHistoryApiStatusCount, err error) {
		return nil, err
	})
	defer monkey.UnpatchAll()
	svc := Service{db: DB}
	_, err := svc.ExecHistoryApiStatusCount(apistructs.StatisticsExecHistoryRequest{})
	if err != nil {
		t.Fatal(err)
	}
}
