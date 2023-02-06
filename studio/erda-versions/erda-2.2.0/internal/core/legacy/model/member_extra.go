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

import "github.com/erda-project/erda/apistructs"

type MemberExtra struct {
	BaseModel
	// UeserID 关联成员的用户id
	UserID string `gorm:"column:user_id"`
	// ParentID 成员的父scope_id
	ParentID int64 `gorm:"column:parent_id"`
	// ScopeType 关联成员的scope_type
	ScopeType apistructs.ScopeType `gorm:"column:scope_type"`
	// ScopeID 关联成员的scope_id
	ScopeID int64 `gorm:"column:scope_id"`
	// ResourceKey 关联资源的key
	ResourceKey apistructs.ExtraResourceKey `gorm:"column:resource_key"`
	// ResourceValue 管理资源的值
	ResourceValue string `gorm:"column:resource_value"`
}

// TableName 表名
func (MemberExtra) TableName() string {
	return "dice_member_extra"
}
