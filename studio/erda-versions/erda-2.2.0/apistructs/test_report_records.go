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

package apistructs

import (
	"encoding/json"
	"strconv"
	"time"
)

type TestReportRecord struct {
	IdentityInfo

	ID           uint64         `json:"id"`
	ProjectID    uint64         `json:"projectID"`
	IterationID  uint64         `json:"iterationID"`
	CreatorID    string         `json:"creatorID"`
	CreatedAt    time.Time      `json:"createdAt"`
	UpdatedAt    time.Time      `json:"updatedAt"`
	Name         string         `json:"name"`
	Summary      string         `json:"summary"`
	QualityScore float64        `json:"qualityScore"`
	ReportData   TestReportData `json:"reportData"`
}

type TestReportData struct {
	IssueDashboard *ComponentProtocolRequest `json:"issue-dashboard,omitempty"`
	TestDashboard  *ComponentProtocolRequest `json:"test-dashboard,omitempty"`
}

type reportQualityScore struct {
	QualityScore float64 `json:"GlobalQualityScore"`
}

// GetQualityScore quality score can be empty
func (t *TestReportData) GetQualityScore() float64 {
	if t.TestDashboard == nil || t.TestDashboard.Protocol == nil || t.TestDashboard.Protocol.GlobalState == nil {
		return 0
	}

	scoreByt, err := json.Marshal(t.TestDashboard.Protocol.GlobalState)
	if err != nil {
		return 0
	}
	var score reportQualityScore
	if err := json.Unmarshal(scoreByt, &score); err != nil {
		return 0
	}
	return score.QualityScore
}

type TestReportRecordListRequest struct {
	IdentityInfo

	ID            uint64   `json:"id"`
	Name          string   `json:"name"`
	ProjectID     uint64   `json:"projectID"`
	IterationIDS  []uint64 `json:"iterationIDS"`
	GetReportData bool     `json:"getReportData"`
	OrderBy       string   `json:"orderBy"`
	Asc           bool     `json:"asc"`
	PageNo        uint64   `json:"pageNo"`
	PageSize      uint64   `json:"pageSize"`
}

func (req *TestReportRecordListRequest) URLQueryString() map[string][]string {
	query := make(map[string][]string)
	if req.Name != "" {
		query["name"] = []string{req.Name}
	}
	if req.ProjectID != 0 {
		query["projectID"] = []string{strconv.FormatInt(int64(req.ProjectID), 10)}
	}
	if len(req.IterationIDS) > 0 {
		query["iterationIDS"] = []string{}
		for _, iteration := range req.IterationIDS {
			query["iterationIDS"] = append(query["iterationIDS"], strconv.FormatInt(int64(iteration), 10))
		}
	}
	query["getReportData"] = []string{strconv.FormatBool(req.GetReportData)}
	if req.OrderBy != "" {
		query["orderBy"] = []string{req.OrderBy}
	}
	query["asc"] = []string{strconv.FormatBool(req.Asc)}
	query["pageNo"] = []string{strconv.FormatInt(int64(req.PageNo), 10)}
	query["pageSize"] = []string{strconv.FormatInt(int64(req.PageSize), 10)}
	return query
}

type TestReportRecordData struct {
	Total uint64             `json:"total"`
	List  []TestReportRecord `json:"list"`
}

type CreateTestReportRecordResponse struct {
	Header
	Id uint64 `json:"id"`
}

type ListTestReportRecordResponse struct {
	Header
	Data *TestReportRecordData `json:"data"`
}

type GetTestReportRecordResponse struct {
	Header
	Data TestReportRecord `json:"data"`
}
