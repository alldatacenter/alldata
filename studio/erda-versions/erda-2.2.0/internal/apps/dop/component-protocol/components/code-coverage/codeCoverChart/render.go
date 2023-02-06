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

package codeCoverChart

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/code-coverage/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/services/code_coverage"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

const (
	defaultMaxSize = 9999
	timeFormat     = "01-02 15:04"
	goTimeFormat   = "2006-01-02 15:04:05"
)

type ComponentAction struct {
	ctxBdl protocol.ContextBundle
	svc    *code_coverage.CodeCoverage

	Name       string                 `json:"name,omitempty"`
	Type       string                 `json:"type,omitempty"`
	Props      map[string]interface{} `json:"props,omitempty"`
	State      State                  `json:"state,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
	Data       map[string]interface{}
}

type Meta struct {
	Data struct {
		Data PointValue `json:"data"`
	} `json:"data"`
}

type PointValue struct {
	RecordID   uint64  `json:"recordId"`
	Value      float64 `json:"value"`
	SymbolSize int     `json:"symbolSize"`
	Symbol     string  `json:"symbol"`
}

type State struct {
	Value     []int64 `json:"value"`
	RecordID  uint64  `json:"recordID"`
	Workspace string  `json:"workspace"`
}

type Operation struct {
	Key      string                 `json:"key"`
	Reload   bool                   `json:"reload"`
	FillMeta string                 `json:"fillMeta"`
	Meta     map[string]interface{} `json:"meta"`
}

func (ca *ComponentAction) setProps(ctx context.Context, data apistructs.CodeCoverageExecRecordData) {
	ca.Type = "Chart"
	ca.Props = make(map[string]interface{})
	ca.Props["title"] = cputil.I18n(ctx, "project-code-coverage-trend")
	ca.Props["chartType"] = "line"
	var timeList []string
	var valueLst []PointValue
	for _, r := range data.List {
		t := r.TimeEnd.Format(timeFormat)
		timeList = append(timeList, t)
		p := PointValue{
			RecordID:   r.ID,
			Value:      r.Coverage,
			SymbolSize: 8,
			Symbol:     "circle",
		}
		if r.ID == ca.State.RecordID {
			p.Symbol = "pin"
			p.SymbolSize = 24
		}
		valueLst = append(valueLst, p)
	}
	ca.Props["option"] = map[string]interface{}{
		"xAxis": map[string]interface{}{
			"data": timeList,
		},
		"yAxis": map[string]interface{}{
			"axisLabel": map[string]interface{}{
				"formatter": "{value}%",
			},
		},
		"grid": map[string]interface{}{
			"top": 40,
		},
		"tooltip": map[string]interface{}{
			"formatter": "{b}<br />{a}: {c}%",
			"trigger":   "axis",
		},
		"series": []interface{}{
			map[string]interface{}{
				"name": cputil.I18n(ctx, "line-coverage"),
				"data": valueLst,
				"label": map[string]interface{}{
					"normal": map[string]interface{}{
						"show":     true,
						"position": "top",
					},
				},
				"areaStyle": map[string]interface{}{
					"opacity": 0.1,
				},
			},
		},
	}
}

// GenComponentState 获取state
func (i *ComponentAction) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	cont, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
		return err
	}
	err = json.Unmarshal(cont, &state)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return err
	}
	fmt.Println(state)
	i.State = state
	return nil
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

func (ca *ComponentAction) setDefaultTimeRange() {
	now := time.Now()
	oneMonthAgo := now.AddDate(0, 0, -30)
	oneMonthRange := []int64{oneMonthAgo.Unix() * 1000, now.Unix() * 1000}
	ca.State.Value = oneMonthRange
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := ca.GenComponentState(c); err != nil {
		return err
	}
	svc := ctx.Value(types.CodeCoverageService).(*code_coverage.CodeCoverage)
	ca.svc = svc
	sdk := cputil.SDK(ctx)
	projectIDStr := sdk.InParams["projectId"].(string)
	projectID, err := strconv.ParseUint(projectIDStr, 10, 64)
	if err != nil {
		return err
	}

	workspace, ok := c.State["workspace"].(string)
	if !ok {
		return fmt.Errorf("workspace was empty")
	}

	switch event.Operation {
	case common.CoverChartSelectItemOperationKey:
		var m Meta
		metaData := event.OperationData["meta"]
		metaByt, _ := json.Marshal(metaData)
		if err := json.Unmarshal(metaByt, &m); err != nil {
			return err
		}
		ca.State.RecordID = m.Data.Data.RecordID
		if len(ca.State.Value) < 2 {
			ca.setDefaultTimeRange()
		}
		start, end := convertTimeRange(ca.State.Value)
		recordRsp, err := svc.ListCodeCoverageRecord(apistructs.CodeCoverageListRequest{
			ProjectID: projectID,
			TimeBegin: start,
			Workspace: workspace,
			TimeEnd:   end,
			Statuses:  []apistructs.CodeCoverageExecStatus{apistructs.SuccessStatus},
			Asc:       true,
			PageSize:  defaultMaxSize,
		})
		if err != nil {
			return err
		}
		ca.setProps(ctx, recordRsp)
	case cptype.InitializeOperation, cptype.DefaultRenderingKey:
		if len(ca.State.Value) < 2 {
			ca.setDefaultTimeRange()
		}
		start, end := convertTimeRange(ca.State.Value)
		recordRsp, err := svc.ListCodeCoverageRecord(apistructs.CodeCoverageListRequest{
			ProjectID: projectID,
			TimeBegin: start,
			TimeEnd:   end,
			Workspace: workspace,
			Statuses:  []apistructs.CodeCoverageExecStatus{apistructs.SuccessStatus},
			Asc:       true,
			PageSize:  defaultMaxSize,
		})
		if err != nil {
			return err
		}
		ca.State.RecordID = 0
		if len(recordRsp.List) > 0 {
			ca.State.RecordID = recordRsp.List[len(recordRsp.List)-1].ID
		}
		ca.setProps(ctx, recordRsp)
		ca.Operations = getOperations()
	case cptype.RenderingOperation:
		if len(ca.State.Value) < 2 {
			ca.setDefaultTimeRange()
		}
		start, end := convertTimeRange(ca.State.Value)
		recordRsp, err := ca.svc.ListCodeCoverageRecord(apistructs.CodeCoverageListRequest{
			ProjectID: uint64(projectID),
			Statuses:  []apistructs.CodeCoverageExecStatus{apistructs.SuccessStatus},
			TimeBegin: start,
			TimeEnd:   end,
			Workspace: workspace,
			Asc:       true,
			PageSize:  defaultMaxSize,
		})
		if err != nil {
			return err
		}
		ca.State.RecordID = 0
		if len(recordRsp.List) > 0 {
			ca.State.RecordID = recordRsp.List[len(recordRsp.List)-1].ID
		}
		ca.setProps(ctx, recordRsp)
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("code-coverage", "codeCoverChart", func() servicehub.Provider {
		return &ComponentAction{}
	})
}

func convertTimeRange(timeRange []int64) (startTime, endTime string) {
	start, end := timeRange[0], timeRange[1]
	startT := time.Unix(start/1000, 0)
	endT := time.Unix(end/1000, 0)
	return startT.Format(goTimeFormat), endT.Format(goTimeFormat)
}
