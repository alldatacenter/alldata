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

package gantt

import (
	"time"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-gantt/filter"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
)

type ComponentGantt struct {
	sdk      *cptype.SDK
	bdl      *bundle.Bundle
	issueSvc query.Interface

	Data       Data                                  `json:"data,omitempty"`
	Operations map[apistructs.OperationKey]Operation `json:"operations,omitempty"`
	State      State                                 `json:"state,omitempty"`

	projectID uint64   `json:"-"`
	users     []string `json:"-"`
}

type InParams struct {
	ParentIDs []uint64 `json:"parentId"`
	ProjectID string   `json:"projectId"`
}

type Data struct {
	UpdateList []Item            `json:"updateList,omitempty"`
	ExpandList map[uint64][]Item `json:"expandList,omitempty"`
	Refresh    bool              `json:"refresh"`
}

type Operation struct {
	Key      string `json:"key"`
	Reload   bool   `json:"reload"`
	FillMeta string `json:"fillMeta"`
	Async    bool   `json:"async,omitempty"`
	Meta     Meta   `json:"meta"`
}

type OperationData struct {
	FillMeta string `json:"fillMeta"`
	Meta     Meta   `json:"meta"`
}

type Meta struct {
	Nodes NodeItem `json:"nodes,omitempty"`
	Keys  []uint64 `json:"keys,omitempty"`
}

type NodeItem struct {
	Start int64  `json:"start,omitempty"`
	End   int64  `json:"end,omitempty"`
	Key   uint64 `json:"key"`
}

type Item struct {
	Start          *time.Time `json:"start"`
	End            *time.Time `json:"end"`
	Title          string     `json:"title,omitempty"`
	Key            uint64     `json:"key"`
	IsLeaf         bool       `json:"isLeaf"`
	ChildrenLength int        `json:"childrenLength"`
	Extra          Extra      `json:"extra,omitempty"`
}

type Extra struct {
	Type        string `json:"type"`
	User        string `json:"user"`
	Status      Status `json:"status"`
	IterationID int64  `json:"iterationID"`
}

type Status struct {
	Text   string `json:"text"`
	Status string `json:"status"`
}

type State struct {
	Values filter.FrontendConditions `json:"values,omitempty"`
}
