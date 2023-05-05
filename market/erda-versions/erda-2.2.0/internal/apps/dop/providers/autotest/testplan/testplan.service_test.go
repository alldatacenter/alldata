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

package testplan

import (
	"reflect"
	"sort"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-proto-go/core/dop/autotest/testplan/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/autotest/testplan/db"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

func Test_convertTime(t *testing.T) {
	time, err := convertUTCTime("2020-01-02 04:00:00")
	assert.NoError(t, err)
	s := time.Format("2006-01-02 15:04:05")
	want := "2020-01-01 20:00:00"
	assert.Equal(t, want, s)
}

func Test_processEvent(t *testing.T) {
	svc := &autotestv2.Service{}
	bdl := &bundle.Bundle{}
	p := &TestPlanService{
		bdl:         bdl,
		autoTestSvc: svc,
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(svc), "GetTestPlanV2",
		func(svc *autotestv2.Service, testPlanID uint64, identityInfo apistructs.IdentityInfo) (*apistructs.TestPlanV2, error) {
			return &apistructs.TestPlanV2{
				Name:      "test",
				ProjectID: 1,
			}, nil
		})
	defer monkey.UnpatchAll()
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryNotifiesBySource",
		func(b *bundle.Bundle, orgID string, sourceType, sourceID, itemName, label string, clusterNames ...string) ([]*apistructs.NotifyDetail, error) {
			return []*apistructs.NotifyDetail{
				{
					NotifyGroup: &apistructs.NotifyGroup{},
					NotifyItems: []*apistructs.NotifyItem{
						{},
					},
				},
			}, nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateGroupNotifyEvent",
		func(b *bundle.Bundle, groupNotifyRequest apistructs.EventBoxGroupNotifyRequest) error {
			want := map[string]string{
				"org_name":         "org",
				"project_name":     "project",
				"plan_name":        "test",
				"pass_rate":        "10.12",
				"execute_duration": "",
				"api_total_num":    "100",
			}
			assert.True(t, reflect.DeepEqual(want, groupNotifyRequest.Params))
			return nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProject",
		func(b *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{
				Name:  "project",
				OrgID: uint64(1),
				ID:    uint64(1),
			}, nil
		})

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProjectWithSetter",
		func(bdl *bundle.Bundle, id uint64, requestSetter ...httpclient.RequestSetter) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{
				Name:  "project",
				OrgID: uint64(1),
				ID:    uint64(1),
			}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetOrg",
		func(b *bundle.Bundle, idOrName interface{}) (*apistructs.OrgDTO, error) {
			return &apistructs.OrgDTO{
				Name: "org",
			}, nil
		})

	err := p.ProcessEvent(&pb.Content{
		TestPlanID:  1,
		ExecuteTime: "2006-01-02 15:04:05",
		PassRate:    10.123,
		ApiTotalNum: 100,
	})
	assert.NoError(t, err)
}

func TestParseExecuteTime(t *testing.T) {
	ti := time.Date(2006, 1, 2, 15, 4, 5, 0, time.Local)

	tt := []struct {
		value string
		want  *time.Time
	}{
		{"2006-01-02 15:04:05",
			&ti,
		},
		{"2006-01-02",
			nil,
		},
	}
	for _, v := range tt {
		assert.Equal(t, v.want, parseExecuteTime(v.value))
	}
}

func TestGetCostTime(t *testing.T) {
	tt := []struct {
		costTimeSec int64
		want        string
	}{
		{
			costTimeSec: 59,
			want:        "00:00:59",
		},

		{
			costTimeSec: 3600,
			want:        "01:00:00",
		},

		{
			costTimeSec: 59*60 + 59,
			want:        "00:59:59",
		},

		{
			costTimeSec: -1,
			want:        "-",
		},
	}
	for _, v := range tt {
		assert.Equal(t, v.want, getCostTime(v.costTimeSec))
	}
}

func TestCreateTestPlanExecHistory(t *testing.T) {
	var (
		bdl bundle.Bundle
	)

	DB := db.TestPlanDB{
		DBClient: &dao.DBClient{
			DBEngine: &dbengine.DBEngine{
				DB: &gorm.DB{},
			},
		},
	}

	monkey.PatchInstanceMethod(reflect.TypeOf(&DB), "GetTestPlan", func(*db.TestPlanDB, uint64) (*db.TestPlanV2, error) {
		return &db.TestPlanV2{}, nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(&gorm.DB{}), "Create", func(_ *gorm.DB, value interface{}) *gorm.DB {
		return &gorm.DB{}
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(&bdl), "GetProject", func(*bundle.Bundle, uint64) (*apistructs.ProjectDTO, error) {
		return &apistructs.ProjectDTO{}, nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(&bdl), "GetProjectWithSetter",
		func(bdl *bundle.Bundle, id uint64, requestSetter ...httpclient.RequestSetter) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{}, nil
		},
	)

	defer monkey.UnpatchAll()

	svc := TestPlanService{
		db:  DB,
		bdl: &bdl,
	}
	err := svc.createTestPlanExecHistory(&pb.TestPlanUpdateByHookRequest{
		Content: &pb.Content{
			ExecuteTime: "2006-01-02 15:04:05",
		},
	})
	if err != nil {
		t.Error("fail")
	}
}

func TestGetSceneIDsIncludeRef(t *testing.T) {
	var DB db.TestPlanDB
	monkey.PatchInstanceMethod(reflect.TypeOf(&DB), "ListSceneBySceneSetID", func(DB *db.TestPlanDB, setID ...uint64) (scenes []db.AutoTestScene, err error) {
		if len(setID) > 1 {
			return []db.AutoTestScene{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
					SetID:    1,
					RefSetID: 0,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 2,
					},
					SetID:    1,
					RefSetID: 2,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 3,
					},
					SetID:    1,
					RefSetID: 3,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 4,
					},
					SetID:    2,
					RefSetID: 0,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 5,
					},
					SetID:    2,
					RefSetID: 3,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 6,
					},
					SetID:    3,
					RefSetID: 0,
				},
			}, nil
		} else if setID[0] == 1 {
			return []db.AutoTestScene{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
					SetID:    1,
					RefSetID: 0,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 2,
					},
					SetID:    1,
					RefSetID: 2,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 3,
					},
					SetID:    1,
					RefSetID: 3,
				},
			}, nil
		} else if setID[0] == 2 {
			return []db.AutoTestScene{
				{
					BaseModel: dbengine.BaseModel{
						ID: 4,
					},
					SetID:    2,
					RefSetID: 0,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 5,
					},
					SetID:    2,
					RefSetID: 3,
				},
			}, nil
		} else if setID[0] == 3 {
			return []db.AutoTestScene{
				{
					BaseModel: dbengine.BaseModel{
						ID: 6,
					},
					SetID:    3,
					RefSetID: 0,
				},
			}, nil
		}
		return []db.AutoTestScene{}, nil
	})
	defer monkey.UnpatchAll()

	svc := TestPlanService{
		db: DB,
	}
	sceneIDs := svc.getSceneIDsIncludeRef(map[uint64]uint64{}, 1, 1, 2, 3)
	sort.Slice(sceneIDs, func(i, j int) bool {
		return sceneIDs[i] < sceneIDs[j]
	})
	if !reflect.DeepEqual([]uint64{1, 1, 4, 4, 4, 6, 6, 6, 6, 6, 6}, sceneIDs) {
		t.Error("fail")
	}
}

