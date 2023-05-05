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

type baseTask struct {
	Params  Params            `json:"params,omitempty"`
	Version Version           `json:"version,omitempty"`
	Envs    map[string]string `json:"envs,omitempty"`
	Filters Filters           `json:"filters,omitempty"`
	Disable *bool             `json:"disable,omitempty"`
	Timeout interface{}       `json:"timeout,omitempty"`
	Pause   *bool             `json:"pause,omitempty"`

	ResourceType string            `json:"-"`
	GlobalEnvs   map[string]string `json:"-"`
	Branch       string            `json:"-"`
	Resource     Resource          `json:"-"`
	TaskConfig   TaskConfig        `json:"-"`
	Status       string            `json:"-"`
}

type GetTask struct {
	baseTask `json:",squash"`

	Get string `json:"get"`
}

func (get *GetTask) GetCustomTaskRunPathArgs() []string {
	return nil
}

func (get *GetTask) GetTimeout() (time.Duration, error) {
	return parseTimeout(get.Timeout)
}

func (get *GetTask) GetStatus() string {
	return get.Status
}

func (get *GetTask) SetStatus(status string) {
	get.Status = status
}

func (get *GetTask) GetTaskParams() Params {
	return get.Params
}

func (get *GetTask) GetVersion() Version {
	return get.Version
}

func (get *GetTask) GetParams() Params {
	return get.Params
}

func (get *GetTask) GetTaskConfig() TaskConfig {
	return get.TaskConfig
}

func (get *GetTask) GetResource() *Resource {
	return &get.Resource
}

func (get *GetTask) IsDisable() bool {
	if get.Disable == nil {
		return get.Filters.needDisable(get.Branch, get.GlobalEnvs)
	}
	return *get.Disable
}

func (get *GetTask) IsPause() bool {
	if get.Pause != nil {
		return *get.Pause
	}
	return false
}

func (get *GetTask) IsResourceTask() bool {
	return true
}

func (get *GetTask) GetResourceType() string {
	return get.ResourceType
}

// TODO more intelligent check
func (get *GetTask) RequiredContext(y PipelineYml) []string {
	return []string{}
}

func (get *GetTask) OutputToContext() []string {
	return []string{get.Get}
}

func (get *GetTask) Type() steptasktype.StepTaskType {
	return steptasktype.GET
}

func (get *GetTask) Name() string {
	return get.Get
}

func (get *GetTask) GetEnvs() map[string]string {
	return get.Envs
}

func (get *GetTask) Validate() error {
	if get.Get == "" {
		return errors.New("value of get cannot be null")
	}
	return nil
}

type GetRequest struct {
	Source  Source  `json:"source"`
	Params  Params  `json:"params"`
	Version Version `json:"version"`
}
