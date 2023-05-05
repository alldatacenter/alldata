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
	"strconv"

	"github.com/jinzhu/gorm"

	_ "github.com/erda-project/erda-proto-go/common/pb"
	commonPb "github.com/erda-project/erda-proto-go/common/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/tools/monitor/dashboard/report/pb"
	dicestructs "github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/mysql"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/crontypes"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
)

type reportService struct {
	p *provider
}

func (r *reportService) ListTasks(ctx context.Context, request *pb.ListTasksRequest) (*pb.ListTasksResponse, error) {
	if request.Scope == "" {
		request.Scope = dicestructs.OrgResource
	}
	if request.PageNo <= 0 {
		request.PageNo = 1
	}
	if request.PageSize <= 0 {
		request.PageSize = 20
	}
	query := reportTaskQuery{
		Scope:         request.Scope,
		ScopeID:       request.ScopeId,
		CreatedAtDesc: true,
	}
	if len(request.Type) > 0 {
		query.Type = request.Type
	}
	reports, total, err := r.p.db.reportTask.List(&query, request.PageSize, request.PageNo)
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return nil, err
	}
	var reportDTOs []*pb.ReportTaskDTO
	for _, obj := range reports {
		obj.NotifyTarget.NotifyGroup = r.p.getNotifyGroupRelByID(ctx, strconv.FormatUint(obj.NotifyTarget.GroupId, 10))
		reportDTO := &pb.ReportTaskDTO{
			Id:           obj.ID,
			Name:         obj.Name,
			Scope:        obj.Scope,
			ScopeId:      obj.ScopeID,
			Type:         string(obj.Type),
			Enable:       obj.Enable,
			NotifyTarget: notify2pb(obj.NotifyTarget),
			DashboardId:  obj.DashboardId,
			CreatedAt:    utils.ConvertTimeToMS(obj.CreatedAt),
			UpdatedAt:    utils.ConvertTimeToMS(obj.UpdatedAt),
		}
		reportDTOs = append(reportDTOs, reportDTO)
	}
	return &pb.ListTasksResponse{
		List:  reportDTOs,
		Total: int64(total),
	}, nil
}

func (r *reportService) CreateTask(ctx context.Context, request *pb.CreateTaskRequest) (*pb.CreateTaskResponse, error) {
	if len(request.Scope) == 0 {
		request.Scope = string(dicestructs.OrgScope)
	}
	request.Enable = true
	var err error
	tx := r.p.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			r.p.Log.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	_, err = tx.systemBlock.Get(&block.DashboardBlockQuery{ID: request.DashboardId})
	if err != nil && gorm.IsRecordNotFoundError(err) {
		return nil, errors.NewNotFoundError("dashboard block")
	}

	obj := &reportTask{
		Name:           request.Name,
		Scope:          request.Scope,
		ScopeID:        request.ScopeId,
		Type:           reportFrequency(request.Type),
		DashboardId:    request.DashboardId,
		DashboardBlock: nil,
		Enable:         request.Enable,
		NotifyTarget:   pb2notify(request.NotifyTarget),
	}

	if err = tx.reportTask.Save(obj); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return nil, errors.NewAlreadyExistsError("report task")
		}
		return nil, errors.NewInternalServerError(err)
	}
	// create pipeline and pipelineCron
	if err = r.p.createReportPipelineCron(obj); err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if err = tx.reportTask.Save(obj); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return nil, errors.NewAlreadyExistsError("report task")
		}
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.CreateTaskResponse{Id: obj.ID}, nil
}

func (r *reportService) UpdateTask(ctx context.Context, request *pb.UpdateTaskRequest) (*commonPb.VoidResponse, error) {
	tx := r.p.db.Begin()
	var err error
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			r.p.Log.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	report, err := tx.reportTask.Get(&reportTaskQuery{ID: &request.Id})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, errors.NewNotFoundError(err.Error())
		}
		return nil, errors.NewInternalServerError(err)
	}
	obj := &reportTaskUpdate{
		Name:         request.Name,
		DashboardId:  request.DashboardId,
		NotifyTarget: request.NotifyTarget,
	}

	report = editReportTaskFields(report, obj)
	if err = tx.reportTask.Save(report); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return nil, errors.NewAlreadyExistsError("report task")
		}
		return nil, errors.NewInternalServerError(err)
	}

	return &commonPb.VoidResponse{}, nil
}

