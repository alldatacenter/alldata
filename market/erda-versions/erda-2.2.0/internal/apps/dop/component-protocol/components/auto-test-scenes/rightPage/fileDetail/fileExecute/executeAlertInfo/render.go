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

package executeAlertInfo

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type ComponentAlertInfo struct {
	bdl *bundle.Bundle

	CommonAlertInfo

	pipelineID uint64
}

type CommonAlertInfo struct {
	Version    string                                           `json:"version,omitempty"`
	Name       string                                           `json:"name,omitempty"`
	Type       string                                           `json:"type,omitempty"`
	Props      map[string]interface{}                           `json:"props,omitempty"`
	Operations map[apistructs.OperationKey]apistructs.Operation `json:"operations,omitempty"`
	Data       map[string]interface{}                           `json:"data,omitempty"`
}

func (a *ComponentAlertInfo) Import(c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, a); err != nil {
		return err
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "executeAlertInfo",
		func() servicehub.Provider { return &ComponentAlertInfo{} })
}

func (i *ComponentAlertInfo) RenderProtocol(c *cptype.Component, g *cptype.GlobalStateData) {
	if c.Data == nil {
		d := make(cptype.ComponentData)
		c.Data = d
	}
	(*c).Data["data"] = i.Data
	c.Props = i.Props

}

func (i *ComponentAlertInfo) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (err error) {
	gh := gshelper.NewGSHelper(gs)
	if err := i.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}
	i.pipelineID = gh.GetExecuteHistoryTablePipelineID()

	i.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	defer func() {
		fail := i.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()
	visible := false
	var message []string
	if i.pipelineID > 0 {
		rsp := gh.GetPipelineInfoWithPipelineID(i.pipelineID, i.bdl)
		if rsp == nil {
			return fmt.Errorf("not find pipelineID %v info", i.pipelineID)
		}
		if rsp.Extra.ShowMessage != nil {
			message = rsp.Extra.ShowMessage.Stacks
			visible = true
		}
	}
	i.Props = map[string]interface{}{
		"visible":  visible,
		"type":     "warning",
		"message":  message,
		"showIcon": true,
	}

	i.RenderProtocol(c, gs)
	return nil
}

func (a *ComponentAlertInfo) marshal(c *cptype.Component) error {
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
