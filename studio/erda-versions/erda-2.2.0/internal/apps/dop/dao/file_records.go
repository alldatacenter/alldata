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
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type TestFileRecord struct {
	dbengine.BaseModel
	FileName    string
	Description string
	ApiFileUUID string
	ProjectID   uint64
	OrgID       uint64
	SpaceID     uint64
	Type        apistructs.FileActionType
	State       apistructs.FileRecordState
	OperatorID  string
	Extra       TestFileExtra
	ErrorInfo   string

	SoftDeletedAt uint
}

type TestFileExtra apistructs.TestFileExtra

func (ex TestFileExtra) Value() (driver.Value, error) {
	if b, err := json.Marshal(ex); err != nil {
		return nil, errors.Wrapf(err, "failed to marshal Extra")
	} else {
		return string(b), nil
	}
}

func (ex *TestFileExtra) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for Extra")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, ex); err != nil {
		return errors.Wrapf(err, "failed to unmarshal Extra")
	}
	return nil
}

// Test TableName
func (TestFileRecord) TableName() string {
	return "erda_file_record"
}

// Create Record
func (client *DBClient) CreateRecord(record *TestFileRecord) error {
	return client.Create(record).Error
}

// Get Record by id
func (client *DBClient) GetRecord(id uint64) (*TestFileRecord, error) {
	var res TestFileRecord
	if err := client.Scopes(NotDeleted).First(&res, id).Error; err != nil {
		return nil, err
	}
	return &res, nil
}

type stateCounter struct {
	Type  string
	Count int
}

// Get Records by projectId, spaceId, types
func (client *DBClient) ListRecordsByProject(req apistructs.ListTestFileRecordsRequest) ([]TestFileRecord, map[string]int, int, error) {
	var res []TestFileRecord
	sql := client.Scopes(NotDeleted).Table("erda_file_record")
	if req.SpaceID > 0 {
		sql = sql.Where("`space_id` = ?", req.SpaceID)
	}
	if req.ProjectID > 0 {
		sql = sql.Where("`project_id` = ?", req.ProjectID)
	}
	if len(req.ProjectIDs) > 0 {
		sql = sql.Where("`project_id` in (?)", req.ProjectIDs)
	}
	if req.OrgID > 0 {
		sql = sql.Where("`org_id` = ?", req.OrgID)
	}
	if len(req.Types) > 0 {
		sql = sql.Where("`type` IN (?)", req.Types)
	}
	if req.Asc {
		sql = sql.Order("created_at")
	} else {
		sql = sql.Order("created_at desc")
	}
	if err := sql.Offset((req.PageNo - 1) * req.PageSize).Limit(req.PageSize).Find(&res).Error; err != nil {
		return nil, nil, 0, err
	}

	var total int
	if err := sql.Count(&total).Error; err != nil {
		return nil, nil, 0, err
	}

	var counterList []stateCounter
	if err := client.Scopes(NotDeleted).Table("erda_file_record").Select("type, count(*) as count").
		Where("`type` IN (?) AND `state` = ?", req.Types, apistructs.FileRecordStatePending).
		Group("type").Find(&counterList).Error; err != nil {
		return nil, nil, 0, err
	}

	counter := make(map[string]int)
	for _, s := range counterList {
		counter[s.Type] = s.Count
	}

	return res, counter, total, nil
}

// Update Record
func (client *DBClient) UpdateRecord(record *TestFileRecord) error {
	return client.Scopes(NotDeleted).Save(record).Error
}

func (client *DBClient) FirstFileReady(actionType ...apistructs.FileActionType) (bool, *TestFileRecord, error) {
	if len(actionType) == 0 {
		return false, nil, fmt.Errorf("failed to get first file ready, err: empty action type")
	}
	var process int64

	if err := client.Scopes(NotDeleted).Model(&TestFileRecord{}).Where("`state` = ? AND `type` in (?)", apistructs.FileRecordStateProcessing, actionType).Count(&process).Error; err != nil {
		return false, nil, err
	}
	if process > 0 {
		return false, nil, nil
	}

	var record TestFileRecord
	if err := client.Scopes(NotDeleted).Model(&TestFileRecord{}).Where("`state` = ? AND `type` in (?)", apistructs.FileRecordStatePending, actionType).Order("created_at").First(&record).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			return false, nil, nil
		}
		return false, nil, err
	}

	return true, &record, nil
}

func (client *DBClient) BatchUpdateRecords() error {
	return client.Scopes(NotDeleted).Model(&TestFileRecord{}).Where("`state` = ?", apistructs.FileRecordStateProcessing).Updates(TestFileRecord{State: apistructs.FileRecordStateFail}).Error
}

type FileUUIDStr struct {
	ApiFileUUID string
}

func (client *DBClient) DeleteFileRecordByTime(t time.Time) ([]FileUUIDStr, error) {
	var res []FileUUIDStr
	if err := client.Scopes(NotDeleted).Table("erda_file_record").Where("`created_at` < ?", t).Select("api_file_uuid").Find(&res).Error; err != nil {
		return nil, err
	}

	if err := client.Scopes(NotDeleted).Where("`created_at` < ?", t).Update("soft_deleted_at", time.Now().UnixNano()/1e6).Error; err != nil {
		return nil, err
	}

	return res, nil
}