func TestGetSceneIDsIncludeRefWithCircularRef(t *testing.T) {
	var DB db.TestPlanDB
	monkey.PatchInstanceMethod(reflect.TypeOf(&DB), "ListSceneBySceneSetID", func(DB *db.TestPlanDB, setID ...uint64) (scenes []db.AutoTestScene, err error) {
		if len(setID) > 1 {
			return []db.AutoTestScene{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
					RefSetID: 0,
					SetID:    1,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 2,
					},
					RefSetID: 2,
					SetID:    1,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 3,
					},
					RefSetID: 1,
					SetID:    1,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 4,
					},
					RefSetID: 0,
					SetID:    1,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 5,
					},
					RefSetID: 0,
					SetID:    2,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 6,
					},
					RefSetID: 1,
					SetID:    2,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 7,
					},
					RefSetID: 0,
					SetID:    2,
				},
			}, nil
		} else if setID[0] == 1 {
			return []db.AutoTestScene{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
					RefSetID: 0,
					SetID:    1,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 2,
					},
					RefSetID: 2,
					SetID:    1,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 3,
					},
					RefSetID: 1,
					SetID:    1,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 4,
					},
					RefSetID: 0,
					SetID:    1,
				},
			}, nil
		} else if setID[0] == 2 {
			return []db.AutoTestScene{
				{
					BaseModel: dbengine.BaseModel{
						ID: 5,
					},
					RefSetID: 0,
					SetID:    2,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 6,
					},
					RefSetID: 1,
					SetID:    2,
				},
				{
					BaseModel: dbengine.BaseModel{
						ID: 7,
					},
					RefSetID: 0,
					SetID:    2,
				},
			}, nil
		}
		return []db.AutoTestScene{}, nil
	})
	defer monkey.UnpatchAll()
	svc := TestPlanService{
		db: DB,
	}
	sceneIDs := svc.getSceneIDsIncludeRef(map[uint64]uint64{}, 1, 2)
	sort.Slice(sceneIDs, func(i, j int) bool {
		return sceneIDs[i] < sceneIDs[j]
	})
	if !reflect.DeepEqual([]uint64{1, 5}, sceneIDs) {
		t.Error("fail")
	}
}

