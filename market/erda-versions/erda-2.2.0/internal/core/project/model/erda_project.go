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

// ErdaProject is the model erda_project
type ErdaProject struct {
	BaseModel

	Name        string
	DisplayName string
	Logo        string
	Desc        string
	// Cluster configuration eg: {"DEV":"terminus-y","TEST":"terminus-y","STAGING":"terminus-y","PROD":"terminus-y"}
	ClusterConfig string
	CpuQuota      float64
	MemQuota      float64
	Creator       string
	OrgID         int64
	Version       string
	// DingTalk Hook
	DDHook     string `gorm:"column:dd_hook"`
	Email      string
	Functions  string
	ActiveTime time.Time
	// Rollback configuration: {"DEV": 1,"TEST": 2,"STAGING": 3,"PROD": 4}
	RollbackConfig string
	// Whether to open the project-level namespace
	EnableNS bool `gorm:"column:enable_ns"`
	// Is it a public project
	IsPublic bool
	// project type
	Type          string
	SoftDeletedAt uint
}

func (ErdaProject) TableName() string {
	return "erda_project"
}
