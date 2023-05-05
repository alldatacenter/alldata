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

import (
	"reflect"
	"time"

	"github.com/erda-project/erda/pkg/database/gormutil"
)

// tables name
const (
	TableProject = "sp_project"
	TableMetric  = "sp_metric"
)

// Project .
type Project struct {
	ID          int64     `gorm:"column:id;primary_key"`
	Identity    string    `gorm:"column:identity"`
	Name        string    `gorm:"column:config"`
	Description string    `gorm:"column:description"`
	Ats         string    `gorm:"column:ats"`
	Callback    string    `gorm:"column:callback"`
	ProjectID   int64     `gorm:"column:project_id"`
	CreateTime  time.Time `gorm:"column:create_time"`
	UpdateTime  time.Time `gorm:"column:update_time"`
	IsDeleted   string    `gorm:"column:is_deleted"`
}

// TableName .
func (Project) TableName() string { return TableProject }

var projectFieldColumns = gormutil.GetFieldToColumnMap(reflect.TypeOf(Project{}))

// Metric .
type Metric struct {
	ID         int64     `gorm:"column:id;primary_key"`
	TenantId   string    `gorm:"column:tenant_id"`
	ProjectID  int64     `gorm:"column:project_id"`
	ServiceID  int64     `gorm:"column:service_id"`
	Name       string    `gorm:"column:name"`
	URL        string    `gorm:"column:url"`
	Mode       string    `gorm:"column:mode"`
	Extra      string    `gorm:"column:extra"`
	AccountID  int64     `gorm:"column:account_id"`
	Status     int64     `gorm:"column:status"`
	Env        string    `gorm:"column:env"`
	Config     string    `gorm:"config"`
	CreateTime time.Time `gorm:"column:create_time"`
	UpdateTime time.Time `gorm:"column:update_time"`
	IsDeleted  string    `gorm:"column:is_deleted"`
}

// TableName .
func (Metric) TableName() string { return TableMetric }

var metricFieldColumns = gormutil.GetFieldToColumnMap(reflect.TypeOf(Metric{}))
