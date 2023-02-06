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
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func Test_provider_AfterIssueAppRelationCreate(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "ListIssueItems",
		func(d *dao.DBClient, req pb.IssueListRequest) ([]dao.IssueItem, error) {
			return []dao.IssueItem{
				{
					ProjectID: 1,
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p := &provider{}
	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "GetNextAvailableState",
		func(d *provider, issue *dao.Issue) (*pb.IssueStateButton, error) {
			return &pb.IssueStateButton{StateID: 1}, nil
		},
	)
	defer p2.Unpatch()

	p3 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "BatchUpdateIssues",
		func(d *dao.DBClient, req *pb.BatchUpdateIssueRequest) error {
			return nil
		},
	)
	defer p3.Unpatch()

	p4 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "BatchCreateIssueStream",
		func(d *dao.DBClient, issueStreams []dao.IssueStream) error {
			return nil
		},
	)
	defer p4.Unpatch()

	p.db = db
	err := p.AfterIssueAppRelationCreate([]int64{1, 2})
	assert.NoError(t, err)
}

func Test_provider_GetIssuesByIssueIDs(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssueByIssueIDs",
		func(d *dao.DBClient, issueIDs []uint64) ([]dao.Issue, error) {
			return []dao.Issue{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
					ProjectID: 1,
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p := &provider{db: db}
	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "BatchConvert",
		func(d *provider, models []dao.Issue, issueTypes []string) ([]*pb.Issue, error) {
			return []*pb.Issue{
				{
					Id:        1,
					ProjectID: 1,
				},
			}, nil
		},
	)
	defer p2.Unpatch()
	_, err := p.GetIssuesByIssueIDs([]uint64{1})
	assert.NoError(t, err)
}
