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
	"context"
	"encoding/hex"
	"fmt"
	"net"
	"net/http"
	"strconv"
	"strings"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	uuid "github.com/satori/go.uuid"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v2"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	dicestructs "github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/mysql"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/crontypes"
	api "github.com/erda-project/erda/pkg/common/httpapi"
	"github.com/erda-project/erda/pkg/discover"
)

func (p *provider) creatOrgReportTask(obj *reportTask) interface{} {
	if len(obj.Scope) == 0 {
		obj.Scope = string(dicestructs.OrgScope)
	}
	obj.Enable = true
	var err error
	tx := p.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			p.Log.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	_, err = tx.systemBlock.Get(&block.DashboardBlockQuery{ID: obj.DashboardId})
	if err != nil && gorm.IsRecordNotFoundError(err) {
		return api.Errors.NotFound("dashboard block")
	}
	if err = tx.reportTask.Save(obj); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return api.Errors.AlreadyExists("report task")
		}
		return api.Errors.Internal(err)
	}
	// create pipeline and pipelineCron
	if err = p.createReportPipelineCron(obj); err != nil {
		return api.Errors.Internal(err)
	}
	if err = tx.reportTask.Save(obj); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return api.Errors.AlreadyExists("report task")
		}
		return api.Errors.Internal(err)
	}
	return api.Success(obj)
}

func (p *provider) updateOrgReportTask(obj *reportTaskUpdate, params struct {
	ID uint64 `param:"id" validate:"required"`
}) interface{} {
	tx := p.db.Begin()
	var err error
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			p.Log.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	report, err := tx.reportTask.Get(&reportTaskQuery{ID: &params.ID})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound(err)
		}
		return api.Errors.Internal(err)
	}
	report = editReportTaskFields(report, obj)
	if err = tx.reportTask.Save(report); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return api.Errors.AlreadyExists("report task")
		}
		return api.Errors.Internal(err)
	}

	return api.Success(true)
}

func editReportTaskFields(report *reportTask, update *reportTaskUpdate) *reportTask {
	if update.Name != nil {
		report.Name = *update.Name
	}
	if update.NotifyTarget != nil {
		report.NotifyTarget = update.NotifyTarget
	}
	if update.DashboardId != nil {
		report.DashboardId = *update.DashboardId
	}
	return report
}

func (p *provider) switchOrgReportTask(params struct {
	ID     uint64 `param:"id" validate:"required"`
	Enable bool   `query:"enable"`
}) interface{} {
	tx := p.db.Begin()
	var err error
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			p.Log.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()

	report, err := tx.reportTask.Get(&reportTaskQuery{ID: &params.ID})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report task")
		}
		return api.Errors.Internal(err)
	}
	result, err := p.CronService.CronGet(context.Background(), &cronpb.CronGetRequest{
		CronID: report.PipelineCronId,
	})
	if err != nil {
		return api.Errors.Internal(err)
	}
	if result.Data == nil {
		return api.Errors.Internal(crontypes.ErrCronNotFound)
	}
	if result.Data.Enable.Value && !params.Enable {
		_, err := p.CronService.CronStop(context.Background(), &cronpb.CronStopRequest{
			CronID: result.Data.ID,
		})
		if err != nil {
			return api.Errors.Internal(err)
		}
	}
	if !result.Data.Enable.Value && params.Enable {
		_, err := p.CronService.CronStart(context.Background(), &cronpb.CronStartRequest{
			CronID: result.Data.ID,
		})
		if err != nil {
			return api.Errors.Internal(err)
		}
	}
	report.Enable = params.Enable
	if err = tx.reportTask.Save(report); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return api.Errors.AlreadyExists("report task")
		}
		return api.Errors.Internal("failed to switch report task :", err)
	}

	return api.Success(true)
}

