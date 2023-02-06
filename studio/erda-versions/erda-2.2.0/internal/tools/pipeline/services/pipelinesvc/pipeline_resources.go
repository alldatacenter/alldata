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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/action_info"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/numeral"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

// calculatePipelineResources calculate pipeline resources according to all tasks grouped by stages.
func (s *PipelineSvc) calculatePipelineResources(pipelineYml *pipelineyml.PipelineYml, p *spec.Pipeline) (*apistructs.PipelineAppliedResources, error) {
	if pipelineYml.Spec() == nil || len(pipelineYml.Spec().Stages) <= 0 {
		return nil, nil
	}

	// load pipelineYml all action define and spec
	var passedDataWhenCreate action_info.PassedDataWhenCreate
	passedDataWhenCreate.InitData(s.bdl, s.actionMgr)
	if err := passedDataWhenCreate.PutPassedDataByPipelineYml(pipelineYml, p); err != nil {
		return nil, err
	}

	// summarize the resources required for all actions
	var stagesPipelineAppliedResources = make([][]*apistructs.PipelineAppliedResources, len(pipelineYml.Spec().Stages))
	pipelineYml.Spec().LoopStagesActions(func(stage int, action *pipelineyml.Action) {
		if !action.Type.IsSnippet() {
			resources := calculateNormalTaskResources(action, passedDataWhenCreate.GetActionJobDefine(s.actionMgr.MakeActionTypeVersion(action)))
			stagesPipelineAppliedResources[stage] = append(stagesPipelineAppliedResources[stage], &resources)
		}
	})

	// merge result
	pipelineResource := apistructs.PipelineAppliedResources{
		Limits:   calculatePipelineLimitResource(stagesPipelineAppliedResources),
		Requests: calculatePipelineRequestResource(stagesPipelineAppliedResources),
	}

	return &pipelineResource, nil
}

// calculatePipelineLimitResource calculate pipeline limit resource according to all tasks grouped by stages.
//
// calculate maxResource
// 1 2 (3)
// 2 3 (5)
// 4   (4)
// => max((1+2), (2+3), (4)) = 5
func calculatePipelineLimitResource(resources [][]*apistructs.PipelineAppliedResources) apistructs.PipelineAppliedResource {
	var (
		allStageCPU   []float64
		allStageMemMB []float64
	)
	for _, stagesResources := range resources {
		var (
			stageCPU   float64 = 0
			stageMemMB float64 = 0
		)
		for _, taskResources := range stagesResources {
			stageCPU += taskResources.Limits.CPU
			stageMemMB += taskResources.Limits.MemoryMB
		}
		allStageCPU = append(allStageCPU, stageCPU)
		allStageMemMB = append(allStageMemMB, stageMemMB)
	}
	maxStageResource := apistructs.PipelineAppliedResource{
		CPU:      numeral.MaxFloat64(allStageCPU),
		MemoryMB: numeral.MaxFloat64(allStageMemMB),
	}

	return maxStageResource
}

// calculatePipelineRequestResource calculate pipeline request resource according to all tasks grouped by stages.
//
// calculate minResource
// 1 2 (2)
// 2 3 (3)
// 4   (4)
// => max(1,2,2,3,4) = 4
func calculatePipelineRequestResource(resources [][]*apistructs.PipelineAppliedResources) apistructs.PipelineAppliedResource {
	var allMinTaskCPUs []float64
	var allMinTaskMemMBs []float64
	for _, stagesResources := range resources {
		for _, taskResources := range stagesResources {
			allMinTaskCPUs = append(allMinTaskCPUs, taskResources.Requests.CPU)
			allMinTaskMemMBs = append(allMinTaskMemMBs, taskResources.Requests.MemoryMB)
		}
	}
	minStageResource := apistructs.PipelineAppliedResource{
		CPU:      numeral.MaxFloat64(allMinTaskCPUs),
		MemoryMB: numeral.MaxFloat64(allMinTaskMemMBs),
	}

	return minStageResource
}
