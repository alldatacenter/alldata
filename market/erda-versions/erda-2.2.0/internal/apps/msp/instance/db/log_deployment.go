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

import "github.com/jinzhu/gorm"

type LogType string

const LogTypeLogAnalytics = LogType("log-analytics")
const LogTypeLogService = LogType("log-service")

type LogDeploymentDB struct {
	*gorm.DB
}

func (db *LogDeploymentDB) GetByClusterName(clusterName string, logType LogType) (*LogDeployment, error) {
	var deployment LogDeployment

	query := db.Table(TableLogDeployment).
		Where("`cluster_name`=?", clusterName)

	if len(logType) > 0 {
		query = query.Where("`log_type`=?", logType)
	}

	result := query.
		Limit(1).
		Find(&deployment)

	if result.RecordNotFound() {
		return nil, nil
	}

	if result.Error != nil {
		return nil, result.Error
	}

	return &deployment, nil
}

func (db *LogDeploymentDB) GetByOrgId(orgId string, logType LogType) (*LogDeployment, error) {
	var deployment LogDeployment

	query := db.Table(TableLogDeployment).
		Where("`org_id`=?", orgId)

	if len(logType) > 0 {
		query = query.Where("`log_type`=?", logType)
	}

	result := query.
		Limit(1).
		Find(&deployment)

	if result.RecordNotFound() {
		return nil, nil
	}

	if result.Error != nil {
		return nil, result.Error
	}

	return &deployment, nil
}

func (db *LogDeploymentDB) GetByClusterNameAndOrgId(clusterName, orgId string, logType LogType) (*LogDeployment, error) {
	var deployment LogDeployment

	query := db.Table(TableLogDeployment).
		Where("`cluster_name`=?", clusterName).
		Where("`org_id`=?", orgId)

	if len(logType) > 0 {
		query = query.Where("`log_type`=?", logType)
	}

	result := query.
		Limit(1).
		Find(&deployment)

	if result.RecordNotFound() {
		return nil, nil
	}

	if result.Error != nil {
		return nil, result.Error
	}

	return &deployment, nil
}
