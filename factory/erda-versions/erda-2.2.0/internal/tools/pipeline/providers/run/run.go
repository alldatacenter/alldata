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

package run

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/container_provider"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/definition/db"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/time/mysql_time"
)

func (s *provider) RunOnePipeline(ctx context.Context, req *apistructs.PipelineRunRequest) (*spec.Pipeline, error) {
	p, err := s.dbClient.GetPipeline(req.PipelineID)
	if err != nil {
		return nil, apierrors.ErrGetPipeline.InvalidParameter(err)
	}

	if req.UserID != "" {
		p.Extra.RunUser = s.User.TryGetUser(ctx, req.UserID)
	}
	if req.InternalClient != "" {
		p.Extra.InternalClient = req.InternalClient
	}

	reason, canManualRun := s.CanManualRun(ctx, &p)
	if !canManualRun {
		return nil, apierrors.ErrRunPipeline.InvalidState(reason)
	}
	if req.ForceRun {
		err := s.Cancel.StopRelatedRunningPipelinesOfOnePipeline(ctx, &p, req.IdentityInfo)
		if err != nil {
			return nil, err
		}
	} else {
		// 校验已运行的 pipeline
		if err := s.limitParallelRunningPipelines(&p); err != nil {
			return nil, err
		}
	}

	p.Extra.ConfigManageNamespaces = append(p.Extra.ConfigManageNamespaces, req.ConfigManageNamespaces...)

	var (
		secrets, cmsDiceFiles         map[string]string
		holdOnKeys, encryptSecretKeys []string
		platformSecrets               map[string]string
	)
	secretCache := s.Cache.GetPipelineSecretByPipelineID(p.PipelineID)
	defer s.Cache.ClearPipelineSecretByPipelineID(p.PipelineID)
	// only autoRun can use cache
	if secretCache != nil {
		secrets = secretCache.Secrets
		cmsDiceFiles = secretCache.CmsDiceFiles
		holdOnKeys = secretCache.HoldOnKeys
		encryptSecretKeys = secretCache.EncryptSecretKeys
	} else {
		// fetch secrets
		secrets, cmsDiceFiles, holdOnKeys, encryptSecretKeys, err = s.Secret.FetchSecrets(ctx, &p)
		if err != nil {
			return nil, apierrors.ErrRunPipeline.InternalError(err)
		}
	}

	// fetch platform secrets
	platformSecrets, err = s.Secret.FetchPlatformSecrets(ctx, &p, holdOnKeys)
	if err != nil {
		return nil, apierrors.ErrRunPipeline.InternalError(err)
	}

	for k, v := range req.Secrets {
		secrets[k] = v
	}
	// replace global config use same random value
	for k, v := range secrets {
		secrets[k] = expression.ReplaceRandomParams(v)
	}

	// 校验私有配置转换出来的 envs
	secretsEnvs := make(map[string]string)
	for k, v := range secrets {
		newK := strings.Replace(strings.Replace(strings.ToUpper(k), ".", "_", -1), "-", "_", -1)
		secretsEnvs[newK] = v
	}
	errs := pipelineyml.CheckEnvs(secretsEnvs)
	if len(errs) > 0 {
		var errMsgs []string
		for _, checkErr := range errs {
			errMsgs = append(errMsgs, checkErr.Error())
		}
		return nil, apierrors.ErrCheckSecrets.InvalidParameter(strutil.Join(errMsgs, "\n", true))
	}

	// Snapshot 快照用于记录
	p.Snapshot.PipelineYml = p.PipelineYml
	p.Snapshot.Secrets = secrets
	p.Snapshot.PlatformSecrets = platformSecrets
	p.Snapshot.CmsDiceFiles = cmsDiceFiles
	// pipeline 运行时的参数
	runParams, err := getRealRunParams(req.PipelineRunParams, p.PipelineYml)
	if err != nil {
		return nil, err
	}
	p.Snapshot.RunPipelineParams = runParams.ToPipelineRunParamsWithValue()
	p.Snapshot.EncryptSecretKeys = encryptSecretKeys

	now := time.Now()
	p.TimeBegin = &now

	cluster, err := s.ClusterInfo.GetClusterInfoByName(p.ClusterName)
	if err != nil {
		return nil, apierrors.ErrRunPipeline.InternalError(err)
	}
	container_provider.DealPipelineProviderBeforeRun(&p, cluster.CM)
	// update pipeline base
	if err := s.dbClient.UpdatePipelineBase(p.ID, &p.PipelineBase); err != nil {
		return nil, apierrors.ErrUpdatePipeline.InternalError(err)
	}

	// update pipeline extra
	if err := s.dbClient.UpdatePipelineExtraByPipelineID(p.ID, &p.PipelineExtra); err != nil {
		return nil, apierrors.ErrRunPipeline.InternalError(err)
	}

	// create pipeline labels
	if err = s.createPipelineRunLabels(p, req); err != nil {
		return nil, apierrors.ErrRunPipeline.InternalError(err)
	}

	// aop
	_ = aop.Handle(aop.NewContextForPipeline(p, aoptypes.TuneTriggerPipelineBeforeExec))

	// send to pipengine reconciler
	s.Engine.DistributedSendPipeline(context.Background(), p.ID)

	// update pipeline definition
	if err = s.updatePipelineDefinition(p); err != nil {
		s.Log.Errorf("failed to updatePipelineDefinition, pipelineID: %d, definitionID: %s, err: %s", p.PipelineID, p.PipelineDefinitionID, err.Error())
	}

	return &p, nil
}

