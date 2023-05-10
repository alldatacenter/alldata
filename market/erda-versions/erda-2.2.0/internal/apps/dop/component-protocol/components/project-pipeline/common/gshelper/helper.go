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

package gshelper

import (
	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

type GSHelper struct {
	gs *cptype.GlobalStateData
}

type TableFilter struct {
	Status            []string `json:"status"`
	Creator           []string `json:"creator"`
	App               []string `json:"app"`
	Executor          []string `json:"executor"`
	CreatedAtStartEnd []int64  `json:"createdAtStartEnd"`
	StartedAtStartEnd []int64  `json:"startedAtStartEnd"`
	Title             string   `json:"title"`
	Branch            []string `json:"branch"`
}

func NewGSHelper(gs *cptype.GlobalStateData) *GSHelper {
	return &GSHelper{gs: gs}
}

func assign(src, dst interface{}) error {
	if src == nil || dst == nil {
		return nil
	}

	return mapstructure.Decode(src, dst)
}

func (h *GSHelper) SetGlobalPipelineTab(t string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalPipelineTab"] = t
}

func (h *GSHelper) GetGlobalPipelineTab() string {
	if h.gs == nil {
		return ""
	}
	var t string
	_ = assign((*h.gs)["GlobalPipelineTab"], &t)
	return t
}

func (h *GSHelper) SetGlobalTableFilter(t TableFilter) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalTableFilter"] = t
}

func (h *GSHelper) GetGlobalTableFilter() *TableFilter {
	if h.gs == nil {
		return nil
	}
	var t TableFilter
	_ = assign((*h.gs)["GlobalTableFilter"], &t)
	return &t
}

func (h *GSHelper) SetGlobalMyAppNames(appNames []string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalMyAppNames"] = appNames
}

func (h *GSHelper) GetGlobalMyAppNames() []string {
	if h.gs == nil {
		return nil
	}
	var appNames []string
	_ = assign((*h.gs)["GlobalMyAppNames"], &appNames)
	return appNames
}
