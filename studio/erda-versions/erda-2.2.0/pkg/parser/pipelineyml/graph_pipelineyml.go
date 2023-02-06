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

package pipelineyml

import (
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
)

// ConvertGraphPipelineYmlContent: YAML(apistructs.PipelineYml) -> YAML(Spec)
func ConvertGraphPipelineYmlContent(data []byte) ([]byte, error) {
	var frontendYmlSpec apistructs.PipelineYml
	if err := yaml.Unmarshal(data, &frontendYmlSpec); err != nil {
		return nil, err
	}
	s := &Spec{}
	s.Version = frontendYmlSpec.Version
	s.Envs = frontendYmlSpec.Envs
	s.Cron = frontendYmlSpec.Cron
	if frontendYmlSpec.CronCompensator != nil {
		s.CronCompensator = &CronCompensator{
			Enable:               frontendYmlSpec.CronCompensator.Enable,
			LatestFirst:          frontendYmlSpec.CronCompensator.LatestFirst,
			StopIfLatterExecuted: frontendYmlSpec.CronCompensator.StopIfLatterExecuted,
		}
	}
	s.Stages = make([]*Stage, 0)
	for _, stage := range frontendYmlSpec.Stages {
		actions := make([]typedActionMap, 0)
		for _, frontendAction := range stage {

			maps := typedActionMap{
				ActionType(frontendAction.Type): &Action{
					Alias:       ActionAlias(frontendAction.Alias),
					Description: frontendAction.Description,
					Version:     frontendAction.Version,
					Params:      frontendAction.Params,
					Image:       frontendAction.Image,
					Commands:    frontendAction.Commands,
					Timeout:     frontendAction.Timeout,
					Disable:     frontendAction.Disable,
					If:          frontendAction.If,
					Loop:        frontendAction.Loop,
					Type:        ActionType(frontendAction.Type),
					Namespaces:  frontendAction.Namespaces,
					Resources: Resources{
						CPU:  frontendAction.Resources.Cpu,
						Mem:  int(frontendAction.Resources.Mem),
						Disk: int(frontendAction.Resources.Disk),
					},
				}}

			if frontendAction.SnippetConfig != nil {
				maps[ActionType(frontendAction.Type)].SnippetConfig = &SnippetConfig{
					Name:   frontendAction.SnippetConfig.Name,
					Source: frontendAction.SnippetConfig.Source,
					Labels: frontendAction.SnippetConfig.Labels,
				}
			}

			for _, cache := range frontendAction.Caches {
				maps[ActionType(frontendAction.Type)].Caches = append(maps[ActionType(frontendAction.Type)].Caches, ActionCache{
					Key:  cache.Key,
					Path: cache.Path,
				})
			}

			if frontendAction.Policy != nil {
				maps[ActionType(frontendAction.Type)].Policy = &Policy{
					Type: frontendAction.Policy.Type,
				}
			}

			actions = append(actions, maps)
		}
		s.Stages = append(s.Stages, &Stage{Actions: actions})
	}

	var pipelineParams []*PipelineParam
	for _, param := range frontendYmlSpec.Params {
		pipelineInput := toPipelineYamlParam(param)
		pipelineParams = append(pipelineParams, pipelineInput)
	}

	var pipelineOutputs []*PipelineOutput
	for _, output := range frontendYmlSpec.Outputs {
		pipelineOutput := toPipelineYamlOutput(output)
		pipelineOutputs = append(pipelineOutputs, pipelineOutput)
	}

	s.Params = pipelineParams
	s.Outputs = pipelineOutputs

	var lifecycle []*NetworkHookInfo
	for _, hookInfo := range frontendYmlSpec.Lifecycle {
		hookInfo := toPipelineYamlHookInfo(hookInfo)
		lifecycle = append(lifecycle, hookInfo)
	}
	s.Lifecycle = lifecycle

	return GenerateYml(s)
}

