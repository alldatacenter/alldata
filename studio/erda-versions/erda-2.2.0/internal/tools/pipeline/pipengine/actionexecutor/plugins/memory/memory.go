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

package memory

import (
	"context"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor/types"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

const (
	Kind = "MEMORY"
)

func init() {
	types.Register(Kind, func(name types.Name, options map[string]string) (types.ActionExecutor, error) {
		return &Memory{
			name:    name,
			options: options,
		}, nil
	})
}

type Memory struct {
	name    types.Name
	options map[string]string
}

func (m *Memory) Kind() types.Kind {
	return Kind
}

func (m *Memory) Name() types.Name {
	return m.name
}

func (m *Memory) Exist(ctx context.Context, action *spec.PipelineTask) (bool, bool, error) {
	panic("implement me")
}

func (m *Memory) Create(ctx context.Context, action *spec.PipelineTask) (interface{}, error) {
	panic("implement me")
}

func (m *Memory) Start(ctx context.Context, action *spec.PipelineTask) (interface{}, error) {
	panic("implement me")
}

func (m *Memory) Update(ctx context.Context, action *spec.PipelineTask) (interface{}, error) {
	panic("implement me")
}

func (m *Memory) Status(ctx context.Context, action *spec.PipelineTask) (apistructs.PipelineStatusDesc, error) {
	panic("implement me")
}

func (m *Memory) Inspect(ctx context.Context, action *spec.PipelineTask) (apistructs.TaskInspect, error) {
	panic("implement me")
}

func (m *Memory) Cancel(ctx context.Context, action *spec.PipelineTask) (interface{}, error) {
	panic("implement me")
}

func (m *Memory) Remove(ctx context.Context, action *spec.PipelineTask) (interface{}, error) {
	panic("implement me")
}

func (m *Memory) BatchDelete(ctx context.Context, actions []*spec.PipelineTask) (interface{}, error) {
	panic("implement me")
}
