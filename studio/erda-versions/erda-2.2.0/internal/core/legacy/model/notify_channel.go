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
	"time"
)

const TableNotifyChannel = "erda_notify_channel"

type NotifyChannel struct {
	Id              string    `gorm:"column:id" db:"id" json:"id" form:"id"`                                                         //id
	Name            string    `gorm:"column:name" db:"name" json:"name" form:"name"`                                                 //渠道名称
	Type            string    `gorm:"column:type" db:"type" json:"type" form:"type"`                                                 //渠道类型
	Config          string    `gorm:"column:config" db:"config" json:"config" form:"config"`                                         //渠道配置
	ScopeType       string    `gorm:"column:scope_type" db:"scope_type" json:"scope_type" form:"scope_type"`                         //域类型
	ScopeId         string    `gorm:"column:scope_id" db:"scope_id" json:"scope_id" form:"scope_id"`                                 //域id
	CreatorId       string    `gorm:"column:creator_id" db:"creator_id" json:"creator_id" form:"creator_id"`                         //创建人Id
	ChannelProvider string    `gorm:"column:channel_provider" db:"channel_provider" json:"channel_provider" form:"channel_provider"` //渠道提供商类型
	IsEnabled       bool      `gorm:"column:is_enabled" db:"is_enabled" json:"is_enabled" form:"is_enabled"`                         //是否启用
	KmsKey          string    `gorm:"column:kms_key" db:"kms_key" json:"kms_key" form:"kms_key"`                                     //kms key
	CreatedAt       time.Time `gorm:"column:created_at" db:"created_at" json:"created_at" form:"created_at"`                         //创建时间
	UpdatedAt       time.Time `gorm:"column:updated_at" db:"updated_at" json:"updated_at" form:"updated_at"`                         //更新时间
	IsDeleted       bool      `gorm:"column:is_deleted" db:"is_deleted" json:"is_deleted" form:"is_deleted"`                         //是否删除
}

func (NotifyChannel) TableName() string {
	return TableNotifyChannel
}
