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

package dbgc

import (
	"context"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type Interface interface {
	GetPipelineIncludeArchived(ctx context.Context, pipelineID uint64) (pipeline spec.Pipeline, exit bool, findFromArchive bool, err error)
	GetPipelineTasksIncludeArchived(ctx context.Context, pipelineID uint64) (tasks []spec.PipelineTask, findFromArchive bool, err error)
}
