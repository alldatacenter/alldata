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

package member

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/ucauth"
)

func Test_checkCreateParam(t *testing.T) {
	m := New()
	req := apistructs.MemberAddRequest{
		Roles: []string{"Auditor"},
		Scope: apistructs.Scope{
			Type: "sys",
			ID:   "0",
		},
		UserIDs: []string{"2", "3"},
	}
	err := m.checkCreateParam(req)
	assert.NoError(t, err)
}

func Test_CheckPermission(t *testing.T) {
	var db *dao.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "IsSysAdmin",
		func(_ *dao.DBClient, userID string) (bool, error) {
			return userID == "1", nil
		})
	defer monkey.UnpatchAll()
	m := New()
	m.db = db
	err := m.CheckPermission("1", apistructs.SysScope, 0)
	assert.NoError(t, err)
}

func Test_checkUCUserInfo(t *testing.T) {
	emptyUsers := make([]ucauth.User, 0)
	emptyUsers = append(emptyUsers, ucauth.User{})
	m := New()
	err := m.checkUCUserInfo(emptyUsers)
	assert.Equal(t, "failed to get user info", err.Error())
}

func TestMember_UpdateMemberUserInfo(t *testing.T) {
	users := []model.Member{
		{
			BaseModel: model.BaseModel{
				ID: 1,
			},
			UserID: "1",
		},
		{
			BaseModel: model.BaseModel{
				ID: 2,
			},
			UserID: "2",
		},
	}
	var db *dao.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetMemberByUserID",
		func(_ *dao.DBClient, userID string) ([]model.Member, error) {
			return users, nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "UpdateMemberUserInfo",
		func(_ *dao.DBClient, ids []int64, fields map[string]interface{}) error {
			return nil
		})
	defer monkey.UnpatchAll()
	type fields struct {
		db *dao.DBClient
	}
	type args struct {
		req apistructs.MemberUserInfoUpdateRequest
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			fields: fields{
				db: db,
			},
			args: args{
				req: apistructs.MemberUserInfoUpdateRequest{
					Members: []apistructs.Member{
						{
							UserID: "1",
							Avatar: "1.png",
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			m := &Member{
				db: tt.fields.db,
			}
			if err := m.UpdateMemberUserInfo(tt.args.req); (err != nil) != tt.wantErr {
				t.Errorf("Member.UpdateMemberUserInfo() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
