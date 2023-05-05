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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func Test_filterReceiversByOperatorID(t *testing.T) {
	type args struct {
		receivers  []string
		operatorID string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			args: args{
				receivers:  []string{"a", "b"},
				operatorID: "b",
			},
			want: []string{"a"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := filterReceiversByOperatorID(tt.args.receivers, tt.args.operatorID); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("filterReceiversByOperatorID() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_groupEventContent(t *testing.T) {
	content, err := groupEventContent([]string{common.ISTChangeContent}, common.ISTParam{}, &mockTranslator{}, "zh")
	assert.NoError(t, err)
	assert.Equal(t, "内容发生变更", content)
}

func Test_provider_CreateIssueEvent(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssue",
		func(d *dao.DBClient, id int64) (dao.Issue, error) {
			return dao.Issue{
				BaseModel: dbengine.BaseModel{
					ID: 1,
				},
				Type: "TASK",
			}, nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetReceiversByIssueID",
		func(d *dao.DBClient, issueID int64) ([]string, error) {
			return []string{"2"}, nil
		},
	)
	defer p2.Unpatch()

	var bdl *bundle.Bundle
	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProject",
		func(d *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{
				ID: 1,
			}, nil
		},
	)
	defer p3.Unpatch()

	p4 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetOrg",
		func(d *bundle.Bundle, idOrName interface{}) (*apistructs.OrgDTO, error) {
			return &apistructs.OrgDTO{
				ID: 1,
			}, nil
		},
	)
	defer p4.Unpatch()

	p5 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetCurrentUser",
		func(d *bundle.Bundle, userID string) (*apistructs.UserInfo, error) {
			return &apistructs.UserInfo{
				ID: "2",
			}, nil
		},
	)
	defer p5.Unpatch()

	p6 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateEvent",
		func(d *bundle.Bundle, ev *apistructs.EventCreateRequest) error {
			return nil
		},
	)
	defer p6.Unpatch()

	p7 := monkey.Patch(getDefaultContentForMsgSending,
		func(ist string, param common.ISTParam, tran i18n.Translator, locale string) (string, error) {
			return "1", nil
		},
	)
	defer p7.Unpatch()

	p := &provider{db: db, bdl: bdl}
	err := p.CreateIssueEvent(&common.IssueStreamCreateRequest{
		IssueID: 1,
	})
	assert.NoError(t, err)
	err = p.CreateIssueEvent(&common.IssueStreamCreateRequest{
		IssueID:    1,
		StreamType: common.ISTChangeContent,
	})
	assert.NoError(t, err)
}
