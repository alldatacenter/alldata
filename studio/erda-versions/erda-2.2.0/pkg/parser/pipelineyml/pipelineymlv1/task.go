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
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/steptasktype"
)

type CustomTask struct {
	Task    string      `json:"task"`
	Config  Config      `json:"config"`
	Filters Filters     `json:"filters,omitempty"`
	Disable *bool       `json:"disable,omitempty"`
	Timeout interface{} `json:"timeout,omitempty"`
	Pause   *bool       `json:"pause,omitempty"`

	GlobalEnvs map[string]string `json:"-"`
	Branch     string            `json:"-"`
	TaskConfig TaskConfig        `json:"-"`
	Status     string            `json:"-"`
}

type Config struct {
	ImageResource ImageResource      `json:"image_resource,omitempty" mapstructure:"image_resource"`
	Inputs        []TaskInputConfig  `json:"inputs,omitempty"`
	Outputs       []TaskOutputConfig `json:"outputs,omitempty"`
	Envs          map[string]string  `json:"envs,omitempty"`
	Run           Run                `json:"run"`
}

type ImageResource struct {
	Type   string `json:"type"`
	Source Source `json:"source"`
}

type Run struct {
	Path string   `json:"path"`
	Args []string `json:"args"`
}

func (t *CustomTask) GetCustomTaskRunPathArgs() []string {
	return append([]string{t.Config.Run.Path}, t.Config.Run.Args...)
}

func (t *CustomTask) GetTimeout() (time.Duration, error) {
	return parseTimeout(t.Timeout)
}

func (t *CustomTask) GetStatus() string {
	return t.Status
}

func (t *CustomTask) SetStatus(status string) {
	t.Status = status
}

func (t *CustomTask) GetTaskParams() Params {
	return nil
}

func (t *CustomTask) GetVersion() Version {
	return nil
}

func (t *CustomTask) GetTaskConfig() TaskConfig {
	return t.TaskConfig
}

func (t *CustomTask) GetResource() *Resource {
	return nil
}

func (t *CustomTask) IsDisable() bool {
	if t.Disable == nil {
		return t.Filters.needDisable(t.Branch, t.GlobalEnvs)
	}
	return *t.Disable
}

func (t *CustomTask) IsPause() bool {
	if t.Pause != nil {
		return *t.Pause
	}
	return false
}

func (t *CustomTask) IsResourceTask() bool {
	return false
}

func (t *CustomTask) GetResourceType() string {
	return ""
}

func (t *CustomTask) GetEnvs() map[string]string {
	return t.Config.Envs
}

func (t *CustomTask) RequiredContext(y PipelineYml) []string {
	result := make([]string, 0)
	for _, input := range t.Config.Inputs {
		result = append(result, input.Name)
	}
	return result
}

func (t *CustomTask) OutputToContext() []string {
	outResources := make([]string, 0)
	for _, output := range t.Config.Outputs {
		outResources = append(outResources, output.Name)
	}
	return outResources
}

func (t *CustomTask) Name() string {
	return t.Task
}

func (t *CustomTask) Type() steptasktype.StepTaskType {
	return steptasktype.TASK
}

type TaskInputConfig struct {
	Name string `json:"name" yaml:"name"`
	//Path string `json:"path" yaml:"path"`
	//Optional bool   `json:"optional" yaml:"optional"`
}

type TaskOutputConfig struct {
	Name string `json:"name" yaml:"name"`
	Path string `json:"path" yaml:"path"`
}

func (t *CustomTask) Resources() []string {
	ress := make([]string, 0)
	for _, input := range t.Config.Inputs {
		ress = append(ress, input.Name)
	}
	return ress
}

func (t *CustomTask) Validate() error {
	if t.Task == "" {
		return errors.New("task name cannot be null")
	}
	if reflect.DeepEqual(t.Config, Config{}) {
		return errors.New("task config cannot be null")
	}
	if t.Config.ImageResource.Type != DockerImageResType {
		return errors.Errorf("not supported image type: [%v], only support: [%v]", t.Config.ImageResource.Type, DockerImageResType)
	}
	if _, ok := t.Config.ImageResource.Source["repository"]; !ok {
		return errors.Errorf("no repository field found in image_resource")
	}
	if t.Config.Run.Path == "" {
		return errors.New("task run path cannot be null")
	}
	return nil
}
