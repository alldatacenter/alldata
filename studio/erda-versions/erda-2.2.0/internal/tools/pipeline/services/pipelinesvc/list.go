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
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (s *PipelineSvc) List(condition apistructs.PipelinePageListRequest) (*apistructs.PipelinePageListData, error) {
	pagingResult, err := s.dbClient.PageListPipelines(condition)
	if err != nil {
		return nil, apierrors.ErrListPipeline.InternalError(err)
	}
	pipelines := pagingResult.Pipelines
	total := pagingResult.Total
	currentPageSize := pagingResult.CurrentPageSize

	var result apistructs.PipelinePageListData
	result.Pipelines = s.BatchConvert2PagePipeline(pipelines)
	result.Total = total
	result.CurrentPageSize = currentPageSize

	return &result, nil
}

// calculateProgress 根据 pipeline tasks 计算 progress
// 若 progress < 0，认为进度还没最终确认，包括 running 或 还没计算过 的情况；
// 若 progress >= 0，认为进度已经计算完毕（包括 0），直接返回
func (s *PipelineSvc) calculateProgress(p spec.Pipeline) (int, error) {

	if p.Progress >= 0 {
		// pipeline 为成功状态，progress 不应该为 0，需要重新计算
		if p.Progress == 0 && p.Status.IsSuccessStatus() {
			// calculate progress
			goto CalculateStatus
		}
		// progress >= 0，直接返回
		return p.Progress, nil
	}

CalculateStatus:
	needStoreToDB := false
	if p.Status.IsEndStatus() {
		needStoreToDB = true
	}

	// calculate pipeline progress
	tasks, err := s.dbClient.ListPipelineTasksByPipelineID(p.ID)
	if err != nil {
		return -1, err
	}
	var successCount int
	for _, t := range tasks {
		if t.Status.IsSuccessStatus() {
			successCount++
		}
	}
	var progress int
	if len(tasks) == 0 { // 存在 task 为 0 的情况
		progress = 100
	} else {
		progress = (successCount / len(tasks)) * 100
	}

	if needStoreToDB {
		go func() {
			// 异步更新 pipeline progress
			err := s.dbClient.UpdatePipelineProgress(p.ID, progress)
			if err != nil {
				logrus.Errorf("[alert] failed to update pipeline progress, pipelineID: %d, progress: %d, err: %v",
					p.ID, progress, err)
			}
		}()
	}
	return progress, nil
}
