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

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/common/errors"
)

// TraceRequestHistoryDB  .
type TraceRequestHistoryDB struct {
	*gorm.DB
}

func (db *TraceRequestHistoryDB) db() *gorm.DB {
	return db.Table(TableSpTraceRequestHistory)
}

func (db *TraceRequestHistoryDB) InsertHistory(history TraceRequestHistory) (*TraceRequestHistory, error) {
	err := db.db().Create(&history).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return &history, nil
}

func (db *TraceRequestHistoryDB) QueryHistoriesByScopeID(scopeID string, timestamp time.Time, limit int64) ([]*TraceRequestHistory, error) {
	var list []*TraceRequestHistory
	err := db.db().Select("`request_id`, `url`, `method`, `create_time`, `update_time`, `terminus_key`").
		Where("`terminus_key` = ?", scopeID).
		Order("`create_time` DESC").
		Limit(limit).Find(&list).Error
	if err != nil {
		return nil, err
	}
	return list, nil
}

func (db *TraceRequestHistoryDB) QueryCountByScopeID(scopeID string) (int32, error) {
	count := 0
	err := db.db().Select("count(`request_id`)").Where("`terminus_key` = ?", scopeID).Count(&count).Error
	if err != nil {
		return 0, err
	}
	return int32(count), nil
}

func (db *TraceRequestHistoryDB) QueryHistoryByRequestID(scopeID string, requestID string) (*TraceRequestHistory, error) {
	history := TraceRequestHistory{}
	err := db.db().Select("*").Where("`terminus_key` = ? AND `request_id` = ?", scopeID, requestID).Find(&history).Error
	if err != nil {
		return nil, err
	}
	return &history, nil
}

func (db *TraceRequestHistoryDB) UpdateDebugStatusByRequestID(scopeID string, requestID string, statusCode int) (*TraceRequestHistory, error) {
	history := TraceRequestHistory{}
	err := db.db().Where("`terminus_key` = ? AND `request_id` = ?", scopeID, requestID).
		Update("status", statusCode).
		Update("update_time", time.Now()).
		Find(&history).
		Error
	if err != nil {
		return nil, err
	}
	return &history, nil
}

func (db *TraceRequestHistoryDB) UpdateDebugResponseByRequestID(scopeID string, requestID string, responseCode int, responseBody string) (*TraceRequestHistory, error) {
	history := TraceRequestHistory{}
	err := db.db().Where("`terminus_key` = ? AND `request_id` = ?", scopeID, requestID).
		Update("response_status", responseCode).
		Update("response_body", responseBody).
		Update("update_time", time.Now()).
		Find(&history).
		Error
	if err != nil {
		return nil, err
	}
	return &history, nil
}
