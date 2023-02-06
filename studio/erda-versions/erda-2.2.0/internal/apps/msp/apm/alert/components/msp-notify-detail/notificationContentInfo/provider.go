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

package notificationContentInfo

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-notify-detail/common"
)

func (cp *ComponentNotifyInfo) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := cp.GenComponentState(c); err != nil {
		return err
	}
	cp.ctx = ctx
	cp.sdk = cputil.SDK(ctx)
	alertIndex := common.GetNotifyIndexFromGlobalState(*gs)
	cp.Data = map[string]string{
		"content": alertIndex.NotifyContent,
	}
	cp.Transfer(c)
	return nil
}

func (cp *ComponentNotifyInfo) Transfer(component *cptype.Component) {
	component.Data = map[string]interface{}{}
	for k, v := range cp.Data {
		component.Data[k] = v
	}
	component.State = map[string]interface{}{}
}

func (cp *ComponentNotifyInfo) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}

	jsonData, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("failed to marshal for eventTable state, %v", err)
		return err
	}
	var state State
	err = json.Unmarshal(jsonData, &state)
	if err != nil {
		logrus.Errorf("failed to unmarshal for eventTable state, %v", err)
		return err
	}
	cp.State = state
	return nil
}

func init() {
	name := fmt.Sprintf("component-protocol.components.%s.%s", common.ScenarioKey, common.ComponentNameNotificationContentInfo)
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator(common.ScenarioKey, common.ComponentNameNotificationContentInfo, func() servicehub.Provider {
		return &ComponentNotifyInfo{}
	})
}
