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

	"github.com/erda-project/erda/pkg/common/errors"
)

// MSPTenantDB msp_tenant
type MSPTenantDB struct {
	*gorm.DB
}

func (db *MSPTenantDB) db() *gorm.DB {
	return db.Table(TableMSPTenant).Where("is_deleted = ?", false)
}

func (db *MSPTenantDB) InsertTenant(tenant *MSPTenant) (*MSPTenant, error) {
	err := db.db().Create(tenant).Error

	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return tenant, nil
}

func (db *MSPTenantDB) QueryTenant(tenantID string) (*MSPTenant, error) {
	tenant := MSPTenant{}
	err := db.db().Where("`id` = ?", tenantID).Find(&tenant).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &tenant, nil
}

func (db *MSPTenantDB) QueryTenantByProjectIDAndWorkspace(projectID, workspace string) (*MSPTenant, error) {
	tenant := MSPTenant{}
	err := db.db().
		Where("`related_project_id` = ?", projectID).
		Where("`related_workspace` = ?", workspace).
		Find(&tenant).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &tenant, nil
}

func (db *MSPTenantDB) QueryTenantByProjectID(projectID string) ([]*MSPTenant, error) {
	var tenants []*MSPTenant
	err := db.db().Where("`related_project_id` = ?", projectID).Find(&tenants).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return tenants, nil
}

func (db *MSPTenantDB) DeleteTenantByTenantID(tenantId string) (*MSPTenant, error) {
	tenant, err := db.QueryTenant(tenantId)
	if err != nil {
		return nil, err
	}
	tenant.IsDeleted = true
	err = db.Model(&tenant).Update(&tenant).Error
	if err != nil {
		return nil, err
	}
	return tenant, err
}
