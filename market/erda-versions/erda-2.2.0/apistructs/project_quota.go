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

package apistructs

import (
	"strings"
	"time"
)

// ProjectQuota is the table "ps_group_projects_quota"
// CPU quota unit is Core * 10^-3
// Mem quota uint is Byte
type ProjectQuota struct {
	ID                 uint64    `json:"id" gorm:"id"`
	UpdatedAt          time.Time `json:"updated_at" gorm:"updated_at"`
	CreatedAt          time.Time `json:"created_at" gorm:"created_at"`
	ProjectID          uint64    `json:"project_id" gorm:"project_id"`
	ProjectName        string    `json:"project_name" gorm:"project_name"`
	ProdClusterName    string    `json:"prod_cluster_name" gorm:"prod_cluster_name"`
	StagingClusterName string    `json:"staging_cluster_name" gorm:"staging_cluster_name"`
	TestClusterName    string    `json:"test_cluster_name" gorm:"test_cluster_name"`
	DevClusterName     string    `json:"dev_cluster_name" gorm:"dev_cluster_name"`
	ProdCPUQuota       uint64    `json:"prod_cpu_quota" gorm:"prod_cpu_quota"`
	ProdMemQuota       uint64    `json:"prod_mem_quota" gorm:"prod_mem_quota"`
	StagingCPUQuota    uint64    `json:"staging_cpu_quota" gorm:"staging_cpu_quota"`
	StagingMemQuota    uint64    `json:"staging_mem_quota" gorm:"staging_mem_quota"`
	TestCPUQuota       uint64    `json:"test_cpu_quota" gorm:"test_cpu_quota"`
	TestMemQuota       uint64    `json:"test_mem_quota" gorm:"test_mem_quota"`
	DevCPUQuota        uint64    `json:"dev_cpu_quota" gorm:"dev_cpu_quota"`
	DevMemQuota        uint64    `json:"dev_mem_quota" gorm:"dev_mem_quota"`
	CreatorID          string    `json:"creator_id" gorm:"creator_id"`
	UpdaterID          string    `json:"updater_id" gorm:"updater_id"`
}

// TableName returns the model's name "ps_group_projects_quota"
func (ProjectQuota) TableName() string {
	return "ps_group_projects_quota"
}

func (p ProjectQuota) GetClusterName(workspace string) string {
	switch strings.ToLower(workspace) {
	case "prod":
		return p.ProdClusterName
	case "staging":
		return p.StagingClusterName
	case "test":
		return p.TestClusterName
	case "dev":
		return p.DevClusterName
	default:
		return ""
	}
}

// GetCPUQuota returns the CPU quota on the workspace.
// The unit is Core * 10^-3
func (p ProjectQuota) GetCPUQuota(workspace string) uint64 {
	switch strings.ToLower(workspace) {
	case "prod":
		return p.ProdCPUQuota
	case "staging":
		return p.StagingCPUQuota
	case "test":
		return p.TestCPUQuota
	case "dev":
		return p.DevCPUQuota
	default:
		return 0
	}
}

// GetMemQuota returns the Mem quota on the workspace.
// The unit is Byte
func (p ProjectQuota) GetMemQuota(workspace string) uint64 {
	switch strings.ToLower(workspace) {
	case "prod":
		return p.ProdMemQuota
	case "staging":
		return p.StagingMemQuota
	case "test":
		return p.TestMemQuota
	case "dev":
		return p.DevMemQuota
	default:
		return 0
	}
}

func (p ProjectQuota) ClustersNames() []string {
	return []string{p.ProdClusterName, p.StagingClusterName, p.TestClusterName, p.DevClusterName}
}
