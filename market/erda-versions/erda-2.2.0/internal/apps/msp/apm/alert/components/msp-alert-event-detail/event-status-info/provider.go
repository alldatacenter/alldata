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

package event_status_info

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail/common"
)

func (cp *ComponentEventStatusInfo) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := cp.GenComponentState(c); err != nil {
		return err
	}
	cp.ctx = ctx
	cp.sdk = cputil.SDK(ctx)

	alertEvent := common.GetAlertEventFromGlobalState(*gs)
	if alertEvent == nil {
		return errors.Errorf("alertEvent should be exists in globalState")
	}

	data := Data{
		AlertState:         []Tag{cp.getAlertStateTags(alertEvent)},
		SuppressExpireTime: common.FormatTimeMs(alertEvent.SuppressExpireTime),
	}
	cp.Props = cp.getProps(alertEvent)
	cp.Data = map[string]Data{
		"data": data,
	}
	cp.Transfer(c)
	return nil
}

func (cp *ComponentEventStatusInfo) GenComponentState(c *cptype.Component) error {
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

func (cp *ComponentEventStatusInfo) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(cp.Props)
	component.Data = map[string]interface{}{}
	for k, v := range cp.Data {
		component.Data[k] = v
	}
	component.State = map[string]interface{}{}
}

func (cp *ComponentEventStatusInfo) getAlertStateTags(alertEvent *monitorpb.AlertEventItem) Tag {
	switch alertEvent.AlertState {
	case "alert":
		return Tag{
			Label: cp.sdk.I18n(alertEvent.AlertState),
			Color: "red",
		}
	case "recover":
		return Tag{
			Label: cp.sdk.I18n(alertEvent.AlertState),
			Color: "green",
		}
	case "pause", "stop":
		return Tag{
			Label: cp.sdk.I18n(alertEvent.AlertState),
			Color: "blue",
		}
	default:
		return Tag{
			Label: cp.sdk.I18n(alertEvent.AlertState),
		}
	}
}

func (cp *ComponentEventStatusInfo) getProps(alertEvent *monitorpb.AlertEventItem) Props {

	props := Props{
		RequestIgnore: []string{"data"},
		ColumnNum:     4,
		Fields: []Field{
			{
				Label:      cp.sdk.I18n("Status"),
				ValueKey:   "alertState",
				RenderType: "tagsRow",
			},
		},
	}

	if alertEvent.AlertState == "pause" {
		props.Fields = append(props.Fields, Field{
			Label:    cp.sdk.I18n("Pause Expire Time"),
			ValueKey: "suppressExpireTime",
		})
	}

	return props
}

func init() {
	name := fmt.Sprintf("component-protocol.components.%s.%s", common.ScenarioKey, common.ComponentNameEventStatusInfo)
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator(common.ScenarioKey, common.ComponentNameEventStatusInfo, func() servicehub.Provider {
		return &ComponentEventStatusInfo{}
	})
}
