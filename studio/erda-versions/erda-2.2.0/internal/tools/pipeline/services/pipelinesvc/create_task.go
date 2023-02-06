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
	"encoding/json"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/action_info"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/numeral"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

// makeNormalPipelineTask 生成普通流水线任务
func (s *PipelineSvc) makeNormalPipelineTask(p *spec.Pipeline, ps *spec.PipelineStage, action *pipelineyml.Action, passedDataWhenCreate *action_info.PassedDataWhenCreate) *spec.PipelineTask {

	task := &spec.PipelineTask{}
	task.PipelineID = p.ID
	task.StageID = ps.ID
	task.Name = action.Alias.String()
	// task.OpType
	task.Type = action.Type.String()
	task.Extra.Namespace = p.Extra.Namespace
	task.Extra.NotPipelineControlledNs = p.Extra.NotPipelineControlledNs
	task.Extra.ClusterName = p.ClusterName
	task.Extra.AllowFailure = false
	task.Extra.Pause = false
	task.Extra.ContainerInstanceProvider = p.Extra.ContainerInstanceProvider
	task.Extra.Timeout = time.Duration(action.Timeout * int64(time.Second))
	if action.Timeout < 0 {
		task.Extra.Timeout = time.Duration(action.Timeout)
	}
	task.Extra.StageOrder = ps.Extra.StageOrder
	// task.Extra.Envs
	// task.Extra.Labels
	// task.Extra.Image
	task.Extra.Action = *action

	// set executor
	executorKind, executorName := s.judgeTaskExecutor(task, passedDataWhenCreate.GetActionJobSpecs(s.actionMgr.MakeActionTypeVersion(action)))
	task.ExecutorKind = executorKind
	task.Extra.ExecutorName = executorName

	// default task resource limit
	task.Extra.RuntimeResource = spec.RuntimeResource{
		CPU:    conf.TaskDefaultCPU(),
		Memory: conf.TaskDefaultMEM(),
		Disk:   0,
	}
	// task.Extra.SelfInputs
	// task.Extra.SelfOutputs
	if isFlinkSparkAction(action.Type.String()) {
		task.Extra.FlinkSparkConf = spec.FlinkSparkConf{
			Depend:    getString(action.Params["depends"]),
			MainClass: getString(action.Params["main_class"]),
			MainArgs:  []string{getString(action.Params["main_args"])},
		}
	}

	// runAfter
	for _, need := range action.Needs {
		task.Extra.RunAfter = append(task.Extra.RunAfter, need.String())
	}

	task.Status = apistructs.PipelineStatusAnalyzed
	if task.Extra.Pause {
		task.Status = apistructs.PipelineStatusPaused
	}
	// if action is disabled, set task status disabled directly
	if action.Disable {
		task.Status = apistructs.PipelineStatusDisabled
	}
	task.CostTimeSec = -1
	task.QueueTimeSec = -1

	// 给 task 设置上 snippet action 定制的 env
	if action.SnippetConfig != nil && action.SnippetConfig.Labels != nil {
		actionEnv := action.SnippetConfig.Labels[apistructs.LabelActionEnv]
		var actionEnvLabels = map[string]string{}
		err := json.Unmarshal([]byte(actionEnv), &actionEnvLabels)
		if err == nil {
			for key, v := range actionEnvLabels {
				if task.Extra.PrivateEnvs == nil {
					task.Extra.PrivateEnvs = map[string]string{}
				}
				task.Extra.PrivateEnvs[key] = v
			}
		} else {
			logrus.Errorf("error load action %v snippetConfig", action)
		}
	}

	// applied resources
	task.Extra.AppliedResources = calculateNormalTaskResources(action, passedDataWhenCreate.GetActionJobDefine(s.actionMgr.MakeActionTypeVersion(action)))

	return task
}

