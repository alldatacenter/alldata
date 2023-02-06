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
	"encoding/json"
	"strconv"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"
	"google.golang.org/protobuf/types/known/wrapperspb"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/container_provider"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/crontypes"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func (s *PipelineSvc) CreateV2(ctx context.Context, req *apistructs.PipelineCreateRequestV2) (*spec.Pipeline, error) {
	// validate
	if err := s.validateCreateRequest(req); err != nil {
		return nil, err
	}
	// set default
	setDefault(req)

	p, err := s.makePipelineFromRequestV2(req)
	if err != nil {
		return nil, err
	}

	var stages []spec.PipelineStage
	if stages, err = s.CreatePipelineGraph(p); err != nil {
		logrus.Errorf("failed to create pipeline graph, pipelineID: %d, err: %v", p.ID, err)
		return nil, err
	}

	// PreCheck
	_ = s.PreCheck(p, stages, p.GetUserID(), req.AutoRunAtOnce)

	// 立即执行一次
	if req.AutoRunAtOnce {
		_p, err := s.run.RunOnePipeline(ctx, &apistructs.PipelineRunRequest{
			PipelineID:        p.ID,
			ForceRun:          req.ForceRun,
			IdentityInfo:      req.IdentityInfo,
			PipelineRunParams: req.RunParams,
			Secrets:           req.Secrets,
		})
		if err != nil {
			logrus.Errorf("failed to run pipeline, pipelineID: %d, err: %v", p.ID, err)
			return nil, err
		}
		p = _p
	}

	// 立即开始定时
	if req.AutoStartCron {
		if p.CronID != nil {
			if _, err := s.pipelineCronSvc.CronStart(context.Background(), &cronpb.CronStartRequest{
				CronID: *p.CronID,
			}); err != nil {
				logrus.Errorf("failed to start cron, pipelineID: %d, cronID: %d, err: %v", p.ID, *p.CronID, err)
				return nil, err
			}
		}
	}

	return p, nil
}

const (
	maxSqlIndexLength = 191
)

// validateCreateRequest validate pipelineCreateRequestV2
func (s *PipelineSvc) validateCreateRequest(req *apistructs.PipelineCreateRequestV2) error {
	if req == nil {
		return apierrors.ErrCreatePipeline.MissingParameter("request")
	}
	// +required
	if req.PipelineYml == "" {
		return apierrors.ErrCreatePipeline.MissingParameter("pipelineYml")
	}
	// +required
	if req.ClusterName == "" {
		return apierrors.ErrCreatePipeline.MissingParameter("clusterName")
	}
	// +optional
	if req.PipelineYmlName == "" {
		req.PipelineYmlName = apistructs.DefaultPipelineYmlName
	}
	// +required
	if req.PipelineSource == "" {
		return apierrors.ErrCreatePipeline.MissingParameter("pipelineSource")
	}
	if !req.PipelineSource.Valid() {
		return apierrors.ErrCreatePipeline.InvalidParameter(errors.Errorf("source: %s", req.PipelineSource))
	}
	// identity
	if req.UserID == "" && req.InternalClient == "" {
		return apierrors.ErrCreatePipeline.MissingParameter("identity")
	}
	// filterLabels
	// if label key or value is too long, it will be moved to NormalLabels automatically.
	if req.NormalLabels == nil {
		req.NormalLabels = make(map[string]string)
	}
	for k, v := range req.Labels {
		if len(k) > maxSqlIndexLength || len(v) > maxSqlIndexLength {
			logrus.Warnf("filterLabel key or value is too long, move to normalLabels automatically, key: %s, value: %s", k, v)
			req.NormalLabels[k] = v
			delete(req.Labels, k)
		}
	}
	// bind queue
	_, err := s.validateQueueFromLabels(req)
	if err != nil {
		return apierrors.ErrCreatePipeline.InvalidParameter(err)
	}
	return nil
}

// setDefault set default value for PipelineCreateRequestV2
func setDefault(req *apistructs.PipelineCreateRequestV2) {
	if req.PipelineYmlName == "" {
		req.PipelineYmlName = apistructs.DefaultPipelineYmlName
	}
}

func logCompatibleFailed(key, value string, err error) {
	logrus.Errorf("compatible from labels failed, key: %s, value: %s, err: %v", key, value, err)
}

