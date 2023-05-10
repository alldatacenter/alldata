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

// IssueAppRelation 事件应用关联
type IssueAppRelation struct {
	dbengine.BaseModel

	IssueID   int64
	CommentID int64
	AppID     int64
	MRID      int64
}

// TableName 表名
func (IssueAppRelation) TableName() string {
	return "dice_issue_app_relations"
}

// CreateIssueAppRelation 创建事件应用关联关系
func (client *DBClient) CreateIssueAppRelation(issueAppRel *IssueAppRelation) error {
	return client.Create(issueAppRel).Error
}

func (client *DBClient) BatchCreateIssueAppRelation(issueAppRel []IssueAppRelation) error {
	return client.BulkInsert(issueAppRel)
}

// DeleteIssueAppRelationsByComment 根据 commentID 删除关联关系
func (client *DBClient) DeleteIssueAppRelationsByComment(commentID int64) error {
	return client.Where("comment_id = ?", commentID).Delete(&IssueAppRelation{}).Error
}

// DeleteIssueAppRelationsByApp 根据 appID 删除关联关系
func (client *DBClient) DeleteIssueAppRelationsByApp(appID int64) error {
	return client.Where("app_id = ?", appID).Delete(&IssueAppRelation{}).Error
}