func (p *provider) listOrgReportTasks(r *http.Request, params struct {
	Scope    string `query:"scope" validate:""`
	ScopeID  string `query:"scopeId" validate:"required"`
	PageNo   int64  `query:"pageNo" validate:"gte=0"`
	PageSize int64  `query:"pageSize" validate:"gte=0,lte=100"`
	Type     string `query:"type" validate:""`
}) interface{} {
	if params.Scope == "" {
		params.Scope = dicestructs.OrgResource
	}
	if params.PageNo <= 0 {
		params.PageNo = 1
	}
	if params.PageSize <= 0 {
		params.PageSize = 20
	}
	query := reportTaskQuery{
		Scope:         params.Scope,
		ScopeID:       params.ScopeID,
		CreatedAtDesc: true,
	}
	if len(params.Type) > 0 {
		query.Type = params.Type
	}
	reports, total, err := p.db.reportTask.List(&query, params.PageSize, params.PageNo)
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return api.Errors.Internal(err)
	}
	var reportDTOs []reportTaskDTO
	for _, obj := range reports {
		obj.NotifyTarget.NotifyGroup = p.getNotifyGroupRelByID(strconv.FormatUint(obj.NotifyTarget.GroupID, 10))
		reportDTO := reportTaskDTO{
			ID:           obj.ID,
			Name:         obj.Name,
			Scope:        obj.Scope,
			ScopeID:      obj.ScopeID,
			Type:         obj.Type,
			Enable:       obj.Enable,
			NotifyTarget: obj.NotifyTarget,
			DashboardId:  obj.DashboardId,
			CreatedAt:    utils.ConvertTimeToMS(obj.CreatedAt),
			UpdatedAt:    utils.ConvertTimeToMS(obj.UpdatedAt),
		}
		reportDTOs = append(reportDTOs, reportDTO)
	}
	return api.Success(&reportTaskResp{
		ReportTasks: reportDTOs,
		Total:       total,
	})
}

func (p *provider) runReportTaskAtOnce(params struct {
	ID uint64 `param:"id" validate:"required"`
}) interface{} {

	report, err := p.db.reportTask.Get(&reportTaskQuery{
		ID: &params.ID,
	})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report task")
		}
		return api.Errors.Internal(err)
	}

	report.RunAtOnce = true
	report.Type = "" // cancel cron settings
	if err = p.createReportPipelineCron(report); err != nil {
		return api.Errors.Internal("failed to run report task at once :", err)
	}
	return api.Success(true)
}

func (p *provider) getOrgReportTask(params struct {
	ID uint64 `param:"id" validate:"required"`
}) interface{} {
	obj, err := p.db.reportTask.Get(&reportTaskQuery{
		ID:                    &params.ID,
		PreLoadDashboardBlock: true,
	})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report task")
		}
		return api.Errors.Internal(err)
	}
	obj.NotifyTarget.NotifyGroup = p.getNotifyGroupRelByID(strconv.FormatUint(obj.NotifyTarget.GroupID, 10))
	return api.Success(&reportTaskDTO{
		ID:      obj.ID,
		Name:    obj.Name,
		Scope:   obj.Scope,
		ScopeID: obj.ScopeID,
		Type:    obj.Type,
		DashboardBlockTemplate: &block.DashboardBlockDTO{
			ID:         obj.DashboardBlock.ID,
			Name:       obj.DashboardBlock.Name,
			Desc:       obj.DashboardBlock.Desc,
			Scope:      obj.DashboardBlock.Scope,
			ScopeID:    obj.DashboardBlock.ScopeID,
			ViewConfig: obj.DashboardBlock.ViewConfig,
			DataConfig: obj.DashboardBlock.DataConfig,
			CreatedAt:  utils.ConvertTimeToMS(obj.CreatedAt),
			UpdatedAt:  utils.ConvertTimeToMS(obj.CreatedAt),
		},
		Enable:       obj.Enable,
		NotifyTarget: obj.NotifyTarget,
		CreatedAt:    utils.ConvertTimeToMS(obj.CreatedAt),
		UpdatedAt:    utils.ConvertTimeToMS(obj.UpdatedAt),
	})
}

func (p *provider) getNotifyGroupRelByID(groupID string) *dicestructs.NotifyGroup {
	if groupID == "" {
		return nil
	}
	notifyGroupsData, err := p.cmdb.QueryNotifyGroup([]string{groupID})
	if err != nil {
		logrus.Errorf("request cmdb: query notify group error: %s\n", err)
		return nil
	}
	if len(notifyGroupsData) > 0 {
		return notifyGroupsData[0]
	}

	return nil
}

