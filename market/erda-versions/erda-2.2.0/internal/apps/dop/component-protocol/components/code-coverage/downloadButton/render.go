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

package downloadButton

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
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/services/code_coverage"
)

type ComponentAction struct {
	svc *code_coverage.CodeCoverage

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

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	ca.Type = "Button"
	if err := ca.GenComponentState(c); err != nil {
		return err
	}
	svc := ctx.Value(types.CodeCoverageService).(*code_coverage.CodeCoverage)
	visible := false
	downloadUrl := ""
	if ca.State.RecordID != 0 {
		record, err := svc.GetCodeCoverageRecord(ca.State.RecordID)
		if err != nil {
			return err
		}
		if record.ReportStatus == apistructs.SuccessStatus.String() {
			visible = true
			downloadUrl = record.ReportUrl
		}
	}
	ca.Operations = map[string]interface{}{
		"click": map[string]interface{}{
			"key":    "downloadReport",
			"reload": false,
			"command": map[string]interface{}{
				"jumpOut": true,
				"key":     "goto",
				"target":  downloadUrl,
			},
		},
	}
	ca.Props = map[string]interface{}{
		"text":    cputil.I18n(ctx, "download-report"),
		"type":    "link",
		"size":    "small",
		"visible": visible,
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("code-coverage", "downloadButton", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
