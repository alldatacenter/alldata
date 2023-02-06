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
	"strconv"

	"github.com/jinzhu/gorm"
)

// LogDeploymentTable .
const LogDeploymentTable = "sp_log_deployment"

// LogDeployment .
type LogDeployment struct {
	ID           int64  `gorm:"column:id" json:"id"`
	OrgID        string `gorm:"column:org_id" json:"org_id"`
	ClusterName  string `gorm:"column:cluster_name" json:"cluster_name"`
	ClusterType  int    `gorm:"column:cluster_type" json:"cluster_type"`
	ESURL        string `gorm:"column:es_url" json:"es_url"`
	ESConfig     string `gorm:"column:es_config" json:"es_config"`
	CollectorURL string `gorm:"column:collector_url" json:"collector_url"`
	Domain       string `gorm:"column:domain" json:"domain"`
	LogType      string `gorm:"column:log_type" json:"log_type"`
}

// TableName .
func (LogDeployment) TableName() string { return LogDeploymentTable }

// LogDeploymentDB .
type LogDeploymentDB struct {
	*gorm.DB
}

// QueryByOrgID .
func (db *LogDeploymentDB) QueryByOrgID(orgID int64) ([]*LogDeployment, error) {
	var list []*LogDeployment
	if err := db.Table(LogDeploymentTable).
		Where("org_id=?", strconv.FormatInt(orgID, 10)).
		Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

// QueryByClusters .
func (db *LogDeploymentDB) QueryByClusters(clusters ...string) ([]*LogDeployment, error) {
	var list []*LogDeployment
	if err := db.Table(LogDeploymentTable).
		Where("cluster_name IN (?)", clusters).
		Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

// QueryByOrgIDAndClusters .
func (db *LogDeploymentDB) QueryByOrgIDAndClusters(orgID int64, clusters ...string) ([]*LogDeployment, error) {
	var list []*LogDeployment
	if err := db.Table(LogDeploymentTable).
		Where("org_id=?", strconv.FormatInt(orgID, 10)).
		Where("cluster_name IN (?)", clusters).
		Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

// List .
func (db *LogDeploymentDB) List() ([]*LogDeployment, error) {
	var list []*LogDeployment
	if err := db.Table(LogDeploymentTable).
		Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}