// makeSnippetPipelineTask 生成嵌套流水线任务
// action: 从 yaml 解析出来的 action 信息
// p: 当前层的 pipeline，已先于 task 创建好
// stage: stage 信息，已先于 task 创建好
func (s *PipelineSvc) makeSnippetPipelineTask(p *spec.Pipeline, stage *spec.PipelineStage, action *pipelineyml.Action) *spec.PipelineTask {
	var task spec.PipelineTask
	task.PipelineID = p.ID
	task.StageID = stage.ID
	task.Name = action.Alias.String()
	task.Type = apistructs.ActionTypeSnippet
	task.ExecutorKind = spec.PipelineTaskExecutorKindScheduler
	task.Status = apistructs.PipelineStatusAnalyzed

	// if snippet action is disabled, set task status disabled directly
	if action.Disable {
		task.Status = apistructs.PipelineStatusDisabled
	}

	// extra
	task.Extra = s.genSnippetTaskExtra(p, action)

	// snippet
	task.IsSnippet = true

	task.CostTimeSec = -1
	task.QueueTimeSec = -1

	// ext resources set outside after created

	return &task
}

func (s *PipelineSvc) genSnippetTaskExtra(p *spec.Pipeline, action *pipelineyml.Action) spec.PipelineTaskExtra {
	var ex spec.PipelineTaskExtra
	ex.Namespace = p.Extra.Namespace
	ex.NotPipelineControlledNs = p.Extra.NotPipelineControlledNs
	ex.ExecutorName = spec.PipelineTaskExecutorNameEmpty
	ex.ClusterName = p.ClusterName
	ex.AllowFailure = false
	ex.Pause = false
	ex.Timeout = s.calculateTaskTimeoutDuration(action)
	ex.PrivateEnvs = nil
	ex.PublicEnvs = nil
	ex.Labels = nil
	ex.RuntimeResource = spec.GenDefaultTaskResource()
	ex.RunAfter = s.calculateTaskRunAfter(action)
	ex.Action = *action
	ex.ContainerInstanceProvider = p.Extra.ContainerInstanceProvider
	return ex
}

func (s *PipelineSvc) calculateTaskTimeoutDuration(action *pipelineyml.Action) time.Duration {
	if action.Timeout == pipelineyml.TimeoutDuration4Forever {
		return pipelineyml.TimeoutDuration4Forever
	}
	return time.Duration(action.Timeout * int64(time.Second))
}

func (s *PipelineSvc) calculateTaskRunAfter(action *pipelineyml.Action) []string {
	var runAfters []string
	for _, need := range action.Needs {
		runAfters = append(runAfters, need.String())
	}
	return runAfters
}

// judgeTaskExecutor judge task executor by action info
func (s *PipelineSvc) judgeTaskExecutor(task *spec.PipelineTask, actionSpec *apistructs.ActionSpec) (spec.PipelineTaskExecutorKind, spec.PipelineTaskExecutorName) {
	if actionSpec == nil ||
		actionSpec.Executor == nil ||
		len(actionSpec.Executor.Kind) <= 0 ||
		len(actionSpec.Executor.Name) <= 0 ||
		!spec.PipelineTaskExecutorKind(actionSpec.Executor.Kind).Check() ||
		!spec.PipelineTaskExecutorName(actionSpec.Executor.Name).Check() {
		kind := spec.PipelineTaskExecutorKindK8sJob
		if bigData, err := task.GetBigDataConf(); err == nil {
			if bigData.FlinkConf != nil {
				kind = spec.PipelineTaskExecutorKindK8sFlink
			}
			if bigData.SparkConf != nil {
				kind = spec.PipelineTaskExecutorKindK8sSpark
			}
		}
		return kind, kind.GenExecutorNameByClusterName(task.Extra.ClusterName)
	}
	// if specify executor k8s kind, add the cluster name to executor name
	if actionSpec.Executor.Kind == spec.PipelineTaskExecutorKindK8sJob.String() ||
		actionSpec.Executor.Kind == spec.PipelineTaskExecutorKindK8sFlink.String() ||
		actionSpec.Executor.Kind == spec.PipelineTaskExecutorKindK8sSpark.String() {
		kind := spec.PipelineTaskExecutorKind(actionSpec.Executor.Kind)
		return kind, kind.GenExecutorNameByClusterName(task.Extra.ClusterName)
	}

	return spec.PipelineTaskExecutorKind(actionSpec.Executor.Kind), spec.PipelineTaskExecutorName(actionSpec.Executor.Name)
}