func (s *provider) createPipelineRunLabels(p spec.Pipeline, req *apistructs.PipelineRunRequest) (err error) {
	labels := make([]spec.PipelineLabel, 0)
	if req.UserID != "" {
		labels = append(labels, spec.PipelineLabel{
			ID:              uuid.SnowFlakeIDUint64(),
			Type:            apistructs.PipelineLabelTypeInstance,
			TargetID:        p.ID,
			PipelineSource:  p.PipelineSource,
			PipelineYmlName: p.PipelineYmlName,
			Key:             apistructs.LabelRunUserID,
			Value:           req.UserID,
		})
	}
	if len(labels) > 0 {
		err = s.dbClient.BatchInsertLabels(labels)
	}
	return err
}

func (s *provider) updatePipelineDefinition(p spec.Pipeline) error {
	if p.PipelineDefinitionID == "" {
		return nil
	}
	var (
		definition     *db.PipelineDefinition
		totalActionNum int64
		err            error
	)
	definition, err = s.dbClient.GetPipelineDefinition(p.PipelineDefinitionID)
	if err != nil {
		return err
	}
	totalActionNum, err = pipelineyml.CountEnabledActionNumByPipelineYml(p.PipelineYml)
	if err != nil {
		return err
	}
	definition.TotalActionNum = totalActionNum
	definition.Status = string(apistructs.StatusRunning)
	definition.Executor = p.GetUserID()
	definition.EndedAt = *mysql_time.GetMysqlDefaultTime()
	definition.PipelineID = p.PipelineID
	if p.Type != apistructs.PipelineTypeRerunFailed {
		definition.ExecutedActionNum = -1
		definition.StartedAt = time.Now()
		definition.CostTime = -1
	}
	return s.dbClient.UpdatePipelineDefinition(definition.ID, definition)
}

func getRealRunParams(runParams []apistructs.PipelineRunParam, yml string) (result apistructs.PipelineRunParams, err error) {

	pipeline, err := pipelineyml.New([]byte(yml))
	if err != nil {
		return nil, apierrors.ErrRunPipeline.InternalError(err)
	}

	var runParamsMap = make(map[string]apistructs.PipelineRunParam)
	if runParams != nil {
		for _, runParam := range runParams {
			runParamsMap[runParam.Name] = runParam
		}
	}

	// 获取真实的运行时参数
	var realParamsMap = make(map[string]interface{})
	for _, param := range pipeline.Spec().Params {
		// 用户没有传 key, 且默认值不为空
		runValue, ok := runParamsMap[param.Name]

		if runValue.Value == nil && param.Default == nil && param.Required && ok {
			return nil, apierrors.ErrRunPipeline.InternalError(fmt.Errorf("pipeline param %s value is empty", param.Name))
		}

		if runValue.Value == nil && param.Default != nil {
			realParamsMap[param.Name] = param.Default
		}

		if runValue.Value == nil && param.Default == nil {
			realParamsMap[param.Name] = pipelineyml.GetParamDefaultValue(param.Type)
		}

		if runValue.Value != nil {
			realParamsMap[param.Name] = runValue.Value
		}

		if realParamsMap[param.Name] == nil {
			return nil, apierrors.ErrRunPipeline.InternalError(fmt.Errorf("pipeline param %s value is empty", param.Name))
		}
	}

	for key, v := range realParamsMap {
		result = append(result, apistructs.PipelineRunParam{
			Name:  key,
			Value: v,
		})
	}

	return result, nil
}

// limitParallelRunningPipelines 判断在 pipelineSource + pipelineYmlName 下只能有一个在运行
// 被嵌套的流水线跳过校验
func (s *provider) limitParallelRunningPipelines(p *spec.Pipeline) error {
	if p.CanSkipRunningCheck() {
		logrus.Infof("pipeline: %d skiped limit parallel running, enqueue condition: %s",
			p.ID, p.GetLabel(apistructs.LabelBindPipelineQueueEnqueueCondition))
		return nil
	}
	// 流水线自身是嵌套流水线时，不做校验
	if p.IsSnippet {
		return nil
	}
	var runningPipelineIDs []uint64
	err := s.dbClient.Table(&spec.PipelineBase{}).
		Select("id").In("status", apistructs.ReconcilerRunningStatuses()).
		Where("is_snippet = ?", false).
		Find(&runningPipelineIDs, &spec.PipelineBase{
			PipelineSource:  p.PipelineSource,
			PipelineYmlName: p.PipelineYmlName,
		})
	if err != nil {
		return apierrors.ErrParallelRunPipeline.InternalError(err)
	}
	if len(runningPipelineIDs) > 0 {
		ctxMap := map[string]interface{}{
			apierrors.ErrParallelRunPipeline.Error(): fmt.Sprintf("%d", runningPipelineIDs[0]),
		}
		return apierrors.ErrParallelRunPipeline.InvalidState("ErrParallelRunPipeline").SetCtx(ctxMap)
	}
	return nil
}
