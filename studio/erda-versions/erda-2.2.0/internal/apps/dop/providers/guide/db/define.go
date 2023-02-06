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
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/dop/guide/pb"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

type GuideDB struct {
	*dao.DBClient
}

type GuideStatus string

const (
	InitStatus      GuideStatus = "init"
	ProcessedStatus GuideStatus = "processed"
	ExpiredStatus   GuideStatus = "expired"
	CanceledStatus  GuideStatus = "canceled"
)

const ExpirationPeriod = 24 * time.Hour

func (g GuideStatus) String() string {
	return string(g)
}

type GuideKind string

const (
	PipelineGuide GuideKind = "pipeline"
)

func (g GuideKind) String() string {
	return string(g)
}

type Guide struct {
	ID            string `gorm:"primary_key"`
	Status        string
	Kind          string
	Creator       string
	OrgID         uint64
	OrgName       string
	ProjectID     uint64
	AppID         uint64
	AppName       string
	Branch        string
	CreatedAt     time.Time
	UpdatedAt     time.Time
	Content       JSON
	SoftDeletedAt uint64
}

func NotDeleted(db *gorm.DB) *gorm.DB {
	return db.Where("soft_deleted_at = 0")
}

func (Guide) TableName() string {
	return "erda_guide"
}

type JSON json.RawMessage

func (j JSON) Value() (driver.Value, error) {
	if len(j) == 0 {
		return nil, nil
	}
	return string(j), nil
}

func (j *JSON) Scan(value interface{}) error {
	if value == nil {
		*j = JSON("null")
		return nil
	}
	var bytes []byte
	switch v := value.(type) {
	case []byte:
		bytes = v
	case string:
		bytes = []byte(v)
	default:
		return errors.New(fmt.Sprint("failed to unmarshal JSON value:", value))
	}

	result := json.RawMessage{}
	err := json.Unmarshal(bytes, &result)
	*j = JSON(result)
	return err
}

func (j JSON) String() string {
	return string(j)
}

type PipelineContent struct {
	Branch       string   `json:"branch"`
	PipelineYmls []string `json:"pipelineYmls"`
}

func (g *Guide) Convert() *pb.Guide {
	return &pb.Guide{
		ID:          g.ID,
		Status:      g.Status,
		Creator:     g.Creator,
		Kind:        g.Kind,
		OrgID:       g.OrgID,
		OrgName:     g.OrgName,
		ProjectID:   g.ProjectID,
		AppID:       g.AppID,
		AppName:     g.AppName,
		Branch:      g.Branch,
		TimeCreated: timestamppb.New(g.CreatedAt),
		TimeUpdated: timestamppb.New(g.UpdatedAt),
		Content:     g.Content.String(),
	}
}

// CreateGuide .
func (db *GuideDB) CreateGuide(guide *Guide) error {
	guide.ID = uuid.New().String()
	return db.Create(guide).Error
}

// GetGuide .
func (db *GuideDB) GetGuide(id string) (guide Guide, err error) {
	err = db.Model(&Guide{}).Scopes(NotDeleted).
		Where("id = ?", id).
		Where("status = ?", InitStatus).
		Where("created_at >= ?", time.Now().Add(-1*(ExpirationPeriod))).
		First(&guide).Error
	return
}

// UpdateGuide .
func (db *GuideDB) UpdateGuide(id string, fields map[string]interface{}) error {
	guide := &Guide{ID: id}
	return db.Model(guide).Scopes(NotDeleted).Updates(fields).Error
}

// BatchUpdateGuideExpiryStatus .
func (db *GuideDB) BatchUpdateGuideExpiryStatus() error {
	return db.Model(&Guide{}).Scopes(NotDeleted).
		Where("status = ?", InitStatus).
		Where("created_at < ? ", time.Now().Add(-1*(ExpirationPeriod))).
		Updates(map[string]interface{}{"status": ExpiredStatus}).Error
}

// UpdateGuideByAppIDAndBranch .
func (db *GuideDB) UpdateGuideByAppIDAndBranch(appID uint64, branch, kind string, fields map[string]interface{}) error {
	return db.Model(&Guide{}).Scopes(NotDeleted).
		Where("app_id = ?", appID).
		Where("branch = ?", branch).
		Where("kind = ?", kind).
		Where("status = ?", InitStatus).
		Updates(fields).Error
}

// ListGuide .
func (db *GuideDB) ListGuide(req *pb.ListGuideRequest, userID string) (guides []Guide, err error) {
	err = db.Model(&Guide{}).
		Scopes(NotDeleted).
		Where("kind = ?", req.Kind).
		Where("project_id = ?", req.ProjectID).
		Where("creator = ?", userID).
		Where("status = ?", InitStatus).
		Where("created_at >= ?", time.Now().Add(-1*(ExpirationPeriod))).
		Order("created_at DESC").
		Find(&guides).Error
	return
}

// GetGuideByAppIDAndBranch .
func (db *GuideDB) GetGuideByAppIDAndBranch(appID uint64, branch, kind string) (guide Guide, err error) {
	err = db.Model(&Guide{}).Scopes(NotDeleted).
		Where("app_id = ?", appID).
		Where("branch = ?", branch).
		Where("kind = ?", kind).
		Where("status = ?", InitStatus).
		First(&guide).Error
	return
}

func (db *GuideDB) CheckUniqueByAppIDAndBranch(appID uint64, branch, kind string) (bool, error) {
	_, err := db.GetGuideByAppIDAndBranch(appID, branch, kind)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			return false, nil
		}
		return false, err
	}
	return true, nil
}

// CancelGuide .
func (db *GuideDB) CancelGuide(id string) error {
	return db.UpdateGuide(id, map[string]interface{}{"status": CanceledStatus})
}
