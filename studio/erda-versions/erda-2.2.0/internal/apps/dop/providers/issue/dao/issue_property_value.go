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

import "github.com/erda-project/erda/pkg/database/dbengine"

// IssuePropertyValue 事件属性值表
type IssuePropertyValue struct {
	dbengine.BaseModel

	PropertyID int64  `gorm:"column:property_id"`
	Value      string `gorm:"column:value"`
	Name       string `gorm:"column:name"`
}

func (IssuePropertyValue) TableName() string {
	return "dice_issue_property_value"
}

func (client *DBClient) CreateIssuePropertyValue(value *IssuePropertyValue) error {
	return client.Create(value).Error
}

func (client *DBClient) CreateIssuePropertyValues(values []IssuePropertyValue) error {
	return client.BulkInsert(values)
}

func (client *DBClient) DeleteIssuePropertyValue(id int64) error {
	return client.Where("id = ?", id).Delete(&IssuePropertyValue{}).Error
}

func (client *DBClient) DeleteIssuePropertyValuesByPropertyID(propertyID int64) error {
	return client.Table("dice_issue_property_value").Where("property_id = ?", propertyID).Delete(&IssuePropertyValue{}).Error
}

func (client *DBClient) UpdateIssuePropertyValue(property *IssuePropertyValue) error {
	return client.Save(property).Error
}

func (client *DBClient) GetIssuePropertyValues(orgID int64) ([]IssuePropertyValue, error) {
	var propertyValues []IssuePropertyValue
	if err := client.Where("property_id = ?", orgID).Find(&propertyValues).Error; err != nil {
		return nil, err
	}
	return propertyValues, nil
}

func (client *DBClient) GetIssuePropertyValue(id int64) (*IssuePropertyValue, error) {
	var propertyValues IssuePropertyValue
	if err := client.Where("id = ?", id).Find(&propertyValues).Error; err != nil {
		return nil, err
	}
	return &propertyValues, nil
}
