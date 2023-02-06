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

	"github.com/erda-project/erda/apistructs"
)

// Application 应用资源模型
type Application struct {
	BaseModel
	Name           string // 应用名称
	DisplayName    string // 应用展示名称
	Desc           string // 应用描述
	Config         string // 钉钉配置
	ProjectID      int64  `gorm:"index:idx_project_id"` // 应用关联项目Id
	ProjectName    string // 应用关联项目名称
	OrgID          int64  // 应用关联组织Id
	Mode           string // 应用模式
	Pined          bool   `gorm:"-"` // 应用是否pined
	GitRepo        string
	GitRepoAbbrev  string
	IsExternalRepo bool
	UnblockStart   *time.Time // 解封开始时间
	UnblockEnd     *time.Time // 解封结束时间
	RepoConfig     string
	SonarConfig    *apistructs.SonarConfig `gorm:"column:sonar_config"`
	Logo           string                  // 应用Logo地址
	UserID         string                  `gorm:"column:creator"` // 所属用户Id
	Extra          string                  // 应用配置namespace等
	IsPublic       bool                    // 应用是否公开
}

// TableName 设置模型对应数据库表名称
func (Application) TableName() string {
	return "dice_app"
}

type ApplicationRuntimeCount struct {
	RuntimeCount  int64 `json:"runtime_count"`
	ApplicationID int64 `json:"application_id"`
}
