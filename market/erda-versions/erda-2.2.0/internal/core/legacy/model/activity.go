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

// Activity 活动模型
type Activity struct {
	BaseModel
	OrgID         int64
	ProjectID     int64
	ApplicationID int64
	BuildID       int64
	RuntimeID     int64
	UserID        string `gorm:"column:operator"`
	Type          string // 活动类型
	Action        string
	Desc          string // 活动描述
	Context       string `json:"context" gorm:"type:text"`
}

// TableName 设置模型对应数据库表名称
func (Activity) TableName() string {
	return "ps_activities"
}
