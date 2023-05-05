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

type LogServiceInstanceDB struct {
	*gorm.DB
}

func (db *LogServiceInstanceDB) AddOrUpdateEsUrls(instanceId, esUrls, esConfig string) error {
	var instance LogServiceInstance
	resp := db.Where("id=?", instanceId).Limit(1).Find(&instance)

	if resp.Error == nil && instance.EsUrls == esUrls && instance.EsConfig == esConfig {
		return nil
	}

	if resp.Error == nil {
		instance.EsUrls = esUrls
		instance.EsConfig = esConfig
		return db.Save(&instance).Error
	}

	if !resp.RecordNotFound() {
		return resp.Error
	}

	instance = LogServiceInstance{ID: instanceId, EsUrls: esUrls, EsConfig: esConfig}
	if db.Dialect().GetName() == "mysql" {
		return db.Set("gorm:insert_modifier", "IGNORE").Save(&instance).Error
	}
	return db.Save(&instance).Error
}

func (db *LogServiceInstanceDB) GetFirst() (*LogServiceInstance, error) {
	var instance LogServiceInstance

	resp := db.First(&instance)

	if resp.RecordNotFound() {
		return nil, nil
	}

	return &instance, resp.Error
}
