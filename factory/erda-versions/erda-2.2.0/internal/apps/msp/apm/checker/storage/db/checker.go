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
	"encoding/json"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/jinzhu/gorm"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
)

// CheckerDB .
type CheckerDB struct {
	*gorm.DB
	ScopeInfoUpdateInterval time.Duration

	cache           map[scopeCacheKey]*scopeInfo
	lastCacheUpdate time.Time
	lock            sync.Mutex
}

type scopeCacheKey struct {
	ProjectID int64
	Env       string
}

// scopeInfo .
type scopeInfo struct {
	ProjectName string `gorm:"column:project_name"`
	ScopeID     string `gorm:"column:scope_id"`
}

// JoinFields .
type JoinFields struct {
	ID          int64  `gorm:"column:id"`
	Name        string `gorm:"column:name"`
	Mode        string `gorm:"column:mode"`
	URL         string `gorm:"column:url"`
	ProjectID   int64  `gorm:"column:project_id"`
	TenantId    string `gorm:"column:tenant_id"`
	ProjectName string `gorm:"column:project_name"`
	Env         string `gorm:"column:env"`
	Config      string `gorm:"column:config"`
	ScopeID     string `gorm:"column:scope_id"`
	IsDeleted   string `gorm:"column:is_deleted"`
}

// Deleted .
func (f *JoinFields) Deleted() bool {
	return f.IsDeleted != "N"
}

func (db *CheckerDB) FullList() (checkers []*pb.Checker, deleted []int64, err error) {
	var list []*JoinFields
	if err := db.DB.Table(TableMetric).Find(&list).Error; err != nil {
		return nil, nil, err
	}
	for _, item := range list {
		item.IsDeleted = strings.ToUpper(item.IsDeleted)
		if item.Deleted() {
			deleted = append(deleted, item.ID)
		}
	}

	db.lock.Lock()
	defer db.lock.Unlock()
	err = db.updateCacheIfNeed(list)
	if err != nil {
		return nil, nil, err
	}
	for _, item := range list {
		if item.Deleted() {
			continue
		}
		scope, _ := db.cache[scopeCacheKey{ProjectID: item.ProjectID, Env: item.Env}]
		if scope == nil {
			continue
		}
		item.ProjectName = scope.ProjectName
		item.ScopeID = scope.ScopeID
		checkers = append(checkers, convertToChecker(item))
	}
	return checkers, deleted, nil
}

func (db *CheckerDB) updateCacheIfNeed(list []*JoinFields) error {
	now := time.Now()
	if db.cache == nil || time.Now().After(db.lastCacheUpdate.Add(db.ScopeInfoUpdateInterval)) {
		cache := make(map[scopeCacheKey]*scopeInfo)
		for _, item := range list {
			if item.Deleted() {
				continue
			}
			info, err := db.queryScopeInfo(item.TenantId)
			if err != nil {
				return err
			}
			cache[scopeCacheKey{ProjectID: item.ProjectID, Env: item.Env}] = info
		}
		db.cache, db.lastCacheUpdate = cache, now
	} else {
		for _, item := range list {
			key := scopeCacheKey{ProjectID: item.ProjectID, Env: item.Env}
			if _, ok := db.cache[key]; !ok && !item.Deleted() {
				info, err := db.queryScopeInfo(item.TenantId)
				if err != nil {
					return err
				}
				db.cache[key] = info
			}
		}
	}
	return nil
}

func (db *CheckerDB) queryScopeInfo(terminusKey string) (*scopeInfo, error) {
	var info scopeInfo
	if err := db.DB.Table("sp_monitor").
		Select("`terminus_key` AS `scope_id`, `project_name`").
		Where("`terminus_key`=?", terminusKey).
		Where("`is_delete`=?", "N").
		Last(&info).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &info, nil
}

func convertToChecker(fields *JoinFields) *pb.Checker {
	config := make(map[string]*structpb.Value)
	if fields.Config == "" {
		// history record
		config["url"] = structpb.NewStringValue(fields.URL)
		config["method"] = structpb.NewStringValue("GET")
		config["interval"] = structpb.NewNumberValue(15)
		config["retry"] = structpb.NewNumberValue(0)
	} else {
		err := json.Unmarshal([]byte(fields.Config), &config)
		if err != nil {
			return nil
		}
	}

	return &pb.Checker{
		Id:     fields.ID,
		Name:   fields.Name,
		Type:   fields.Mode,
		Config: config,
		Tags: map[string]string{
			"_metric_scope":    "micro_service",
			"_metric_scope_id": fields.ScopeID,
			"terminus_key":     fields.ScopeID,
			"project_id":       strconv.FormatInt(fields.ProjectID, 10),
			"project_name":     fields.ProjectName,
			"env":              fields.Env,
			"metric":           strconv.FormatInt(fields.ID, 10),
			"metric_name":      fields.Name,
		},
	}
}
