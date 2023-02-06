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
	"encoding/json"
	"time"

	"github.com/erda-project/erda/apistructs"
)

// Project 项目资源模型
type Project struct {
	BaseModel
	Name           string // Project name
	DisplayName    string // Project display name
	Desc           string // Project description
	Logo           string // Project logo address
	OrgID          int64  // Project related organization ID
	UserID         string `gorm:"column:creator"` // 所属用户Id
	DDHook         string `gorm:"column:dd_hook"` // 钉钉Hook
	ClusterConfig  string // Cluster configuration eg: {"DEV":"terminus-y","TEST":"terminus-y","STAGING":"terminus-y","PROD":"terminus-y"}
	RollbackConfig string // Rollback configuration: {"DEV": 1,"TEST": 2,"STAGING": 3,"PROD": 4}
	CpuQuota       float64
	MemQuota       float64
	Functions      string    `gorm:"column:functions"`
	ActiveTime     time.Time `gorm:"column:active_time"`
	EnableNS       bool      `gorm:"column:enable_ns"` // Whether to open the project-level namespace
	IsPublic       bool      `gorm:"column:is_public"` // Is it a public project
	Type           string    `gorm:"column:type"`      // project type

	Quota *apistructs.ProjectQuota `json:"quota,omitempty" gorm:"-"`

	SoftDeletedAt uint
}

// TableName 设置模型对应数据库表名称
func (Project) TableName() string {
	return "erda_project"
}

func (p *Project) GetClusterConfig() map[string]string {
	var clusterConfig = make(map[string]string)
	_ = json.Unmarshal([]byte(p.ClusterConfig), &clusterConfig)
	return clusterConfig
}

type ProjectUnblockAppCount struct {
	UnblockAppCount int64 `json:"unblock_app_count"`
	ProjectID       int64 `json:"project_id"`
}
