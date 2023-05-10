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

package dao

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

// CreateApprove 创建Approve
func (client *DBClient) CreateApprove(approve *model.Approve) error {
	return client.Create(approve).Error
}

// UpdateApprove 更新Approve
func (client *DBClient) UpdateApprove(approve *model.Approve) error {
	return client.Save(approve).Error
}

// DeleteApprove 删除Approve
func (client *DBClient) DeleteApprove(approveID int64) error {
	return client.Where("id = ?", approveID).Delete(&model.Approve{}).Error
}

// GetApproveByID 根据approveID获取Approve信息
func (client *DBClient) GetApproveByID(approveID int64) (model.Approve, error) {
	var approve model.Approve
	if err := client.Where("id = ?", approveID).Find(&approve).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return approve, ErrNotFoundApprove
		}
		return approve, err
	}
	return approve, nil
}

// GetApprovesByOrgIDAndStatus 根据orgID与审批状态获取Approve列表
func (client *DBClient) GetApprovesByOrgIDAndStatus(params *apistructs.ApproveListRequest) (
	int, []model.Approve, error) {
	var (
		approves []model.Approve
		total    int
	)
	db := client.Where("org_id = ?", params.OrgID)
	if params.Status != nil {
		db = db.Where("status in (?)", params.Status)
	}
	db = db.Order("updated_at DESC")
	if err := db.Offset((params.PageNo - 1) * params.PageSize).Limit(params.PageSize).
		Find(&approves).Error; err != nil {
		return 0, nil, err
	}

	// 获取总量
	db = client.Model(&model.Approve{}).Where("org_id = ?", params.OrgID)
	if params.Status != nil {
		db = db.Where("status in (?)", params.Status)
	}
	if err := db.Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, approves, nil
}

// GetApproveByOrgAndID 根据orgID & Approve名称 获取证书信息
func (client *DBClient) GetApproveByOrgAndID(approveType apistructs.ApproveType,
	orgID, targetTD, entityID uint64) (*model.Approve, error) {
	var approve model.Approve
	if err := client.Where("org_id = ?", orgID).
		Where("type = ?", approveType).
		Where("target_id = ?", targetTD).
		Where("status = ?", apistructs.ApprovalStatusPending).
		Where("entity_id = ?", entityID).Find(&approve).Error; err != nil {
		return nil, err
	}
	return &approve, nil
}

func (client *DBClient) ListUnblockApplicationApprove(orgID uint64) ([]model.Approve, error) {
	var approves []model.Approve
	if err := client.Where("org_id = ?", orgID).
		Where("type = 'unblock-application'").
		Find(&approves).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return approves, nil
}
