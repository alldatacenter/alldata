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

type Notify struct {
	BaseModel
	Name          string `gorm:"size:150"`
	ScopeType     string `gorm:"size:150;index:idx_scope_type"`
	ScopeID       string `gorm:"size:150;index:idx_scope_id"`
	Label         string `gorm:"size:150"`
	ClusterName   string
	Channels      string `gorm:"type:text"`
	NotifyGroupID int64  `gorm:"index:notify_group_id"`
	OrgID         int64  `gorm:"index:idx_org_id"`
	Creator       string
	Enabled       bool
	Data          string `gorm:"type:text"`
}

// TableName 设置模型对应数据库表名称
func (Notify) TableName() string {
	return "dice_notifies"
}
