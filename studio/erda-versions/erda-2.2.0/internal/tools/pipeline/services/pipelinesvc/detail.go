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

package pipelinesvc

import (
	"context"
	"fmt"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"
	corev1 "k8s.io/api/core/v1"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	common "github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/commonutil/costtimeutil"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/crontypes"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/metadata"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

func (s *PipelineSvc) Get(pipelineID uint64) (*spec.Pipeline, error) {
	p, err := s.dbClient.GetPipeline(pipelineID)
	if err != nil {
		return nil, apierrors.ErrGetPipeline.InternalError(err)
	}
	return &p, nil
}

func (s *PipelineSvc) SimplePipelineBaseDetail(pipelineID uint64) (*apistructs.PipelineDetailDTO, error) {
	base, find, err := s.dbClient.GetPipelineBase(pipelineID)
	if err != nil {
		return nil, err
	}
	if !find {
		return nil, fmt.Errorf("not find this pipeline id %v", pipelineID)
	}

	var detail apistructs.PipelineDetailDTO
	detail.PipelineDTO = s.convertPipelineBase(base)

	return &detail, nil
}

func (s *PipelineSvc) Detail(pipelineID uint64) (*apistructs.PipelineDetailDTO, error) {
	p, err := s.dbClient.GetPipeline(pipelineID)

	if err != nil {
		return nil, apierrors.ErrGetPipelineDetail.InternalError(err)
	}

	p.CostTimeSec = costtimeutil.CalculatePipelineCostTimeSec(&p)

	// 创建时间特殊处理
	if len(p.Extra.CronExpr) > 0 {
		p.TimeCreated = p.Extra.CronTriggerTime
	}

	// 不展示 secret
	p.Snapshot.Secrets = nil

	stages, err := s.dbClient.ListPipelineStageByPipelineID(p.ID)
	if err != nil {
		return nil, apierrors.ErrGetPipelineDetail.InternalError(err)
	}

	tasks, err := s.dbClient.ListPipelineTasksByPipelineID(p.ID)
	if err != nil {
		return nil, apierrors.ErrGetPipelineDetail.InternalError(err)
	}

	// init yml
	pipelineYml, err := pipelineyml.New(
		[]byte(p.PipelineYml),
	)
	if err != nil {
		return nil, apierrors.ErrGetPipelineDetail.InternalError(err)
	}
	// merge yml task and db task
	tasks, err = s.MergePipelineYmlTasks(pipelineYml, tasks, &p, stages, nil)
	if err != nil {
		return nil, apierrors.ErrGetPipelineDetail.InternalError(err)
	}

	var needApproval bool
	var stageDetailDTO []apistructs.PipelineStageDetailDTO

	for _, stage := range stages {
		var taskDTOs []apistructs.PipelineTaskDTO
		for _, task := range tasks {
			if task.StageID != stage.ID {
				continue
			}
			if task.Type == "manual-review" {
				needApproval = true
			}
			task.CostTimeSec = costtimeutil.CalculateTaskCostTimeSec(&task)
			if task.Result == nil {
				task.Result = &taskresult.Result{}
				task.Result.Metadata = make([]metadata.MetadataField, 0)
			}
			// add task events to result metadata if task status isn`t success and events it`s failed
			if !task.Status.IsSuccessStatus() && task.Inspect.Events != "" && !isEventsLatestNormal(task.Inspect.Events) {
				task.Result.Metadata = append(task.Result.Metadata, metadata.MetadataField{
					Name:  "task-events",
					Value: task.Inspect.Events,
				})
			}
			// set analyzed yaml task to NoNeedBySystem to simulate db task's behaviour
			if p.Status.IsStopByUser() && task.Status == apistructs.PipelineStatusAnalyzed {
				task.Status = apistructs.PipelineStatusNoNeedBySystem
			}
			taskDTOs = append(taskDTOs, *task.Convert2DTO())
		}
		stageDetailDTO = append(stageDetailDTO,
			apistructs.PipelineStageDetailDTO{PipelineStageDTO: *stage.Convert2DTO(), PipelineTasks: taskDTOs})
	}

	var pc *common.Cron
	// CronExpr 不为空，则 cron 必须存在
	if len(p.Extra.CronExpr) > 0 {
		if p.CronID == nil {
			return nil, apierrors.ErrGetPipelineDetail.MissingParameter("cronID")
		}

		result, err := s.pipelineCronSvc.CronGet(context.Background(), &cronpb.CronGetRequest{
			CronID: *p.CronID,
		})
		if err != nil {
			return nil, apierrors.ErrGetPipelineDetail.InternalError(err)
		}
		if result.Data == nil {
			return nil, apierrors.ErrNotFoundPipelineCron.InternalError(crontypes.ErrCronNotFound)
		}
		pc = result.Data
	} else {
		// cron 按钮（开始、停止操作）目前挂在 pipeline 实例上，非周期创建的实例，也需要有 cron 按钮信息进行操作
		// 尝试根据 pipelineSource + pipelineYmlName 获取 cron
		resp, err := s.pipelineCronSvc.CronPaging(context.Background(), &cronpb.CronPagingRequest{
			Sources:  []string{p.PipelineSource.String()},
			YmlNames: []string{p.PipelineYmlName},
			PageSize: 1,
			PageNo:   1,
		})
		if err != nil {
			return nil, apierrors.ErrPagingPipelineCron.InternalError(err)
		}
		if len(resp.Data) > 0 {
			pc = resp.Data[0]
		}
	}

	var detail apistructs.PipelineDetailDTO
	detail.NeedApproval = needApproval
	detail.PipelineDTO = *s.ConvertPipeline(&p)

	// 插入 label
	pipelineLabels, _ := s.dbClient.ListLabelsByPipelineID(p.ID)
	labels := make(map[string]string, len(pipelineLabels))
	for _, v := range pipelineLabels {
		labels[v.Key] = v.Value
	}
	detail.PipelineDTO.Labels = labels
	detail.PipelineStages = stageDetailDTO
	detail.PipelineCron = pc
	// 前端需要 cron 对象不为空
	if detail.PipelineCron == nil {
		detail.PipelineCron = &common.Cron{}
	}

	buttons, err := s.setPipelineButtons(p, pc)
	if err != nil {
		return nil, apierrors.ErrGetPipelineDetail.InternalError(err)
	}

	detail.PipelineButton = buttons

	s.setPipelineTaskActionDetail(&detail)

	pipelineParams, err := getPipelineParams(p.PipelineYml, p.Snapshot.RunPipelineParams)
	if err != nil {
		return nil, err
	}
	detail.RunParams = pipelineParams

	// events
	detail.Events = s.getPipelineEvents(pipelineID)

	return &detail, nil
}

func getPipelineParams(pipelineYml string, runParams []apistructs.PipelineRunParamWithValue) ([]apistructs.PipelineParamDTO, error) {

	pipeline, err := pipelineyml.New([]byte(pipelineYml))
	if err != nil {
		return nil, err
	}

	if pipeline == nil {
		return nil, errors.New("  getPipelineParams error: yml to pipeline error, pipeline is empty")
	}

	if pipeline.Spec() == nil {
		return nil, errors.New("  getPipelineParams error: pipeline spec is empty")
	}

	params := pipeline.Spec().Params
	if params == nil {
		return nil, nil
	}

	runParamsMap := make(map[string]interface{})
	if runParams != nil {
		for _, v := range runParams {
			runParamsMap[v.Name] = v.Value
		}
	}

	var pipelineParamDTOs []apistructs.PipelineParamDTO
	for _, param := range params {
		pipelineParamDTOs = append(pipelineParamDTOs, apistructs.PipelineParamDTO{
			PipelineParam: apistructs.PipelineParam{
				Name:     param.Name,
				Desc:     param.Desc,
				Default:  param.Default,
				Required: param.Required,
				Type:     param.Type,
			},
			Value: runParamsMap[param.Name],
		})
	}
	return pipelineParamDTOs, nil
}

func (s *PipelineSvc) getPipelineEvents(pipelineID uint64) []*apistructs.PipelineEvent {
	_, events, err := s.dbClient.GetPipelineEvents(pipelineID)
	if err != nil {
		logrus.Errorf("failed to get pipeline events, pipelineID: %d, err: %v", pipelineID, err)
		return nil
	}
	return events
}

// 给 pipelineTask 设置 action 的 logo 和 displayName 给前端展示
func (s *PipelineSvc) setPipelineTaskActionDetail(detail *apistructs.PipelineDetailDTO) {
	stageDetails := detail.PipelineStages

	var extensionSearchRequest = apistructs.ExtensionSearchRequest{}
	extensionSearchRequest.YamlFormat = true
	// 循环 stageDetails 数组，获取里面的 task 并设置到 Extensions 中
	loopStageDetails(stageDetails, func(task apistructs.PipelineTaskDTO) {
		if task.Type == apistructs.ActionTypeSnippet {
			return
		}
		extensionSearchRequest.Extensions = append(extensionSearchRequest.Extensions, task.Type)
	})
	if extensionSearchRequest.Extensions != nil {
		extensionSearchRequest.Extensions = strutil.DedupSlice(extensionSearchRequest.Extensions, true)
	}

	// 根据 Extensions 数组批量查询详情
	resultMap, err := s.bdl.SearchExtensions(extensionSearchRequest)
	if err != nil {
		logrus.Errorf("pipeline Detail to SearchExtensions error: %v", err)
		return
	}
	// 遍历 stageDetails 数组，根据 task 的 name 获取其 extension 详情
	var actionDetails = make(map[string]apistructs.PipelineTaskActionDetail)
	loopStageDetails(stageDetails, func(task apistructs.PipelineTaskDTO) {

		if task.Type == pipelineyml.Snippet {
			actionDetails[task.Type] = apistructs.PipelineTaskActionDetail{
				LogoUrl:     pipelineyml.SnippetLogo,
				DisplayName: pipelineyml.SnippetDisplayName,
				Description: pipelineyml.SnippetDesc,
			}
			return
		}

		version, ok := resultMap[task.Type]
		if !ok {
			return
		}

		specYmlStr, ok := version.Spec.(string)
		if !ok {
			return
		}

		var actionSpec apistructs.ActionSpec
		if err := yaml.Unmarshal([]byte(specYmlStr), &actionSpec); err != nil {
			return
		}

		actionDetails[task.Type] = apistructs.PipelineTaskActionDetail{
			LogoUrl:     actionSpec.LogoUrl,
			DisplayName: actionSpec.GetLocaleDisplayName(i18n.GetGoroutineBindLang()),
			Description: actionSpec.GetLocaleDesc(i18n.GetGoroutineBindLang()),
		}
	})
	detail.PipelineTaskActionDetails = actionDetails
}

// 遍历 stageDetails 数组，内部的每个 task 都执行一遍 doing 函数
func loopStageDetails(stageDetails []apistructs.PipelineStageDetailDTO, doing func(dto apistructs.PipelineTaskDTO)) {
	if stageDetails != nil {
		for _, stage := range stageDetails {
			tasks := stage.PipelineTasks
			if tasks == nil {
				continue
			}

			for _, task := range tasks {
				doing(task)
			}
		}
	}
}

// Statistic pipeline 运行情况统计
func (s *PipelineSvc) Statistic(source, clusterName string) (*apistructs.PipelineStatisticResponseData, error) {
	return s.dbClient.PipelineStatistic(source, clusterName)
}

// 设置按钮状态
func (s *PipelineSvc) setPipelineButtons(p spec.Pipeline, pc *common.Cron) (button apistructs.PipelineButton, err error) {
	defer func() {
		err = errors.Wrap(err, "failed to set pipeline button")
	}()

	button = apistructs.PipelineButton{
		CanManualRun:   func() bool { _, can := s.run.CanManualRun(context.Background(), &p); return can }(),
		CanCancel:      canCancel(p),
		CanForceCancel: canForceCancel(p),
		CanRerun:       canRerun(p),
		CanRerunFailed: canRerunFailed(p),
		CanStartCron:   canStartCron(p, pc),
		CanStopCron:    canStopCron(p, pc),
		CanPause:       canPause(p),
		CanUnpause:     canUnpause(p),
		CanDelete:      func() bool { ok, _ := canDelete(p); return ok }(),
	}

	return
}

func canCancel(p spec.Pipeline) bool {
	return p.Status.IsReconcilerRunningStatus()
}

// TODO 强制取消
func canForceCancel(p spec.Pipeline) bool {
	return false
}

// canRerun 重试全流程
func canRerun(p spec.Pipeline) bool {
	return p.Status.IsEndStatus()
}

// canRerunFailed 重试失败节点
func canRerunFailed(p spec.Pipeline) bool {
	// pipeline 状态为失败，且未被 gc 前，可以重试失败节点
	if p.Status.IsFailedStatus() && !p.Extra.CompleteReconcilerGC {
		return true
	}
	return false
}

// canStartCron p.cronID = pc.id
func canStartCron(p spec.Pipeline, pc *common.Cron) bool {
	return pc != nil && pc.Enable != nil && !pc.Enable.Value
}

// canStopCron p.cronID = pc.id
func canStopCron(p spec.Pipeline, pc *common.Cron) bool {
	return pc != nil && pc.Enable != nil && pc.Enable.Value
}

// canPause TODO 需要关心所有节点运行状态，如果所有节点都在运行中，则不能暂停
func canPause(p spec.Pipeline) bool {
	return false
}

// canUnpause TODO
func canUnpause(p spec.Pipeline) bool {
	return p.Status.CanUnpause()
}

func canDelete(p spec.Pipeline) (bool, string) {
	// status
	if !p.Status.CanDelete() {
		return false, fmt.Sprintf("invalid status: %s", p.Status)
	}
	// 终态后需要判断 complete gc
	if p.Status.IsEndStatus() {
		if !p.Extra.CompleteReconcilerGC {
			return false, fmt.Sprintf("waiting gc")
		}
	}
	return true, ""
}

func polishTask(p *spec.Pipeline, task *spec.PipelineTask, runningStageID uint64, dbClient *dbclient.Client) {
	changed := false
	defer func() {
		if changed {
			if err := dbClient.UpdatePipelineTaskStatus(task.ID, task.Status); err != nil {
				logrus.Errorf("[alert] failed to update pipeline task status when polishTask, pipelineID: %d, taskID: %d, err: %v",
					p.ID, task.ID, err)
			}
		}
	}()
	// 1. pipeline 为终态
	// 2. pipeline 非终态，只修改 stageID 小于 runningStageID 的任务
	if p.Status.IsEndStatus() || task.StageID < runningStageID {
		// task 仍在运行
		if task.Status == apistructs.PipelineStatusAnalyzed || task.Status.IsReconcilerRunningStatus() {
			// 整体成功，则 task 一定成功
			if p.Status.IsSuccessStatus() {
				task.Status = apistructs.PipelineStatusSuccess
				changed = true
				return
			}
			// 判断 task 状态
			if len(task.Inspect.Errors) > 0 {
				task.Status = apistructs.PipelineStatusFailed
				changed = true
				return
			}
			// 无法判断，判断为成功
			task.Status = apistructs.PipelineStatusSuccess
			changed = true
			return
		}
	}
}

// findRunningStageID 若 pipeline 为终态，返回 0
// R: Running A: Analyzed S: Success
// 1 R       1 R
// 2 S => 3  2 A => 1
// 3 S       3 A
func findRunningStageID(p spec.Pipeline, tasks []spec.PipelineTask) uint64 {
	if p.Status.IsEndStatus() {
		return 0
	}
	var runningStageID uint64 = 0
	for i := range tasks {
		task := tasks[i]
		if !task.Status.IsEndStatus() {
			if runningStageID == 0 {
				runningStageID = task.StageID
			}
			if task.StageID < runningStageID {
				runningStageID = task.StageID
			}
		} else { // task 终态
			if task.StageID > runningStageID {
				runningStageID = task.StageID
			}
		}
	}
	return runningStageID
}

//isEventsContainWarn return k8s events is contain warn
//Events:
// Type    Reason     Age   From               Message
// ----    ------     ----  ----               -------
// Normal  Scheduled  7s    default-scheduler  Successfully assigned pipeline-4152/pipeline-4152.pipeline-task-8296-tgxd7 to node-010000006200
// Normal  Pulled     6s    kubelet            Container image "registry.erda.cloud/erda-actions/action-agent:1.2-20210804-75232495" already present on machine
func isEventsContainWarn(events string) bool {
	eventLst := strings.Split(events, "\n")
	if len(eventLst) <= 3 {
		return false
	}
	for _, ev := range eventLst {
		if strings.Contains(ev, corev1.EventTypeWarning) {
			return true
		}
	}
	return false
}

// isEventsLatestNormal judge k8s events latest event is normal
func isEventsLatestNormal(events string) bool {
	eventLst := strings.Split(events, "\n")
	if len(eventLst) <= 3 {
		return true
	}
	return strings.Contains(eventLst[len(eventLst)-2], corev1.EventTypeNormal)
}
