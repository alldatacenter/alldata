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

package daemon

import (
	"context"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/db"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type CreatePipelineFunc func(ctx context.Context, req *apistructs.PipelineCreateRequestV2) (*spec.Pipeline, error)

type Interface interface {
	AddIntoPipelineCrond(cron *db.PipelineCron) error
	DeleteFromPipelineCrond(cron *db.PipelineCron) error
	ReloadCrond(ctx context.Context) ([]string, error)
	CrondSnapshot() []string

	// todo Can be removed after all objects are provider
	WithPipelineFunc(createPipelineFunc CreatePipelineFunc)
}
