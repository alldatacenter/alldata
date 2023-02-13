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

package mbox

import (
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/ucauth"
)

type MBox struct {
	db  *dao.DBClient
	uc  *ucauth.UCClient
	bdl *bundle.Bundle
}

type Option func(*MBox)

func New(options ...Option) *MBox {
	o := &MBox{}
	for _, op := range options {
		op(o)
	}
	return o
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(o *MBox) {
		o.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(o *MBox) {
		o.bdl = bdl
	}
}

func (o *MBox) CreateMBox(createReq *apistructs.CreateMBoxRequest) error {
	for _, userID := range createReq.UserIDs {
		if userID == "" {
			continue
		}
		if createReq.DeduplicateID != "" {
			mbox, err := o.db.GetMboxByDeduplicateID(createReq.OrgID, createReq.DeduplicateID, userID)
			if err != nil {
				return err
			}
			if mbox != nil {
				// Accumulate the number of unread
				setDuplicateMbox(mbox, createReq)
				if err := o.db.UpdateMbox(mbox); err != nil {
					return err
				}
				continue
			}
		}
		// Create a new mbox
		mbox := &model.MBox{
			Title:   createReq.Title,
			Content: createReq.Content,
			Label:   createReq.Label,
			OrgID:   createReq.OrgID,
			UserID:  userID,
			Status:  apistructs.MBoxUnReadStatus,
		}
		if createReq.DeduplicateID != "" {
			mbox.UnreadCount = 1
			mbox.DeduplicateID = createReq.DeduplicateID
		}
		if err := o.db.CreateMBox(mbox); err != nil {
			return err
		}
	}

	return nil
}

func setDuplicateMbox(mbox *model.MBox, createReq *apistructs.CreateMBoxRequest) {
	switch mbox.Status {
	case apistructs.MBoxUnReadStatus:
		mbox.UnreadCount++
	case apistructs.MBoxReadStatus:
		mbox.UnreadCount = 1
		mbox.Status = apistructs.MBoxUnReadStatus
	}
	mbox.Content = createReq.Content
	mbox.Title = createReq.Title
	mbox.CreatedAt = time.Now()
}

func (o *MBox) QueryMBox(queryReq *apistructs.QueryMBoxRequest) (*apistructs.QueryMBoxData, error) {
	return o.db.QueryMBox(queryReq)
}

func (o *MBox) GetMBox(id int64, orgID int64, userID string) (*apistructs.MBox, error) {
	mbox, err := o.db.GetMBox(id, orgID, userID)
	if err != nil {
		return nil, err
	}
	if mbox.Status == apistructs.MBoxUnReadStatus {
		err := o.db.SetMBoxReadStatus(&apistructs.SetMBoxReadStatusRequest{
			OrgID:  orgID,
			IDs:    []int64{id},
			UserID: userID,
		})
		if err != nil {
			return nil, err
		}
	}
	return mbox, nil
}

func (o *MBox) SetMBoxReadStatus(request *apistructs.SetMBoxReadStatusRequest) error {
	return o.db.SetMBoxReadStatus(request)
}

func (o *MBox) GetMBoxStats(orgID int64, userID string) (*apistructs.QueryMBoxStatsData, error) {
	return o.db.GetMBoxStats(orgID, userID)
}

func (o *MBox) OneClickRead(orgID int64, userID string) error {
	return o.db.ReadAll(orgID, userID)
}
