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

package db

import (
	"time"

	"github.com/pkg/errors"
	"gorm.io/gorm"

	"github.com/erda-project/erda-infra/providers/mysql/v2/plugins/fields"
)

type OpusDB struct {
	DB *gorm.DB
}

func (o OpusDB) CreateOpus(opus *ReleaseOpus) error {
	return o.DB.Create(opus).Error
}

func (o OpusDB) DeleteOpusByReleaseID(orgID uint32, releaseID string) error {
	return o.DB.Where("org_id = ?", orgID).
		Where("release_id = ?", releaseID).
		Delete(new(ReleaseOpus)).Error
}

func (o OpusDB) QueryReleaseOpus(orgID uint32, releaseIDs []string, pageNo, pageSize int) (int64, []*ReleaseOpus, error) {
	q := o.DB.Where("org_id = ?", orgID).Order("updated_at DESC")
	if len(releaseIDs) > 0 {
		q = q.Where("release_id IN (?)", releaseIDs)
	}
	if pageSize > 0 {
		q = q.Limit(pageSize).Offset((pageNo - 1) * pageSize)
	}

	var (
		opuses []*ReleaseOpus
		total  int64
	)
	err := q.Find(&opuses).Count(&total).Error
	if err == nil {
		return total, opuses, nil
	}
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return 0, nil, nil
	}
	return 0, nil, err
}

type Model struct {
	ID        fields.UUID      `gorm:"id"`
	CreatedAt time.Time        `gorm:"created_at"`
	UpdatedAt time.Time        `gorm:"updated_at"`
	DeletedAt fields.DeletedAt `gorm:"deleted_at"`
}

type Common struct {
	OrgID     uint32 `gorm:"org_id"`
	OrgName   string `gorm:"org_name"`
	CreatorID string `gorm:"creator_id"`
	UpdaterID string `gorm:"updater_id"`
}

type ReleaseOpus struct {
	Model
	Common

	ReleaseID     string `gorm:"release_id"`
	OpusID        string `gorm:"opus_id"`
	OpusVersionID string `gorm:"opus_version_id"`
}

func (ReleaseOpus) TableName() string {
	return "erda_release_opus"
}
