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

package treeMapChart

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/services/code_coverage"
)

type ComponentAction struct {
	ctxBdl *bundle.Bundle
	svc    *code_coverage.CodeCoverage

	Name       string                 `json:"name,omitempty"`
	Type       string                 `json:"type,omitempty"`
	Props      map[string]interface{} `json:"props,omitempty"`
	State      State                  `json:"state,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
	Data       map[string]interface{}
}

type State struct {
	RecordID uint64 `json:"recordID"`
}

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

func (c *ComponentAction) setProps(ctx context.Context, recordID uint64) error {
	var (
		data      []*apistructs.CodeCoverageNode
		root      *apistructs.CodeCoverageNode
		path      string
		rootValue []float64
		toolTip   apistructs.ToolTip
	)
	title := cputil.I18n(ctx, "report-details")
	var maxDepth int
	projectName := ""
	if recordID != 0 {
		record, err := c.svc.GetCodeCoverageRecord(recordID)
		if err != nil {
			return err
		}
		reportContent := record.ReportContent
		if len(reportContent) > 0 {
			maxDepth = reportContent[0].MaxDepth()
			data = reportContent[0].Nodes
			root = reportContent[0]
			path = root.Path
			rootValue = root.Value
			toolTip = root.ToolTip
		}
		// mysql default time 1000-01-01
		if record.ReportTime.Year() != 1000 {
			title = fmt.Sprintf("%s %s", title, record.ReportTime.Format("2006-01-02 15:04:05"))
		}
		project, err := c.ctxBdl.GetProject(record.ProjectID)
		if err != nil {
			return err
		}
		projectName = project.DisplayName
	}
	levels := []interface{}{
		map[string]interface{}{
			"itemStyle": map[string]interface{}{
				"borderWidth": 0,
				"gapWidth":    5,
			},
			"color": []string{"#EC7D32", "#FEC100", "#4FAED4", "#A7BA64", "#36A47C"},
		},
		map[string]interface{}{
			"itemStyle": map[string]interface{}{
				"gapWidth": 1,
			},
			"color": []string{"#EC7D32", "#FEC100", "#4FAED4", "#A7BA64", "#36A47C"},
		},
	}
	for i := 2; i < maxDepth; i++ {
		levels = append(levels, map[string]interface{}{
			"color": []string{"#EC7D32", "#FEC100", "#4FAED4", "#A7BA64", "#36A47C"},
			"itemStyle": map[string]interface{}{
				"gapWidth":              1,
				"borderColorSaturation": 0.6,
			},
		})
	}
	c.Props = map[string]interface{}{
		//"requestIgnore": []string{"props"},
		"title": title,
		"style": map[string]interface{}{
			"height": 600,
		},
		"chartType": "treemap",
		"option": map[string]interface{}{
			//"tooltip": map[string]interface{}{
			//	"show":      true,
			//	"formatter": "{@parent}: {@[1]} <br /> {@abc}: {@[2]}",
			//},
			"tooltip": map[string]interface{}{
				"show": true,
			},
			"series": []interface{}{
				map[string]interface{}{
					"name":            projectName,
					"type":            "treemap",
					"roam":            false,
					"leafDepth":       maxDepth,
					"tooltip":         toolTip,
					"value":           rootValue,
					"path":            path,
					"bottom":          30,
					"width":           "100%",
					"height":          "90%",
					"colorMappingBy":  "value",
					"visualDimension": 6,
					"visualMin":       0,
					"visualMax":       100,
					"breadcrumb": map[string]interface{}{
						"itemStyle": map[string]interface{}{
							"color": "#996cd3",
						},
					},
					"data": data,
					//"color":           []string{"#808080", "#C0C0C0", "#87CEFA", "#00FF00", "#228B22"},
					"levels": levels,
				},
			},
		},
	}
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := ca.GenComponentState(c); err != nil {
		return err
	}
	svc := ctx.Value(types.CodeCoverageService).(*code_coverage.CodeCoverage)
	ca.svc = svc
	bdl := ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	ca.ctxBdl = bdl
	ca.Type = "Chart"
	recordID := ca.State.RecordID
	if err := ca.setProps(ctx, recordID); err != nil {
		return err
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("code-coverage", "treeMapChart", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
