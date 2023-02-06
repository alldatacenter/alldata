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

package stackChart

import (
	"context"
	"encoding/json"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

type StackChart struct {
	issueSvc       query.Interface
	InParams       InParams                                 `json:"-"`
	Type           string                                   `json:"type"`
	Props          Props                                    `json:"props"`
	Issues         []dao.IssueItem                          `json:"-"`
	StateMap       map[uint64]string                        `json:"-"`
	StatesTransMap map[time.Time][]dao.IssueStateTransition `json:"-"`
	DateMap        map[time.Time]map[uint64]int             `json:"-"`
	Dates          []time.Time                              `json:"-"`
	Itr            apistructs.Iteration                     `json:"-"`
	States         []dao.IssueState                         `json:"-"`
}

type InParams struct {
	FrontEndProjectID string `json:"projectId,omitempty"`
	FrontendUrlQuery  string `json:"filter__urlQuery,omitempty"`
	ProjectID         uint64
}

type (
	Props struct {
		ChartType string `json:"chartType"`
		Title     string `json:"title"`
		PureChart bool   `json:"pureChart"`
		Option    Option `json:"option"`
	}

	Option struct {
		Grid     map[string]interface{} `json:"grid"`
		XAxis    XAxis                  `json:"xAxis"`
		YAxis    YAxis                  `json:"yAxis"`
		Legend   Legend                 `json:"legend"`
		DataZoom []DataZoom             `json:"dataZoom"`
		Tooltip  map[string]interface{} `json:"tooltip"`
		Color    []string               `json:"color"`
		Series   []Series               `json:"series"`
	}

	XAxis struct {
		Type string   `json:"type"`
		Data []string `json:"data"`
	}

	YAxis struct {
		Type      string                 `json:"type"`
		AxisLine  map[string]interface{} `json:"axisLine"`
		AxisLabel map[string]interface{} `json:"axisLabel"`
	}

	Legend struct {
		Show   bool     `json:"show"`
		Bottom bool     `json:"bottom"`
		Data   []string `json:"data"`
	}

	DataZoom struct {
		Type   string `json:"type"`
		Start  int    `json:"start"`
		End    int    `json:"end"`
		Height int    `json:"height"`
		Bottom int    `json:"bottom"`
	}

	Series struct {
		Data      []int                  `json:"data"`
		Name      string                 `json:"name"`
		Stack     string                 `json:"stack"`
		Type      string                 `json:"type"`
		Smooth    bool                   `json:"smooth"`
		Symbol    string                 `json:"symbol"`
		AreaStyle map[string]interface{} `json:"areaStyle"`
		LineStyle map[string]interface{} `json:"lineStyle"`
	}
)

func (f *StackChart) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, &c)
}

func (f *StackChart) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(b, f); err != nil {
		return err
	}

	f.issueSvc = ctx.Value(types.IssueService).(query.Interface)
	f.DateMap = make(map[time.Time]map[uint64]int, 0)
	f.Dates = make([]time.Time, 0)
	return f.setInParams(ctx)
}

func (f *StackChart) setInParams(ctx context.Context) error {
	b, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &f.InParams); err != nil {
		return err
	}

	f.InParams.ProjectID, err = strconv.ParseUint(f.InParams.FrontEndProjectID, 10, 64)
	return err
}
