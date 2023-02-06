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

package comment

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/internal/pkg/user"
)

// Comment 工单评论操作封装
type Comment struct {
	db  *dao.DBClient
	bdl *bundle.Bundle
}

// Option 定义 Comment 配置选项
type Option func(*Comment)

// New 新建 Comment 实例
func New(options ...Option) *Comment {
	t := &Comment{}
	for _, op := range options {
		op(t)
	}
	return t
}

// WithDBClient 配置 Comment 数据库选项
func WithDBClient(db *dao.DBClient) Option {
	return func(t *Comment) {
		t.db = db
	}
}

// WithBundle 配置 Comment bundle选项
func WithBundle(bdl *bundle.Bundle) Option {
	return func(t *Comment) {
		t.bdl = bdl
	}
}

// Create 创建工单评论
func (c *Comment) Create(userID user.ID, req *apistructs.CommentCreateRequest) (int64, error) {
	if req.UserID != userID.String() {
		return 0, errors.Errorf("user id doesn't match")
	}
	if ticket, err := c.db.GetTicket(req.TicketID); err != nil || ticket == nil {
		return 0, errors.Errorf("invalid ticket id %v, (%v)", req.TicketID, err)
	}

	if req.CommentType == "" {
		req.CommentType = apistructs.NormalTCType
	}
	comment := model.Comment{
		TicketID:    req.TicketID,
		CommentType: req.CommentType,
		Content:     req.Content,
		IRComment:   req.IRComment,
		UserID:      req.UserID,
	}
	if err := c.db.CreateComment(&comment); err != nil {
		return 0, err
	}

	return int64(comment.ID), nil
}

// Update 更新工单评论
func (c *Comment) Update(commentID int64, userID user.ID, req *apistructs.CommentUpdateRequestBody) error {
	if req.Content == "" {
		return errors.Errorf("content is empty")
	}
	comment, err := c.db.GetCommentByID(commentID)
	if err != nil {
		return err
	}
	if comment.UserID != userID.String() {
		return errors.Errorf("user id doesn't match")
	}
	comment.Content = req.Content

	return c.db.UpdateComment(comment)
}

// List 工单评论列表
func (c *Comment) List(ticketID int64, pageNo, pageSize int) (*apistructs.CommentListResponseData, error) {
	total, comments, err := c.db.GetCommentsByTicketID(ticketID, pageNo, pageSize)
	if err != nil {
		return nil, err
	}
	commentDTOs := make([]apistructs.Comment, 0, len(comments))
	for i := range comments {
		commentDTO := c.convertToCommentDTO(&comments[i])
		commentDTOs = append(commentDTOs, *commentDTO)
	}
	return &apistructs.CommentListResponseData{Total: total, Comments: commentDTOs}, nil
}

func (c *Comment) convertToCommentDTO(comment *model.Comment) *apistructs.Comment {
	return &apistructs.Comment{
		CommentID:   int64(comment.ID),
		TicketID:    comment.TicketID,
		CommentType: comment.CommentType,
		Content:     comment.Content,
		IRComment:   comment.IRComment,
		UserID:      comment.UserID,
		CreatedAt:   comment.CreatedAt,
		UpdatedAt:   comment.UpdatedAt,
	}
}
