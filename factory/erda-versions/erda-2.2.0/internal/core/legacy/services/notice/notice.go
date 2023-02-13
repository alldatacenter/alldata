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

package notice

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
)

// Notice 公告
type Notice struct {
	db *dao.DBClient
}

// Option .
type Option func(*Notice)

// New 新建 notice service
func New(options ...Option) *Notice {
	n := &Notice{}
	for _, op := range options {
		op(n)
	}
	return n
}

// WithDBClient 设置 dbclient
func WithDBClient(db *dao.DBClient) Option {
	return func(n *Notice) {
		n.db = db
	}
}

// Create 创建公告
func (n *Notice) Create(orgID uint64, createReq *apistructs.NoticeCreateRequest) (uint64, error) {
	if createReq.Content == "" {
		return 0, apierrors.ErrCreateNotice.MissingParameter("content")
	}
	notice := &dao.Notice{
		OrgID:   orgID,
		Content: createReq.Content,
		Status:  apistructs.NoticeUnpublished,
		Creator: createReq.UserID,
	}
	if err := n.db.CreateNotice(notice); err != nil {
		return 0, err
	}

	return uint64(notice.ID), nil
}

// Update 编辑公告
func (n *Notice) Update(updateReq *apistructs.NoticeUpdateRequest) error {
	if updateReq.Content == "" {
		return apierrors.ErrUpdateNotice.MissingParameter("content")
	}

	notice, err := n.db.GetNotice(updateReq.ID)
	if err != nil {
		return err
	}
	if notice == nil {
		return apierrors.ErrUpdateNotice.NotFound()
	}

	notice.Content = updateReq.Content
	if err := n.db.UpdateNotice(notice); err != nil {
		return err
	}
	return nil
}

// Publish 发布公告
func (n *Notice) Publish(noticeID uint64) error {
	notice, err := n.db.GetNotice(noticeID)
	if err != nil {
		return err
	}
	if notice == nil {
		return apierrors.ErrPublishNotice.NotFound()
	}

	notice.Status = apistructs.NoticePublished
	return n.db.UpdateNotice(notice)
}

// Unpublish 停用公告
func (n *Notice) Unpublish(noticeID uint64) error {
	notice, err := n.db.GetNotice(noticeID)
	if err != nil {
		return err
	}
	if notice == nil {
		return apierrors.ErrUnpublishNotice.NotFound()
	}

	notice.Status = apistructs.NoticeDeprecated
	return n.db.UpdateNotice(notice)
}

// Delete 删除公告
func (n *Notice) Delete(noticeID uint64) error {
	// 状态限制, 仅下架公告可删除
	notice, err := n.db.GetNotice(noticeID)
	if err != nil {
		return err
	}
	if notice == nil {
		return apierrors.ErrDeleteNotice.NotFound()
	}
	if notice.Status == apistructs.NoticePublished {
		return apierrors.ErrDeleteNotice.InvalidState("published")
	}
	return n.db.DeleteNotice(noticeID)
}

// Get 获取公告
func (n *Notice) Get(noticeID uint64) (*apistructs.Notice, error) {

	notice, err := n.db.GetNotice(noticeID)
	if err != nil {
		return nil, err
	}

	return n.Convert(notice), nil
}

// List 公告列表
func (n *Notice) List(listReq *apistructs.NoticeListRequest) (*apistructs.NoticeListResponseData, error) {
	if listReq.PageNo == 0 {
		listReq.PageNo = 1
	}
	if listReq.PageSize == 0 {
		listReq.PageSize = 20
	}
	total, notices, err := n.db.ListNotice(listReq)
	if err != nil {
		return nil, err
	}
	list := make([]apistructs.Notice, 0, len(notices))
	for _, v := range notices {
		list = append(list, *n.Convert(&v))
	}
	listResp := apistructs.NoticeListResponseData{
		Total: total,
		List:  list,
	}

	return &listResp, nil
}

// Convert 结构转换
func (n *Notice) Convert(notice *dao.Notice) *apistructs.Notice {
	return &apistructs.Notice{
		ID:        uint64(notice.ID),
		OrgID:     notice.OrgID,
		Content:   notice.Content,
		Status:    notice.Status,
		Creator:   notice.Creator,
		CreatedAt: &notice.CreatedAt,
		UpdateAt:  &notice.UpdatedAt,
	}
}
