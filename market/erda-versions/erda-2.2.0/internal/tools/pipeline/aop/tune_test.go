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

package aop

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/services/reportsvc"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func init() {
	bdl := bundle.New(bundle.WithAllAvailableClients())
	dbClient, _ := dbclient.New()
	report := reportsvc.New(reportsvc.WithDBClient(dbClient))

	Initialize(bdl, dbClient, report)
}

func TestHandlePipeline(t *testing.T) {
	ctx := NewContextForTask(spec.PipelineTask{ID: 1}, spec.Pipeline{}, aoptypes.TuneTriggerTaskBeforeWait)
	err := Handle(ctx)
	assert.NoError(t, err)

	// pipeline end
	ctx = NewContextForPipeline(spec.Pipeline{
		PipelineBase: spec.PipelineBase{ID: 1},
	}, aoptypes.TuneTriggerPipelineAfterExec)
	err = Handle(ctx)
	assert.NoError(t, err)
}

func TestHandleTask(t *testing.T) {
	// task end
	ctx := NewContextForTask(
		spec.PipelineTask{ID: 1, Status: apistructs.PipelineStatusSuccess},
		spec.Pipeline{
			PipelineBase: spec.PipelineBase{ID: 1},
		}, aoptypes.TuneTriggerTaskAfterExec,
	)
	err := Handle(ctx)
	assert.NoError(t, err)
}
