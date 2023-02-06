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

package common

const (
	ScenarioKey          = "project-pipeline"
	DefaultPageSize      = 10
	ColumnPipelineStatus = "pipelineStatus"
)

type StateValue string

func (s StateValue) String() string {
	return string(s)
}

const (
	MineState    StateValue = "mine"
	PrimaryState StateValue = "primary"
	AllState     StateValue = "all"
)

var DefaultState = MineState

type Sort struct {
	FieldKey  string
	Ascending bool
}

const (
	Participated = "participated"
)

var DefaultBranch = []string{"master", "develop"}
