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
	"reflect"

	"github.com/ghodss/yaml"
	"github.com/mitchellh/mapstructure"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/pipelineymlvars"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/steptasktype"
)

func (tc TaskConfig) decodeAggregateStepTasks() ([]StepTask, error) {
	var agg aggregateTask
	d, err := mapstructureJsonDecoder(&agg)
	if err != nil {
		return nil, err
	}
	if err = d.Decode(tc); err != nil {
		return nil, err
	}
	var stepTasks []StepTask
	for _, stepTC := range agg.Aggregate {
		// 判断 steptask 类型
		_, getOK := stepTC[pipelineymlvars.FieldGet.String()]
		_, putOK := stepTC[pipelineymlvars.FieldPut.String()]
		_, taskOK := stepTC[pipelineymlvars.FieldTask.String()]

		// steptask 均不匹配
		if !getOK && !putOK && !taskOK {
			return nil, errors.Wrapf(errInvalidStepTaskConfig, "%+v", stepTC)
		}
		// 同时匹配多个的情况由具体的 decoder.ErrorUnused 自己保证
		// put 类型可能隐含一个 get，所以不能简单地在外层判断，需要由 steptask 自己判断

		switch true {
		case getOK:
			get, err := stepTC.decodeSingleStepTask(steptasktype.GET)
			if err != nil {
				return nil, err
			}
			stepTasks = append(stepTasks, get)
		case putOK:
			put, err := stepTC.decodeSingleStepTask(steptasktype.PUT)
			if err != nil {
				return nil, err
			}
			stepTasks = append(stepTasks, put)
		case taskOK:
			task, err := stepTC.decodeSingleStepTask(steptasktype.TASK)
			if err != nil {
				return nil, err
			}
			stepTasks = append(stepTasks, task)
		default:
			return nil, errors.Errorf("invalid task config, content: %v, type: %v\n", tc, reflect.TypeOf(tc))
		}
	}
	return stepTasks, nil
}

func (tc TaskConfig) decodeSingleStepTask(typ steptasktype.StepTaskType) (StepTask, error) {
	var step StepTask
	switch typ {
	case steptasktype.GET:
		step = &GetTask{}
	case steptasktype.PUT:
		step = &PutTask{}
	case steptasktype.TASK:
		step = &CustomTask{}
	default:
		return nil, errors.Errorf("invalid step task type: %v", typ)
	}
	d, err := mapstructureJsonDecoder(step)
	if err != nil {
		return nil, err
	}
	if err = d.Decode(tc); err != nil {
		return nil, err
	}
	switch typ {
	case steptasktype.GET:
		step.(*GetTask).TaskConfig = tc
	case steptasktype.PUT:
		step.(*PutTask).TaskConfig = tc
	case steptasktype.TASK:
		step.(*CustomTask).TaskConfig = tc
	default:
		return nil, errors.Errorf("invalid step task type: %v", typ)
	}
	return step, nil
}

func (tc TaskConfig) decodeStepTaskWithValidate(typ steptasktype.StepTaskType, y *PipelineYml) (StepTask, error) {
	step, err := tc.decodeSingleStepTask(typ)
	if err != nil {
		return nil, err
	}
	switch typ {
	case steptasktype.GET:
		get := step.(*GetTask)
		res, find := y.FindResourceByName(get.Get)
		if !find {
			return nil, errors.Wrap(errInvalidResource, get.Get)
		}
		get.ResourceType = res.Type
		get.GlobalEnvs = y.obj.Envs
		get.Branch = y.option.branch
		get.Resource = res
		get.Status = "Born"
	case steptasktype.PUT:
		put := step.(*PutTask)
		res, find := y.FindResourceByName(put.Put)
		if !find {
			return nil, errors.Wrap(errInvalidResource, put.Put)
		}
		put.ResourceType = res.Type
		put.GlobalEnvs = y.obj.Envs
		put.Branch = y.option.branch
		put.Resource = res
		put.Status = "Born"
	case steptasktype.TASK:
		task := step.(*CustomTask)
		task.GlobalEnvs = y.obj.Envs
		task.Branch = y.option.branch
		task.Status = "Born"
	default:
		return nil, errors.Errorf("invalid step task type: %v", typ)
	}
	if err = step.Validate(); err != nil {
		return nil, err
	}
	if step.IsPause() {
		step.SetStatus("Paused")
	}
	return step, nil
}

func mapstructureJsonDecoder(result interface{}, decodeConfig ...mapstructure.DecoderConfig) (*mapstructure.Decoder, error) {
	var config mapstructure.DecoderConfig
	if len(decodeConfig) > 0 {
		config = decodeConfig[0]
	}
	config.Result = result
	config.ErrorUnused = true
	config.TagName = "json"
	return mapstructure.NewDecoder(&config)
}

func convertObjectToTaskConfig(obj interface{}) (TaskConfig, error) {
	newByte, err := yaml.Marshal(&obj)
	if err != nil {
		return nil, err
	}
	var newMap TaskConfig
	if err = yaml.Unmarshal(newByte, &newMap); err != nil {
		return nil, err
	}
	return newMap, nil
}
