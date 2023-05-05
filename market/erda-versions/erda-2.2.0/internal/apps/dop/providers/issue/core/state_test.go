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
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func TestIssueService_GetIssueStatesRelations(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStateRelations",
		func(d *dao.DBClient, projectID uint64, issueType string) ([]dao.IssueStateJoinSQL, error) {
			return []dao.IssueStateJoinSQL{
				{
					ID: 1,
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	i := &IssueService{db: db}
	_, err := i.GetIssueStatesRelations(&pb.GetIssueStateRelationRequest{})
	assert.NoError(t, err)
}
