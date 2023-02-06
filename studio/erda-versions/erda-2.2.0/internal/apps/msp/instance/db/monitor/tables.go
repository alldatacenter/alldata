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

package monitor

import (
	"reflect"
	"time"

	"github.com/erda-project/erda/pkg/database/gormutil"
)

// tables name
const (
	TableMonitor = "sp_monitor"
)

// Monitor .
type Monitor struct {
	Id                 int       `gorm:"column:id;primary_key"`
	MonitorId          string    `gorm:"column:monitor_id"`
	TerminusKey        string    `gorm:"column:terminus_key"`
	TerminusKeyRuntime string    `gorm:"column:terminus_key_runtime"`
	Workspace          string    `gorm:"column:workspace"`
	RuntimeId          string    `gorm:"column:runtime_id"`
	RuntimeName        string    `gorm:"column:runtime_name"`
	ApplicationId      string    `gorm:"column:application_id"`
	ApplicationName    string    `gorm:"column:application_name"`
	ProjectId          string    `gorm:"column:project_id"`
	ProjectName        string    `gorm:"column:project_name"`
	OrgId              string    `gorm:"column:org_id"`
	OrgName            string    `gorm:"column:org_name"`
	ClusterId          string    `gorm:"column:cluster_id"`
	ClusterName        string    `gorm:"column:cluster_name"`
	Config             string    `gorm:"column:config;default:''"`
	CallbackUrl        string    `gorm:"column:callback_url"`
	Version            string    `gorm:"column:version"`
	Plan               string    `gorm:"column:plan"`
	IsDelete           int       `gorm:"column:is_delete"`
	Created            time.Time `gorm:"column:created;"`
	Updated            time.Time `gorm:"column:updated;"`
}

func (Monitor) TableName() string { return TableMonitor }

var monitorFieldColumns = gormutil.GetFieldToColumnMap(reflect.TypeOf(Monitor{}))
