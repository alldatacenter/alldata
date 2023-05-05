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

	"github.com/erda-project/erda/pkg/database/gormutil"
)

// InstanceTenantDB .
type InstanceTenantDB struct {
	*gorm.DB
}

func (db *InstanceTenantDB) query() *gorm.DB {
	return db.Table(TableInstanceTenant).Where("`is_deleted`='N'")
}

func (db *InstanceTenantDB) GetByFields(fields map[string]interface{}) (*InstanceTenant, error) {
	query := db.query()
	query, err := gormutil.GetQueryFilterByFields(query, instanceTenantFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*InstanceTenant
	if err := query.Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}

func (db *InstanceTenantDB) GetByID(id string) (*InstanceTenant, error) {
	return db.GetByFields(map[string]interface{}{
		"ID": id,
	})
}

func (db *InstanceTenantDB) GetByTenantGroup(group string) ([]*InstanceTenant, error) {
	if len(group) <= 0 {
		return nil, nil
	}
	var list []*InstanceTenant
	if err := db.query().
		Where("tenant_group=?", group).Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

func (db *InstanceTenantDB) GetInstanceByTenantGroup(group string) (*InstanceTenant, error) {
	if len(group) <= 0 {
		return nil, nil
	}
	var instanceTenant InstanceTenant
	if err := db.query().Where("tenant_group=?", group).
		Where("engine=?", "monitor").Limit(1).Find(&instanceTenant).Error; err != nil {
		return nil, err
	}
	return &instanceTenant, nil
}

func (db *InstanceTenantDB) GetClusterNameByTenantGroup(group string) (string, error) {
	if len(group) <= 0 {
		return "", nil
	}
	var list []*InstanceTenant
	if err := db.query().
		Where("tenant_group=?", group).Limit(1).Find(&list).Error; err != nil {
		return "", err
	}
	if len(list) <= 0 {
		return "", nil
	}
	return list[0].Az, nil
}

func (db *InstanceTenantDB) GetByEngineAndTenantGroup(engine string, tenantGroup string) (*InstanceTenant, error) {
	return db.GetByFields(map[string]interface{}{
		"Engine":      engine,
		"TenantGroup": tenantGroup,
	})
}