func (p *provider) delOrgReportTask(params struct {
	ID uint64 `param:"id" validate:"required"`
}) interface{} {
	tx := p.db.Begin()
	var err error
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			p.Log.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	query := reportTaskQuery{ID: &params.ID}
	report, err := tx.reportTask.Get(&query)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report task")
		}
		return api.Errors.Internal("failed to get report task :", err)
	}
	err = p.stopAndDelPipelineCron(report)
	if err != nil {
		return api.Errors.Internal("report task failed to stop pipeline", err)
	}
	err = tx.reportTask.Del(&query)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report task")
		}
		return api.Errors.Internal("failed to delete report task", err)
	}
	err = tx.reportHistory.Del(&reportHistoryQuery{TaskId: &report.ID})
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return api.Errors.NotFound("report task")
		}
		return api.Errors.Internal(err)
	}

	if report != nil {
		return api.Success(map[string]interface{}{
			"name": report.Name,
		})
	}
	return api.Success(true)
}

func (p *provider) createReportPipelineCron(obj *reportTask) error {
	pipeline, err := p.generatePipeline(obj)
	if err != nil {
		return err
	}
	createResp, err := p.bdl.CreatePipeline(&pipeline)
	if err != nil {
		return err
	}
	if createResp.CronID != nil {
		obj.PipelineCronId = *createResp.CronID
	}
	return nil
}

// stop and delete pipelineCron , ignored error
func (p *provider) stopAndDelPipelineCron(obj *reportTask) error {
	if obj.PipelineCronId != 0 {
		_, err := p.CronService.CronStop(context.Background(), &cronpb.CronStopRequest{
			CronID: obj.PipelineCronId,
		})
		_, _ = p.CronService.CronDelete(context.Background(), &cronpb.CronDeleteRequest{
			CronID: obj.PipelineCronId,
		})
		if err != nil {
			return err
		}
	}
	return nil
}

func (p *provider) generatePipeline(r *reportTask) (pipeline dicestructs.PipelineCreateRequestV2, err error) {
	pipeline.PipelineYml, err = p.generatePipelineYml(r)
	if err != nil {
		return pipeline, err
	}
	pipeline.PipelineSource = dicestructs.PipelineSourceDice
	pipeline.PipelineYmlName = hex.EncodeToString(uuid.NewV4().Bytes()) + ".yml"
	pipeline.ClusterName = p.Cfg.ClusterName
	pipeline.AutoRunAtOnce = r.RunAtOnce
	if r.Enable {
		pipeline.AutoStartCron = true
	} else {
		pipeline.AutoStartCron = false
	}

	return pipeline, nil
}

func (p *provider) generatePipelineYml(r *reportTask) (string, error) {
	pipelineYml := &dicestructs.PipelineYml{
		Version: p.Cfg.Pipeline.Version,
	}
	switch r.Type {
	case monthly:
		pipelineYml.Cron = p.Cfg.ReportCron.MonthlyCron
	case weekly:
		pipelineYml.Cron = p.Cfg.ReportCron.WeeklyCron
	case daily:
		pipelineYml.Cron = p.Cfg.ReportCron.DailyCron
	}
	org, err := p.bdl.GetOrg(r.ScopeID)
	if err != nil {
		return "", errors.Errorf("failed to generate pipeline yaml, can not get OrgName by OrgID:%v,(%+v)", r.ScopeID, err)
	}

	maddr, err := p.createFQDN(discover.Monitor())
	if err != nil {
		return "", err
	}
	eaddr, err := p.createFQDN(discover.CoreServices())
	if err != nil {
		return "", err
	}
	pipelineYml.Stages = [][]*dicestructs.PipelineYmlAction{{{
		Type:    p.Cfg.Pipeline.ActionType,
		Version: p.Cfg.Pipeline.ActionVersion,
		Params: map[string]interface{}{
			"monitor_addr":       maddr,
			"core_services_addr": eaddr,
			"report_id":          r.ID,
			"org_name":           org.Name,
			"domain_addr":        fmt.Sprintf("%s://%s", p.Cfg.DiceProtocol, org.Domain),
		},
	}}}
	byteContent, err := yaml.Marshal(pipelineYml)
	if err != nil {
		return "", errors.Errorf("failed to generate pipeline yaml, pipelineYml:%+v, (%+v)", pipelineYml, err)
	}

	logrus.Debugf("[PipelineYml]: %s", string(byteContent))
	return string(byteContent), nil
}

func (p *provider) createFQDN(addr string) (string, error) {
	host, port, err := net.SplitHostPort(addr)
	if err != nil {
		return "", err
	}
	var svc string
	idx := strings.Index(host, ".")
	if idx == -1 {
		svc = host
	} else {
		svc = host[:idx]
	}
	return net.JoinHostPort(svc+"."+p.Cfg.DiceNameSpace, port), nil
}
