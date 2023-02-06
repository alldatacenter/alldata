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

// AddonAttachment addon & runtime 关联关系
type AddonAttachment struct {
	ID                uint64 `gorm:"primary_key"`
	InstanceID        string `gorm:"type:varchar(64)"` // AddonInstance 主键
	RoutingInstanceID string `gorm:"type:varchar(64)"` // AddonInstanceRouting 主键
	TenantInstanceID  string `gorm:"type:varchar(64)"`

	MySQLAccountID         string `gorm:"column:mysql_account_id"`
	PreviousMySQLAccountID string `gorm:"column:previous_mysql_account_id"`
	// MySQLAccountState CUR, PRE
	MySQLAccountState string `gorm:"column:mysql_account_state"`

	Options       string `gorm:"type:text"`
	OrgID         string
	ProjectID     string
	ApplicationID string
	RuntimeID     string `gorm:"column:app_id"`
	InsideAddon   string `gorm:"type:varchar(1)"` // N or Y
	RuntimeName   string
	Deleted       string    `gorm:"column:is_deleted"` // Y: 已删除 N: 未删除
	CreatedAt     time.Time `gorm:"column:create_time"`
	UpdatedAt     time.Time `gorm:"column:update_time"`
}

// TableName 数据库表名
func (AddonAttachment) TableName() string {
	return "tb_addon_attachment"
}

// CreateAttachment insert addonAttachment
func (db *DBClient) CreateAttachment(addonAttachment *AddonAttachment) error {
	return db.Create(addonAttachment).Error
}

// UpdateAttachment update addonAttachment
func (db *DBClient) UpdateAttachment(addonAttachment *AddonAttachment) error {
	return db.Save(addonAttachment).Error
}

// DestroyByIntsanceID 根据instanceID逻辑删除attach信息
func (db *DBClient) DestroyByIntsanceID(instanceID string) error {
	if err := db.Model(&AddonAttachment{}).
		Where("instance_id = ?", instanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return errors.Wrapf(err, "failed to delete addon attachments, instanceID: %v", instanceID)
	}
	return nil
}

// DeleteAttachmentsByRoutingInstanceID 根据 routingInstanceID 删除 attachment
func (db *DBClient) DeleteAttachmentsByRoutingInstanceID(routingInstanceID string) error {
	if err := db.Model(&AddonAttachment{}).
		Where("routing_instance_id = ?", routingInstanceID).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return err
	}
	return nil
}

// DeleteAttachmentByRuntimeAndRoutingInstanceID 根据 runtimeID & routingInstanceID 删除 attachment
func (db *DBClient) DeleteAttachmentByRuntimeAndRoutingInstanceID(runtimeID, routingInstanceID string) error {
	if err := db.Model(&AddonAttachment{}).
		Where("app_id = ?", runtimeID).
		Where("routing_instance_id = ?", routingInstanceID).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return err
	}
	return nil
}

// DeleteAttachmentByRuntimeAndInstanceID 根据 runtimeID & InstanceID 删除 attachment
func (db *DBClient) DeleteAttachmentByRuntimeAndInstanceID(runtimeID, instanceID string) error {
	if err := db.Model(&AddonAttachment{}).
		Where("app_id = ?", runtimeID).
		Where("instance_id = ?", instanceID).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return err
	}
	return nil
}

// GetAttachmentCountByRoutingInstanceID count数据量
func (db *DBClient) GetAttachmentCountByRoutingInstanceID(routingInstanceID string) (int64, error) {
	// 获取匹配搜索结果总量
	var total int64
	if err := db.
		Where("routing_instance_id = ?", routingInstanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Model(&AddonAttachment{}).
		Count(&total).Error; err != nil {
		return 0, err
	}
	return total, nil
}

// GetAttachmentCountByInstanceID count数据量
func (db *DBClient) GetAttachmentCountByInstanceID(instanceID string) (int64, error) {
	// 获取匹配搜索结果总量
	var total int64
	if err := db.
		Where("instance_id = ?", instanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Model(&AddonAttachment{}).
		Count(&total).Error; err != nil {
		return 0, err
	}
	return total, nil
}

func (db *DBClient) GetAttachmentByID(id uint64) (*AddonAttachment, error) {
	var addonAttachment AddonAttachment
	if err := db.
		Where("id = ?", id).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		First(&addonAttachment).Error; err != nil {
		return nil, err
	}
	return &addonAttachment, nil
}

// GetAttachMentsByRuntimeID 根据runtimeID获取attachment信息
func (db *DBClient) GetAttachMentsByRuntimeID(runtimeID uint64) (*[]AddonAttachment, error) {
	var attachments []AddonAttachment
	if err := db.
		Where("app_id = ?", runtimeID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&attachments).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon attachments info, runtimeID : %d",
			runtimeID)
	}
	return &attachments, nil
}