func (s *PipelineSvc) makePipelineFromRequestV2(req *apistructs.PipelineCreateRequestV2) (*spec.Pipeline, error) {
	p := &spec.Pipeline{}

	// 解析 pipeline yml 文件，生成最终 pipeline yml 文件
	// 只解析最外层，获取 storage 和 cron 信息
	pipelineYml, err := pipelineyml.New([]byte(req.PipelineYml), pipelineyml.WithEnvs(req.Envs))
	if err != nil {
		return nil, apierrors.ErrParsePipelineYml.InternalError(err)
	}

	p.PipelineYml = req.PipelineYml
	p.PipelineYmlName = req.PipelineYmlName
	p.PipelineSource = req.PipelineSource
	p.ClusterName = req.ClusterName
	p.PipelineDefinitionID = req.DefinitionID
	// labels
	p.NormalLabels = req.NormalLabels
	if p.NormalLabels == nil {
		p.NormalLabels = make(map[string]string)
	}
	p.Labels = req.Labels
	if p.Labels == nil {
		p.Labels = make(map[string]string)
	}
	p.Labels[apistructs.LabelCreateUserID] = req.UserID

	// envs
	p.Snapshot.Envs = req.Envs
	p.Snapshot.RunPipelineParams = req.RunParams.ToPipelineRunParamsWithValue()

	// status
	p.Status = apistructs.PipelineStatusAnalyzed

	// identity
	if req.UserID != "" {
		p.Extra.SubmitUser = s.user.TryGetUser(context.Background(), req.UserID)
	}
	p.Extra.InternalClient = req.InternalClient
	p.Snapshot.IdentityInfo = req.IdentityInfo

	// namespace
	// if upper layer customize namespace, use custom namespace
	// default namespace is pipeline controlled
	if req.Namespace != "" {
		p.Extra.Namespace = req.Namespace
		p.Extra.NotPipelineControlledNs = true
	}

	// 挂载配置
	p.Extra.StorageConfig.EnableNFS = true
	storageConfig := pipelineYml.Spec().Storage
	if storageConfig != nil && storageConfig.Context == "local" {
		p.Extra.StorageConfig.EnableLocal = true
	}
	// 是否全局配置开启流水线挂载
	if conf.DisablePipelineVolume() {
		p.Extra.StorageConfig.EnableNFS = false
		p.Extra.StorageConfig.EnableLocal = false
	}

	// auto run
	p.Extra.IsAutoRun = req.AutoRun

	version, err := pipelineyml.GetVersion([]byte(p.PipelineYml))
	if err != nil {
		return nil, apierrors.ErrParsePipelineYml.InvalidParameter(errors.Errorf("version (%v)", err))
	}
	p.Extra.Version = version

	p.CostTimeSec = -1

	// 解析 labels，填充表字段
	labels := p.MergeLabels()

	// workspace
	p.Extra.DiceWorkspace = apistructs.DiceWorkspace(labels[apistructs.LabelDiceWorkspace])

	// vcs
	if v, ok := labels[apistructs.LabelCommitDetail]; ok {
		var detail apistructs.CommitDetail
		if err := json.Unmarshal([]byte(v), &detail); err != nil {
			logCompatibleFailed(apistructs.LabelCommitDetail, v, err)
		}
		p.CommitDetail = detail
	}
	if v, ok := labels[apistructs.LabelCommit]; ok {
		p.CommitDetail.CommitID = v
	}

	// container instance provider
	extensionItems := make([]string, 0)
	for _, stage := range pipelineYml.Spec().Stages {
		for _, actionMap := range stage.Actions {
			for _, action := range actionMap {
				if action.Type.IsSnippet() {
					continue
				}
				extensionItems = append(extensionItems, s.actionMgr.MakeActionTypeVersion(action))
			}
		}
	}
	_, extensions, err := s.actionMgr.SearchActions(extensionItems, s.actionMgr.MakeActionLocationsBySource(p.PipelineSource))
	if err != nil {
		return nil, apierrors.ErrCreatePipeline.InternalError(err)
	}
	p.Extra.ContainerInstanceProvider = container_provider.ConstructContainerProvider(container_provider.WithLabels(labels),
		container_provider.WithExtensions(extensions))

	// pipelineYmlSource
	p.Extra.PipelineYmlSource = apistructs.PipelineYmlSourceContent
	if v, ok := labels[apistructs.LabelPipelineYmlSource]; ok {
		if !apistructs.PipelineYmlSource(v).Valid() {
			logCompatibleFailed(apistructs.LabelPipelineYmlSource, v, nil)
		}
		p.Extra.PipelineYmlSource = apistructs.PipelineYmlSource(v)
	}

	// pipelineType
	if v, ok := labels[apistructs.LabelPipelineType]; ok {
		if !apistructs.PipelineType(v).Valid() {
			logCompatibleFailed(apistructs.LabelPipelineType, v, nil)
		}
		p.Type = apistructs.PipelineType(v)
	}

	// cronTriggerTime
	if v, ok := labels[apistructs.LabelPipelineCronTriggerTime]; ok {
		nano, err := strconv.ParseInt(v, 10, 64)
		if err != nil {
			return nil, apierrors.ErrCreatePipeline.InvalidParameter(err)
		}
		cronTriggerTime := time.Unix(0, nano)
		p.Extra.CronTriggerTime = &cronTriggerTime
	}

	// configManage
	p.Extra.ConfigManageNamespaces = req.ConfigManageNamespaces

	// secrets
	p.Extra.IncomingSecrets = req.Secrets

	// cron
	p.Extra.CronExpr = pipelineYml.Spec().Cron
	if v, ok := labels[apistructs.LabelPipelineCronID]; ok {
		cronID, err := strconv.ParseUint(v, 10, 64)
		if err != nil {
			return nil, apierrors.ErrCreatePipeline.InvalidParameter(err)
		}
		pc, err := s.pipelineCronSvc.CronGet(context.Background(), &cronpb.CronGetRequest{
			CronID: cronID,
		})
		if err != nil {
			return nil, apierrors.ErrGetPipelineCron.InvalidParameter(err)
		}
		if pc.Data == nil {
			return nil, apierrors.ErrNotFoundPipelineCron.InvalidParameter(crontypes.ErrCronNotFound)
		}
		p.CronID = &pc.Data.ID
		p.Extra.CronExpr = pc.Data.CronExpr
	}

	// triggerMode
	if v, ok := labels[apistructs.LabelPipelineTriggerMode]; ok {
		if !apistructs.PipelineTriggerMode(v).Valid() {
			logCompatibleFailed(apistructs.LabelPipelineTriggerMode, v, nil)
		}
		p.TriggerMode = apistructs.PipelineTriggerMode(v)
	}

	// progress
	p.Progress = -1

	// gc
	p.Extra.GC = req.GC

	if err := s.UpdatePipelineCron(p, req.CronStartFrom, req.ConfigManageNamespaces, pipelineYml.Spec().CronCompensator); err != nil {
		return nil, apierrors.ErrCreatePipeline.InternalError(err)
	}

	// defined outputs
	for _, output := range pipelineYml.Spec().Outputs {
		p.Extra.DefinedOutputs = append(p.Extra.DefinedOutputs,
			apistructs.PipelineOutput{
				Name: output.Name,
				Desc: output.Desc,
				Ref:  output.Ref,
			})
	}

	// queue
	if req.BindQueue != nil {
		customPriority := req.BindQueue.Priority
		customPriorityStr, ok := p.MergeLabels()[apistructs.LabelBindPipelineQueueCustomPriority]
		if ok {
			_customPriority, err := strconv.ParseInt(customPriorityStr, 10, 64)
			if err == nil {
				customPriority = _customPriority
			}
		}
		p.Extra.QueueInfo = &spec.QueueInfo{
			QueueID:          req.BindQueue.ID,
			CustomPriority:   customPriority,
			EnqueueCondition: apistructs.EnqueueConditionType(p.MergeLabels()[apistructs.LabelBindPipelineQueueEnqueueCondition]),
		}
	}

	return p, nil
}

