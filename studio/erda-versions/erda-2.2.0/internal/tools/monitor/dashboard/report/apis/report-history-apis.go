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

package apis

import (
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/pkg/mysql"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) createReportHistory(history *reportHistory) interface{} {
	report, err := p.db.reportTask.Get(&reportTaskQuery{ID: &history.TaskId})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report task")
		}
		return api.Errors.NotFound("failed to create report history :", err)
	}

	setHistoryTime(history, report)
	if err = p.db.reportHistory.Save(history); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return api.Errors.AlreadyExists("report history")
		}
		return api.Errors.Internal("failed to save report history :", err)
	}

	return api.Success(struct {
		Id uint64 `json:"id"`
	}{Id: history.ID})
}

// Set report history start time and end time on the basis of report type
func setHistoryTime(history *reportHistory, report *reportTask) {
	// set end time at 23：59：59
	if history.End == 0 {
		t := time.Now()
		history.End = time.Date(t.Year(), t.Month(), t.Day()-1, 23, 59, 59, 0, t.Location()).Unix() * 1000
	}
	// set start time at 23：59：59
	end := time.Unix(history.End/1000, 0)
	switch report.Type {
	case daily:
		history.Start = end.Unix() * 1000
	case weekly:
		history.Start = end.AddDate(0, 0, -7).Unix() * 1000
	case monthly:
		history.Start = end.AddDate(0, -1, 0).Unix() * 1000
	}
}

func (p *provider) getReportHistory(params struct {
	ID uint64 `param:"id" validate:"required"`
}) interface{} {
	history, err := p.db.reportHistory.Get(&reportHistoryQuery{
		ID:                    &params.ID,
		PreLoadDashboardBlock: true,
		PreLoadTask:           true})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report history")
		}
		return api.Errors.Internal(err)
	}

	if history.DashboardBlock.ViewConfig != nil {
		for _, v := range *history.DashboardBlock.ViewConfig {
			v.View.StaticData = struct{}{}
			if history.DashboardBlock.DataConfig != nil {
				for _, d := range *history.DashboardBlock.DataConfig {
					if v.I == d.I {
						v.View.StaticData = d.StaticData
					}
				}
			}
		}
	}
	return api.Success(&reportHistoryDTO{
		ID:      history.ID,
		Scope:   history.Scope,
		ScopeID: history.ScopeID,
		DashboardBlock: &block.DashboardBlockDTO{
			ID:         history.DashboardBlock.ID,
			Name:       history.DashboardBlock.Name,
			Desc:       history.DashboardBlock.Desc,
			Scope:      history.DashboardBlock.Scope,
			ScopeID:    history.DashboardBlock.ScopeID,
			ViewConfig: history.DashboardBlock.ViewConfig,
			DataConfig: history.DashboardBlock.DataConfig,
			CreatedAt:  utils.ConvertTimeToMS(history.DashboardBlock.CreatedAt),
			UpdatedAt:  utils.ConvertTimeToMS(history.DashboardBlock.UpdatedAt),
		},
		ReportTask: &reportTaskOnly{
			ID:           history.ReportTask.ID,
			Name:         history.ReportTask.Name,
			Scope:        history.ReportTask.Scope,
			ScopeID:      history.ReportTask.ScopeID,
			Type:         history.ReportTask.Type,
			Enable:       history.ReportTask.Enable,
			NotifyTarget: history.ReportTask.NotifyTarget,
			CreatedAt:    utils.ConvertTimeToMS(history.ReportTask.CreatedAt),
			UpdatedAt:    utils.ConvertTimeToMS(history.ReportTask.UpdatedAt),
		},
		Start: history.Start,
		End:   history.End,
	})
}

func (p *provider) listReportHistories(params struct {
	TaskId  uint64 `param:"taskId" validate:"required"`
	Scope   string `param:"scope" validate:"required"`
	ScopeId string `param:"scopeId" validate:"required"`
	Start   int64  `param:"start"`
	End     int64  `param:"end"`
}) interface{} {
	query := reportHistoryQuery{
		Scope:         params.Scope,
		ScopeID:       params.ScopeId,
		TaskId:        &params.TaskId,
		CreatedAtDesc: true,
	}
	if params.Start > 0 {
		startTime := time.Unix(params.Start/1000, 0)
		s := time.Date(startTime.Year(), startTime.Month(), startTime.Day(), 0, 0, 0, 0, startTime.Location()).Unix() * 1000
		query.StartTime = &s
	}

	if params.End > 0 {
		endTime := time.Unix(params.End/1000, 0)
		e := time.Date(endTime.Year(), endTime.Month(), endTime.Day()+1, 0, 0, 0, 0, endTime.Location()).Unix() * 1000
		query.EndTime = &e
	}

	histories, total, err := p.db.reportHistory.List(&query, 0, -1)
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return api.Errors.Internal("failed to list report history :", err)
	}

	var historyDTOs []reportHistoryDTO
	ad, _ := time.ParseDuration("24h")
	for _, obj := range histories {
		historyDTO := reportHistoryDTO{
			ID:      obj.ID,
			Scope:   obj.Scope,
			ScopeID: obj.ScopeID,
			Start:   obj.Start,
		}
		if obj.End-obj.Start > ad.Milliseconds() {
			historyDTO.End = obj.End
		}
		historyDTOs = append(historyDTOs, historyDTO)
	}
	resp := &reportHistoriesResp{
		ReportHistories: historyDTOs,
		Total:           total,
	}
	return api.Success(resp)
}

func (p *provider) delReportHistory(params struct {
	ID uint64 `param:"id" validate:"required"`
}) interface{} {
	err := p.db.reportHistory.Del(&reportHistoryQuery{ID: &params.ID})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report history")
		}
		return api.Errors.Internal(err)
	}
	return api.Success(true)
}