func calculateNormalTaskResources(action *pipelineyml.Action, actionDefine *diceyml.Job) apistructs.PipelineAppliedResources {
	defaultRes := apistructs.PipelineAppliedResource{CPU: conf.TaskDefaultCPU(), MemoryMB: conf.TaskDefaultMEM()}
	overSoldRes := apistructs.PipelineOverSoldResource{CPURate: conf.TaskDefaultCPUOverSoldRate(), MaxCPU: conf.TaskMaxAllowedOverSoldCPU()}
	return apistructs.PipelineAppliedResources{
		Limits:   calculateOversoldTaskLimitResource(calculateNormalTaskLimitResource(action, actionDefine, defaultRes), overSoldRes),
		Requests: calculateNormalTaskRequestResource(action, actionDefine, defaultRes),
	}
}

// calculateOversoldTaskLimitResource cpu multiply the default oversold rate. if larger than max cpu default,use default max cpu
// TODO memory oversold
func calculateOversoldTaskLimitResource(limits apistructs.PipelineAppliedResource, overSoldRes apistructs.PipelineOverSoldResource) apistructs.PipelineAppliedResource {
	maxCPU := limits.CPU
	maxMemoryMB := limits.MemoryMB
	// Cpu is usually be wasted, if action and action define cpu is lower than default, use default cpu
	maxCPU = maxCPU * float64(overSoldRes.CPURate)
	if maxCPU > overSoldRes.MaxCPU {
		maxCPU = overSoldRes.MaxCPU
	}
	return apistructs.PipelineAppliedResource{
		CPU:      maxCPU,
		MemoryMB: maxMemoryMB,
	}
}

func calculateNormalTaskLimitResource(action *pipelineyml.Action, actionDefine *diceyml.Job, defaultRes apistructs.PipelineAppliedResource) apistructs.PipelineAppliedResource {
	// Calculate if actionDefine not empty
	var maxCPU, maxMemoryMB float64
	if actionDefine != nil {
		maxCPU = numeral.MaxFloat64([]float64{
			actionDefine.Resources.MaxCPU, actionDefine.Resources.CPU,
			action.Resources.MaxCPU, action.Resources.CPU,
		})
		maxMemoryMB = numeral.MaxFloat64([]float64{
			float64(actionDefine.Resources.MaxMem), float64(actionDefine.Resources.Mem),
			float64(action.Resources.Mem),
		})
	}

	// use default if is empty
	if maxCPU == 0 {
		maxCPU = defaultRes.CPU
	}
	if maxMemoryMB == 0 {
		maxMemoryMB = defaultRes.MemoryMB
	}

	return apistructs.PipelineAppliedResource{
		CPU:      maxCPU,
		MemoryMB: maxMemoryMB,
	}
}

func calculateNormalTaskRequestResource(action *pipelineyml.Action, actionDefine *diceyml.Job, defaultRes apistructs.PipelineAppliedResource) apistructs.PipelineAppliedResource {
	// calculate if requestCPU not empty
	var requestCPU, requestMemoryMB float64
	if actionDefine != nil {
		requestCPU = numeral.MinFloat64([]float64{actionDefine.Resources.MaxCPU, actionDefine.Resources.CPU}, true)
		requestMemoryMB = numeral.MinFloat64([]float64{float64(actionDefine.Resources.MaxMem), float64(actionDefine.Resources.Mem)}, true)
	}

	// user explicit declaration has the highest priority, overwrite value from actionDefine
	if c := numeral.MinFloat64([]float64{action.Resources.MaxCPU, action.Resources.CPU}, true); c > 0 {
		requestCPU = c
	}
	if m := action.Resources.Mem; m > 0 {
		requestMemoryMB = float64(m)
	}

	// use default if is empty
	if requestCPU == 0 {
		requestCPU = defaultRes.CPU
	}
	if requestMemoryMB == 0 {
		requestMemoryMB = defaultRes.MemoryMB
	}

	return apistructs.PipelineAppliedResource{
		CPU:      requestCPU,
		MemoryMB: requestMemoryMB,
	}
}
