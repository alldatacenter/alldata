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

package reportsvc

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (svc *ReportSvc) Create(req apistructs.PipelineReportCreateRequest) (*apistructs.PipelineReport, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrCreatePipelineReport.InvalidParameter(err)
	}
	// 校验 pipeline 是否存在
	p, exist, err := svc.dbClient.GetPipelineBase(req.PipelineID)
	if err != nil {
		return nil, apierrors.ErrCreatePipelineReport.InvalidParameter(fmt.Errorf("failed to find pipeline, err: %v", err))
	}
	if !exist {
		return nil, apierrors.ErrCreatePipelineReport.InvalidParameter(fmt.Errorf("pipeline not exist"))
	}
	// 插入数据库
	dbReport := spec.PipelineReport{
		PipelineID: req.PipelineID,
		Type:       req.Type,
		Meta:       req.Meta,
		CreatorID:  req.IdentityInfo.UserID,
		UpdaterID:  req.IdentityInfo.UserID,
	}
	if err := svc.dbClient.CreatePipelineReport(&dbReport); err != nil {
		return nil, apierrors.ErrCreatePipelineReport.InternalError(fmt.Errorf("failed to create report in database, err: %v", err))
	}
	// 插入 label 作用于分页查询
	reportLabelKey, reportLabelValue := svc.dbClient.MakePipelineReportTypeLabelKey(req.Type)
	if err := svc.dbClient.CreatePipelineLabels(&spec.Pipeline{
		PipelineBase: spec.PipelineBase{ID: p.ID, PipelineSource: p.PipelineSource, PipelineYmlName: p.PipelineYmlName},
		Labels:       map[string]string{reportLabelKey: reportLabelValue},
	}); err != nil {
		return nil, apierrors.ErrCreatePipelineReport.InternalError(fmt.Errorf("failed to create related pipeline labels, err: %v", err))
	}
	// 转换
	report := convert(dbReport)

	return &report, nil
}

func (svc *ReportSvc) GetPipelineReportSet(pipelineID uint64, types ...string) (*apistructs.PipelineReportSet, error) {

	dbReports, err := svc.dbClient.BatchListPipelineReportsByPipelineID([]uint64{pipelineID}, types)
	if err != nil {
		return nil, apierrors.ErrQueryPipelineReportSet.InternalError(err)
	}

	var reports []apistructs.PipelineReport
	for _, v := range dbReports[pipelineID] {
		reports = append(reports, convert(v))
	}

	return &apistructs.PipelineReportSet{
		PipelineID: pipelineID,
		Reports:    reports,
	}, nil
}

func (svc *ReportSvc) PagingPipelineReportSets(req apistructs.PipelineReportSetPagingRequest) (*apistructs.PipelineReportSetPagingResponseData, error) {
	// 查询数据库
	sets, total, err := svc.dbClient.PagingPipelineReportSets(req)
	if err != nil {
		return nil, apierrors.ErrQueryPipelineReportSet.InternalError(err)
	}
	result := apistructs.PipelineReportSetPagingResponseData{
		Total:     total,
		Pipelines: sets,
	}
	return &result, nil
}

func convert(dbReport spec.PipelineReport) apistructs.PipelineReport {
	return apistructs.PipelineReport{
		ID:         dbReport.ID,
		PipelineID: dbReport.PipelineID,
		Type:       dbReport.Type,
		Meta:       dbReport.Meta,
		CreatorID:  dbReport.CreatorID,
		UpdaterID:  dbReport.UpdaterID,
		CreatedAt:  dbReport.CreatedAt,
		UpdatedAt:  dbReport.UpdatedAt,
	}
}
