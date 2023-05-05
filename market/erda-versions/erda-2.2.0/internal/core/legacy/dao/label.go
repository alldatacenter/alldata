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
	"github.com/erda-project/erda/apistructs"
)

// Label 标签
type Label struct {
	BaseModel
	Name      string                      // 标签名称
	Type      apistructs.ProjectLabelType // 标签作用类型
	Color     string                      // 标签颜色
	ProjectID uint64                      // 标签所属项目
	Creator   string                      // 创建人
}

// TableName 表名
func (Label) TableName() string {
	return "dice_labels"
}

// CreateLabel 创建 label
func (client *DBClient) CreateLabel(label *Label) error {
	return client.Create(label).Error
}

// UpdateLabel 更新 label
func (client *DBClient) UpdateLabel(label *Label) error {
	return client.Save(label).Error
}

// DeleteLabel 删除标签
func (client *DBClient) DeleteLabel(labelID int64) error {
	return client.Where("id = ?", labelID).Delete(&Label{}).Error
}

// ListLabel 获取标签列表
func (client *DBClient) ListLabel(req *apistructs.ProjectLabelListRequest) (int64, []Label, error) {
	var (
		total  int64
		labels []Label
	)
	cond := Label{
		ProjectID: req.ProjectID,
	}
	if req.Type != "" {
		cond.Type = req.Type
	}
	sql := client.Where(cond)
	if req.Key != "" {
		sql = sql.Where("name LIKE ?", "%"+req.Key+"%")
	}
	if err := sql.Order("updated_at desc").Offset((req.PageNo - 1) * req.PageSize).Limit(req.PageSize).
		Find(&labels).Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, labels, nil
}

// GetLabel 根据标签ID获取标签详情
func (client *DBClient) GetLabel(labelID int64) (*Label, error) {
	var label Label
	if err := client.Where("id = ?", labelID).Find(&label).Error; err != nil {
		return nil, err
	}
	return &label, nil
}

// GetLabelByNameAndType 根据 name 获取标签详情
func (client *DBClient) GetLabelByNameAndType(projectID uint64, name string, typ string) (*Label, error) {
	var label Label
	if err := client.Where("project_id = ?", projectID).Where("name = ?", name).Where("type = ?", typ).
		First(&label).Error; err != nil {
		return nil, err
	}
	return &label, nil
}

// ListLabelByNames 根据 name 列表获取标签列表
func (client *DBClient) ListLabelByNames(projectID uint64, names []string) ([]Label, error) {
	var labels []Label
	if err := client.Where("project_id = ?", projectID).Where("name in (?)", names).Find(&labels).Error; err != nil {
		return nil, err
	}
	return labels, nil
}

// GetLabels 根据 labelID 获取标签列表
func (client *DBClient) GetLabels(labelIDs []uint64) ([]Label, error) {
	var labels []Label
	if err := client.Where("id in (?)", labelIDs).Find(&labels).Error; err != nil {
		return nil, err
	}
	return labels, nil
}
