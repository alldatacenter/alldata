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

func Test_provider_GetIssueStage(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStage",
		func(d *dao.DBClient, orgID int64, issueType string) ([]dao.IssueStage, error) {
			return []dao.IssueStage{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
					Name:  "1",
					Value: "a",
					OrgID: 1,
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p := &provider{db: db}
	_, err := p.GetIssueStage(&pb.IssueStageRequest{OrgID: 1})
	assert.NoError(t, err)
}
