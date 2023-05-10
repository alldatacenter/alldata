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

package pipelineymlv1

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/pipelineymlvars"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/steptasktype"
)

type TaskUpdateParams struct {
	Envs           *map[string]string // 环境变量
	Disable        *bool              // 是否禁用
	ForceBuildpack *bool              // 是否强制打包
	Pause          *bool              // 是否暂停

}

// UpdatePipelineOnTask update pipeline object and byteData together.
func (y *PipelineYml) UpdatePipelineOnTask(expectUUID string, params TaskUpdateParams) error {

	for si, stage := range y.obj.Stages {
		for configIndex, config := range stage.TaskConfigs {
			stepTasks, isAggregate, err := y.taskConfig2StepTasks(config)
			if err != nil {
				return err
			}
			for ti, step := range stepTasks {
				tmpUUID := GenerateTaskUUID(si, stage.Name, ti, step.Name(), y.metadata.instanceID)
				if tmpUUID == expectUUID {
					newTaskConfig, err := config.foundTaskConfigSnippetAndUpdate(isAggregate, ti, step.Type(), params)
					if err != nil {
						return err
					}
					stage.TaskConfigs[configIndex] = newTaskConfig
					newYmlByte, err := y.YAML()
					if err != nil {
						return err
					}
					y.byteData = []byte(newYmlByte)
					if err = y.Parse(); err != nil {
						return err
					}
					return nil
				}
			}
		}
	}

	return errors.Errorf("no matched taskConfig found for task uuid: %s", expectUUID)
}

func (tc TaskConfig) foundTaskConfigSnippetAndUpdate(isAggregate bool, taskIndex int, typ steptasktype.StepTaskType, params TaskUpdateParams) (newTC TaskConfig, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = errors.Errorf("%v", r)
		}
	}()

	// found task config snippet
	if isAggregate {
		var agg aggregateTask
		aggDecoder, err := mapstructureJsonDecoder(&agg)
		if err != nil {
			return nil, err
		}
		if err = aggDecoder.Decode(tc); err != nil {
			return nil, errors.Wrap(err, "decode aggregate")
		}
		innerTC := agg.Aggregate[taskIndex]
		newTC, err = innerTC.updateSingleTaskConfig(typ, params)
		if err != nil {
			return nil, err
		}
		agg.Aggregate[taskIndex] = newTC
		newTc, err := convertObjectToTaskConfig(&agg)
		if err != nil {
			return nil, err
		}
		return newTc, nil
	} else {
		newTC, err = tc.updateSingleTaskConfig(typ, params)
		if err != nil {
			return nil, err
		}
		return newTC, nil
	}
}

func (tc TaskConfig) updateSingleTaskConfig(typ steptasktype.StepTaskType, params TaskUpdateParams) (newTC TaskConfig, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = errors.Errorf("%v", r)
		}
	}()

	switch typ {
	case steptasktype.GET:
		getStep, err := tc.decodeSingleStepTask(steptasktype.GET)
		if err != nil {
			return nil, err
		}
		get := getStep.(*GetTask)
		if params.Envs != nil {
			get.Envs = *params.Envs
		}
		if params.Disable != nil {
			get.Disable = params.Disable
		}
		if params.Pause != nil {
			get.Pause = params.Pause
		}
		newTC, err = convertObjectToTaskConfig(&get)
		if err != nil {
			return nil, err
		}

	case steptasktype.PUT:
		putStep, err := tc.decodeSingleStepTask(steptasktype.PUT)
		if err != nil {
			return nil, err
		}
		put := putStep.(*PutTask)
		if params.Envs != nil {
			put.Envs = *params.Envs
		}
		if params.Disable != nil {
			put.Disable = params.Disable
		}
		if params.ForceBuildpack != nil {
			if put.Params == nil {
				put.Params = make(Params)
			}
			put.Params[pipelineymlvars.FieldParamForceBuildpack.String()] = params.ForceBuildpack
		}
		if params.Pause != nil {
			put.Pause = params.Pause
		}
		newTC, err = convertObjectToTaskConfig(&put)
		if err != nil {
			return nil, err
		}

	case steptasktype.TASK:
		taskStep, err := tc.decodeSingleStepTask(steptasktype.TASK)
		if err != nil {
			return nil, err
		}
		task := taskStep.(*CustomTask)
		if params.Envs != nil {
			task.Config.Envs = *params.Envs
		}
		if params.Disable != nil {
			task.Disable = params.Disable
		}
		if params.Pause != nil {
			task.Pause = params.Pause
		}
		newTC, err = convertObjectToTaskConfig(&task)
		if err != nil {
			return nil, err
		}

	default:
		return nil, errors.Errorf("invalid StepTaskType: %s", typ)
	}

	return newTC, nil
}
