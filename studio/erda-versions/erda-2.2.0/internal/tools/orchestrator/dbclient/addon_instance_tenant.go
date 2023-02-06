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

	"github.com/erda-project/erda/apistructs"
)

// AddonInstance addon 租户
type AddonInstanceTenant struct {
	ID                     string `gorm:"type:varchar(64);primary_key"` // 主键
	Name                   string `gorm:"type:varchar(64)"`             // project 级唯一
	AddonInstanceID        string `gorm:"type:varchar(64)"`             // addon 实例 ID
	AddonInstanceRoutingID string `gorm:"type:varchar(64)"`             // addon 实例 ID
	Config                 string `gorm:"type:text"`
	OrgID                  string
	ProjectID              string
	AppID                  string `gorm:"column:app_id"`
	Workspace              string `gorm:"type:varchar(20)"`  // DEV/TEST/STAGING/PROD
	Deleted                string `gorm:"column:is_deleted"` // Y: 已删除 N: 未删除
	KmsKey                 string `gorm:"column:kms_key"`
	Reference              int
	CreatedAt              time.Time `gorm:"column:create_time"`
	UpdatedAt              time.Time `gorm:"column:update_time"`
}

func (AddonInstanceTenant) TableName() string {
	return "tb_addon_instance_tenant"
}

func (db *DBClient) CreateAddonInstanceTenant(tenant *AddonInstanceTenant) error {
	return db.Create(tenant).Error
}

func (db *DBClient) UpdateAddonInstanceTenant(tenant *AddonInstanceTenant) error {
	return db.Save(tenant).Error
}

func (db *DBClient) DeleteAddonInstanceTenant(tenantID string) error {
	if err := db.Where("id = ?", tenantID).
		Updates(map[string]interface{}{
			"is_deleted": apistructs.AddonDeleted}).Error; err != nil {
		return err
	}
	return nil
}

func (db *DBClient) GetAddonInstanceTenant(id string) (*AddonInstanceTenant, error) {
	var instance AddonInstanceTenant
	if err := db.Where("id = ?", id).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&instance).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return &instance, nil
}

func (db *DBClient) ListAddonInstanceTenantByAddonInstanceID(addoninstanceId string) ([]AddonInstanceTenant, error) {
	var instances []AddonInstanceTenant
	if err := db.Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("addon_instance_id = ?", addoninstanceId).
		Find(&instances).Error; err != nil {
		return nil, err
	}
	return instances, nil
}

func (db *DBClient) ListAddonInstanceTenantByAddonInstanceRoutingID(addoninstanceroutingId string) ([]AddonInstanceTenant, error) {
	var instances []AddonInstanceTenant
	if err := db.Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("addon_instance_routing_id = ?", addoninstanceroutingId).
		Find(&instances).Error; err != nil {
		return nil, err
	}
	return instances, nil
}

func (db *DBClient) ListAddonInstanceTenantByProjectIDs(projectIDs []uint64, workspace ...string) ([]AddonInstanceTenant, error) {
	var instances []AddonInstanceTenant

	q := db.Where("project_id in (?)", projectIDs).Where("is_deleted = ?", apistructs.AddonNotDeleted)

	if len(workspace) != 0 {
		q = q.Where("workspace = ?", workspace[0])
	}

	if err := q.Find(&instances).Error; err != nil {
		return nil, err
	}

	return instances, nil
}

func (db *DBClient) ListAddonInstanceTenant() ([]AddonInstanceTenant, error) {
	var instances []AddonInstanceTenant

	q := db.Where("is_deleted = ?", apistructs.AddonNotDeleted)

	if err := q.Find(&instances).Error; err != nil {
		return nil, err
	}

	return instances, nil
}
