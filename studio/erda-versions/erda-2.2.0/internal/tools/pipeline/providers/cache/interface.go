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

package cache

import (
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/action_info"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

// TODO the cache should be standardized

type Interface interface {
	GetOrSetPipelineRerunSuccessTasksFromContext(pipelineID uint64) (successTasks map[string]*spec.PipelineTask, err error)
	GetOrSetStagesFromContext(pipelineID uint64) (stages []spec.PipelineStage, err error)
	GetOrSetPipelineYmlFromContext(pipelineID uint64) (yml *pipelineyml.PipelineYml, err error)
	GetOrSetPassedDataWhenCreateFromContext(pipelineYml *pipelineyml.PipelineYml, pipeline *spec.Pipeline) (passedDataWhenCreate *action_info.PassedDataWhenCreate, err error)
	ClearReconcilerPipelineContextCaches(pipelineID uint64)
	SetPipelineSecretByPipelineID(pipelineID uint64, secret *SecretCache)
	GetPipelineSecretByPipelineID(pipelineID uint64) (secret *SecretCache)
	ClearPipelineSecretByPipelineID(pipelineID uint64)
}