func (r *reportService) SwitchTask(ctx context.Context, request *pb.SwitchTaskRequest) (*commonPb.VoidResponse, error) {
	tx := r.p.db.Begin()
	var err error
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			r.p.Log.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()

	report, err := tx.reportTask.Get(&reportTaskQuery{ID: &request.Id})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, errors.NewNotFoundError("report task")
		}
		return nil, errors.NewInternalServerError(err)
	}
	result, err := r.p.CronService.CronGet(context.Background(), &cronpb.CronGetRequest{
		CronID: report.PipelineCronId,
	})
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if result.Data == nil {
		return nil, errors.NewInternalServerError(crontypes.ErrCronNotFound)
	}
	if result.Data.Enable.Value && !request.Enable {
		_, err := r.p.CronService.CronStop(context.Background(), &cronpb.CronStopRequest{
			CronID: result.Data.ID,
		})
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
	}
	if !result.Data.Enable.Value && request.Enable {
		_, err := r.p.CronService.CronStart(context.Background(), &cronpb.CronStartRequest{
			CronID: result.Data.ID,
		})
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
	}
	report.Enable = request.Enable
	if err = tx.reportTask.Save(report); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return nil, errors.NewAlreadyExistsError("report task")
		}
		return nil, errors.NewInternalServerError(fmt.Errorf("failed to switch report task: %w", err))
	}

	return &commonPb.VoidResponse{}, nil
}

func (r *reportService) GetTask(ctx context.Context, request *pb.GetTaskRequest) (*pb.ReportTaskDTO, error) {
	// obj, err := r.p.db.reportTask.Get(&reportTaskQuery{
	// 	ID:                    &request.Id,
	// 	PreLoadDashboardBlock: true,
	// })
	// if err != nil {
	// 	if gorm.IsRecordNotFoundError(err) {
	// 		return nil, errors.NewNotFoundError("report task")
	// 	}
	// 	return nil, errors.NewInternalServerError(err)
	// }
	// obj.NotifyTarget.NotifyGroup = r.p.getNotifyGroupRelByID(ctx, strconv.FormatUint(obj.NotifyTarget.GroupId, 10))
	//
	// return &pb.ReportTaskDTO{
	// 	Id:           obj.ID,
	// 	Name:         obj.Name,
	// 	Scope:        obj.Scope,
	// 	ScopeId:      obj.ScopeID,
	// 	Type:         string(obj.Type),
	// 	DashboardId:  obj.DashboardId,
	// 	Enable:       obj.Enable,
	// 	NotifyTarget: obj.NotifyTarget,
	// 	CreatedAt:    utils.ConvertTimeToMS(obj.CreatedAt),
	// 	UpdatedAt:    utils.ConvertTimeToMS(obj.UpdatedAt),
	// 	DashboardBlockTemplate: &block.DashboardBlockDTO{
	// 		ID:         obj.DashboardBlock.ID,
	// 		Name:       obj.DashboardBlock.Name,
	// 		Desc:       obj.DashboardBlock.Desc,
	// 		Scope:      obj.DashboardBlock.Scope,
	// 		ScopeID:    obj.DashboardBlock.ScopeID,
	// 		ViewConfig: obj.DashboardBlock.ViewConfig,
	// 		DataConfig: obj.DashboardBlock.DataConfig,
	// 		CreatedAt:  utils.ConvertTimeToMS(obj.CreatedAt),
	// 		UpdatedAt:  utils.ConvertTimeToMS(obj.CreatedAt),
	// 	},
	// }, nil
	return nil, fmt.Errorf("not implement")
}

func (r *reportService) DeleteTask(ctx context.Context, request *pb.DeleteTaskRequest) (*commonPb.VoidResponse, error) {
	tx := r.p.db.Begin()
	var err error
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			r.p.Log.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	query := reportTaskQuery{ID: &request.Id}
	report, err := tx.reportTask.Get(&query)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, errors.NewNotFoundError("report task")
		}
		return nil, errors.NewInternalServerError(fmt.Errorf("failed to get report task :%w", err))
	}
	err = r.p.stopAndDelPipelineCron(report)
	if err != nil {
		return nil, errors.NewInternalServerError(fmt.Errorf("report task failed to stop pipeline: %w", err))
	}
	err = tx.reportTask.Del(&query)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, errors.NewNotFoundError("report task")
		}
		return nil, errors.NewInternalServerError(fmt.Errorf("failed to delete report task: %w", err))
	}
	err = tx.reportHistory.Del(&reportHistoryQuery{TaskId: &report.ID})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, errors.NewNotFoundError("report task")
		}
		return nil, errors.NewInternalServerError(err)
	}

	return &commonPb.VoidResponse{}, nil
}

func (r *reportService) ListTypes(ctx context.Context, request *pb.ListTypesRequest) (*pb.ListTypesResponse, error) {
	if request.Scope == "" {
		request.Scope = dicestructs.OrgResource
	}
	types := []*pb.Type{
		{Name: r.p.t.Text(apis.Language(ctx), "日报"), Value: string(daily)},
		{Name: r.p.t.Text(apis.Language(ctx), "周报"), Value: string(weekly)},
	}

	return &pb.ListTypesResponse{
		List:  types,
		Total: int64(len(types)),
	}, nil
}
