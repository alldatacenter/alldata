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

// IssuePropertyRelation 事件属性实例关联表
type IssuePropertyRelation struct {
	dbengine.BaseModel

	OrgID           int64  `gorm:"column:org_id"` // 冗余 OrgID，方便删除企业时删除所有相关issue
	ProjectID       int64  `gorm:"column:project_id"`
	IssueID         int64  `gorm:"column:issue_id"`          // 事件ID
	PropertyID      int64  `gorm:"column:property_id"`       // 字段ID
	PropertyValueID int64  `gorm:"column:property_value_id"` // 枚举值ID
	ArbitraryValue  string `gorm:"column:arbitrary_value"`   // 字段自定义输入值
}

func (IssuePropertyRelation) TableName() string {
	return "dice_issue_property_relation"
}

func (client *DBClient) CreatePropertyRelation(relation *IssuePropertyRelation) error {
	return client.Create(relation).Error
}

func (client *DBClient) CreatePropertyRelations(relations []IssuePropertyRelation) error {
	return client.BulkInsert(relations)
}

func (client *DBClient) DeletePropertyRelationsByPropertyID(issueID int64, propertyID int64) error {
	db := client.Table("dice_issue_property_relation").Where("property_id = ?", propertyID)
	// issueID为-1 表示删除全部issue
	if issueID != -1 {
		db = db.Where("issue_id = ?", issueID)
	}
	return db.Delete(IssuePropertyRelation{}).Error
}

func (client *DBClient) UpdatePropertyRelationArbitraryValue(issueID int64, propertyID int64, value string) error {
	return client.Table("dice_issue_property_relation").Where("issue_id = ?", issueID).
		Where("property_id = ?", propertyID).Update("arbitrary_value", value).Error
}

func (client *DBClient) DeletePropertyRelationByIssueID(issueID int64) error {
	return client.Table("dice_issue_property_relation").Where("issue_id = ?", issueID).
		Delete(IssuePropertyRelation{}).Error
}

func (client *DBClient) GetPropertyRelationByID(issueID int64) ([]IssuePropertyRelation, error) {
	var relations []IssuePropertyRelation
	err := client.Table("dice_issue_property_relation").Where("issue_id = ?", issueID).Where("property_id != 0").Find(&relations).Error
	if err != nil {
		return nil, err
	}
	return relations, nil
}

func (client *DBClient) PagingPropertyRelationByIDs(issueID []int64) ([]IssuePropertyRelation, error) {
	var relations []IssuePropertyRelation
	err := client.Table("dice_issue_property_relation").Where("issue_id in (?)", issueID).Find(&relations).Error
	if err != nil {
		return nil, err
	}
	return relations, nil
}

func (client *DBClient) GetPropertyRelationByPropertyID(propertyID int64) (*IssuePropertyRelation, error) {
	var relations IssuePropertyRelation
	err := client.Table("dice_issue_property_relation").Where("property_id = ?", propertyID).Limit(1).Find(&relations).Error
	if err != nil {
		return nil, err
	}
	return &relations, nil
}

func (client *DBClient) GetPropertyRelationByIssueID(issueID int64, propertyID int64) (*IssuePropertyRelation, error) {
	var relations IssuePropertyRelation
	err := client.Table("dice_issue_property_relation").Where("property_id = ?", propertyID).Where("issue_id = ?", issueID).Find(&relations).Error
	if err != nil {
		return nil, err
	}
	return &relations, nil
}
