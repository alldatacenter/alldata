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

package scatterPlot

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

type ComponentAction struct {
	sdk   *cptype.SDK
	bdl   *bundle.Bundle
	State State `json:"state,omitempty"`
	Props `json:"Props,omitempty"`

	IssueList []dao.IssueItem `json:"-"`
}

type State struct {
	Values               common.FilterConditions `json:"values,omitempty"`
	Base64UrlQueryParams string                  `json:"issueFilter__urlQuery,omitempty"`
}

type Props struct {
	Title     string `json:"title,omitempty"`
	ChartType string `json:"chartType,omitempty"`
	Option    Option `json:"option,omitempty"`
}

type Option struct {
	XAxis  common.XAxis `json:"xAxis,omitempty"`
	YAxis  common.YAxis `json:"yAxis,omitempty"`
	Series []Series     `json:"series,omitempty"`
	Grid   common.Grid  `json:"grid,omitempty"`
}

type Series struct {
	Name      string           `json:"name,omitempty"`
	Type      string           `json:"type,omitempty"`
	Data      [][]float32      `json:"data,omitempty"`
	MarkPoint common.MarkPoint `json:"markPoint,omitempty"`
	MarkLine  common.MarkLine  `json:"markLine,omitempty"`
}
