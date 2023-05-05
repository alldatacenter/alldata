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

package model

import (
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
)

// Member 企业/项目/应用三级关系成员信息(包含平台管理员)
type Member struct {
	BaseModel
	ScopeType     apistructs.ScopeType // 系统管理员(sys)/企业(org)/项目(project)/应用(app)
	ScopeID       int64                // 企业ID/项目ID/应用ID
	ScopeName     string               // 企业/项目/应用名称
	ParentID      int64
	UserID        string
	Email         string    // 用户邮箱
	Mobile        string    // 用户手机号
	Name          string    // 用户名
	Nick          string    // 用户昵称
	Avatar        string    // 用户头像
	Token         string    // 用户鉴权token
	UserSyncAt    time.Time // 用户信息同步时间
	OrgID         int64     // 冗余 OrgID，方便用于退出企业时删除所有企业相关 member
	ProjectID     int64     // 冗余 ProjectID，方便用户退出项目时删除所有项目相关 member
	ApplicationID int64     // 冗余 AppID，目前等价于 scopeType=app & scopeID=appID
	Roles         []string  `gorm:"-"` // Manager/Developer/Tester
	Labels        []string  `gorm:"-"` // 不是表字段，用来记录join表后返回的标签字段。
	Deleted       bool      `gorm:"-"` // 不是表字段，用来过滤uc已删除的用户
}

// TableName 设置模型对应数据库表名称
func (Member) TableName() string {
	return "dice_member"
}

// MemberJoin 用于和 memberExtra 连表查询获取member额外的信息
type MemberJoin struct {
	Member
	ResourceKey   apistructs.ExtraResourceKey `gorm:"column:resource_key"`
	ResourceValue string                      `gorm:"column:resource_value"`
}

func (m *MemberJoin) Convert2APIDTO() apistructs.Member {
	return apistructs.Member{
		UserID: m.UserID,
		Email:  m.Email,
		Mobile: m.Mobile,
		Name:   m.Name,
		Nick:   m.Nick,
		Avatar: m.Avatar,
		Scope: apistructs.Scope{
			Type: m.ScopeType,
			ID:   strconv.FormatInt(m.ScopeID, 10),
		},
		Roles:   m.Roles,
		Labels:  m.Labels,
		Deleted: m.Deleted,
		Token:   m.Token,
	}
}

func (m *Member) Convert2APIDTO() apistructs.Member {
	return apistructs.Member{
		UserID: m.UserID,
		Email:  m.Email,
		Mobile: m.Mobile,
		Name:   m.Name,
		Nick:   m.Nick,
		Avatar: m.Avatar,
		Scope: apistructs.Scope{
			Type: m.ScopeType,
			ID:   strconv.FormatInt(m.ScopeID, 10),
		},
		Roles:   m.Roles,
		Labels:  m.Labels,
		Deleted: m.Deleted,
	}
}
