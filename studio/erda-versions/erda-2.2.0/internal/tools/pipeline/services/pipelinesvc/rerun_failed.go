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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (s *PipelineSvc) RerunFailed(ctx context.Context, req *apistructs.PipelineRerunFailedRequest) (*spec.Pipeline, error) {
	// base pipeline
	origin, err := s.dbClient.GetPipeline(req.PipelineID)
	if err != nil {
		return nil, apierrors.ErrRerunFailedPipeline.InternalError(err)
	}

	if origin.Extra.CompleteReconcilerGC {
		return nil, apierrors.ErrRerunFailedPipeline.InvalidState("cannot rerun, already complete gc")
	}

	if !origin.Status.IsEndStatus() {
		return nil, apierrors.ErrRerunFailedPipeline.InvalidState("cannot rerun, not end status")
	}

	// 寻找上一次的失败节点
	rerunFailedDetail, err := s.dbClient.FindCauseFailedPipelineTasks(origin.ID)
	if err != nil {
		return nil, apierrors.ErrRerunFailedPipeline.InternalError(err)
	}

	p, err := s.makePipelineFromCopy(&origin)
	if err != nil {
		return nil, apierrors.ErrRerunFailedPipeline.InternalError(err)
	}

	// 重试失败节点必须在同一个集群
	if origin.ClusterName != p.ClusterName {
		return nil, apierrors.ErrRerunFailedPipeline.InvalidState(fmt.Sprintf(
			"cannot rerun pipeline in another cluster, before: %s, now: %s", origin.ClusterName, p.ClusterName))
	}

	p.Extra.RerunFailedDetail = &rerunFailedDetail
	if req.UserID != "" {
		p.Extra.SubmitUser = s.user.TryGetUser(ctx, req.UserID)
	}
	p.Type = apistructs.PipelineTypeRerunFailed

	var stages []spec.PipelineStage
	if stages, err = s.CreatePipelineGraph(p); err != nil {
		return nil, apierrors.ErrRerunFailedPipeline.InternalError(err)
	}
	// PreCheck
	_ = s.PreCheck(p, stages, p.GetUserID(), req.AutoRunAtOnce)

	// 立即执行一次
	if req.AutoRunAtOnce {
		if p, err = s.run.RunOnePipeline(ctx, &apistructs.PipelineRunRequest{
			PipelineID:        p.ID,
			IdentityInfo:      req.IdentityInfo,
			PipelineRunParams: origin.Snapshot.RunPipelineParams.ToPipelineRunParams(),
			Secrets:           req.Secrets,
		},
		); err != nil {
			return nil, err
		}
	}

	return p, nil
}
