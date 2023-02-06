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
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// RolePermission 角色资源操作
type RolePermission struct {
	dbengine.BaseModel
	Scope        string `gorm:"type:varchar(30);unique_index:idx_resource_action" yaml:"scope"`
	Role         string `gorm:"type:varchar(30);unique_index:idx_resource_action" yaml:"role"`
	ResourceRole string `gorm:"type:varchar(30);unique_index:idx_resource_action" yaml:"resource_role"`
	Resource     string `gorm:"type:varchar(40);unique_index:idx_resource_action" yaml:"resource"`
	Action       string `gorm:"type:varchar(30);unique_index:idx_resource_action" yaml:"action"`
	Creator      string
}

func (RolePermission) TableName() string {
	return "dice_role_permission"
}
