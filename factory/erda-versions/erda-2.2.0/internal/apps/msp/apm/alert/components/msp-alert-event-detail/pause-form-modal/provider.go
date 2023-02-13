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

package pause_form_modal

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
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail/common"
)

func (cp *ComponentPauseModalFormInfo) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	err := cp.InitComponent(ctx, c)
	if err != nil {
		return err
	}

	switch event.Operation {
	case "submit":
		alertEvent, err := cp.GetAlertEvent(ctx)
		if err != nil {
			return err
		}
		err = cp.PauseAlertEvent(alertEvent)
		if err != nil {
			return err
		}
		cp.State.Paused = true
	}

	cp.Props = cp.getProps()
	cp.Operations = cp.getOperations()
	cp.Data = map[string]Data{}
	cp.Transfer(c)
	return nil
}

func (cp *ComponentPauseModalFormInfo) InitComponent(ctx context.Context, c *cptype.Component) error {
	if err := cp.GenComponentState(c); err != nil {
		return err
	}
	cp.ctx = ctx
	sdk := cputil.SDK(ctx)
	inParams, _ := common.ParseFromCpSdk(sdk)
	cp.inParams = inParams
	cp.sdk = sdk
	cp.MonitorAlertService = common.GetMonitorAlertServiceFromContext(ctx)
	return nil
}

func (cp *ComponentPauseModalFormInfo) GenComponentState(c *cptype.Component) error {
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

func (cp *ComponentPauseModalFormInfo) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(cp.Props)
	component.Data = map[string]interface{}{}
	component.Operations = cptype.ComponentOperations{}
	for k, operation := range cp.Operations {
		component.Operations[k] = operation
	}
	for k, v := range cp.Data {
		component.Data[k] = v
	}
	component.State = map[string]interface{}{}
}

func (cp *ComponentPauseModalFormInfo) getOperations() map[string]Operation {
	return map[string]Operation{
		"submit": {
			Key:    "submit",
			Reload: true,
		},
	}
}

func (cp *ComponentPauseModalFormInfo) getProps() Props {
	props := Props{
		Fields: []Field{
			{
				Label:     cp.sdk.I18n("Pause Expire Time"),
				Key:       "pauseExpireTime",
				Required:  true,
				Component: "datePicker",
				ComponentProps: map[string]interface{}{
					"dateType":  "date",
					"valueType": "timestamp",
					"showTime":  true,
				},
			},
		},
	}
	return props
}

func init() {
	name := fmt.Sprintf("component-protocol.components.%s.%s", common.ScenarioKey, common.ComponentNamePauseFormModal)
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator(common.ScenarioKey, common.ComponentNamePauseFormModal, func() servicehub.Provider {
		return &ComponentPauseModalFormInfo{}
	})
}
