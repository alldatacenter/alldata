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

	"github.com/erda-project/erda/apistructs"
)

type LabelRelationConfigDB struct {
	*gorm.DB
}

// CreateLabelRelation 创建标签关联关系
func (client *LabelRelationConfigDB) CreateLabelRelation(lr *LabelRelation) error {
	return client.Create(lr).Error
}

// DeleteLabelRelations 删除标签关联关系
func (client *LabelRelationConfigDB) DeleteLabelRelations(refType apistructs.ProjectLabelType, refID string) error {
	return client.Where("ref_type = ?", refType).Where("ref_id = ?", refID).
		Delete(LabelRelation{}).Error
}

// DeleteLabelRelationsByLabel 根据 labelID 删除标签
func (client *LabelRelationConfigDB) DeleteLabelRelationsByLabel(labelID uint64) error {
	return client.Where("label_id = ?", labelID).Delete(LabelRelation{}).Error
}

// GetLabelRelationsByRef 获取标签关联关系列表
func (client *LabelRelationConfigDB) GetLabelRelationsByRef(refType apistructs.ProjectLabelType, refID string) ([]LabelRelation, error) {
	var lrs []LabelRelation
	if err := client.Where("ref_type = ?", refType).Where("ref_id = ?", refID).
		Find(&lrs).Error; err != nil {
		return nil, err
	}

	return lrs, nil
}

// GetLabelRelationsByLabels 获取标签关联关系列表
func (client *LabelRelationConfigDB) GetLabelRelationsByLabels(refType apistructs.ProjectLabelType, labelIDs []uint64) ([]LabelRelation, error) {
	var lrs []LabelRelation
	if err := client.Debug().Where("ref_type = ?", refType).Where("label_id in (?)", labelIDs).
		Find(&lrs).Error; err != nil {
		return nil, err
	}

	return lrs, nil
}

// BatchQueryReleaseTagIDMap 批量查询 release label id
func (client *LabelRelationConfigDB) BatchQueryReleaseTagIDMap(releaseIDs []string) (map[string][]uint64, error) {
	if len(releaseIDs) == 0 {
		return nil, nil
	}
	var refs []LabelRelation
	sql := client.Where("`ref_type` = ?", "release").Where("`ref_id` IN (?)", releaseIDs).Find(&refs)
	if err := sql.Error; err != nil {
		return nil, err
	}
	if len(refs) == 0 {
		return nil, nil
	}
	// key: releaseID, value: labelIDs
	m := make(map[string][]uint64, len(releaseIDs))
	for _, ref := range refs {
		m[ref.RefID] = append(m[ref.RefID], ref.LabelID)
	}
	return m, nil
}