// 非定时触发的，如果有定时配置，需要插入或更新 pipeline_crons enable 配置
// 不管是定时还是非定时，只要定时配置是空的，就将pipeline_crons disable
func (s *PipelineSvc) UpdatePipelineCron(p *spec.Pipeline, cronStartFrom *time.Time, configManageNamespaces []string, cronCompensator *pipelineyml.CronCompensator) error {
	if p.TriggerMode == apistructs.PipelineTriggerModeCron {
		return nil
	}

	var createRequest = constructToCreateCronRequest(p, cronStartFrom, configManageNamespaces)
	result, err := s.pipelineCronSvc.CronCreate(context.Background(), createRequest)
	if err != nil {
		return apierrors.ErrUpdatePipelineCron.InternalError(err)
	}
	// todo CronCreate should be simple, do not contains disable logic.
	//  Add an interface method HandleCron to merge all logic for easy use.
	// When cron create cron express is empty, cron create will execute disable logic, if not find cron by source and ymlName the ID of result may be 0
	if result.Data.ID > 0 {
		p.CronID = &result.Data.ID
	}

	// report
	if s.edgeRegister != nil {
		if s.edgeRegister.IsEdge() {
			s.edgeReporter.TriggerOnceCronReport(*p.CronID)
		}
	}
	return nil
}

func constructToCreateCronRequest(p *spec.Pipeline, cronStartFrom *time.Time, configManageNamespaces []string) *cronpb.CronCreateRequest {
	createReq := &cronpb.CronCreateRequest{
		PipelineSource:         p.PipelineSource.String(),
		PipelineYmlName:        p.PipelineYmlName,
		CronExpr:               p.Extra.CronExpr,
		Enable:                 wrapperspb.Bool(false),
		PipelineYml:            p.PipelineYml,
		ClusterName:            p.ClusterName,
		FilterLabels:           p.Labels,
		NormalLabels:           p.GenerateNormalLabelsForCreateV2(),
		Envs:                   p.Snapshot.Envs,
		ConfigManageNamespaces: configManageNamespaces,
		CronStartFrom: func() *timestamppb.Timestamp {
			if cronStartFrom == nil {
				return nil
			}
			return timestamppb.New(*cronStartFrom)
		}(),
		IncomingSecrets:      p.Extra.IncomingSecrets,
		PipelineDefinitionID: p.PipelineDefinitionID,
	}
	return createReq
}
