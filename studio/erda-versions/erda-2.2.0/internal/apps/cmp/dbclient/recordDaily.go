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

package dbclient

import "time"

// ProjectDaily
type ProjectDaily struct {
	ID          int       `gorm:"type:BIGINT(20)"`
	ProjectName string    `gorm:"type:project_name"`
	ProjectID   string    `gorm:"column:project_id"`
	CpuQuota    float64   `gorm:"column:cpu_quota"`
	CpuRequest  float64   `gorm:"column:cpu_request"`
	MemQuota    float64   `gorm:"column:mem_quota"`
	MemRequest  float64   `gorm:"column:mem_request"`
	CreatedAt   time.Time `gorm:"column:created_at"`
	UpdatedAt   time.Time `gorm:"column:updated_at"`
}

// ProjectDaily
type ClusterDaily struct {
	ID          int       `gorm:"type:BIGINT(20)"`
	ClusterName string    `gorm:"type:cluster_name"`
	cpuQuota    float64   `gorm:"column:cpu_quota"`
	cpuRequest  float64   `gorm:"column:cpu_request"`
	memQuota    float64   `gorm:"column:mem_quota"`
	memRequest  float64   `gorm:"column:mem_request"`
	CreatedAt   time.Time `gorm:"column:created_at"`
	UpdatedAt   time.Time `gorm:"column:updated_at"`
}
