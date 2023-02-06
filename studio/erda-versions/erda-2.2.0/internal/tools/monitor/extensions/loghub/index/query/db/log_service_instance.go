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
	"github.com/jinzhu/gorm"
)

// LogServiceInstanceTable .
const LogServiceInstanceTable = "sp_log_service_instance"

// LogServiceInstance .
type LogServiceInstance struct {
	ID         int    `gorm:"column:id;primary_key"`
	InstanceID string `gorm:"column:instance_id"`
	EsUrls     string `gorm:"column:es_urls"`
	EsConfig   string `gorm:"column:es_config"`
}

// TableName .
func (LogServiceInstance) TableName() string { return LogServiceInstanceTable }

// LogServiceInstanceDB .
type LogServiceInstanceDB struct {
	*gorm.DB
}

func (db *LogServiceInstanceDB) GetFirst() (*LogServiceInstance, error) {
	var instance LogServiceInstance

	resp := db.First(&instance)

	if resp.RecordNotFound() {
		return nil, nil
	}

	return &instance, resp.Error
}
