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
	"github.com/erda-project/erda-proto-go/dop/issue/stream/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// IssueStream 事件流水表
type IssueStream struct {
	dbengine.BaseModel

	IssueID      int64           // issue id
	Operator     string          // 操作人
	StreamType   string          // 通过事件流类型找到对应模板
	StreamParams common.ISTParam // 事件流模板值, 参数取值范围见: apistructs.ISTParam
}

// TableName 表名
func (IssueStream) TableName() string {
	return "dice_issue_streams"
}

// CreateIssueStream 创建 issue 事件流
func (client *DBClient) CreateIssueStream(is *IssueStream) error {
	return client.Create(is).Error
}

func (client *DBClient) BatchCreateIssueStream(issueStreams []IssueStream) error {
	return client.BulkInsert(issueStreams)
}

// UpdateIssueStream 更新 issue 事件流
func (client *DBClient) UpdateIssueStream(is *IssueStream) error {
	return client.Update(is).Error
}

// PagingIssueStream issue 分页查询事件流
func (client *DBClient) PagingIssueStream(req *pb.PagingIssueStreamsRequest) (int64, []IssueStream, error) {
	var (
		total        int64
		issueStreams []IssueStream
	)

	if err := client.Where("issue_id = ?", req.IssueID).
		Order("id DESC").
		Offset((req.PageNo - 1) * req.PageSize).Limit(req.PageSize).Find(&issueStreams).
		// reset offset & limit before count
		Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return 0, nil, err
	}
	return total, issueStreams, nil
}

func (client *DBClient) FindIssueStream(issueId int) ([]IssueStream, error) {
	var issueStreams []IssueStream

	if err := client.Where("issue_id = ?", issueId).
		Order("id DESC").Find(&issueStreams).Error; err != nil {
		return nil, err
	}
	return issueStreams, nil
}

type IssueStreamExtra struct {
	IssueStream
	ProjectID uint64
	IssueType apistructs.IssueType
}

func (client *DBClient) ListIssueStreamExtraForIssueStateTransMigration() ([]IssueStreamExtra, error) {
	var issueStreamExtras []IssueStreamExtra
	err := client.Table("dice_issue_streams AS stream").
		Select("stream.*, issue.project_id, issue.type AS issue_type").
		Joins("LEFT JOIN dice_issues issue ON stream.issue_id = issue.id").
		Where("stream.stream_type = ?", apistructs.ISTTransferState).
		Find(&issueStreamExtras).Error
	return issueStreamExtras, err
}
