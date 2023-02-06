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

package aoptypes

import (
	"context"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/services/reportsvc"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

type TuneContext struct {
	context.Context
	SDK SDK
}

type SDK struct {
	Bundle   *bundle.Bundle
	DBClient *dbclient.Client
	Report   *reportsvc.ReportSvc

	TuneType    TuneType
	TuneTrigger TuneTrigger

	Pipeline spec.Pipeline
	Task     spec.PipelineTask
}

func (sdk SDK) Clone() SDK {
	return SDK{
		Bundle:   sdk.Bundle,
		DBClient: sdk.DBClient,
		Report:   sdk.Report,
	}
}

const (
	CtxKeyTasks = iota
)

func (ctx *TuneContext) PutKV(k, v interface{}) {
	ctx.Context = context.WithValue(ctx.Context, k, v)
}

func (ctx *TuneContext) TryGet(k interface{}) (interface{}, bool) {
	v := ctx.Context.Value(k)
	if v == nil {
		return nil, false
	}
	return v, true
}
