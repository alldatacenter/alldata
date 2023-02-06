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

package actionmgr

import (
	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type Interface interface {
	pb.ActionServiceServer
	SearchActions(items []string, locations []string, ops ...OpOption) (map[string]*diceyml.Job, map[string]*apistructs.ActionSpec, error)
	MakeActionTypeVersion(action *pipelineyml.Action) string
	MakeActionLocationsBySource(source apistructs.PipelineSource) []string
}

type SearchOption struct {
	NeedRender   bool
	Placeholders map[string]string
}
type OpOption func(*SearchOption)

func SearchOpWithRender(placeholders map[string]string) OpOption {
	return func(so *SearchOption) {
		so.NeedRender = true
		so.Placeholders = placeholders
	}
}
