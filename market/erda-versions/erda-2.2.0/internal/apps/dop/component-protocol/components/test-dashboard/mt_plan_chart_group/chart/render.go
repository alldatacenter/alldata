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

package chart

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "mt_plan_chart",
		func() servicehub.Provider { return &Chart{} })
}

type Chart struct {
}

type (
	Props struct {
		ChartType string `json:"chartType"`
		Option    Option `json:"option"`
		PureChart bool   `json:"pureChart"`
	}
	Option struct {
		Color  []string     `json:"color"`
		Legend Legend       `json:"legend"`
		Series []SeriesItem `json:"series"`
		XAxis  Axis         `json:"xAxis"`
		YAxis  Axis         `json:"yAxis"`
	}
	Legend struct {
		Show bool `json:"show"`
	}
	SeriesItem struct {
		Data  []string `json:"data"`
		Label Label    `json:"label"`
		Name  string   `json:"name"`
		Stack string   `json:"stack"`
	}
	Label struct {
		Show bool `json:"show"`
	}
	Axis struct {
		Type string   `json:"type"`
		Data []string `json:"data"`
	}
)

type (
	CategoryLine struct {
		Name string
		Data CategoryLineData
	}
	CategoryLineData struct {
		NumSucc  uint64
		NumBlock uint64
		NumFail  uint64
		NumInit  uint64
	}
)

func (ch *Chart) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	h := gshelper.NewGSHelper(gs)
	mtPlans := h.GetMtPlanChartFilterTestPlanList()
	//statuses := h.GetMtPlanChartFilterStatusList()

	var table []CategoryLine
	for _, plan := range mtPlans {
		table = append(table, CategoryLine{
			Name: plan.Name,
			Data: CategoryLineData{
				NumSucc:  plan.RelsCount.Succ,
				NumBlock: plan.RelsCount.Block,
				NumFail:  plan.RelsCount.Fail,
				NumInit:  plan.RelsCount.Init,
			},
		})
	}

	c.Props = cputil.MustConvertProps(tableToBarChartProps(ctx, table))

	return nil
}

func tableToBarChartProps(ctx context.Context, table []CategoryLine) Props {
	return Props{
		ChartType: "bar",
		Option: Option{
			Color:  []string{"green", "orange", "red", "grey"},
			Legend: Legend{Show: true},
			Series: func() (series []SeriesItem) {
				succ := SeriesItem{
					Label: Label{Show: true},
					Name:  cputil.I18n(ctx, string(apistructs.CaseExecStatusSucc)),
					Stack: "total",
				}
				block := SeriesItem{
					Label: Label{Show: true},
					Name:  cputil.I18n(ctx, string(apistructs.CaseExecStatusBlocked)),
					Stack: "total",
				}
				fail := SeriesItem{
					Label: Label{Show: true},
					Name:  cputil.I18n(ctx, string(apistructs.CaseExecStatusFail)),
					Stack: "total",
				}
				init := SeriesItem{
					Label: Label{Show: true},
					Name:  cputil.I18n(ctx, string(apistructs.CaseExecStatusInit)),
					Stack: "total",
				}
				for _, line := range table {
					succ.Data = append(succ.Data, polishData(line.Data.NumSucc))
					block.Data = append(block.Data, polishData(line.Data.NumBlock))
					fail.Data = append(fail.Data, polishData(line.Data.NumFail))
					init.Data = append(init.Data, polishData(line.Data.NumInit))
				}
				return []SeriesItem{succ, block, fail, init}
			}(),
			XAxis: Axis{
				Type: "value",
				Data: nil,
			},
			YAxis: Axis{
				Type: "category",
				Data: func() (categories []string) {
					for _, line := range table {
						categories = append(categories, line.Name)
					}
					return
				}(),
			},
		},
		PureChart: true,
	}
}

// polishData polish data for exhibition.
func polishData(num uint64) string {
	// not return 0
	if num == 0 {
		return ""
	}
	// normal
	return strutil.String(num)
}