// ConvertToGraphPipelineYml: YAML(Spec) -> apistructs.PipelineYml
func ConvertToGraphPipelineYml(data []byte) (*apistructs.PipelineYml, error) {

	pipelineYml, err := New(data, WithFlatParams(false))
	if err != nil {
		return nil, err
	}

	params := pipelineYml.Spec().Params
	var pipelineParams []*apistructs.PipelineParam
	if params != nil {
		for _, param := range params {
			pipelineInput := toApiParam(param)
			pipelineParams = append(pipelineParams, pipelineInput)
		}
	}

	outputs := pipelineYml.Spec().Outputs
	var pipelineOutputs []*apistructs.PipelineOutput
	if outputs != nil {
		for _, output := range outputs {
			pipelineOutput := toApiOutput(output)
			pipelineOutputs = append(pipelineOutputs, pipelineOutput)
		}
	}

	var on *apistructs.TriggerConfig
	if pipelineYml.Spec().On != nil {
		merge := pipelineYml.Spec().On.Merge
		push := pipelineYml.Spec().On.Push
		if merge != nil || push != nil {
			on = &apistructs.TriggerConfig{}
			if merge != nil {
				var branches []string
				if merge.Branches != nil {
					branches = merge.Branches
				}
				on.Merge = &apistructs.MergeTrigger{Branches: branches}
			}
			if push != nil {
				var branches, tags []string
				if push.Branches != nil {
					branches = push.Branches
				}
				if push.Tags != nil {
					tags = push.Tags
				}
				on.Push = &apistructs.PushTrigger{
					Branches: branches,
					Tags:     tags,
				}
			}
		}
	}

	triggers := pipelineYml.Spec().Triggers
	var pipelineTriggers []*apistructs.PipelineTrigger
	if triggers != nil {
		for _, trigger := range triggers {
			eventName := trigger.On
			filter := trigger.Filter
			if eventName != "" && filter != nil {
				pipelineTriggers = append(pipelineTriggers, &apistructs.PipelineTrigger{On: eventName, Filter: filter})
			}
		}
	}

	result := &apistructs.PipelineYml{
		Version:     pipelineYml.Spec().Version,
		Envs:        pipelineYml.Spec().Envs,
		Cron:        pipelineYml.Spec().Cron,
		NeedUpgrade: pipelineYml.needUpgrade,
		Params:      pipelineParams,
		Outputs:     pipelineOutputs,
		On:          on,
		Triggers:    pipelineYml.Spec().Triggers,
		CronCompensator: func() *apistructs.CronCompensator {
			if pipelineYml.Spec().CronCompensator == nil {
				return nil
			}
			return &apistructs.CronCompensator{
				Enable:               pipelineYml.Spec().CronCompensator.Enable,
				LatestFirst:          pipelineYml.Spec().CronCompensator.LatestFirst,
				StopIfLatterExecuted: pipelineYml.Spec().CronCompensator.StopIfLatterExecuted,
			}
		}(),
	}

	var lifecycle []*apistructs.NetworkHookInfo
	for _, hookInfo := range pipelineYml.Spec().Lifecycle {
		hook := apistructs.NetworkHookInfo{
			Hook:   hookInfo.Hook,
			Client: hookInfo.Client,
			Labels: hookInfo.Labels,
		}
		lifecycle = append(lifecycle, &hook)
	}
	result.Lifecycle = lifecycle

	if result.NeedUpgrade {
		result.YmlContent = string(pipelineYml.upgradedYmlContent)
	} else {
		graphYmlContent, err := GenerateYml(pipelineYml.s)
		if err != nil {
			return nil, err
		}
		result.YmlContent = string(graphYmlContent)
	}

	for _, stage := range pipelineYml.Spec().Stages {
		stageActions := make([]*apistructs.PipelineYmlAction, 0)
		for _, typedAction := range stage.Actions {
			for _, action := range typedAction {
				resultAction := &apistructs.PipelineYmlAction{}
				resultAction.Type = action.Type.String()
				resultAction.Alias = action.Alias.String()
				resultAction.Version = action.Version
				resultAction.Params = action.Params
				resultAction.Image = action.Image
				resultAction.Commands = action.Commands
				resultAction.Timeout = action.Timeout
				resultAction.Namespaces = action.Namespaces
				resultAction.If = action.If
				resultAction.Disable = action.Disable
				resultAction.Loop = action.Loop
				resultAction.Resources = apistructs.Resources{Cpu: action.Resources.CPU, Mem: float64(action.Resources.Mem), Disk: float64(action.Resources.Disk)}

				caches := action.Caches
				if caches != nil {
					var resultActionCaches []apistructs.ActionCache
					for _, v := range caches {
						resultActionCaches = append(resultActionCaches, apistructs.ActionCache{
							Path: v.Path,
							Key:  v.Key,
						})
					}
					resultAction.Caches = resultActionCaches
				}

				if action.SnippetConfig != nil {
					resultAction.SnippetConfig = action.SnippetConfig.toApiSnippetConfig()
				}

				if action.Policy != nil {
					resultAction.Policy = &apistructs.Policy{
						Type: action.Policy.Type,
					}
				}

				stageActions = append(stageActions, resultAction)
			}
		}
		result.Stages = append(result.Stages, stageActions)
	}

	result.CronCompensator = cronCompensatorReset(result.CronCompensator)
	return result, nil
}

func cronCompensatorReset(cronCompensator *apistructs.CronCompensator) *apistructs.CronCompensator {
	if cronCompensator != nil {
		if cronCompensator.Enable == DefaultCronCompensator.Enable &&
			cronCompensator.LatestFirst == DefaultCronCompensator.LatestFirst &&
			cronCompensator.StopIfLatterExecuted == DefaultCronCompensator.StopIfLatterExecuted {
			return nil
		}
	}
	return cronCompensator
}

func toApiParam(pipelineInput *PipelineParam) (params *apistructs.PipelineParam) {
	return &apistructs.PipelineParam{
		Name:     pipelineInput.Name,
		Required: pipelineInput.Required,
		Default:  pipelineInput.Default,
		Desc:     pipelineInput.Desc,
		Type:     pipelineInput.Type,
	}
}

func toApiOutput(pipelineOutput *PipelineOutput) (outputs *apistructs.PipelineOutput) {
	return &apistructs.PipelineOutput{
		Desc: pipelineOutput.Desc,
		Name: pipelineOutput.Name,
		Ref:  pipelineOutput.Ref,
	}
}

func toPipelineYamlParam(params *apistructs.PipelineParam) (pipelineInput *PipelineParam) {
	return &PipelineParam{
		Name:     params.Name,
		Required: params.Required,
		Default:  params.Default,
		Desc:     params.Desc,
		Type:     params.Type,
	}
}

func toPipelineYamlOutput(outputs *apistructs.PipelineOutput) (pipelineOutput *PipelineOutput) {
	return &PipelineOutput{
		Desc: outputs.Desc,
		Name: outputs.Name,
		Ref:  outputs.Ref,
	}
}

func toPipelineYamlHookInfo(hookInfo *apistructs.NetworkHookInfo) (pipelineHookInfo *NetworkHookInfo) {
	return &NetworkHookInfo{
		Hook:   hookInfo.Hook,
		Client: hookInfo.Client,
		Labels: hookInfo.Labels,
	}
}
