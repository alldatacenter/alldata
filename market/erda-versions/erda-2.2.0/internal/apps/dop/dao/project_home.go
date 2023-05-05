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
	"github.com/jinzhu/gorm"
)

type ProjectHome struct {
	ID            string `gorm:"primary_key"`
	CreatedAt     time.Time
	UpdatedAt     time.Time
	ProjectID     string
	Readme        string
	Links         string
	UpdaterID     string
	SoftDeletedAt uint64
}

func (ProjectHome) TableName() string {
	return "erda_project_home"
}

func NotDeleted(db *gorm.DB) *gorm.DB {
	return db.Where("soft_deleted_at = ?", 0)
}

func (db *DBClient) CreateOrUpdateProjectHome(projectID string, readme string, links string, updater string) error {
	var count uint64
	if err := db.Model(&ProjectHome{}).Scopes(NotDeleted).Where("project_id = ?", projectID).Count(&count).Error; err != nil {
		return err
	}
	if count == 0 {
		id, err := uuid.NewRandom()
		if err != nil {
			return err
		}
		return db.Create(&ProjectHome{
			ID:        id.String(),
			ProjectID: projectID,
			Readme:    readme,
			Links:     links,
			UpdaterID: updater,
		}).Error
	}
	return db.Model(&ProjectHome{}).Scopes(NotDeleted).Where("project_id = ?", projectID).
		Updates(map[string]interface{}{"readme": readme, "links": links, "updater_id": updater}).Error
}

func (db *DBClient) GetProjectHome(projectID string) (*ProjectHome, error) {
	var res ProjectHome
	if err := db.Scopes(NotDeleted).Where("project_id = ?", projectID).First(&res).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return nil, nil
		}
		return nil, err
	}
	return &res, nil
}
