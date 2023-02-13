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

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

// Microservices and project associations
type AddonMicroAttach struct {
	ID                uint64    `gorm:"primary_key"`
	AddonName         string    `gorm:"type:varchar(64)"`
	RoutingInstanceID string    `gorm:"type:varchar(64)"`
	InstanceID        string    `gorm:"type:varchar(64)"`
	ProjectID         string    `gorm:"type:varchar(64)"`
	OrgID             string    `gorm:"type:varchar(64)"`
	Env               string    `gorm:"type:varchar(16)"`
	Count             uint32    `gorm:"type:int(11)"`
	Deleted           string    `gorm:"column:is_deleted"`
	CreatedAt         time.Time `gorm:"column:create_time"`
	UpdatedAt         time.Time `gorm:"column:update_time"`
}

func (AddonMicroAttach) TableName() string {
	return "tb_addon_micro_attach"
}

// CreateMicroAttach insert microservice attachment
func (db *DBClient) CreateMicroAttach(addonMicroAttach *AddonMicroAttach) error {
	return db.Create(addonMicroAttach).Error
}

// GetMicroAttachByAddonName 通过addonName来获取microservice attach信息
func (db *DBClient) GetMicroAttachByAddonName(addonName string) (*[]AddonMicroAttach, error) {
	var addonMicroAttachs []AddonMicroAttach
	if err := db.
		Where("addon_name = ?", addonName).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addonMicroAttachs).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get microservice attachments, addonName : %s",
			addonName)
	}
	return &addonMicroAttachs, nil
}

// GetMicroAttachByOrgId 通过orgID来获取microservice attach信息
func (db *DBClient) GetMicroAttachByOrgId(orgID string) (*[]AddonMicroAttach, error) {
	var addonMicroAttachs []AddonMicroAttach
	if err := db.
		Where("org_id = ?", orgID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addonMicroAttachs).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get microservice attachments, orgID : %s",
			orgID)
	}
	return &addonMicroAttachs, nil
}

// GetMicroAttachByProjectAndEnv 通过projectID和env来获取microservice attach信息
func (db *DBClient) GetMicroAttachByProjectAndEnv(projectID, env string) (*[]AddonMicroAttach, error) {
	var addonMicroAttachs []AddonMicroAttach
	if err := db.
		Where("project_id = ?", projectID).
		Where("env = ?", env).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addonMicroAttachs).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get microservice attachments, project_id 、env: %s, %s",
			projectID, env)
	}
	return &addonMicroAttachs, nil
}

// GetMicroAttachByProjects 通过projectID列表获取microservice attach信息
func (db *DBClient) GetMicroAttachByProjects(projectIDs []string) (*[]AddonMicroAttach, error) {
	var addonMicroAttachs []AddonMicroAttach
	if err := db.
		Where("project_id in (?) and is_deleted = ?", projectIDs, apistructs.AddonNotDeleted).
		Find(&addonMicroAttachs).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get microservice attachments.")
	}
	return &addonMicroAttachs, nil
}

// UpdateCount 更新引用数量信息
func (db *DBClient) UpdateCount(id uint64, diff int) error {
	if err := db.Model(&AddonMicroAttach{}).
		Raw("update tb_addon_micro_attach set count = count + ? where id = ? and count > 0", diff, id).Error; err != nil {
		return errors.Wrapf(err, "failed to update count, id: %v", id)
	}
	return nil
}

// DestroyById 根据Id删除引用
func (db *DBClient) DestroyById(id uint64) error {
	if err := db.
		Where("id = ?", id).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return errors.Wrapf(err, "failed to delete microservice attachments, attachId: %v", id)
	}
	return nil
}

// DestroyByInstanceId 根据addon实例Id，删除引用
func (db *DBClient) DestroyByInstanceId(instanceID string) error {
	if err := db.Model(&AddonMicroAttach{}).
		Where("instance_id = ?", instanceID).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return errors.Wrapf(err, "failed to delete microservice attachments, instanceID: %v", instanceID)
	}
	return nil
}

// DestroyByProjectAndEnvAndRoutingId 根据项目Id、环境、addon路由Id，删除引用
func (db *DBClient) DestroyByProjectAndEnvAndRoutingId(instanceID string) error {
	if err := db.Model(&AddonMicroAttach{}).
		Where("instance_id = ?", instanceID).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return errors.Wrapf(err, "failed to delete microservice attachments, instanceID: %v", instanceID)
	}
	return nil
}

// GetAttachmentsByProjectIDs 根据项目 ID 和环境获取微服务引用
func (db *DBClient) GetMicroAttachesByAddonNameAndProjectIDs(addonName string, projectIDs []string, env string) (*[]AddonMicroAttach, error) {
	var attaches []AddonMicroAttach
	conn := db.DB.
		Where("addon_name = ?", addonName).
		Where("project_id in (?)", projectIDs).
		Where("is_deleted = ?", apistructs.AddonNotDeleted)
	if env != "" {
		conn = conn.Where("env = ?", env)
	}

	if err := conn.Find(&attaches).Error; err != nil {
		return nil, err
	}
	return &attaches, nil
}

// GetAttachmentsByProjectIDs 根据项目 ID 和环境获取微服务引用
func (db *DBClient) GetMicroAttachesByAddonName(addonName, orgID string, projectIDs []string) (*[]AddonMicroAttach, error) {
	var attaches []AddonMicroAttach

	dbClient := db.Where("addon_name = ?", addonName).Where("is_deleted = ?", apistructs.AddonNotDeleted)
	if orgID != "" {
		dbClient = dbClient.Where("org_id = ?", orgID)
	}
	if len(projectIDs) > 0 {
		dbClient = dbClient.Where("project_id in (?)", projectIDs)
	}
	if err := dbClient.Find(&attaches).Error; err != nil {
		return nil, err
	}
	return &attaches, nil
}
