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
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type CodeCoverageExecRecord struct {
	dbengine.BaseModel

	ProjectID     uint64                            `json:"project_id"`
	Status        apistructs.CodeCoverageExecStatus `json:"status"`
	ReportStatus  apistructs.CodeCoverageExecStatus `json:"report_status"`
	Msg           string                            `json:"msg"`
	ReportMsg     string                            `json:"report_msg"`
	Coverage      float64                           `json:"coverage"`
	ReportUrl     string                            `json:"report_url"`
	ReportTime    time.Time                         `json:"report_time"`
	ReportContent CodeCoverageNodes                 `json:"report_content" sql:"TYPE:json"`
	StartExecutor string                            `json:"start_executor"`
	EndExecutor   string                            `json:"end_executor"`
	TimeBegin     time.Time                         `json:"time_begin"`
	TimeEnd       time.Time                         `json:"time_end"`
	Workspace     string                            `json:"workspace"`
}

type CodeCoverageNodes []*apistructs.CodeCoverageNode

func (c CodeCoverageNodes) Value() (driver.Value, error) {
	b, err := json.Marshal(c)
	return string(b), err
}

func (c *CodeCoverageNodes) Scan(input interface{}) error {
	bytes := input.([]byte)
	if len(bytes) == 0 {
		return nil
	}
	return json.Unmarshal(bytes, c)
}

func (CodeCoverageExecRecord) TableName() string {
	return "dice_code_coverage_exec_record"
}

func (c *CodeCoverageExecRecord) Covert() *apistructs.CodeCoverageExecRecordDto {
	return &apistructs.CodeCoverageExecRecordDto{
		ID:            c.ID,
		ProjectID:     c.ProjectID,
		Status:        c.Status.String(),
		ReportStatus:  c.ReportStatus.String(),
		Msg:           c.Msg,
		Coverage:      c.Coverage,
		ReportUrl:     c.ReportUrl,
		ReportContent: c.ReportContent,
		StartExecutor: c.StartExecutor,
		EndExecutor:   c.EndExecutor,
		TimeBegin:     c.TimeBegin,
		TimeEnd:       c.TimeEnd,
		TimeCreated:   c.CreatedAt,
		TimeUpdated:   c.UpdatedAt,
		ReportTime:    c.ReportTime,
	}
}

type CodeCoverageExecRecordShort struct {
	dbengine.BaseModel

	ProjectID     uint64                            `json:"project_id"`
	Status        apistructs.CodeCoverageExecStatus `json:"status"`
	ReportStatus  apistructs.CodeCoverageExecStatus `json:"report_status"`
	Msg           string                            `json:"msg"`
	ReportMsg     string                            `json:"report_msg"`
	Coverage      float64                           `json:"coverage"`
	ReportUrl     string                            `json:"report_url"`
	StartExecutor string                            `json:"start_executor"`
	EndExecutor   string                            `json:"end_executor"`
	TimeBegin     time.Time                         `json:"time_begin"`
	TimeEnd       time.Time                         `json:"time_end"`
	ReportTime    time.Time                         `json:"report_time"`
}

func (CodeCoverageExecRecordShort) TableName() string {
	return "dice_code_coverage_exec_record"
}

func (c *CodeCoverageExecRecordShort) Covert() apistructs.CodeCoverageExecRecordDto {
	return apistructs.CodeCoverageExecRecordDto{
		ID:            c.ID,
		ProjectID:     c.ProjectID,
		Status:        c.Status.String(),
		ReportStatus:  c.ReportStatus.String(),
		Msg:           c.Msg,
		ReportMsg:     c.ReportMsg,
		Coverage:      c.Coverage,
		ReportUrl:     c.ReportUrl,
		StartExecutor: c.StartExecutor,
		EndExecutor:   c.EndExecutor,
		TimeBegin:     c.TimeBegin,
		TimeEnd:       c.TimeEnd,
		TimeCreated:   c.CreatedAt,
		TimeUpdated:   c.UpdatedAt,
		ReportTime:    c.ReportTime,
	}
}

// CreateCodeCoverage .
func (client *DBClient) CreateCodeCoverage(record *CodeCoverageExecRecord) error {
	return client.Create(record).Error
}

// UpdateCodeCoverage .
func (client *DBClient) UpdateCodeCoverage(record *CodeCoverageExecRecord) error {
	return client.Save(record).Error
}

// GetCodeCoverageByID .
func (client *DBClient) GetCodeCoverageByID(id uint64) (*CodeCoverageExecRecord, error) {
	var record CodeCoverageExecRecord
	err := client.Model(&CodeCoverageExecRecord{}).First(&record, id).Error
	return &record, err
}

// CancelCodeCoverage .
func (client *DBClient) CancelCodeCoverage(projectID uint64, workspace string, record *CodeCoverageExecRecord) error {
	return client.Model(&CodeCoverageExecRecord{}).
		Where("project_id = ? and workspace = ?", projectID, workspace).
		Where("status IN (?)", apistructs.WorkingStatus).Updates(record).Error
}

// ListCodeCoverageByStatus .
func (client *DBClient) ListCodeCoverageByStatus(projectID uint64, status []apistructs.CodeCoverageExecStatus, workspace string) (records []CodeCoverageExecRecord, err error) {
	err = client.Where("project_id = ?", projectID).Where("status IN (?) and workspace = ?", status, workspace).Find(&records).Error
	return
}

// ListCodeCoverage .
func (client *DBClient) ListCodeCoverage(req apistructs.CodeCoverageListRequest) (records []CodeCoverageExecRecordShort, total uint64, err error) {
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 10
	}

	offset := (req.PageNo - 1) * req.PageSize
	db := client.Model(&CodeCoverageExecRecordShort{}).
		Where("project_id = ?", req.ProjectID)

	if len(req.Statuses) > 0 {
		db = db.Where("status in (?)", req.Statuses)
	}
	if len(req.ReportStatuses) > 0 {
		db = db.Where("report_status in (?)", req.ReportStatuses)
	}
	if req.TimeBegin != "" {
		db = db.Where("time_begin >= ?", req.TimeBegin)
	}
	if req.TimeEnd != "" {
		db = db.Where("time_begin <= ?", req.TimeEnd)
	}

	if req.Workspace != "" {
		db = db.Where("workspace = ?", req.Workspace)
	}

	if req.Asc {
		db = db.Order("id ASC")
	} else {
		db = db.Order("id DESC")
	}

	err = db.Offset(offset).Limit(req.PageSize).
		Find(&records).
		Offset(0).Limit(-1).Count(&total).Error
	return
}
