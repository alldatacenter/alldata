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

package scenesSetConfig

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
)

type ComponentAction struct {
	State    State `json:"state"`
	gsHelper *gshelper.GSHelper
}

type State struct {
	ActiveKey apistructs.ActiveKey `json:"activeKey"`
}

func (ca *ComponentAction) GenComponentState(c *cptype.Component, gs *cptype.GlobalStateData) error {
	ca.gsHelper = gshelper.NewGSHelper(gs)
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
	ca.State = state
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := ca.GenComponentState(c, gs); err != nil {
		return err
	}
	c.Props = map[string]interface{}{
		"visible": func() bool {
			return ca.gsHelper.GetGlobalActiveConfig() == gshelper.SceneSetConfigKey && ca.gsHelper.GetGlobalSelectedSetID() != 0
		}(),
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "scenesSetConfig",
		func() servicehub.Provider { return &ComponentAction{} })
}