func TestCalcRate(t *testing.T) {
	tt := []struct {
		num   int64
		total int64
		want  float64
	}{
		{2, 0, 0},
		{2, 3, 66.66666666666666},
		{2, 5, 40},
	}
	for _, v := range tt {
		assert.Equal(t, v.want, calcRate(v.num, v.total))
	}
}

func TestCountApiBySceneIDRepeat(t *testing.T) {
	var DB db.TestPlanDB
	monkey.PatchInstanceMethod(reflect.TypeOf(&DB), "CountApiBySceneID", func(*db.TestPlanDB, ...uint64) (apiCounts []db.ApiCount, err error) {
		return []db.ApiCount{
			{
				Count:   10,
				SceneID: 1,
			},
			{
				Count:   5,
				SceneID: 2,
			},
			{
				Count:   2,
				SceneID: 3,
			},
		}, nil
	})
	defer monkey.UnpatchAll()
	svc := TestPlanService{
		db: DB,
	}
	count, err := svc.countApiBySceneIDRepeat(1, 2, 2, 3, 3, 3)
	if err != nil {
		t.Error(err)
	}
	if count != 1*10+2*5+3*2 {
		t.Error("fail")
	}
}

func TestGetSceneIDsNotIncludeRef(t *testing.T) {
	var DB db.TestPlanDB
	monkey.PatchInstanceMethod(reflect.TypeOf(&DB), "ListSceneBySceneSetID", func(DB *db.TestPlanDB, setID ...uint64) (scenes []db.AutoTestScene, err error) {
		return []db.AutoTestScene{
			{
				BaseModel: dbengine.BaseModel{
					ID: 1,
				},
				SetID:    1,
				RefSetID: 0,
			},
			{
				BaseModel: dbengine.BaseModel{
					ID: 2,
				},
				SetID:    1,
				RefSetID: 0,
			},
			{
				BaseModel: dbengine.BaseModel{
					ID: 3,
				},
				SetID:    1,
				RefSetID: 3,
			},
			{
				BaseModel: dbengine.BaseModel{
					ID: 4,
				},
				SetID:    2,
				RefSetID: 2,
			},
			{
				BaseModel: dbengine.BaseModel{
					ID: 5,
				},
				SetID:    2,
				RefSetID: 0,
			},
		}, nil
	})
	defer monkey.UnpatchAll()

	svc := TestPlanService{db: DB}

	sceneIDs := svc.getSceneIDsNotIncludeRef(1, 1, 2)
	sort.Slice(sceneIDs, func(i, j int) bool {
		return sceneIDs[i] < sceneIDs[j]
	})
	if !reflect.DeepEqual([]uint64{1, 1, 2, 2, 5}, sceneIDs) {
		t.Error("fail")
	}
}
