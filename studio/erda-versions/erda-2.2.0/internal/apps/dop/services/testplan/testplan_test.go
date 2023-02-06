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
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/iteration"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func TestTestPlan_createAudit(t *testing.T) {
	type args struct {
		testPlan *dao.TestPlan
		req      apistructs.TestPlanUpdateRequest
	}
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProject",
		func(bdl *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateAuditEvent",
		func(bdl *bundle.Bundle, audits *apistructs.AuditCreateRequest) error {
			return nil
		},
	)
	defer monkey.UnpatchAll()

	tr := New(WithBundle(bdl))
	var archive = true
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test",
			args: args{
				testPlan: &dao.TestPlan{},
				req: apistructs.TestPlanUpdateRequest{
					IsArchived: &archive,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := tr.createAudit(tt.args.testPlan, tt.args.req); (err != nil) != tt.wantErr {
				t.Errorf("TestPlan.createAudit() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestGetWithIteration(t *testing.T) {
	db := &dao.DBClient{}
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetTestPlan",
		func(*dao.DBClient, uint64) (*dao.TestPlan, error) {
			return &dao.TestPlan{
				IterationID: 1,
			}, nil
		},
	)
	defer monkey.UnpatchAll()

	monkey.PatchInstanceMethod(reflect.TypeOf(db), "ListTestPlanMembersByPlanID",
		func(*dao.DBClient, uint64, ...apistructs.TestPlanMemberRole) ([]dao.TestPlanMember, error) {
			return nil, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(db), "ListTestPlanCaseRelsCount",
		func(*dao.DBClient, []uint64) (map[uint64]apistructs.TestPlanRelsCount, error) {
			return nil, nil
		},
	)

	var iterationSvc *iteration.Iteration
	monkey.PatchInstanceMethod(reflect.TypeOf(iterationSvc), "Get",
		func(svc *iteration.Iteration, id uint64) (*dao.Iteration, error) {
			if id == 1 {
				return &dao.Iteration{
					BaseModel: dbengine.BaseModel{ID: 1},
					Title:     "erda1.1",
				}, nil
			} else {
				return &dao.Iteration{
					BaseModel: dbengine.BaseModel{ID: 1},
					Title:     "erda",
				}, nil
			}

		},
	)
	tr := New(WithDBClient(db), WithIterationSvc(iterationSvc))
	dto, err := tr.Get(1)
	if err != nil {
		t.Error(err)
	}
	if dto.IterationName != "erda1.1" {
		t.Error("fail")
	}
}

func TestPagingWithIteration(t *testing.T) {
	db := &dao.DBClient{}
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "PagingTestPlan",
		func(*dao.DBClient, apistructs.TestPlanPagingRequest) (uint64, []dao.TestPlan, error) {
			return 1, []dao.TestPlan{
				{
					BaseModel:   dbengine.BaseModel{ID: 1},
					IterationID: 1,
				},
			}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(db), "ListTestPlanMembersByPlanIDs",
		func(*dao.DBClient, []uint64, ...apistructs.TestPlanMemberRole) (map[uint64][]dao.TestPlanMember, error) {
			return nil, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(db), "ListTestPlanCaseRelsCount",
		func(*dao.DBClient, []uint64) (map[uint64]apistructs.TestPlanRelsCount, error) {
			return nil, nil
		},
	)

	var iterationSvc *iteration.Iteration
	monkey.PatchInstanceMethod(reflect.TypeOf(iterationSvc), "Paging",
		func(svc *iteration.Iteration, req apistructs.IterationPagingRequest) ([]dao.Iteration, uint64, error) {
			if req.IDs[0] == 1 {
				return []dao.Iteration{{
					BaseModel: dbengine.BaseModel{ID: 1},
					Title:     "erda1.1",
				}}, 1, nil
			} else {
				return nil, 0, nil
			}
		},
	)
	defer monkey.UnpatchAll()

	tr := New(WithDBClient(db), WithIterationSvc(iterationSvc))
	result, err := tr.Paging(apistructs.TestPlanPagingRequest{
		Statuses:  []apistructs.TPStatus{apistructs.TPStatusDoing},
		ProjectID: 1,
	})
	if err != nil {
		t.Error(err)
	}
	if result == nil || len(result.List) == 0 {
		t.Error("fail")
	}
	if result.List[0].IterationName != "erda1.1" {
		t.Error("fail")
	}
}

func TestUpdate(t *testing.T) {
	db := &dao.DBClient{}
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetTestPlan", func(*dao.DBClient, uint64) (*dao.TestPlan, error) {
		return &dao.TestPlan{}, nil
	})
	defer monkey.UnpatchAll()

	monkey.PatchInstanceMethod(reflect.TypeOf(db), "UpdateTestPlan", func(*dao.DBClient, *dao.TestPlan) error {
		return nil
	})
	tp := TestPlan{
		db: db,
	}
	if err := tp.Update(apistructs.TestPlanUpdateRequest{}); err != nil {
		t.Error(err)
	}
}

func TestTestPlan_PagingTestPlanCaseRels(t *testing.T) {
	tp := &TestPlan{}
	monkey.PatchInstanceMethod(reflect.TypeOf(tp), "Get", func(_ *TestPlan, _ uint64) (*apistructs.TestPlan, error) {
		return &apistructs.TestPlan{Name: "tp"}, nil
	})

	// req without testPlan
	req1 := apistructs.TestPlanCaseRelPagingRequest{TestPlan: nil}
	_, err := tp.PagingTestPlanCaseRels(req1)
	assert.Error(t, err)

	// req with testPlan, but rels is empty
	req2 := apistructs.TestPlanCaseRelPagingRequest{TestPlan: &apistructs.TestPlan{ID: 2, ProjectID: 2, Name: "tp2"}}
	dbWithPagingZero := &dao.DBClient{}
	monkey.PatchInstanceMethod(reflect.TypeOf(dbWithPagingZero), "PagingTestPlanCaseRelations", func(*dao.DBClient, apistructs.TestPlanCaseRelPagingRequest) ([]dao.TestPlanCaseRelDetail, uint64, error) {
		data := []dao.TestPlanCaseRelDetail{}
		return data, uint64(len(data)), nil
	})
	tp.db = dbWithPagingZero
	result, err := tp.PagingTestPlanCaseRels(req2)
	assert.NoError(t, err)
	assert.True(t, 0 == result.Total)
	monkey.UnpatchInstanceMethod(reflect.TypeOf(dbWithPagingZero), "PagingTestPlanCaseRelations")

	// req with testPlan, and return some rels
	req3 := apistructs.TestPlanCaseRelPagingRequest{TestPlan: &apistructs.TestPlan{ID: 3, ProjectID: 3, Name: "tp3"}}
	dbWithPaging3 := &dao.DBClient{}
	monkey.PatchInstanceMethod(reflect.TypeOf(dbWithPaging3), "PagingTestPlanCaseRelations", func(*dao.DBClient, apistructs.TestPlanCaseRelPagingRequest) ([]dao.TestPlanCaseRelDetail, uint64, error) {
		data := []dao.TestPlanCaseRelDetail{
			{Name: "rel1", TestPlanCaseRel: dao.TestPlanCaseRel{TestPlanID: 3, TestSetID: 1, TestCaseID: 1}},
			{Name: "rel2", TestPlanCaseRel: dao.TestPlanCaseRel{TestPlanID: 3, TestSetID: 2, TestCaseID: 2}},
			{Name: "rel3", TestPlanCaseRel: dao.TestPlanCaseRel{TestPlanID: 3, TestSetID: 2, TestCaseID: 3}},
		}
		return data, uint64(len(data)), nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(dbWithPaging3), "ListTestSets", func(*dao.DBClient, apistructs.TestSetListRequest) ([]dao.TestSet, error) {
		data := []dao.TestSet{
			{BaseModel: dbengine.BaseModel{ID: 1}, Name: "ts1"},
			{BaseModel: dbengine.BaseModel{ID: 2}, Name: "ts2"},
		}
		return data, nil
	})
	tp.db = dbWithPaging3
	result, err = tp.PagingTestPlanCaseRels(req3)
	assert.NoError(t, err)
	assert.True(t, 3 == result.Total)
	monkey.UnpatchInstanceMethod(reflect.TypeOf(dbWithPaging3), "PagingTestPlanCaseRelations")
}
