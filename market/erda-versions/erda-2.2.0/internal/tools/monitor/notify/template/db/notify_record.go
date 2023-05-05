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

package db

import "time"

type NotifyRecord struct {
	NotifyId    string    `json:"notify_id" gorm:"column:notify_id"`
	NotifyName  string    `json:"notify_name" gorm:"column:notify_name"`
	ScopeType   string    `json:"scope_type" gorm:"column:scope_type"`
	ScopeId     int64     `json:"scope_id" gorm:"column:scope_id"`
	GroupId     string    `json:"group_id" gorm:"column:group_id"`
	NotifyGroup string    `json:"notify_group" gorm:"column:notify_group"`
	Title       string    `json:"title" gorm:"column:title"`
	NotifyTime  time.Time `json:"notify_time" gorm:"column:notify_time"`
	CreateTime  time.Time `json:"create_time" gorm:"column:create_time"`
	UpdateTime  time.Time `json:"update_time" gorm:"column:update_time"`
}

func (n *NotifyRecord) TableName() string {
	return "sp_notify_record"
}
