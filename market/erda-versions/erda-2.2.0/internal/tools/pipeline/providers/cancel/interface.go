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

package cancel

import (
	"context"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type Interface interface {
	CancelOnePipeline(ctx context.Context, req *apistructs.PipelineCancelRequest) error
	StopRelatedRunningPipelinesOfOnePipeline(ctx context.Context, p *spec.Pipeline, identityInfo apistructs.IdentityInfo) error
}

func (s *provider) CancelOnePipeline(ctx context.Context, req *apistructs.PipelineCancelRequest) error {
	p, err := s.dbClient.GetPipeline(req.PipelineID)
	if err != nil {
		return apierrors.ErrGetPipeline.InternalError(err)
	}

	// support idempotent cancel
	if p.Status.IsEndStatus() {
		return nil
	}

	// check type and status
	if !p.IsSnippet && !p.Status.CanCancel() {
		return fmt.Errorf("invalid status [%s]", p.Status)
	}

	// set cancel user
	if req.UserID != "" {
		p.Extra.CancelUser = s.User.TryGetUser(ctx, req.UserID)
		if err := s.dbClient.UpdatePipelineExtraExtraInfoByPipelineID(p.ID, p.Extra); err != nil {
			return err
		}
	}

	return s.Engine.DistributedStopPipeline(ctx, p.ID)
}

func (s *provider) StopRelatedRunningPipelinesOfOnePipeline(ctx context.Context, p *spec.Pipeline, identityInfo apistructs.IdentityInfo) error {
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
	for _, runningPipelineID := range runningPipelineIDs {
		if err := s.CancelOnePipeline(ctx, &apistructs.PipelineCancelRequest{
			PipelineID:   runningPipelineID,
			IdentityInfo: identityInfo,
		}); err != nil {
			return err
		}
	}
	return nil
}
