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

import "time"

// Approve 审批信息模型
type Approve struct {
	BaseModel
	OrgID        uint64
	TargetID     uint64
	EntityID     uint64
	TargetName   string
	Extra        string
	Title        string
	Priority     string
	Status       string
	Submitter    string
	Approver     string
	Type         string // IOS发布证书/Android证书/消息推送证书
	Desc         string
	ApprovalTime *time.Time
}

// TableName 设置模型对应数据库表名称
func (Approve) TableName() string {
	return "dice_approves"
}
