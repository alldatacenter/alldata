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
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// IssueRelation 事件事件关联
type IssueRelation struct {
	dbengine.BaseModel

	IssueID      uint64
	RelatedIssue uint64
	Comment      string
	Type         string
}

// TableName 表名
func (IssueRelation) TableName() string {
	return "dice_issue_relation"
}

// CreateIssueRelations 创建事件与事件的关联关系
func (client *DBClient) CreateIssueRelations(issueRelation *IssueRelation) error {
	return client.Create(issueRelation).Error
}

func (client *DBClient) IssueRelationsExist(issueRelation *IssueRelation, relatedIssues []uint64) (bool, error) {
	if issueRelation.Type == apistructs.IssueRelationInclusion {
		var count int64
		if err := client.Table("dice_issue_relation").Where("related_issue in (?) and type = ?", relatedIssues, issueRelation.Type).Count(&count).Error; err != nil {
			return false, err
		}
		return count > 0, nil
	}
	sql := client.Table("dice_issue_relation").Where("type = ?", issueRelation.Type)
	var count int64
	if err := sql.Where("(issue_id = ? and related_issue in (?)) or (issue_id in (?) and related_issue = ?)", issueRelation.IssueID, relatedIssues, relatedIssues, issueRelation.IssueID).
		Count(&count).Error; err != nil {
		return false, err
	}
	return count > 0, nil
}

// GetRelatingIssues 获取该事件关联了哪些事件
func (client *DBClient) GetRelatingIssues(issueID uint64, relationType []string) ([]uint64, error) {
	var (
		issueIDs       []uint64
		issueRelations []IssueRelation
	)
	query := client.Table("dice_issue_relation").Where("issue_id = ?", issueID)
	if len(relationType) > 0 {
		query = query.Where("type IN (?)", relationType)
	}
	if err := query.Order("id desc").Find(&issueRelations).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	for _, v := range issueRelations {
		issueIDs = append(issueIDs, v.RelatedIssue)
	}

	return issueIDs, nil
}

// GetRelatedIssues 获取该事件被哪些事件关联了
func (client *DBClient) GetRelatedIssues(issueID uint64, relationType []string) ([]uint64, error) {
	var (
		issueIDs       []uint64
		issueRelations []IssueRelation
	)
	query := client.Table("dice_issue_relation").Where("related_issue = ?", issueID)
	if len(relationType) > 0 {
		query = query.Where("type IN (?)", relationType)
	}
	if err := query.Order("id desc").Find(&issueRelations).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	for _, v := range issueRelations {
		issueIDs = append(issueIDs, v.IssueID)
	}

	return issueIDs, nil
}

// DeleteIssueRelation 删除两条issue之间的关联关系
func (client *DBClient) DeleteIssueRelation(issueID, relatedIssueID uint64, relationTypes []string) error {
	query := client.Table("dice_issue_relation").Where("(issue_id = ? and related_issue = ?) or (issue_id = ? and related_issue = ?)",
		issueID, relatedIssueID, relatedIssueID, issueID)
	if len(relationTypes) > 0 {
		query = query.Where("type IN (?)", relationTypes)
	}
	return query.Delete(IssueRelation{}).Error
}

// ClearIssueRelation 清理所有的issue关联关系
func (client *DBClient) CleanIssueRelation(issueID uint64) error {
	if err := client.Table("dice_issue_relation").Where("issue_id = ? or related_issue = ?", issueID, issueID).
		Delete(IssueRelation{}).Error; err != nil {
		return err
	}

	return nil
}

func (client *DBClient) GetIssueRelationsByIDs(issueIDs []uint64, relationTypes []string) ([]IssueRelation, error) {
	sql := client.Table("dice_issue_relation").Where("issue_id in (?)", issueIDs)
	if len(relationTypes) > 0 {
		sql = sql.Where("type IN (?)", relationTypes)
	}
	var issueRelations []IssueRelation
	if err := sql.Find(&issueRelations).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return issueRelations, nil
}

type childrenCount struct {
	IssueID uint64
	Count   int
}

func (client *DBClient) IssueChildrenCount(issueIDs []uint64, relationType []string) ([]childrenCount, error) {
	sql := client.Table("dice_issue_relation").Where("issue_id IN (?)", issueIDs)
	if len(relationType) > 0 {
		sql = sql.Where("type IN (?)", relationType)
	}
	var res []childrenCount
	if err := sql.Select("issue_id, count(id) as count").Group("issue_id").Find(&res).Error; err != nil {
		return nil, err
	}
	return res, nil
}

func (client *DBClient) BatchCreateIssueRelations(issueRels []IssueRelation) error {
	return client.BulkInsert(issueRels)
}
