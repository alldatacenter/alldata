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
	"time"

	"github.com/google/uuid"
)

// IssueFilterBookmark users' bookmark of issue filter
type IssueFilterBookmark struct {
	ID           string `gorm:"primary_key"`
	CreatedAt    time.Time
	UpdatedAt    time.Time
	Name         string
	UserID       string
	ProjectID    string
	PageKey      string // 4 page and each page has 2 variants, thus 8 page for bookmarks
	FilterEntity string
}

func (IssueFilterBookmark) TableName() string {
	return "erda_issue_filter_bookmark"
}

func (db *DBClient) ListIssueFilterBookmarkByUserIDAndProjectID(userID, projectID string) ([]IssueFilterBookmark, error) {
	if userID == "" {
		return nil, nil
	}
	var bms []IssueFilterBookmark
	if err := db.Where("user_id = ?", userID).Where("project_id = ?", projectID).Find(&bms).Error; err != nil {
		return nil, err
	}
	return bms, nil
}

func (db *DBClient) CreateIssueFilterBookmark(bm *IssueFilterBookmark) error {
	id, err := uuid.NewRandom()
	if err != nil {
		return err
	}
	bm.ID = id.String()
	return db.Create(bm).Error
}

func (db *DBClient) DeleteIssueFilterBookmarkByID(id string) error {
	if id == "" {
		return nil
	}
	return db.Delete(&IssueFilterBookmark{ID: id}).Error
}
