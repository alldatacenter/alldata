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

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type TestReportRecord struct {
	dbengine.BaseModel

	Name         string         `json:"name"`          // report name
	ProjectID    uint64         `json:"project_id"`    // project id
	IterationID  uint64         `json:"iteration_id"`  // belong iteration id
	CreatorID    string         `json:"creator_id"`    // creator id
	QualityScore float64        `json:"quality_score"` // total quality score
	ReportData   TestReportData `json:"report_data"`   // issue and test dashboard component protocol
	Summary      string         `json:"summary"`       // test report summary
}

type TestReportData apistructs.TestReportData

func (dat TestReportData) Value() (driver.Value, error) {
	if b, err := json.Marshal(dat); err != nil {
		return nil, errors.Wrapf(err, "failed to marshal report data")
	} else {
		return string(b), nil
	}
}

func (dat *TestReportData) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for report data")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, dat); err != nil {
		return errors.Wrapf(err, "failed to unmarshal report data")
	}
	return nil
}

func (TestReportRecord) TableName() string {
	return "erda_test_report_records"
}

func (t *TestReportRecord) Convert() apistructs.TestReportRecord {
	return apistructs.TestReportRecord{
		ID:           t.ID,
		ProjectID:    t.ProjectID,
		CreatorID:    t.CreatorID,
		IterationID:  t.IterationID,
		CreatedAt:    t.CreatedAt,
		UpdatedAt:    t.UpdatedAt,
		Name:         t.Name,
		Summary:      t.Summary,
		QualityScore: t.QualityScore,
		ReportData: apistructs.TestReportData{
			IssueDashboard: t.ReportData.IssueDashboard,
			TestDashboard:  t.ReportData.TestDashboard,
		},
	}
}

func (client *DBClient) CreateTestReportRecord(record *TestReportRecord) error {
	return client.Create(record).Error
}

func (client *DBClient) UpdateTestReportRecord(record *TestReportRecord) error {
	return client.Save(record).Error
}

func (client *DBClient) ListTestReportRecord(req apistructs.TestReportRecordListRequest) ([]TestReportRecord, uint64, error) {
	var (
		total   uint64
		records []TestReportRecord
	)
	sql := client.Model(&TestReportRecord{}).Where("project_id = ?", req.ProjectID)
	if req.ID > 0 {
		sql = sql.Where("`id` = ?", req.ID)
	}
	if len(req.IterationIDS) > 0 {
		sql = sql.Where("`iteration_id` IN (?)", req.IterationIDS)
	}
	if req.Name != "" {
		sql = sql.Where("`name` like ?", "%"+req.Name+"%")
	}
	if req.OrderBy != "" {
		if req.Asc {
			sql = sql.Order(fmt.Sprintf("%s", req.OrderBy))
		} else {
			sql = sql.Order(fmt.Sprintf("%s DESC", req.OrderBy))
		}
	}
	// if field GetReportData is false, don't need select report_data column
	if req.GetReportData {
		sql = sql.Select("*")
	} else {
		sql = sql.Select("id,created_at,updated_at,project_id,name,iteration_id,creator_id,quality_score")
	}

	offset := (req.PageNo - 1) * req.PageSize
	if err := sql.Offset(offset).Limit(req.PageSize).Find(&records).Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return records, total, err
	}
	return records, total, nil
}

func (client *DBClient) GetTestReportRecordByID(id uint64) (TestReportRecord, error) {
	var record TestReportRecord
	if err := client.Where("id = ?", id).First(&record).Error; err != nil {
		return record, err
	}
	return record, nil
}
