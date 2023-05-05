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
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type IssueStage struct {
	dbengine.BaseModel

	OrgID     int64                `gorm:"column:org_id"`
	IssueType apistructs.IssueType `gorm:"column:issue_type"`
	Name      string               `gorm:"column:name"`
	Value     string               `gorm:"column:value"`
}

func (i IssueStage) TableName() string {
	return "dice_issue_stage"
}

func (client *DBClient) CreateIssueStage(stages []IssueStage) error {
	return client.BulkInsert(stages)
}

func (client *DBClient) DeleteIssuesStage(orgID int64, issueType apistructs.IssueType) error {
	return client.Table("dice_issue_stage").Where("org_id = ?", orgID).
		Where("issue_type = ?", issueType).Delete(IssueStage{}).Error
}

func (client *DBClient) GetIssuesStage(orgID int64, issueType apistructs.IssueType) ([]IssueStage, error) {
	var stages []IssueStage
	err := client.Table("dice_issue_stage").Where("org_id = ?", orgID).
		Where("issue_type = ?", issueType).Find(&stages).Error
	if err != nil {
		return nil, err
	}
	return stages, nil
}

// GetIssuesStageByOrgID get issuesStage by orgID
func (client *DBClient) GetIssuesStageByOrgID(orgID int64) ([]IssueStage, error) {
	var stages []IssueStage
	err := client.Table("dice_issue_stage").Where("org_id = ?", orgID).
		Find(&stages).Error
	return stages, err
}
