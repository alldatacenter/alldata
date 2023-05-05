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

package more_operations

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail/common"
)

const (
	operationPauseAlert   = "pauseAlert"
	operationStopAlert    = "stopAlert"
	operationResumeAlert  = "resumeAlert"
	operationRestartAlert = "restartAlert"
)

func init() {
	name := fmt.Sprintf("component-protocol.components.%s.%s", common.ScenarioKey, common.ComponentNameMoreOperations)
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator(common.ScenarioKey, common.ComponentNameMoreOperations, func() servicehub.Provider {
		return &ComponentOperationButton{}
	})
}

func (b *ComponentOperationButton) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	b.InitComponent(ctx)
	if err := b.GenComponentState(component); err != nil {
		return fmt.Errorf("failed to gen operationButton component state, %v", err)
	}

	alertEvent, err := b.GetAlertEvent(ctx)
	if err != nil {
		return err
	}
	common.SetAlertEventToGlobalState(*gs, alertEvent)

	switch event.Operation {
	case operationPauseAlert:
		return fmt.Errorf("should not execute here")
	case operationStopAlert:
		err := b.StopAlertEvent(alertEvent)
		if err != nil {
			return fmt.Errorf("failed to stop alert event: %s", err)
		}
	case operationRestartAlert, operationResumeAlert:
		err := b.CancelSuppressAlertEvent(alertEvent)
		if err != nil {
			return fmt.Errorf("failed to resume/restart alert event: %s", err)
		}
	}

	alertEvent, err = b.GetAlertEvent(ctx)
	if err != nil {
		return err
	}
	common.SetAlertEventToGlobalState(*gs, alertEvent)

	b.SetComponentValue(alertEvent)
	b.Transfer(component)
	return nil
}

func (b *ComponentOperationButton) InitComponent(ctx context.Context) {
	b.ctx = ctx
	sdk := cputil.SDK(ctx)
	inParams, _ := common.ParseFromCpSdk(sdk)
	b.inParams = inParams
	b.sdk = sdk
	b.MonitorAlertService = common.GetMonitorAlertServiceFromContext(ctx)
}

func (b *ComponentOperationButton) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	jsonData, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(jsonData, &state); err != nil {
		return err
	}
	b.State = state
	return nil
}

func (b *ComponentOperationButton) SetComponentValue(alertEvent *monitorpb.AlertEventItem) {
	b.Props.Text = b.sdk.I18n(common.ComponentNameMoreOperations)
	b.Props.Type = "primary"
	b.Props.Menu = []Menu{}

	candidateMenus := map[string]Menu{
		operationPauseAlert: {
			Key:  operationPauseAlert,
			Text: b.sdk.I18n(operationPauseAlert),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:    operationPauseAlert,
					Reload: false,
					Command: Command{
						Key: "set",
						State: CommandState{
							Visible: true,
						},
						Target: common.ComponentNamePauseFormModal,
					},
				},
			},
		},
		operationStopAlert: {
			Key:  operationStopAlert,
			Text: b.sdk.I18n(operationStopAlert),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:        operationStopAlert,
					Reload:     true,
					SuccessMsg: b.sdk.I18n("stopAlertSuccessfully"),
					Confirm:    b.sdk.I18n("confirmToStopAlert"),
				},
			},
		},
		operationResumeAlert: {
			Key:  operationResumeAlert,
			Text: b.sdk.I18n(operationResumeAlert),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:        operationResumeAlert,
					Reload:     true,
					SuccessMsg: b.sdk.I18n("resumeAlertSuccessfully"),
					Confirm:    b.sdk.I18n("confirmToResumeAlert"),
				},
			},
		},
		operationRestartAlert: {
			Key:  operationRestartAlert,
			Text: b.sdk.I18n(operationRestartAlert),
			Operations: map[string]interface{}{
				"click": Operation{
					Key:        operationRestartAlert,
					Reload:     true,
					SuccessMsg: b.sdk.I18n("restartAlertSuccessfully"),
					Confirm:    b.sdk.I18n("confirmToRestartAlert"),
				},
			},
		},
	}

	switch alertEvent.AlertState {
	case "alert", "recover":
		b.Props.Menu = append(b.Props.Menu,
			candidateMenus[operationPauseAlert],
			candidateMenus[operationStopAlert])
	case "pause":
		b.Props.Menu = append(b.Props.Menu,
			candidateMenus[operationResumeAlert])
	case "stop":
		b.Props.Menu = append(b.Props.Menu,
			candidateMenus[operationRestartAlert])
	}
}

func (b *ComponentOperationButton) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(b.Props)
}
