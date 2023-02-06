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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/precheck"
	"github.com/erda-project/erda/internal/tools/pipeline/precheck/prechecktype"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cache"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func (s *PipelineSvc) PreCheck(p *spec.Pipeline, stages []spec.PipelineStage, userID string, autoRun bool) error {
	pipelineYml, err := pipelineyml.New(
		[]byte(p.PipelineYml),
	)
	if err != nil {
		return err
	}

	tasks, err := s.MergePipelineYmlTasks(pipelineYml, nil, p, stages, nil)
	if err != nil {
		return apierrors.ErrPreCheckPipeline.InternalError(err)
	}

	// ItemsForCheck
	itemsForCheck := prechecktype.ItemsForCheck{
		PipelineYml:               p.PipelineYml,
		Files:                     make(map[string]string),
		ActionSpecs:               make(map[string]apistructs.ActionSpec),
		Labels:                    p.MergeLabels(),
		Envs:                      p.Snapshot.Envs,
		ClusterName:               p.ClusterName,
		GlobalSnippetConfigLabels: p.Labels,
	}

	// 从 extension marketplace 获取 action
	extSearchReq := make([]string, 0)
	actionTypeVerMap := make(map[string]struct{})
	for _, task := range tasks {
		if task.Type == apistructs.ActionTypeSnippet {
			continue
		}
		if task.Status.IsDisabledStatus() {
			continue
		}
		typeVersion := task.Extra.Action.GetActionTypeVersion()
		if _, ok := actionTypeVerMap[typeVersion]; ok {
			continue
		}
		actionTypeVerMap[typeVersion] = struct{}{}
		extSearchReq = append(extSearchReq, typeVersion)
	}
	_, actionSpecs, err := s.actionMgr.SearchActions(extSearchReq, s.actionMgr.MakeActionLocationsBySource(p.PipelineSource))
	if err != nil {
		return apierrors.ErrPreCheckPipeline.InternalError(err)
	}
	for typeVersion, actionSpec := range actionSpecs {
		if actionSpec != nil {
			itemsForCheck.ActionSpecs[typeVersion] = *actionSpec
		}
	}

	if p.Extra.StorageConfig.EnableShareVolume() {
		for _, task := range tasks {
			typeVersion := task.Extra.Action.GetActionTypeVersion()
			value, exist := actionSpecs[typeVersion].Labels["new_workspace"]
			if exist && value == "true" {
				// action带有new_workspace标签,使用独立目录
				p.Extra.TaskWorkspaces = append(p.Extra.TaskWorkspaces, task.Name)
			}
		}
		err = s.dbClient.UpdatePipelineExtraByPipelineID(p.ID, &p.PipelineExtra)
		if err != nil {
			return apierrors.ErrPreCheckPipeline.InternalError(err)
		}
	}

	// secrets
	secrets, cmsDiceFiles, holdOnKeys, encryptSecretKeys, err := s.secret.FetchSecrets(context.Background(), p)
	if err != nil {
		return apierrors.ErrPreCheckPipeline.InternalError(err)
	}
	platformSecrets, err := s.secret.FetchPlatformSecrets(context.Background(), p, holdOnKeys)
	if err != nil {
		return apierrors.ErrPreCheckPipeline.InternalError(err)
	}
	itemsForCheck.Secrets = platformSecrets
	for k, v := range secrets {
		itemsForCheck.Secrets[k] = v
	}

	precheckCtx := prechecktype.InitContext()
	abort, showMessage := precheck.PreCheck(precheckCtx, []byte(p.PipelineYml), itemsForCheck)
	if len(showMessage.Stacks) > 0 {
		if err := s.dbClient.UpdatePipelineShowMessage(p.ID, showMessage); err != nil {
			return apierrors.ErrPreCheckPipeline.InternalError(err)
		}
	}
	if abort {
		return apierrors.ErrPreCheckPipeline.InvalidParameter("precheck failed")
	}

	analyzedCrossCluster, ok := prechecktype.GetContextResult(precheckCtx, prechecktype.CtxResultKeyCrossCluster).(bool)
	if ok {
		p.Snapshot.AnalyzedCrossCluster = &analyzedCrossCluster
		if err := s.dbClient.StoreAnalyzedCrossCluster(p.ID, analyzedCrossCluster); err != nil {
			return apierrors.ErrPreCheckPipeline.InternalError(err)
		}
	}

	if autoRun {
		s.cache.SetPipelineSecretByPipelineID(p.PipelineID, &cache.SecretCache{
			Secrets:           secrets,
			CmsDiceFiles:      cmsDiceFiles,
			HoldOnKeys:        holdOnKeys,
			EncryptSecretKeys: encryptSecretKeys,
		})
	}

	return nil
}