// GetUnDeletableAttachMentsByRuntimeID 根据runtimeID获取不可删除的attachment信息
func (db *DBClient) GetUnDeletableAttachMentsByRuntimeID(runtimeID uint64) (*[]AddonAttachment, error) {
	var attachments []AddonAttachment
	if err := db.
		Where("app_id = ?", runtimeID).
		Where("is_deleted != ?", apistructs.AddonDeleted).
		Find(&attachments).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon attachments info, runtimeID : %d",
			runtimeID)
	}
	return &attachments, nil
}

// GetByRuntimeIDAndRoutingInstanceID 根据runtimeID、routingInstanceID获取attachment信息
func (db *DBClient) GetByRuntimeIDAndRoutingInstanceID(runtimeID, routingInstanceID string) (*[]AddonAttachment, error) {
	var attachments []AddonAttachment
	if err := db.
		Where("app_id = ?", runtimeID).
		Where("routing_instance_id = ?", routingInstanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&attachments).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon attachments info, runtimeID : %s, routingInstanceID : %s",
			runtimeID, routingInstanceID)
	}
	return &attachments, nil
}

// GetByRuntimeIDAndInstanceID 根据runtimeID、instanceId获取attachment信息
func (db *DBClient) GetByRuntimeIDAndInstanceID(runtimeID, instanceID string) (*[]AddonAttachment, error) {
	var attachments []AddonAttachment
	if err := db.
		Where("app_id = ?", runtimeID).
		Where("instance_id = ?", instanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&attachments).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon attachments info, runtimeID : %s, instanceID : %s",
			runtimeID, instanceID)
	}
	return &attachments, nil
}

// GetAttachmentsByInstanceID 根据instanceId获取attachment信息
func (db *DBClient) GetAttachmentsByInstanceID(instanceID string) (*[]AddonAttachment, error) {
	var attachments []AddonAttachment
	if err := db.
		Where("instance_id = ?", instanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&attachments).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon attachments info, instanceID : %s",
			instanceID)
	}
	return &attachments, nil
}

// GetAttachmentsByRoutingInstanceID 根据routingInstanceID获取attachment信息
func (db *DBClient) GetAttachmentsByRoutingInstanceID(routingInstanceID string) (*[]AddonAttachment, error) {
	var attachments []AddonAttachment
	if err := db.
		Where("routing_instance_id = ?", routingInstanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&attachments).Error; err != nil {
		return nil, err
	}
	return &attachments, nil
}

// GetAttachmentsByTenantInstanceID 根据 tenantInstanceID 获取 attachment 信息
func (db *DBClient) GetAttachmentsByTenantInstanceID(tenantInstanceID string) (*[]AddonAttachment, error) {
	var attachments []AddonAttachment
	if err := db.
		Where("tenant_instance_id = ?", tenantInstanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&attachments).Error; err != nil {
		return nil, err
	}
	return &attachments, nil
}

// ListAttachmentIDRuntimeIDNotExist Find attachments whose runtime does not exist
func (db *DBClient) ListAttachmentIDRuntimeIDNotExist() ([]AddonAttachment, error) {
	var attachments []AddonAttachment
	if err := db.Model(&AddonAttachment{}).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Where("app_id NOT IN (SELECT id FROM ps_v2_project_runtimes)").
		Find(&attachments).Error; err != nil {
		return nil, err
	}
	return attachments, nil
}

// DeleteAttachmentByIDs Delete attachments by ids
func (db *DBClient) DeleteAttachmentByIDs(id ...uint64) error {
	if err := db.Model(&AddonAttachment{}).
		Where("id IN (?)", id).
		Update("is_deleted", apistructs.AddonDeleted).Error; err != nil {
		return err
	}
	return nil
}
