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

package reportapisv1

import (
	"context"
	"fmt"
	"time"

	"github.com/jinzhu/gorm"

	commonPb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/tools/monitor/dashboard/report/pb"
	"github.com/erda-project/erda/pkg/common/errors"
)

func (r *reportService) ListHistories(ctx context.Context, request *pb.ListHistoriesRequest) (*pb.ListHistoriesResponse, error) {
	histories, total, err := r.p.db.reportHistory.List(getListHistoriesQuery(request), 0, -1)
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return nil, errors.NewInternalServerError(fmt.Errorf("failed to list report history: %w", err))
	}

	return &pb.ListHistoriesResponse{
		List:  constructHistoryDTOs(histories),
		Total: int64(total),
	}, nil
}

func getListHistoriesQuery(request *pb.ListHistoriesRequest) *reportHistoryQuery {
	query := reportHistoryQuery{
		Scope:         request.Scope,
		ScopeID:       request.ScopeId,
		TaskId:        &request.TaskId,
		CreatedAtDesc: true,
	}
	if request.Start > 0 {
		startTime := time.Unix(request.Start/1000, 0)
		s := time.Date(startTime.Year(), startTime.Month(), startTime.Day(), 0, 0, 0, 0, startTime.Location()).Unix() * 1000
		query.StartTime = &s
	}

	if request.End > 0 {
		endTime := time.Unix(request.End/1000, 0)
		e := time.Date(endTime.Year(), endTime.Month(), endTime.Day()+1, 0, 0, 0, 0, endTime.Location()).Unix() * 1000
		query.EndTime = &e
	}
	return &query
}

func constructHistoryDTOs(histories []reportHistory) []*pb.ReportHistoryDTO {
	var historyDTOs []*pb.ReportHistoryDTO
	ad, _ := time.ParseDuration("24h")
	for _, obj := range histories {
		historyDTO := &pb.ReportHistoryDTO{
			Id:      obj.ID,
			Scope:   obj.Scope,
			ScopeId: obj.ScopeID,
			Start:   obj.Start,
		}
		if obj.End-obj.Start > ad.Milliseconds() {
			historyDTO.End = obj.End
		}
		historyDTOs = append(historyDTOs, historyDTO)
	}
	return historyDTOs
}

func (r *reportService) CreateHistory(ctx context.Context, request *pb.CreateHistoryRequest) (*pb.CreateHistoryResponse, error) {
	// report, err := r.p.db.reportTask.Get(&reportTaskQuery{ID: &request.TaskId})
	// if err != nil {
	// 	if gorm.IsRecordNotFoundError(err) {
	// 		return nil, errors.NewNotFoundError("report task")
	// 	}
	// 	return nil, errors.NewNotFoundError(fmt.Sprintf("failed to create report history : %s", err))
	// }
	//
	// history := &reportHistory{
	// 	ID:             request.Id,
	// 	Scope:          request.Scope,
	// 	ScopeID:        request.ScopeId,
	// 	TaskId:         request.TaskId,
	// 	ReportTask:     request.ReportTask,
	// 	DashboardId:    request.DashboardId,
	// 	DashboardBlock: request.DashboardBlock,
	// }
	//
	// setHistoryTime(history, report)
	// if err = r.p.db.reportHistory.Save(history); err != nil {
	// 	if mysql.IsUniqueConstraintError(err) {
	// 		return nil, errors.NewAlreadyExistsError("report history")
	// 	}
	// 	return nil, errors.NewInternalServerError(fmt.Errorf("failed to save report history : %w", err))
	// }
	//
	// return &pb.CreateHistoryResponse{Id: history.ID}, nil
	return nil, fmt.Errorf("not implement")
}

func (r *reportService) GetHistory(ctx context.Context, request *pb.GetHistoryRequest) (*pb.GetHistoryResponse, error) {
	// history, err := r.p.db.reportHistory.Get(&reportHistoryQuery{
	// 	ID:                    &request.Id,
	// 	PreLoadDashboardBlock: true,
	// 	PreLoadTask:           true})
	// if err != nil {
	// 	if gorm.IsRecordNotFoundError(err) {
	// 		return nil, errors.NewNotFoundError("report history")
	// 	}
	// 	return nil, errors.NewInternalServerError(err)
	// }
	//
	// if history.DashboardBlock.ViewConfig != nil {
	// 	for _, v := range *history.DashboardBlock.ViewConfig {
	// 		v.View.StaticData = struct{}{}
	// 		if history.DashboardBlock.DataConfig != nil {
	// 			for _, d := range *history.DashboardBlock.DataConfig {
	// 				if v.I == d.I {
	// 					v.View.StaticData = d.StaticData
	// 				}
	// 			}
	// 		}
	// 	}
	// }
	// return &pb.GetHistoryResponse{
	// 	Id:      history.ID,
	// 	Scope:   history.Scope,
	// 	ScopeId: history.ScopeID,
	// 	ReportTask: &reportTaskOnly{
	// 		ID:           history.ReportTask.ID,
	// 		Name:         history.ReportTask.Name,
	// 		Scope:        history.ReportTask.Scope,
	// 		ScopeID:      history.ReportTask.ScopeID,
	// 		Type:         history.ReportTask.Type,
	// 		Enable:       history.ReportTask.Enable,
	// 		NotifyTarget: history.ReportTask.NotifyTarget,
	// 		CreatedAt:    utils.ConvertTimeToMS(history.ReportTask.CreatedAt),
	// 		UpdatedAt:    utils.ConvertTimeToMS(history.ReportTask.UpdatedAt),
	// 	},
	// 	DashboardBlock: &block.DashboardBlockDTO{
	// 		ID:         history.DashboardBlock.ID,
	// 		Name:       history.DashboardBlock.Name,
	// 		Desc:       history.DashboardBlock.Desc,
	// 		Scope:      history.DashboardBlock.Scope,
	// 		ScopeID:    history.DashboardBlock.ScopeID,
	// 		ViewConfig: history.DashboardBlock.ViewConfig,
	// 		DataConfig: history.DashboardBlock.DataConfig,
	// 		CreatedAt:  utils.ConvertTimeToMS(history.DashboardBlock.CreatedAt),
	// 		UpdatedAt:  utils.ConvertTimeToMS(history.DashboardBlock.UpdatedAt),
	// 	},
	// 	Start:          history.Start,
	// 	End:            history.End,
	// }, nil
	return nil, fmt.Errorf("not implement")
}

func (r *reportService) DeleteHistory(ctx context.Context, request *pb.DeleteHistoryRequest) (*commonPb.VoidResponse, error) {
	err := r.p.db.reportHistory.Del(&reportHistoryQuery{ID: &request.Id})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, errors.NewNotFoundError("report history")
		}
		return nil, errors.NewInternalServerError(err)
	}
	return &commonPb.VoidResponse{}, nil
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
