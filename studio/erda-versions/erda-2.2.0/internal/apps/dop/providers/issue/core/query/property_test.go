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
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func Test_provider_GetBatchProperties(t *testing.T) {
	p := &provider{}
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(p), "GetProperties",
		func(p *provider, req *pb.GetIssuePropertyRequest) ([]*pb.IssuePropertyIndex, error) {
			return []*pb.IssuePropertyIndex{
				{PropertyID: 1},
			}, nil
		},
	)
	defer p1.Unpatch()

	r, err := p.GetBatchProperties(1, []string{"TASK", "BUG"})
	assert.NoError(t, err)
	assert.Nil(t, r)
	r, err = p.GetBatchProperties(1, []string{"TASK"})
	assert.NoError(t, err)
	assert.NotNil(t, r)
}

func Test_provider_CreatePropertyRelation(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "DeletePropertyRelationsByPropertyID",
		func(d *dao.DBClient, issueID int64, propertyID int64) error {
			return nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "CreatePropertyRelations",
		func(d *dao.DBClient, relations []dao.IssuePropertyRelation) error {
			return nil
		},
	)
	defer p2.Unpatch()

	req := &pb.CreateIssuePropertyInstanceRequest{
		Property: []*pb.IssuePropertyInstance{
			{
				PropertyType:   pb.PropertyTypeEnum_Text,
				Required:       true,
				ArbitraryValue: structpb.NewStringValue("adf"),
			},
			{
				PropertyType: pb.PropertyTypeEnum_CheckBox,
			},
		},
		OrgID:     1,
		ProjectID: 1,
	}
	p := &provider{db: db}
	err := p.CreatePropertyRelation(req)
	assert.NoError(t, err)

	req = &pb.CreateIssuePropertyInstanceRequest{
		Property: []*pb.IssuePropertyInstance{
			{
				PropertyType: pb.PropertyTypeEnum_Text,
				Required:     true,
			},
		},
		OrgID:     1,
		ProjectID: 1,
	}
	err = p.CreatePropertyRelation(req)
	assert.Error(t, err)
}

func Test_provider_GetProperties(t *testing.T) {
	var db *dao.DBClient
	p1 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssueProperties",
		func(d *dao.DBClient, req pb.GetIssuePropertyRequest) ([]dao.IssueProperty, error) {
			return []dao.IssueProperty{
				{
					BaseModel: dbengine.BaseModel{ID: 1},
				},
			}, nil
		},
	)
	defer p1.Unpatch()

	p2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuePropertyValues",
		func(d *dao.DBClient, orgID int64) ([]dao.IssuePropertyValue, error) {
			return []dao.IssuePropertyValue{
				{
					BaseModel: dbengine.BaseModel{
						ID: 1,
					},
				},
			}, nil
		},
	)
	defer p2.Unpatch()

	p := &provider{db: db}
	req := &pb.GetIssuePropertyRequest{
		OrgID: 1,
	}
	_, err := p.GetProperties(req)
	assert.NoError(t, err)
}
