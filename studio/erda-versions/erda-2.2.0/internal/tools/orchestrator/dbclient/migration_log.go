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

import (
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// MigrationLog migration执行记录表
type MigrationLog struct {
	dbengine.BaseModel
	ProjectID           uint64
	ApplicationID       uint64
	RuntimeID           uint64
	DeploymentID        uint64
	OperatorID          uint64
	Status              string
	AddonInstanceID     string
	AddonInstanceConfig string
}

// TableName 数据库表名
func (MigrationLog) TableName() string {
	return "dice_db_migration_log"
}

// CreateMigrationLog insert migrationLog
func (db *DBClient) CreateMigrationLog(migrationLog *MigrationLog) error {
	return db.Create(migrationLog).Error
}

// UpdateMigrationLog update migrationLog
func (db *DBClient) UpdateMigrationLog(migrationLog *MigrationLog) error {
	return db.Save(migrationLog).Error
}

// GetMigrationLogByDeploymentID 根据 deployID 查询migration信息
func (db *DBClient) GetMigrationLogByDeploymentID(deploymentID uint64) (*MigrationLog, error) {
	var migrationLog MigrationLog
	if err := db.Where("deployment_id = ?", deploymentID).
		Find(&migrationLog).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return &migrationLog, nil
}

// GetMigrationLogExpired 获取已经过期的migration操作记录
func (db *DBClient) GetMigrationLogExpiredThreeDays() (*[]MigrationLog, error) {
	var migrationLogs []MigrationLog
	currentTime := time.Now()
	LastThreeDaysTime := currentTime.AddDate(0, 0, -3)
	if err := db.Where("created_at < ?", LastThreeDaysTime).
		Where("status != ?", apistructs.MigrationStatusDeleted).
		Find(&migrationLogs).Error; err != nil {
		return nil, errors.Wrap(err, "failed to get migration records")
	}

	return &migrationLogs, nil
}
