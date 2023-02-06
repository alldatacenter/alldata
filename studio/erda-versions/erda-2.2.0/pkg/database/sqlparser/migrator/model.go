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

package migrator

import (
	"time"

	"github.com/sirupsen/logrus"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

const SchemaMigrationHistory = `schema_migration_history`

const CreateTableHistoryModel = `
CREATE TABLE IF NOT EXISTS schema_migration_history (
	id 			BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
	created_at 	DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at 	DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

	service_name	VARCHAR(32) 	NOT NULL COMMENT '微服务名',
	filename		VARCHAR(191) 	NOT NULL COMMENT '脚本文件名',
	checksum		CHAR(128)		NOT NULL COMMENT '脚本文本 checksum',
	installed_by	VARCHAR(32)		NOT NULL COMMENT '执行人',
	installed_on	VARCHAR(32)		NOT NULL COMMENT '执行平台',
	language_type	VARCHAR(16)		NOT NULL COMMENT '.sql, .py',
	reversed 		LONGTEXT 		NOT NULL COMMENT '反转的 DDL',
	CONSTRAINT uk_filename UNIQUE (filename),
	CONSTRAINT uk_checksum UNIQUE (checksum)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'erda migration 执行记录表'
`

type HistoryModel struct {
	ID        uint64    `gorm:"primary key"`
	CreatedAt time.Time `json:"createdAt" gorm:"created_at"`
	UpdatedAt time.Time `json:"updatedAt" gorm:"updated_at"`

	ServiceName  string `json:"serviceName"  gorm:"service_name"`
	Filename     string `json:"filename"     gorm:"filename"`
	Checksum     string `json:"checksum"     gorm:"checksum"`
	InstalledBy  string `json:"installedBy"  gorm:"installed_by"`
	InstalledOn  string `json:"installedOn"  gorm:"install_on"`
	LanguageType string `json:"languageType" gorm:"language_type"`
	Reversed     string `json:"reversed"     gorm:"reversed"`
}

func (m HistoryModel) TableName() string {
	return SchemaMigrationHistory
}

func (m HistoryModel) CreateTable(db *gorm.DB) {
	db.Logger.LogMode(logger.Silent)
	if err := db.Exec(CreateTableHistoryModel).Error; err != nil {
		logrus.WithError(err).Fatal("failed to create table")
	}
}
