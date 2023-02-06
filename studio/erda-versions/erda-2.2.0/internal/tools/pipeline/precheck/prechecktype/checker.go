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

package prechecktype

import (
	"context"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type PreChecker interface {
	Check(ctx context.Context, data interface{}, itemsForCheck ItemsForCheck) (abort bool, message []string)
}

type ActionPreChecker interface {
	PreChecker
	ActionType() pipelineyml.ActionType
}

type DiceYmlPreChecker interface {
	PreChecker
}

type ItemsForCheck struct {
	// PipelineYml is the pipeline yml content
	PipelineYml string

	// Files include all related files will be used in check process
	Files map[string]string

	// ActionSpecs include all required action specs
	// key: actionType / actionType@actionVersion
	ActionSpecs map[string]apistructs.ActionSpec

	// Labels include all labels declared when create pipeline
	// see: apistructs/labels.go
	Labels map[string]string

	// Envs include all envs:
	// - declared when create pipeline
	Envs map[string]string

	// ClusterName represents which cluster to execute pipeline
	ClusterName string

	Secrets map[string]string

	GlobalSnippetConfigLabels map[string]string
}
