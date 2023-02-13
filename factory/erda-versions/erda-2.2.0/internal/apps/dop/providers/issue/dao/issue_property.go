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
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// IssueProperty 事件属性表
type IssueProperty struct {
	dbengine.BaseModel

	ScopeType         string `gorm:"column:scope_type"`          // 系统管理员(sys)/企业(org)/项目(project)/应用(app)
	ScopeID           int64  `gorm:"column:scope_id"`            // 企业ID/项目ID/应用ID
	OrgID             int64  `gorm:"column:org_id"`              // 冗余 OrgID，方便删除企业时删除所有相关issue
	Required          bool   `gorm:"column:required"`            // 是否是必填项
	PropertyType      string `gorm:"column:property_type"`       // 属性的的类型（单选，多选，文本）
	PropertyName      string `gorm:"column:property_name"`       // 属性的名称
	DisplayName       string `gorm:"column:display_name"`        // 属性的展示名称
	PropertyIssueType string `gorm:"column:property_issue_type"` // 事件类型
	Relation          int64  `gorm:"column:relation"`            // 关联公用事件
	Index             int64  `gorm:"column:index"`               // 字段排序级
}

func (IssueProperty) TableName() string {
	return "dice_issue_property"
}

func (client *DBClient) CreateIssueProperty(property *IssueProperty) error {
	return client.Create(property).Error
}

func (client *DBClient) DeleteIssueProperty(orgId int64, propertyIssueType string, id int64, index int64) error {
	return client.Where(&IssueProperty{}).Where("id = ?", id).Delete(&IssueProperty{}).Error
}

func (client *DBClient) UpdateIssuePropertiesIndex(properties []IssueProperty) error {
	for i := range properties {
		property := properties[i]
		if err := client.Save(&property).Error; err != nil {
			return err
		}
	}
	return nil
}

func (client *DBClient) UpdateIssueProperty(property *IssueProperty) error {
	return client.Save(property).Error
}

func (client *DBClient) GetIssueProperties(req pb.GetIssuePropertyRequest) ([]IssueProperty, error) {
	var properties []IssueProperty
	db := client.Where(IssueProperty{}).Where("org_id = ?", req.OrgID)
	if req.PropertyIssueType != "" {
		db = db.Where("property_issue_type = ?", req.PropertyIssueType)
	}
	str := "%" + req.PropertyName + "%"
	if req.PropertyName != "" {
		db = db.Where("property_name LIKE ?", str)
	}
	if err := db.Order("index").Find(&properties).Error; err != nil {
		return nil, err
	}

	return properties, nil
}

func (client *DBClient) GetIssuePropertyByID(id int64) (*IssueProperty, error) {
	var property IssueProperty
	if err := client.Where(IssueProperty{}).Where("id = ?", id).Limit(1).Find(&property).Error; err != nil {
		return nil, err
	}
	return &property, nil
}

func (client *DBClient) GetIssuePropertiesByTime(orgID int64) ([]IssueProperty, error) {
	var properties []IssueProperty
	if err := client.Where(IssueProperty{}).Where("org_id = ?", orgID).Order("updated_at").Find(&properties).Error; err != nil {
		return nil, err
	}
	return properties, nil
}

func (client *DBClient) GetIssuePropertiesByRelation(ID int64) (*IssueProperty, error) {
	var property IssueProperty
	if err := client.Table("dice_issue_property").Where("relation = ?", ID).First(&property).Error; err != nil {
		return nil, err
	}
	return &property, nil
}

// GetIssuePropertyByName 根据 name 获取 property 信息
func (client *DBClient) GetIssuePropertyByName(orgID int64, Name string, PropertyIssueType string) (*IssueProperty, error) {
	var property IssueProperty
	if err := client.Table("dice_issue_property").Where("org_id = ?", orgID).
		Where("property_name = ?", Name).Where("property_issue_type = ?", PropertyIssueType).First(&property).Error; err != nil {
		return nil, err
	}
	return &property, nil
}
