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

package model

import (
	"time"

	"github.com/erda-project/erda/apistructs"
)

type MBox struct {
	BaseModel
	Title   string
	Content string
	Label   string //站内信所属模块  monitor|pipeline
	UserID  string
	Status  apistructs.MBoxStatus //read|unread
	OrgID   int64
	ReadAt  *time.Time
	// The UnreadCount not empty only when DeduplicateID isn't empty
	DeduplicateID string `gorm:"column:deduplicate_id"`
	UnreadCount   int64  `gorm:"column:unread_count"`
}

func (MBox) TableName() string {
	return "dice_mboxs"
}

func (mbox MBox) ToApiData() *apistructs.MBox {
	return &apistructs.MBox{
		ID:            mbox.ID,
		Title:         mbox.Title,
		Content:       mbox.Content,
		Label:         mbox.Label,
		Status:        mbox.Status,
		CreatedAt:     mbox.CreatedAt,
		ReadAt:        mbox.ReadAt,
		DeduplicateID: mbox.DeduplicateID,
		UnreadCount:   mbox.UnreadCount,
	}
}
