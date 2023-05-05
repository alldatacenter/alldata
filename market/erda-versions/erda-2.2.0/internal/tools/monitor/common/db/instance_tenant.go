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
	"encoding/json"
	"errors"

	"github.com/jinzhu/gorm"
)

type InstanceTenantDb struct {
	*gorm.DB
}

func (db *InstanceTenantDb) QueryTkByTenantGroup(tenantGroup string) (string, error) {
	var tenantInfo InstanceTenant
	err := db.
		Select("*").
		Where("tenant_group = ?", tenantGroup).
		Where("engine = ?", "monitor").
		Where("is_deleted = ?", "N").
		Order("create_time", false).
		Limit(1).
		Find(&tenantInfo).
		Error
	if err != nil {
		return "", err
	}
	var config map[string]interface{}
	err = json.Unmarshal([]byte(tenantInfo.Config), &config)
	if err != nil {
		return "", err
	}
	tk := config["TERMINUS_KEY"]
	if tk == nil {
		return "", errors.New("tk not exist")
	}
	return tk.(string), nil
}
