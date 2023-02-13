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

package query

import (
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"
	"google.golang.org/protobuf/types/known/timestamppb"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/i18n"
)

func Test_provider_convertIssueToExcelList(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStatesByProjectID",
		func(d *dao.DBClient, projectID uint64, issueType string) ([]dao.IssueState, error) {
			return []dao.IssueState{
				{
					ProjectID: projectID,
					Name:      "待处理",
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "PagingIterations",
		func(d *dao.DBClient, req apistructs.IterationPagingRequest) ([]dao.Iteration, uint64, error) {
			return []dao.Iteration{
				{
					ProjectID: req.ProjectID,
					Title:     "1.1",
				},
			}, 1, nil
		},
	)
	defer p2.Unpatch()

	bdl := bundle.New(bundle.WithI18nLoader(&i18n.LocaleResourceLoader{}))
	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetLocale",
		func(bdl *bundle.Bundle, local ...string) *i18n.LocaleResource {
			return &i18n.LocaleResource{}
		})
	defer p3.Unpatch()

	p4 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "PagingPropertyRelationByIDs",
		func(d *dao.DBClient, issueID []int64) ([]dao.IssuePropertyRelation, error) {
			return []dao.IssuePropertyRelation{
				{
					IssueID: 1,
				},
			}, nil
		},
	)
	defer p4.Unpatch()

	p := &provider{db: db, bdl: bdl}
	p5 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "GetIssueRelationsByIssueIDs",
		func(p *provider, issueID uint64, relationType []string) ([]uint64, []uint64, error) {
			return []uint64{}, []uint64{}, nil
		},
	)
	defer p5.Unpatch()

	finishTime := time.Now()
	_, err := p.convertIssueToExcelList([]*pb.Issue{{Id: 1, FinishTime: timestamppb.New(finishTime)}}, []*pb.IssuePropertyIndex{}, 1, false, map[IssueStage]string{}, "cn")
	assert.Equal(t, err, nil)
}

func Test_provider_getIssueExportDataI18n(t *testing.T) {
	bdl := bundle.New(bundle.WithI18nLoader(&i18n.LocaleResourceLoader{}))
	m := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetLocale",
		func(bdl *bundle.Bundle, local ...string) *i18n.LocaleResource {
			return &i18n.LocaleResource{}
		})
	defer m.Unpatch()

	p := &provider{bdl: bdl}
	content := ",a,b,c"
	expected := []string{"", "a", "b", "c"}
	strs := p.getIssueExportDataI18n("testKey", content)
	assert.Equal(t, strs, expected)
}

func Test_provider_ConvertWithoutButton(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetLabelRelationsByRef",
		func(d *dao.DBClient, refType string, refID string) ([]dao.LabelRelation, error) {
			return []dao.LabelRelation{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	m := dao.Issue{
		BaseModel: dbengine.BaseModel{ID: 1},
	}
	p := &provider{db: db}
	pl := map[uint64]apistructs.ProjectLabel{
		1: {
			ID: 1,
		},
	}
	got := p.ConvertWithoutButton(m, true, []uint64{1}, false, pl)
	assert.Equal(t, m.ID, uint64(got.Id))
}

func Test_provider_Convert(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssueSubscribersByIssueID",
		func(d *dao.DBClient, issueID int64) ([]dao.IssueSubscriber, error) {
			return []dao.IssueSubscriber{
				{
					IssueID: 1,
					UserID:  "2",
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p := &provider{db: db}
	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "ConvertWithoutButton",
		func(p *provider, model dao.Issue,
			needQueryLabelRef bool, labelIDs []uint64,
			needQueryLabels bool, projectLabels map[uint64]apistructs.ProjectLabel,
		) *pb.Issue {
			return &pb.Issue{
				Id: 1,
			}
		},
	)
	defer p2.Unpatch()

	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "GenerateButton",
		func(p *provider, issueModel dao.Issue, identityInfo *commonpb.IdentityInfo,
			permCheckItems map[string]bool, store map[string]bool, relations map[dao.IssueStateRelation]bool,
			typeState map[string][]*pb.IssueStateButton) ([]*pb.IssueStateButton, error) {
			return []*pb.IssueStateButton{
				{StateID: 1},
			}, nil
		},
	)
	defer p3.Unpatch()

	_, err := p.Convert(dao.Issue{BaseModel: dbengine.BaseModel{ID: 1}}, nil)
	assert.NoError(t, err)
}

func Test_provider_BatchConvert(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "BatchQueryIssueLabelIDMap",
		func(d *dao.DBClient, issueIDs []int64) (map[uint64][]uint64, error) {
			return map[uint64][]uint64{
				1: {1, 2},
			}, nil
		},
	)
	defer p1.Unpatch()

	bdl := bundle.New()
	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListLabel",
		func(bdl *bundle.Bundle, req apistructs.ProjectLabelListRequest) (*apistructs.ProjectLabelListResponseData, error) {
			return &apistructs.ProjectLabelListResponseData{
				Total: 2,
				List: []apistructs.ProjectLabel{
					{
						ID: 1,
					},
				},
			}, nil
		})
	defer p2.Unpatch()

	p := &provider{db: db, bdl: bdl}
	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "ConvertWithoutButton",
		func(p *provider, model dao.Issue,
			needQueryLabelRef bool, labelIDs []uint64,
			needQueryLabels bool, projectLabels map[uint64]apistructs.ProjectLabel,
		) *pb.Issue {
			return &pb.Issue{
				Id: 1,
			}
		},
	)
	defer p3.Unpatch()

	p4 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "GenerateButtonMap",
		func(p *provider, projectID uint64, issueTypes []string) (map[string]map[int64][]*pb.IssueStateButton, error) {
			return nil, nil
		},
	)
	defer p4.Unpatch()
	_, err := p.BatchConvert([]dao.Issue{{ProjectID: 1}}, []string{"TASK", "BUG"})
	assert.NoError(t, err)
}
