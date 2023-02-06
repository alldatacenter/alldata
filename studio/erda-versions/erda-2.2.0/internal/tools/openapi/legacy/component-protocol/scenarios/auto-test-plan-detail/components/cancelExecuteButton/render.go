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

package cancelExecuteButton

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
)

type ComponentAction struct {
	Version    string                                           `json:"version,omitempty"`
	Name       string                                           `json:"name,omitempty"`
	Type       string                                           `json:"type,omitempty"`
	Props      map[string]interface{}                           `json:"props,omitempty"`
	State      State                                            `json:"state,omitempty"`
	Operations map[apistructs.OperationKey]apistructs.Operation `json:"operations,omitempty"`
	Data       map[string]interface{}                           `json:"data,omitempty"`
}

type State struct {
	PipelineDetail *apistructs.PipelineDetailDTO `json:"pipelineDetail"`
}

func (ca *ComponentAction) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	if err := ca.Import(c); err != nil {
		logrus.Errorf("failed to import component, err: %v", err)
		return err
	}

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	i18nLocale := bdl.Bdl.GetLocale(bdl.Locale)
	switch event.Operation {
	case "cancelExecute":

		var req apistructs.PipelineCancelRequest
		req.PipelineID = uint64(c.State["pipelineId"].(float64))
		req.UserID = bdl.Identity.UserID
		err := bdl.Bdl.CancelPipeline(req)
		if err != nil {
			return err
		}

		c.State["reloadScenesInfo"] = true
		c.Props = map[string]interface{}{
			"text":    i18nLocale.Get(i18n.I18nKeyUnExecute),
			"visible": false,
		}
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
		c.Type = "Button"
		visible := true
		if _, ok := c.State["visible"]; ok {
			visible = c.State["visible"].(bool)
		}
		if ca.State.PipelineDetail == nil {
			visible = false
		} else {
			pipelineId := ca.State.PipelineDetail.ID
			if pipelineId > 0 {
				if !ca.State.PipelineDetail.Status.IsReconcilerRunningStatus() {
					visible = false
				}
			} else {
				visible = false
			}
		}
		c.Props = map[string]interface{}{
			"text":    i18nLocale.Get(i18n.I18nKeyUnExecute),
			"visible": visible,
		}
		c.Operations = map[string]interface{}{
			"click": map[string]interface{}{
				"key":     "cancelExecute",
				"confirm": i18nLocale.Get(i18n.I18nKeyWhetherCancel),
				"reload":  true,
			},
		}
	}
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}

func (a *ComponentAction) Import(c *apistructs.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func (a *ComponentAction) marshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(a.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	propValue, err := json.Marshal(a.Props)
	if err != nil {
		return err
	}
	var props interface{}
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	c.Props = props
	c.State = state
	c.Type = a.Type
	return nil
}
