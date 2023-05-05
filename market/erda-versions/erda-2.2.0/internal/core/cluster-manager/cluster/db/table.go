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

// BaseModel common info for all models
type BaseModel struct {
	ID        int64     `json:"id" gorm:"primary_key"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
}

// Cluster cluster model
type Cluster struct {
	BaseModel
	OrgID           uint64
	Name            string
	DisplayName     string
	CloudVendor     string
	Description     string
	Type            string
	Logo            string
	WildcardDomain  string
	SchedulerConfig string `gorm:"column:scheduler;type:text"`
	OpsConfig       string `gorm:"column:opsconfig;type:text"`
	ManageConfig    string `gorm:"column:manage_config;type:text"`
	SysConfig       string `gorm:"column:sys;type:text"`
	// Deprecated
	URLs        string `gorm:"type:text"`
	ClusterInfo string `gorm:"column:cluster_info;type:text"`
}

// TableName cluster table name
func (Cluster) TableName() string {
	return "co_clusters"
}
