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

	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1/steptasktype"
)

type StepTask interface {
	Type() steptasktype.StepTaskType
	Name() string
	GetEnvs() map[string]string
	Validate() error
	OutputToContext() []string
	// includes required resources and explicit context
	RequiredContext(y PipelineYml) []string

	IsResourceTask() bool
	// if IsResourceTask, GetResourceType will return resource type name.
	GetResourceType() string

	IsDisable() bool
	IsPause() bool

	GetStatus() string
	SetStatus(status string)

	GetTaskParams() Params
	GetTaskConfig() TaskConfig
	GetCustomTaskRunPathArgs() []string
	GetResource() *Resource
	GetVersion() Version

	GetTimeout() (time.Duration, error)
}
