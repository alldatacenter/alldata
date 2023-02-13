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
	"time"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	common "github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/crontypes"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

// Rerun commit 不变
func (s *PipelineSvc) Rerun(ctx context.Context, req *apistructs.PipelineRerunRequest) (*spec.Pipeline, error) {

	origin, err := s.dbClient.GetPipeline(req.PipelineID)
	if err != nil {
		return nil, apierrors.ErrRerunPipeline.InternalError(err)
	}

	var originCron *common.Cron
	if origin.CronID != nil {
		result, err := s.pipelineCronSvc.CronGet(ctx, &cronpb.CronGetRequest{
			CronID: *origin.CronID,
		})
		if err != nil {
			return nil, apierrors.ErrRerunPipeline.InternalError(err)
		}
		if result.Data == nil {
			return nil, apierrors.ErrNotFoundPipelineCron.InternalError(crontypes.ErrCronNotFound)
		}
		originCron = result.Data
	}

	if origin.Labels == nil {
		origin.Labels = map[string]string{}
	}
	origin.Labels[apistructs.LabelPipelineType] = apistructs.PipelineTypeRerun.String()

	p, err := s.CreateV2(ctx, &apistructs.PipelineCreateRequestV2{
		PipelineYml:            origin.PipelineYml,
		ClusterName:            origin.ClusterName,
		PipelineYmlName:        origin.PipelineYmlName,
		RunParams:              origin.Snapshot.RunPipelineParams.ToPipelineRunParams(),
		PipelineSource:         origin.PipelineSource,
		Labels:                 origin.Labels,
		NormalLabels:           origin.GenerateNormalLabelsForCreateV2(),
		Envs:                   origin.Snapshot.Envs,
		ConfigManageNamespaces: origin.GetConfigManageNamespaces(),
		AutoRunAtOnce:          req.AutoRunAtOnce,
		AutoStartCron:          false,
		CronStartFrom: func() *time.Time {
			if originCron == nil {
				return nil
			}
			cronStartFrom := originCron.Extra.CronStartFrom.AsTime()
			return &cronStartFrom
		}(),
		IdentityInfo: req.IdentityInfo,
		DefinitionID: origin.PipelineDefinitionID,
		Secrets:      req.Secrets,
	})
	if err != nil {
		return nil, err
	}

	p.Extra.CopyFromPipelineID = &origin.ID

	if err := s.dbClient.UpdatePipelineExtraExtraInfoByPipelineID(p.ID, p.Extra); err != nil {
		return nil, apierrors.ErrUpdatePipeline.InternalError(err)
	}

	return p, nil
}
