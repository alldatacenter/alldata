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
	"github.com/erda-project/erda/internal/apps/dop/model"
)

// CreateComment 创建工单评论
func (client *DBClient) CreateComment(comment *model.Comment) error {
	return client.Create(comment).Error
}

// UpdateComment 更新工单评论
func (client *DBClient) UpdateComment(comment *model.Comment) error {
	return client.Save(comment).Error
}

// GetCommentByID 根据commentID获取评论
func (client *DBClient) GetCommentByID(commentID int64) (*model.Comment, error) {
	var comment model.Comment
	if err := client.Where("id = ?", commentID).Find(&comment).Error; err != nil {
		return nil, err
	}
	return &comment, nil
}

// GetCommentsByTicketID 根据ticketID获取工单评论
func (client *DBClient) GetCommentsByTicketID(ticketID int64, pageNo, pageSize int) (int64, []model.Comment, error) {
	var (
		total    int64
		comments []model.Comment
	)
	if err := client.Where("ticket_id = ?", ticketID).
		Offset((pageNo - 1) * pageSize).Limit(pageSize).Find(&comments).Error; err != nil {
		return 0, nil, err
	}
	if err := client.Model(&model.Comment{}).Where("ticket_id = ?", ticketID).Count(&total).Error; err != nil {
		return 0, nil, err
	}
	return total, comments, nil
}

// GetLastCommentByTicket 根据 ticketID 获取最新评论
func (client *DBClient) GetLastCommentByTicket(ticketID int64) (*model.Comment, error) {
	var comment model.Comment
	if err := client.Where("ticket_id = ?", ticketID).Order("created_at DESC").First(&comment).Error; err != nil {
		return nil, err
	}
	return &comment, nil
}
