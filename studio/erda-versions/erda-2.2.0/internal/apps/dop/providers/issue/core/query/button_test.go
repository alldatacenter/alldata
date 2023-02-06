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

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/i18n"
)

func Test_provider_generateButtonMap(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStateRelations",
		func(d *dao.DBClient, projectID uint64, issueType string) ([]dao.IssueStateJoinSQL, error) {
			return []dao.IssueStateJoinSQL{
				{
					ProjectID: projectID,
					Name:      "s1",
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStatesByProjectID",
		func(d *dao.DBClient, projectID uint64, issueType string) ([]dao.IssueState, error) {
			return []dao.IssueState{
				{
					ProjectID: projectID,
					Name:      "s2",
				},
			}, nil
		},
	)
	defer p2.Unpatch()

	p := &provider{db: db}
	_, err := p.GenerateButtonMap(1, []string{"TASK"})
	assert.NoError(t, err)
}

func Test_provider_generateButtonWithoutPerm(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStateRelations",
		func(d *dao.DBClient, projectID uint64, issueType string) ([]dao.IssueStateJoinSQL, error) {
			return []dao.IssueStateJoinSQL{
				{
					ProjectID: projectID,
					Name:      "s1",
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStatesByProjectID",
		func(d *dao.DBClient, projectID uint64, issueType string) ([]dao.IssueState, error) {
			return []dao.IssueState{
				{
					ProjectID: projectID,
					Name:      "s2",
				},
			}, nil
		},
	)
	defer p2.Unpatch()

	p := &provider{db: db}

	is := dao.IssueStateRelation{
		ProjectID: 1,
	}
	relations := map[dao.IssueStateRelation]bool{
		is: true,
	}
	ist := &pb.IssueStateButton{StateName: "3"}
	ts := map[string][]*pb.IssueStateButton{
		"1": {ist},
	}
	_, err := p.GenerateButtonWithoutPerm(dao.Issue{Title: "s"}, relations, ts)
	assert.NoError(t, err)
}

func Test_provider_StateCheckPermission(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssueStatePermission",
		func(d *dao.DBClient, role string, st int64, ed int64) (*dao.IssueStateRelation, error) {
			return &dao.IssueStateRelation{ProjectID: 1}, nil
		},
	)
	defer p1.Unpatch()

	bdl := bundle.New(bundle.WithI18nLoader(&i18n.LocaleResourceLoader{}))
	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "StateCheckPermission",
		func(bdl *bundle.Bundle, req *apistructs.PermissionCheckRequest) (*apistructs.StatePermissionCheckResponseData, error) {
			return &apistructs.StatePermissionCheckResponseData{
				Access: true,
				Roles:  []string{"DEV"},
			}, nil
		})
	defer p2.Unpatch()

	p := &provider{db: db, bdl: bdl}
	_, err := p.StateCheckPermission(nil, 1, 2)
	assert.NoError(t, err)
}

func Test_provider_GenerateButton(t *testing.T) {
	p := &provider{}
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "GenerateButtonWithoutPerm",
		func(d *provider, issueModel dao.Issue, relations map[dao.IssueStateRelation]bool, typeState map[string][]*pb.IssueStateButton) ([]*pb.IssueStateButton, error) {
			return []*pb.IssueStateButton{
				{
					StateID:    1,
					Permission: true,
				},
			}, nil
		},
	)
	defer p1.Unpatch()
	_, err := p.GenerateButton(dao.Issue{ProjectID: 1}, nil, map[string]bool{"1": true}, nil, nil, nil)
	assert.NoError(t, err)
}
