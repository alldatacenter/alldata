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
)

func Test_provider_GetIssue(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssue",
		func(d *dao.DBClient, id int64) (dao.Issue, error) {
			return dao.Issue{
				ProjectID: 1,
			}, nil
		},
	)
	defer p1.Unpatch()

	p := &provider{}
	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "Convert",
		func(p *provider, model dao.Issue, identityInfo *commonpb.IdentityInfo) (*pb.Issue, error) {
			return &pb.Issue{
				ProjectID: 1,
			}, nil
		},
	)
	defer p2.Unpatch()
	_, err := p.GetIssue(1, nil)
	assert.NoError(t, err)
}
