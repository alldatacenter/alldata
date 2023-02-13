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

import "github.com/erda-project/erda/pkg/database/dbengine"

// Publisher 资源模型
type Publisher struct {
	dbengine.BaseModel

	Name          string // Publisher名称
	PublisherType string // Publisher类型
	PublisherKey  string // PublisherKey，可以作为唯一标示，主要用于监控
	Desc          string // Publisher描述
	Logo          string // Publisher logo地址
	OrgID         int64  // Publisher关联组织ID
	UserID        string `gorm:"column:creator"` // 所属用户Id
}

// TableName 设置模型对应数据库表名称
func (Publisher) TableName() string {
	return "dice_publishers"
}
