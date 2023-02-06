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
	TableInstance           = "tb_tmc_instance"
	TableInstanceTenant     = "tb_tmc_instance_tenant"
	TableTmc                = "tb_tmc"
	TableTmcVersion         = "tb_tmc_version"
	TableRequestRelation    = "tb_tmc_request_relation"
	TableTmcIni             = "tb_tmc_ini"
	TableProject            = "sp_project"
	TableLogDeployment      = "sp_log_deployment"
	TableLogInstance        = "sp_log_instance"
	TableLogServiceInstance = "sp_log_service_instance"
)

// InstanceTenant .
type InstanceTenant struct {
	ID          string    `gorm:"column:id;primary_key"`
	InstanceID  string    `gorm:"column:instance_id"`
	Config      string    `gorm:"column:config"`
	Options     string    `gorm:"column:options"`
	TenantGroup string    `gorm:"column:tenant_group"`
	Engine      string    `gorm:"column:engine"`
	Az          string    `gorm:"column:az"`
	CreateTime  time.Time `gorm:"column:create_time"`
	UpdateTime  time.Time `gorm:"column:update_time"`
	IsDeleted   string    `gorm:"column:is_deleted"`
}

// TableName .
func (InstanceTenant) TableName() string { return TableInstanceTenant }

var instanceTenantFieldColumns = gormutil.GetFieldToColumnMap(reflect.TypeOf(InstanceTenant{}))

// Instance .
type Instance struct {
	ID         string    `gorm:"column:id;primary_key"`
	Engine     string    `gorm:"column:engine"`
	Version    string    `gorm:"column:version"`
	ReleaseID  string    `gorm:"column:release_id"`
	Status     string    `gorm:"column:status"`
	Az         string    `gorm:"column:az"`
	Config     string    `gorm:"column:config"`
	Options    string    `gorm:"column:options"`
	IsCustom   string    `gorm:"column:is_custom;default:'N'"`
	IsDeleted  string    `gorm:"column:is_deleted;default:'N'"`
	CreateTime time.Time `gorm:"column:create_time;default:CURRENT_TIMESTAMP"`
	UpdateTime time.Time `gorm:"column:update_time;default:CURRENT_TIMESTAMP"`
}

// TableName .
func (Instance) TableName() string { return TableInstance }

var instanceFieldColumns = gormutil.GetFieldToColumnMap(reflect.TypeOf(Instance{}))

// Tmc .
type Tmc struct {
	ID          int       `gorm:"column:id;primary_key"`
	Name        string    `gorm:"column:name"`
	Engine      string    `gorm:"column:engine"`
	ServiceType string    `gorm:"column:service_type"`
	DeployMode  string    `gorm:"column:deploy_mode"`
	IsDeleted   string    `gorm:"column:is_deleted"`
	CreateTime  time.Time `gorm:"column:create_time"`
	UpdateTime  time.Time `gorm:"column:update_time"`
}

// TableName .
func (Tmc) TableName() string { return TableTmc }

var tmcFieldColumns = gormutil.GetFieldToColumnMap(reflect.TypeOf(Tmc{}))

// TmcVersion .
type TmcVersion struct {
	ID         int       `gorm:"column:id;primary_key"`
	Engine     string    `gorm:"column:engine"`
	Version    string    `gorm:"column:version"`
	ReleaseId  string    `gorm:"column:release_id"`
	CreateTime time.Time `gorm:"column:create_time"`
	UpdateTime time.Time `gorm:"column:update_time"`
	IsDeleted  string    `gorm:"column:is_deleted"`
}

func (TmcVersion) TableName() string {
	return TableTmcVersion
}

type TmcRequestRelation struct {
	ID              int       `gorm:"column:id;primary_key;"`
	ParentRequestId string    `gorm:"column:parent_request_id"`
	ChildRequestId  string    `gorm:"column:child_request_id"`
	CreateTime      time.Time `gorm:"column:create_time;default:CURRENT_TIMESTAMP"`
	UpdateTime      time.Time `gorm:"column:update_time;default:CURRENT_TIMESTAMP"`
	IsDeleted       string    `gorm:"column:is_deleted;default:'N'"`
}

