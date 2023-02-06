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

// ConfigNamespaceRelation 配置信息
type ConfigNamespaceRelation struct {
	ID               int64     `json:"id" gorm:"primary_key"`
	CreatedAt        time.Time `json:"createdAt" gorm:"column:create_time"`
	UpdatedAt        time.Time `json:"updatedAt" gorm:"column:update_time"`
	IsDeleted        string
	Namespace        string `gorm:"index:namespace"`
	DefaultNamespace string `gorm:"index:default_namespace"`
}

// TableName 设置模型对应数据库表名称
func (ConfigNamespaceRelation) TableName() string {
	return "dice_config_namespace_relation"
}
