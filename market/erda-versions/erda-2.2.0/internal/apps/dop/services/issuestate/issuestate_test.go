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

package issuestate

import (
	"errors"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"
	"github.com/golang/mock/gomock"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

func TestIssueState_GetIssueStateIDs(t *testing.T) {
	db := &dao.DBClient{}
	is := New(WithDBClient(db))
	m := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStates",
		func(db *dao.DBClient, req *apistructs.IssueStatesGetRequest) ([]dao.IssueState, error) {
			return []dao.IssueState{
				{
					ProjectID: 1,
				},
			}, nil
		})
	defer m.Unpatch()

	res, err := is.GetIssueStateIDs(&apistructs.IssueStatesGetRequest{
		ProjectID: 1,
	})
	assert.NoError(t, err)
	assert.Equal(t, 1, len(res))
}

func TestIssueState_GetIssueStatesMap(t *testing.T) {
	db := &dao.DBClient{}
	is := New(WithDBClient(db))
	m := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetIssuesStatesByProjectID",
		func(db *dao.DBClient, projectID uint64, issuetype apistructs.IssueType) ([]dao.IssueState, error) {
			return []dao.IssueState{
				{
					ProjectID: 1,
					IssueType: apistructs.IssueTypeBug,
				},
				{
					ProjectID: 1,
					IssueType: apistructs.IssueTypeRequirement,
				},
				{
					ProjectID: 1,
					IssueType: apistructs.IssueTypeTask,
				},
			}, nil
		})
	defer m.Unpatch()

	res, err := is.GetIssueStatesMap(&apistructs.IssueStatesGetRequest{
		ProjectID: 1,
	})
	assert.NoError(t, err)
	assert.Equal(t, 4, len(res))
}

func TestInitProjectState(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	m := NewMockIssueStater(ctrl)

	m.EXPECT().CreateIssuesState(gomock.Any()).AnyTimes().Return(nil)
	m.EXPECT().UpdateIssueStateRelations(gomock.Any(), gomock.Any(), gomock.Any()).AnyTimes().Return(nil)

	is := New(WithDBClient(m))
	if err := is.InitProjectState(1, ""); err != nil {
		t.Error(err)
	}

	s := NewMockIssueStater(ctrl)
	s.EXPECT().CreateIssuesState(gomock.Any()).AnyTimes().Return(errors.New("db error"))
	is = New(WithDBClient(s))
	if err := is.InitProjectState(1, ""); err != nil {
		assert.Error(t, err)
	}
}

func Test_getStateInitData(t *testing.T) {
	is := New()
	zhInitData := is.getStateInitData("")
	enInitData := is.getStateInitData("en-US")
	assert.Equal(t, 5, len(zhInitData))
	assert.Equal(t, 5, len(enInitData))
}
