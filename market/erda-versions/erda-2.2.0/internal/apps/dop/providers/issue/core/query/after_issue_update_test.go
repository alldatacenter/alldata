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

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func Test_provider_SetIssueChildrenCount(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "IssueChildrenCount",
		func(d *dao.DBClient, issueIDs []uint64, relationType []string) ([]dao.ChildrenCount, error) {
			return []dao.ChildrenCount{
				{
					IssueID: 1,
					Count:   2,
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	issues := []dao.IssueItem{
		{
			BaseModel: dbengine.BaseModel{ID: 1},
		},
	}
	p := &provider{db: db}
	err := p.SetIssueChildrenCount(issues)
	assert.NoError(t, err)
	assert.Equal(t, 2, issues[0].ChildrenLength)
}

func Test_provider_AfterIssueUpdate(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssueParents",
		func(d *dao.DBClient, issueID uint64, relationType []string) ([]dao.IssueItem, error) {
			return []dao.IssueItem{
				{
					ProjectID: 1,
					Name:      "s1",
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "UpdateIssue",
		func(d *dao.DBClient, id uint64, fields map[string]interface{}) error {
			return nil
		},
	)
	defer p2.Unpatch()

	p := &provider{db: db}
	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "UpdateIssuePlanTimeByIteration",
		func(p *provider, u *IssueUpdated, c *issueValidationConfig) error {
			return nil
		},
	)
	defer p3.Unpatch()

	p4 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "GetNextAvailableState",
		func(p *provider, issue *dao.Issue) (*pb.IssueStateButton, error) {
			return nil, nil
		},
	)
	defer p4.Unpatch()

	p5 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "AfterIssueInclusionRelationChange",
		func(p *provider, id uint64) error {
			return nil
		},
	)
	defer p5.Unpatch()

	err := p.AfterIssueUpdate(&IssueUpdated{})
	assert.NoError(t, err)
}

func Test_provider_GetIssueChildren(t *testing.T) {
	type args struct {
		id  uint64
		req pb.PagingIssueRequest
	}
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "FindIssueRoot",
		func(d *dao.DBClient, req pb.PagingIssueRequest) ([]dao.IssueItem, []dao.IssueItem, uint64, error) {
			return []dao.IssueItem{
				{
					ProjectID: 1,
					Name:      "s1",
				},
			}, nil, 1, nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "FindIssueChildren",
		func(d *dao.DBClient, id uint64, req pb.PagingIssueRequest) ([]dao.IssueItem, uint64, error) {
			return []dao.IssueItem{
				{
					ProjectID: 1,
					Name:      "s1",
				},
			}, 1, nil
		},
	)
	defer p2.Unpatch()

	p := &provider{db: db}
	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "SetIssueChildrenCount",
		func(d *provider, issues []dao.IssueItem) error {
			return nil
		},
	)
	defer p3.Unpatch()

	tests := []struct {
		name    string
		args    args
		want    []dao.IssueItem
		want1   uint64
		wantErr bool
	}{
		{
			args: args{
				id: 1,
				req: pb.PagingIssueRequest{
					ProjectID: 1,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, _, err := p.GetIssueChildren(tt.args.id, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("provider.GetIssueChildren() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_provider_GetNextAvailableState(t *testing.T) {
	p := &provider{}
	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "GenerateButton",
		func(d *provider, issueModel dao.Issue, identityInfo *commonpb.IdentityInfo,
			permCheckItems map[string]bool, store map[string]bool, relations map[dao.IssueStateRelation]bool,
			typeState map[string][]*pb.IssueStateButton) ([]*pb.IssueStateButton, error) {
			return []*pb.IssueStateButton{
				{
					Permission: true,
				},
			}, nil
		},
	)
	defer p3.Unpatch()
	_, err := p.GetNextAvailableState(&dao.Issue{ProjectID: 1})
	assert.NoError(t, err)
}

func Test_updateParentCondition(t *testing.T) {
	type args struct {
		state string
		u     *IssueUpdated
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			args: args{
				state: "OPEN",
				u: &IssueUpdated{
					stateNew: "n",
				},
			},
		},
		{
			args: args{
				state: "OPEN",
				u: &IssueUpdated{
					stateOld: "n",
				},
			},
		},
		{
			args: args{
				state: "OPEN",
				u: &IssueUpdated{
					stateOld: "n",
					stateNew: "n",
				},
			},
		},
		{
			args: args{
				state: "OPEN",
				u: &IssueUpdated{
					stateOld: "ni",
					stateNew: "n",
				},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := updateParentCondition(tt.args.state, tt.args.u); got != tt.want {
				t.Errorf("updateParentCondition() = %v, want %v", got, tt.want)
			}
		})
	}
}
