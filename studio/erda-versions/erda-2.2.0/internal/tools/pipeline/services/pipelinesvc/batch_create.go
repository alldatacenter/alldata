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
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (s *PipelineSvc) BatchCreate(ctx context.Context, batchReq *apistructs.PipelineBatchCreateRequest) (
	map[string]*apistructs.PipelineDTO, error) {

	// convert pipelineBatchCreateRequest to []pipelineCreateRequest
	var reqs []*apistructs.PipelineCreateRequest
	for _, ymlPath := range batchReq.BatchPipelineYmlPaths {
		reqs = append(reqs, &apistructs.PipelineCreateRequest{
			AppID:             batchReq.AppID,
			Branch:            batchReq.Branch,
			Source:            batchReq.Source,
			PipelineYmlName:   ymlPath,
			CallbackURLs:      batchReq.CallbackURLs,
			AutoRun:           batchReq.AutoRun,
			PipelineYmlSource: apistructs.PipelineYmlSourceGittar, // 通过批量接口创建暂时只需要支持 gittar 类型
			UserID:            batchReq.UserID,
		})
	}

	result := make(map[string]*apistructs.PipelineDTO)

	for _, req := range reqs {
		p, err := s.makePipelineFromRequest(req)
		if err != nil {
			return nil, apierrors.ErrBatchCreatePipeline.InternalError(err)
		}
		var stages []spec.PipelineStage
		if stages, err = s.CreatePipelineGraph(p); err != nil {
			return nil, apierrors.ErrBatchCreatePipeline.InternalError(err)
		}

		// PreCheck
		_ = s.PreCheck(p, stages, p.GetUserID(), batchReq.AutoRun)

		identityInfo := apistructs.IdentityInfo{UserID: req.UserID}

		// 是否自动执行
		if batchReq.AutoRun {
			if p, err = s.run.RunOnePipeline(ctx, &apistructs.PipelineRunRequest{
				PipelineID:   p.ID,
				IdentityInfo: identityInfo,
				Secrets:      getSecrets(p)},
			); err != nil {
				return nil, apierrors.ErrRunPipeline.InternalError(err)
			}
		}
		result[req.PipelineYmlName] = s.ConvertPipeline(p)
	}

	return result, nil
}

// getSecrets Compatible with bigdata application
func getSecrets(p *spec.Pipeline) map[string]string {
	return map[string]string{
		"gittar.repo":   p.CommitDetail.Repo,
		"gittar.branch": p.Labels[apistructs.LabelBranch],
		"gittar.commit": p.CommitDetail.CommitID,
		"gittar.commit.abbrev": func() string {
			if len(p.CommitDetail.CommitID) > 8 {
				return p.CommitDetail.CommitID[:8]
			}
			return p.CommitDetail.CommitID
		}(),
		"gittar.message": p.CommitDetail.Comment,
		"gittar.author":  p.CommitDetail.Author,
	}
}
