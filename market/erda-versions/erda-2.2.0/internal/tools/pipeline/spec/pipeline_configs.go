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

package spec

type PipelineConfig struct {
	ID    uint64             `json:"id" xorm:"pk autoincr"`
	Type  PipelineConfigType `json:"type"`
	Value interface{}        `json:"value" xorm:"json"`
}

func (PipelineConfig) TableName() string {
	return "pipeline_configs"
}

type ActionExecutorConfig struct {
	Kind    string            `json:"kind,omitempty"`
	Name    string            `json:"name,omitempty"`
	Options map[string]string `json:"options,omitempty"`
}

func (a *ActionExecutorConfig) IsK8sKind() bool {
	return a.Kind == string(PipelineTaskExecutorKindK8sJob) ||
		a.Kind == string(PipelineTaskExecutorKindK8sFlink) ||
		a.Kind == string(PipelineTaskExecutorKindK8sSpark)
}

type PipelineConfigType string

var (
	PipelineConfigTypeActionExecutor PipelineConfigType = "action_executor"
)

type TaskExecutorConfig struct {
	Kind        string            `json:"kind,omitempty"`
	Name        string            `json:"name,omitempty"`
	ClusterName string            `json:"clusterName,omitempty"`
	Options     map[string]string `json:"options,omitempty"`
}
