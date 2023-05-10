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

package test_report

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

type TestReport struct {
	db  *dao.DBClient
	bdl *bundle.Bundle
}

type Option func(*TestReport)

func New(options ...Option) *TestReport {
	t := &TestReport{}
	for _, op := range options {
		op(t)
	}
	return t
}

func WithDBClient(db *dao.DBClient) Option {
	return func(t *TestReport) {
		t.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(t *TestReport) {
		t.bdl = bdl
	}
}

func (svc *TestReport) CreateTestReport(req apistructs.TestReportRecord) (uint64, error) {
	// check permission
	if !req.IdentityInfo.IsInternalClient() {
		access, err := svc.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   req.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  req.ProjectID,
			Resource: "testReport",
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return 0, err
		}
		if !access.Access {
			return 0, apierrors.ErrCreateTestReportRecord.AccessDenied()
		}
	}

	if req.Name == "" {
		return 0, apierrors.ErrCreateTestReportRecord.InvalidParameter("name")
	}
	if req.Summary == "" {
		return 0, apierrors.ErrCreateTestReportRecord.InvalidParameter("summary")
	}
	iteration, err := svc.db.GetIteration(req.IterationID)
	if err != nil {
		return 0, apierrors.ErrCreateTestReportRecord.NotFound()
	}
	if iteration.ProjectID != req.ProjectID {
		return 0, apierrors.ErrCreateTestReportRecord.InvalidParameter("iterationID")
	}
	qualityScore := req.ReportData.GetQualityScore()
	if qualityScore > 500 || qualityScore < 0 {
		return 0, apierrors.ErrCreateTestReportRecord.InvalidParameter(fmt.Sprintf("qualityScore: %.2f", qualityScore))
	}
	record := &dao.TestReportRecord{
		Name:         req.Name,
		ProjectID:    req.ProjectID,
		IterationID:  iteration.ID,
		CreatorID:    req.UserID,
		QualityScore: qualityScore,
		Summary:      req.Summary,
		ReportData: dao.TestReportData{
			IssueDashboard: req.ReportData.IssueDashboard,
			TestDashboard:  req.ReportData.TestDashboard,
		},
	}
	if err := svc.db.CreateTestReportRecord(record); err != nil {
		return 0, err
	}
	return record.ID, nil
}

func (svc *TestReport) ListTestReportByRequest(req apistructs.TestReportRecordListRequest) (apistructs.TestReportRecordData, error) {
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 20
	}
	var orderBy string
	switch req.OrderBy {
	case "createdAt":
		orderBy = "created_at"
	case "qualityScore":
		orderBy = "quality_score"
	default:
		orderBy = ""
	}
	req.OrderBy = orderBy
	var res apistructs.TestReportRecordData
	records, total, err := svc.db.ListTestReportRecord(req)
	if err != nil {
		return res, err
	}
	for _, record := range records {
		res.List = append(res.List, record.Convert())
	}
	res.Total = total
	return res, nil
}

func (svc *TestReport) GetTestReportByID(id uint64) (apistructs.TestReportRecord, error) {
	record, err := svc.db.GetTestReportRecordByID(id)
	if err != nil {
		return apistructs.TestReportRecord{}, err
	}
	return record.Convert(), nil
}
