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
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/steptasktype"
)

type PutTask struct {
	baseTask `json:",squash"`

	Put       string `json:"put"`
	GetParams Params `json:"get_params,omitempty" mapstructure:"get_params"`
}

func (put *PutTask) GetCustomTaskRunPathArgs() []string {
	return nil
}

func (put *PutTask) GetTimeout() (time.Duration, error) {
	return parseTimeout(put.Timeout)
}

func (put *PutTask) GetStatus() string {
	return put.Status
}

func (put *PutTask) SetStatus(status string) {
	put.Status = status
}

func (put *PutTask) GetTaskParams() Params {
	return put.Params
}

func (put *PutTask) GetVersion() Version {
	return nil
}

func (put *PutTask) GetTaskConfig() TaskConfig {
	return put.TaskConfig
}

func (put *PutTask) GetResource() *Resource {
	return &put.Resource
}

func (put *PutTask) IsDisable() bool {
	if put.Disable == nil {
		return put.Filters.needDisable(put.Branch, put.GlobalEnvs)
	}
	return *put.Disable
}

func (put *PutTask) IsPause() bool {
	if put.Pause != nil {
		return *put.Pause
	}
	return false
}

func (put *PutTask) IsResourceTask() bool {
	return true
}

func (put *PutTask) GetResourceType() string {
	return put.ResourceType
}

func (put *PutTask) GetEnvs() map[string]string {
	return put.Envs
}

// TODO more intelligent check
func (put *PutTask) RequiredContext(y PipelineYml) []string {
	required := make([]string, 0)
	implicitContextEntries := y.metadata.contextMap[put.Name()]
	for _, entry := range implicitContextEntries {
		if entry != put.Name() {
			required = append(required, entry)
		}
	}
	return required
}

func (put *PutTask) OutputToContext() []string {
	return []string{put.Put}
}

func (put *PutTask) Name() string {
	return put.Put
}

func (put *PutTask) Type() steptasktype.StepTaskType {
	return steptasktype.PUT
}

func (put *PutTask) Validate() error {
	if put.Put == "" {
		return errors.New("value of put cannot be null")
	}
	return nil
}
