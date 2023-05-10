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

package core

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda-infra/providers/i18n"
	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

func Test_provider_CreateStream(t *testing.T) {
	streamFields := map[string][]interface{}{
		"title":            {"a", "b"},
		"state":            {1, 2},
		"plan_started_at":  {"2021-12-01 00:00:00", "2021-12-02 00:00:00"},
		"plan_finished_at": {"2021-12-01 00:00:00", "2021-12-02 00:00:00"},
		"owner":            {"1", "2"},
		"stage":            {"a", "b"},
		"priority":         {apistructs.IssuePriorityLow, apistructs.IssuePriorityHigh},
		"complexity":       {apistructs.IssueComplexityEasy, apistructs.IssueComplexityHard},
		"severity":         {apistructs.IssueSeverityNormal, apistructs.IssueSeveritySerious},
		"content":          {},
		"label":            {},
		"assignee":         {"1", "2"},
		"iteration_id":     {1, 2},
	}
	db := &dao.DBClient{}
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssueStateByID", func(client *dao.DBClient, ID int64) (*dao.IssueState, error) {
		return &dao.IssueState{Name: "a"}, nil
	})
	defer pm1.Unpatch()

	uc := &ucauth.UCClient{}
	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(uc), "FindUsers", func(c *ucauth.UCClient, ids []string) ([]ucauth.User, error) {
		return []ucauth.User{{Name: "a", Nick: "a"}, {Name: "b", Nick: "b"}}, nil
	})
	defer pm2.Unpatch()

	pm3 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssue", func(client *dao.DBClient, id int64) (dao.Issue, error) {
		return dao.Issue{Type: "TASK"}, nil
	})
	defer pm3.Unpatch()

	bdl := &bundle.Bundle{}
	pm4 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProject", func(b *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
		return &apistructs.ProjectDTO{OrgID: 1}, nil
	})
	defer pm4.Unpatch()

	pm5 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStage", func(client *dao.DBClient, orgID int64, issueType string) ([]dao.IssueStage, error) {
		return []dao.IssueStage{{Name: "a"}}, nil
	})
	defer pm5.Unpatch()

	pm6 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIteration", func(client *dao.DBClient, id uint64) (*dao.Iteration, error) {
		return &dao.Iteration{Title: "iteration"}, nil
	})
	defer pm6.Unpatch()

	p := &provider{db: db, bdl: bdl, uc: uc}
	pm7 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "Create", func(s *provider, req *common.IssueStreamCreateRequest) (int64, error) {
		return 1, nil
	})
	defer pm7.Unpatch()

	pm8 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProjectWithSetter",
		func(bdl *bundle.Bundle, id uint64, requestSetter ...httpclient.RequestSetter) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{OrgID: 1}, nil
		},
	)
	defer pm8.Unpatch()
	err := p.CreateStream(&pb.UpdateIssueRequest{Id: 1, IdentityInfo: &commonpb.IdentityInfo{UserID: "1"}}, streamFields)
	assert.NoError(t, err)
}

func Test_provider_HandleIssueStreamChangeIteration(t *testing.T) {
	// mock db to mock iteration
	db := &dao.DBClient{}
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIteration", func(client *dao.DBClient, id uint64) (*dao.Iteration, error) {
		return &dao.Iteration{BaseModel: dbengine.BaseModel{ID: id}, Title: strutil.String(id)}, nil
	})
	svc := &provider{db: db, I18n: &mockTranslator{}}

	// from unassigned to concrete iteration
	streamType, params, err := svc.HandleIssueStreamChangeIteration(nil, apistructs.UnassignedIterationID, 1)
	assert.NoError(t, err)
	assert.Equal(t, common.ISTChangeIterationFromUnassigned, streamType)
	assert.Equal(t, "1", params.NewIteration)

	// from concrete iteration to unassigned
	streamType, params, err = svc.HandleIssueStreamChangeIteration(nil, 2, apistructs.UnassignedIterationID)
	assert.NoError(t, err)
	assert.Equal(t, common.ISTChangeIterationToUnassigned, streamType)
	assert.Equal(t, "2", params.CurrentIteration)

	// from concrete to concrete iteration
	streamType, params, err = svc.HandleIssueStreamChangeIteration(nil, 3, 4)
	assert.NoError(t, err)
	assert.Equal(t, common.ISTChangeIteration, streamType)
	assert.Equal(t, "3", params.CurrentIteration)
	assert.Equal(t, "4", params.NewIteration)
}

func Test_provider_CreateIssueStreamBySystem(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssueStateByID",
		func(d *dao.DBClient, ID int64) (*dao.IssueState, error) {
			return &dao.IssueState{
				BaseModel: dbengine.BaseModel{
					ID: 1,
				},
			}, nil
		},
	)

	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "BatchCreateIssueStream",
		func(d *dao.DBClient, issueStreams []dao.IssueStream) error {
			return nil
		},
	)

	defer p2.Unpatch()

	p := &provider{db: db}
	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "HandleIssueStreamChangeIteration",
		func(d *provider, lang i18n.LanguageCodes, currentIterationID, newIterationID int64) (
			streamType string, params common.ISTParam, err error) {
			return "", common.ISTParam{}, nil
		},
	)

	defer p3.Unpatch()

	streamFields := map[string][]interface{}{
		"title":            {"a", "b", "system"},
		"state":            {1, 2},
		"plan_started_at":  {"2021-12-01 00:00:00", "2021-12-02 00:00:00"},
		"plan_finished_at": {"2021-12-01 00:00:00", "2021-12-02 00:00:00"},
		"label":            {},
		"assignee":         {"1", "2", "system"},
		"iteration_id":     {1, 2},
	}
	err := p.CreateIssueStreamBySystem(1, streamFields)
	assert.NoError(t, err)
}
