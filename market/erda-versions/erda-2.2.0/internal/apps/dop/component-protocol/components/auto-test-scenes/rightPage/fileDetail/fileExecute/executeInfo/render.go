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

package executeInfo

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
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/util"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type ComponentFileInfo struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle

	CommonFileInfo
}

type CommonFileInfo struct {
	Version    string                                           `json:"version,omitempty"`
	Name       string                                           `json:"name,omitempty"`
	Type       string                                           `json:"type,omitempty"`
	Props      map[string]interface{}                           `json:"props,omitempty"`
	Operations map[apistructs.OperationKey]apistructs.Operation `json:"operations,omitempty"`
	Data       map[string]interface{}                           `json:"data,omitempty"`
}

type PropColumn struct {
	Label    string `json:"label"`
	ValueKey string `json:"valueKey"`
}

type reportNew struct {
	APIFailedNum  int64 `json:"apiFailedNum"`
	APINotExecNum int64 `json:"apiNotExecNum"`
	APISuccessNum int64 `json:"apiSuccessNum"`
	APITotalNum   int64 `json:"apiTotalNum"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "executeInfo",
		func() servicehub.Provider { return &ComponentFileInfo{} })
}

func (a *ComponentFileInfo) Import(c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func (i *ComponentFileInfo) RenderProtocol(c *cptype.Component, g *cptype.GlobalStateData) {
	if c.Data == nil {
		d := make(cptype.ComponentData)
		c.Data = d
	}
	(*c).Data["data"] = i.Data
	c.Props = i.Props

}

func (i *ComponentFileInfo) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (err error) {
	gh := gshelper.NewGSHelper(gs)
	if err := i.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}

	i.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)

	defer func() {
		fail := i.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	pipelineID := gh.GetExecuteHistoryTablePipelineID()

	if pipelineID > 0 {
		rsp := gh.GetPipelineInfoWithPipelineID(pipelineID, i.bdl)
		if rsp == nil {
			return fmt.Errorf("not find pipelineID %v info", pipelineID)
		}
		status := cputil.I18n(ctx, util.ColumnPipelineStatus+rsp.Status.String())
		if rsp.TimeBegin != nil && (rsp.TimeEnd != nil || rsp.TimeUpdated != nil) && rsp.Status.IsEndStatus() {
			var timeLayoutStr = "2006-01-02 15:04:05" //go中的时间格式化必须是这个时间
			if rsp.TimeEnd == nil {
				rsp.TimeEnd = rsp.TimeUpdated
			}
			t := rsp.TimeEnd.Sub(*rsp.TimeBegin)
			h := strconv.FormatInt(int64(t.Hours()), 10) + ":"
			if t.Hours() < 10 {
				h = "0" + strconv.FormatInt(int64(t.Hours()), 10) + ":"
			}
			i.Data = map[string]interface{}{
				"pipelineID": pipelineID,
				"status":     status,
				"time":       h + time.Unix(int64(t.Seconds())-8*3600, 0).Format("04:05"),
				"timeBegin":  rsp.TimeBegin.Format(timeLayoutStr),
				"timeEnd":    rsp.TimeEnd.Format(timeLayoutStr),
			}
		} else if rsp.TimeBegin != nil {
			var timeLayoutStr = "2006-01-02 15:04:05" //go中的时间格式化必须是这个时间
			i.Data = map[string]interface{}{
				"pipelineID": pipelineID,
				"status":     status,
				"timeBegin":  rsp.TimeBegin.Format(timeLayoutStr),
			}
		} else {
			i.Data = map[string]interface{}{
				"pipelineID": pipelineID,
				"status":     status,
			}
		}
		if rsp.Status == apistructs.PipelineStatusStopByUser {
			i.Data["status"] = status
		}
		if rsp.Status == apistructs.PipelineStatusNoNeedBySystem {
			i.Data["status"] = status
		}
		res, err := i.bdl.GetPipelineReportSet(pipelineID, []string{"api-test"})
		if err != nil {
			return err
		}
		if res != nil && len(res.Reports) > 0 && res.Reports[0].Meta != nil {
			value, err := json.Marshal(res.Reports[0].Meta)
			if err != nil {
				i.Data["autoTestExecPercent"] = "-"
				i.Data["autoTestSuccessPercent"] = "-"
				goto Label
			}
			var report reportNew
			err = json.Unmarshal(value, &report)
			if err != nil {
				i.Data["autoTestExecPercent"] = "-"
				i.Data["autoTestSuccessPercent"] = "-"
				goto Label
			}
			i.Data["autoTestNum"] = report.APITotalNum
			if report.APITotalNum == 0 {
				i.Data["autoTestExecPercent"] = "0.00%"
				i.Data["autoTestSuccessPercent"] = "0.00%"
			} else {
				i.Data["autoTestExecPercent"] = strconv.FormatFloat(100-float64(report.APINotExecNum)/float64(report.APITotalNum)*100, 'f', 2, 64) + "%"
				i.Data["autoTestSuccessPercent"] = strconv.FormatFloat(float64(report.APISuccessNum)/float64(report.APITotalNum)*100, 'f', 2, 64) + "%"
			}
		}
	}
Label:
	i.Props = make(map[string]interface{})
	i.Props["fields"] = []PropColumn{
		{
			Label:    cputil.I18n(ctx, "pipelineID"),
			ValueKey: "pipelineID",
		},
		{
			Label:    cputil.I18n(ctx, "state"),
			ValueKey: "status",
		},
		{
			Label:    cputil.I18n(ctx, "duration"),
			ValueKey: "time",
		},
		{
			Label:    cputil.I18n(ctx, "start-time"),
			ValueKey: "timeBegin",
		},
		{
			Label:    cputil.I18n(ctx, "endTime"),
			ValueKey: "timeEnd",
		},
		{
			Label:    cputil.I18n(ctx, "totalApi"),
			ValueKey: "autoTestNum",
		},
		{
			Label:    cputil.I18n(ctx, "apiExecRate"),
			ValueKey: "autoTestExecPercent",
		},
		{
			Label:    cputil.I18n(ctx, "apiPassRate"),
			ValueKey: "autoTestSuccessPercent",
		},
	}

	i.RenderProtocol(c, gs)
	return
}

func (a *ComponentFileInfo) marshal(c *cptype.Component) error {
	propValue, err := json.Marshal(a.Props)
	if err != nil {
		return err
	}
	var props cptype.ComponentProps
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	c.Props = props
	c.Type = a.Type
	return nil
}
