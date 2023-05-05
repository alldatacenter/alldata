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
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
)

const RateTrendingSelectItemOperationKey cptype.OperationKey = "selectChartItem"

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "at_case_rate_trending_chart", func() servicehub.Provider {
		return &Chart{}
	})
}

type Chart struct {
	PData []Data                 `json:"pData"`
	EData []Data                 `json:"eData"`
	XAxis XAxis                  `json:"xAxis"`
	State map[string]interface{} `json:"state"`
}

type Props struct {
	ChartType string `json:"chartType"`
	Option    Option `json:"option"`
	PureChart bool   `json:"pureChart"`
}

type Data struct {
	Value    string                                  `json:"value"`
	MetaData map[string]gshelper.SelectChartItemData `json:"metaData"`
}

type TestPlanV2 struct {
	PipelineID uint64 `json:"pipelineID"`
	PlanID     uint64 `json:"planID"`
	Name       string `json:"name"`
}

type Option struct {
	Legend struct {
		Show bool `json:"show"`
	} `json:"legend"`
	Series []Series `json:"series"`
	XAxis  XAxis    `json:"xAxis"`
	YAxis  YAxis    `json:"yAxis"`
}

type Series struct {
	AreaStyle struct {
		Opacity float64 `json:"opacity"`
	} `json:"areaStyle"`
	Data  []Data `json:"data"`
	Label struct {
		Show bool `json:"show"`
	} `json:"label"`
	Name string `json:"name"`
}
type XAxis struct {
	Data []string `json:"data"`
}

type AxisLabel struct {
	Formatter string `json:"formatter"`
}

type YAxis struct {
	AxisLabel AxisLabel `json:"axisLabel"`
}

type OperationData struct {
	FillMeta string   `json:"fillMeta"`
	MetaData MetaData `json:"meta"`
}

type MetaData struct {
	Data PData `json:"data"`
}

type PData struct {
	Data Data `json:"data"`
}

func (ch *Chart) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	h := gshelper.NewGSHelper(gs)
	if c.State == nil {
		c.State = map[string]interface{}{}
	}
	c.State["isClick"] = false
	switch event.Operation {
	case RateTrendingSelectItemOperationKey:
		opData := OperationData{}
		b, err := json.Marshal(&event.OperationData)
		if err != nil {
			return err
		}
		if err = json.Unmarshal(b, &opData); err != nil {
			return err
		}

		h.SetSelectChartItemData(opData.MetaData.Data.Data.MetaData)
		c.State["isClick"] = true
		return nil
	case cptype.InitializeOperation, cptype.DefaultRenderingKey, cptype.RenderingOperation:
		atPlans := h.GetRateTrendingFilterTestPlanList()
		atSvc := ctx.Value(types.AutoTestPlanService).(*autotestv2.Service)
		timeFilter := h.GetAtCaseRateTrendingTimeFilter()
		historyList, err := atSvc.ListAutoTestExecHistory(
			timeFilter.TimeStart,
			timeFilter.TimeEnd,
			func() []uint64 {
				planIDs := make([]uint64, 0, len(atPlans))
				for _, v := range atPlans {
					planIDs = append(planIDs, v.ID)
				}
				return planIDs
			}()...)
		if err != nil {
			return err
		}
		pData := make([]Data, 0, len(historyList))
		eData := make([]Data, 0, len(historyList))
		xAxis := make([]string, 0, len(historyList))
		var sucApiNum, execApiNum, totalApiNum int64
		metaData := make(map[string]gshelper.SelectChartItemData, 0)
		for _, v := range historyList {
			if v.Type != apistructs.AutoTestPlan {
				continue
			}
			// not include stopByUser
			if v.Status == apistructs.PipelineStatusStopByUser {
				continue
			}
			sucApiNum += v.SuccessApiNum
			execApiNum += v.ExecuteApiNum
			totalApiNum += v.TotalApiNum
			metaData[strconv.FormatUint(v.PlanID, 10)] = gshelper.SelectChartItemData{
				PlanID: v.PlanID,
				Name: func() string {
					for _, v2 := range h.GetGlobalAutoTestPlanList() {
						if v2.ID == v.PlanID {
							return v2.Name
						}
					}
					return ""
				}(),
				PipelineID:  v.PipelineID,
				ExecuteTime: v.ExecuteTime.Format("2006-01-02 15:04:05"),
			}
			currMeta := make(map[string]gshelper.SelectChartItemData, 0)
			for k, data := range metaData {
				currMeta[k] = data
			}
			pData = append(pData, Data{
				MetaData: currMeta,
				Value:    calRate(sucApiNum, totalApiNum),
			})
			eData = append(eData, Data{
				MetaData: currMeta,
				Value:    calRate(execApiNum, totalApiNum),
			})
			xAxis = append(xAxis, v.ExecuteTime.Format("2006-01-02 15:04:05"))
		}
		ch.EData = eData
		ch.PData = pData
		ch.XAxis = XAxis{xAxis}
		c.Props = cputil.MustConvertProps(ch.convertToProps(ctx))
		c.Operations = getOperations()
		h.SetSelectChartItemData(metaData)
		return nil
	}
	return nil
}

func calRate(num, numTotal int64) string {
	if numTotal == 0 {
		return "0"
	}
	return fmt.Sprintf("%.2f", float64(num)/float64(numTotal)*100)
}

func (ch *Chart) convertToProps(ctx context.Context) Props {
	return Props{
		ChartType: "line",
		PureChart: true,
		Option: Option{
			Legend: struct {
				Show bool `json:"show"`
			}{Show: true},
			Series: []Series{
				{
					AreaStyle: struct {
						Opacity float64 `json:"opacity"`
					}{Opacity: 0.1},
					Data: ch.PData,
					Label: struct {
						Show bool `json:"show"`
					}{Show: true},
					Name: cputil.I18n(ctx, "test-case-rate-passed"),
				},
				{
					AreaStyle: struct {
						Opacity float64 `json:"opacity"`
					}{Opacity: 0.1},
					Data: ch.EData,
					Label: struct {
						Show bool `json:"show"`
					}{Show: true},
					Name: cputil.I18n(ctx, "test-case-rate-executed"),
				},
			},
			XAxis: ch.XAxis,
			YAxis: YAxis{AxisLabel: AxisLabel{
				Formatter: "{value}%",
			}},
		},
	}
}

type Operation struct {
	Key      string                 `json:"key"`
	Reload   bool                   `json:"reload"`
	FillMeta string                 `json:"fillMeta"`
	Meta     map[string]interface{} `json:"meta"`
}

func getOperations() map[string]interface{} {
	return map[string]interface{}{
		"click": Operation{
			Key:      "selectChartItem",
			Reload:   true,
			FillMeta: "data",
			Meta:     map[string]interface{}{"data": ""},
		},
	}
}