func (TmcRequestRelation) TableName() string {
	return TableRequestRelation
}

type TmcIni struct {
	ID         int       `gorm:"column:id,primary_key"`
	IniName    string    `gorm:"column:ini_name"`
	IniDesc    string    `gorm:"column:ini_desc"`
	IniValue   string    `gorm:"column:ini_value"`
	CreateTime time.Time `gorm:"column:create_time;default:CURRENT_TIMESTAMP"`
	UpdateTime time.Time `gorm:"column:update_time;default:CURRENT_TIMESTAMP"`
	IsDeleted  string    `gorm:"column:is_deleted;default:'N'"`
}

func (TmcIni) TableName() string {
	return TableTmcIni
}

type Project struct {
	ID          int       `gorm:"column:id;primary_key"`
	Identity    string    `gorm:"column:identity"`
	Name        string    `gorm:"column:name"`
	Description string    `gorm:"column:description"`
	Ats         string    `gorm:"column:ats"`
	Callback    string    `gorm:"column:callback"`
	ProjectId   string    `gorm:"column:project_id"`
	CreateTime  time.Time `gorm:"column:create_time;default:CURRENT_TIMESTAMP"`
	UpdateTime  time.Time `gorm:"column:update_time;default:CURRENT_TIMESTAMP"`
	IsDeleted   string    `gorm:"column:is_deleted;default:'N'"`
}

func (Project) TableName() string {
	return TableProject
}

type LogDeployment struct {
	ID           int       `gorm:"column:id;primary_key"`
	OrgId        string    `gorm:"column:org_id"`
	ClusterName  string    `gorm:"column:cluster_name"`
	ClusterType  int       `gorm:"column:cluster_type"`
	EsUrl        string    `gorm:"column:es_url"`
	EsConfig     string    `gorm:"column:es_config"`
	KafkaServers string    `gorm:"column:kafka_servers"`
	KafkaConfig  string    `gorm:"column:kafka_config"`
	CollectorUrl string    `gorm:"column:collector_url"`
	Domain       string    `gorm:"column:domain"`
	Created      time.Time `gorm:"column:created"`
	Updated      time.Time `gorm:"column:updated"`
	LogType      string    `gorm:"column:log_type;default:'log-analytics'"`
}

func (LogDeployment) TableName() string {
	return TableLogDeployment
}

type LogInstance struct {
	ID              int       `gorm:"column:id;primary_key"`
	LogKey          string    `gorm:"column:log_key"`
	OrgId           string    `gorm:"column:org_id"`
	OrgName         string    `gorm:"column:org_name"`
	ClusterName     string    `gorm:"column:cluster_name"`
	ProjectId       string    `gorm:"column:project_id"`
	ProjectName     string    `gorm:"column:project_name"`
	Workspace       string    `gorm:"column:workspace"`
	ApplicationId   string    `gorm:"column:application_id"`
	ApplicationName string    `gorm:"column:application_name"`
	RuntimeId       string    `gorm:"column:runtime_id"`
	RuntimeName     string    `gorm:"column:runtime_name"`
	Config          string    `gorm:"column:config"`
	Version         string    `gorm:"column:version"`
	Plan            string    `gorm:"column:plan"`
	IsDelete        int       `gorm:"column:is_delete"`
	Created         time.Time `gorm:"column:created"`
	Updated         time.Time `gorm:"column:updated"`
	LogType         string    `gorm:"column:log_type;default:'log-analytics'"`
}

func (LogInstance) TableName() string {
	return TableLogInstance
}

type LogServiceInstance struct {
	ID       string `gorm:"column:id;primary_key"`
	EsUrls   string `gorm:"column:es_urls"`
	EsConfig string `gorm:"column:es_config"`
}

func (LogServiceInstance) TableName() string {
	return TableLogServiceInstance
}
