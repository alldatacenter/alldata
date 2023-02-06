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

package event_overview_info

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

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

func (cp *ComponentEventOverviewInfo) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := cp.GenComponentState(c); err != nil {
		return err
	}
	cp.ctx = ctx
	cp.sdk = cputil.SDK(ctx)
	cp.Metric = common.GetMonitorMetricServiceFromContext(ctx)

	alertEvent := common.GetAlertEventFromGlobalState(*gs)
	if alertEvent == nil {
		return errors.Errorf("alertEvent should be exists in globalState")
	}

	alertCount, err := cp.countAlertEvents(ctx, alertEvent.Id)
	if err != nil {
		return fmt.Errorf("failed to calc alertCount")
	}

	data := Data{
		RelatedRuleName:  alertEvent.RuleName,
		RelatedAlertName: alertEvent.AlertName,
		AlertLevel:       cp.sdk.I18n(alertEvent.AlertLevel),
		AlertSource:      cp.sdk.I18n(alertEvent.AlertSource),
		AlertSubject:     cp.getAlertSubjectTags(alertEvent),
		AlertCount:       strconv.FormatInt(alertCount, 10),
		FirstTriggerTime: common.FormatTimeMs(alertEvent.FirstTriggerTime),
		LastTriggerTime:  common.FormatTimeMs(alertEvent.LastTriggerTime),
	}
	cp.Props = cp.getProps(alertEvent)
	cp.Data = map[string]Data{
		"data": data,
	}
	cp.Transfer(c)
	return nil
}

func (cp *ComponentEventOverviewInfo) GenComponentState(c *cptype.Component) error {
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

func (cp *ComponentEventOverviewInfo) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(cp.Props)
	component.Data = map[string]interface{}{}
	for k, v := range cp.Data {
		component.Data[k] = v
	}
	component.State = map[string]interface{}{}
}

func (cp *ComponentEventOverviewInfo) getAlertSubjectTags(alertEvent *monitorpb.AlertEventItem) (list []Tag) {
	var subjects map[string]interface{}
	err := json.Unmarshal([]byte(alertEvent.AlertSubject), &subjects)
	if err != nil {
		return []Tag{{Label: alertEvent.AlertSubject}}
	}

	for k, v := range subjects {
		list = append(list, Tag{Label: fmt.Sprintf("%s=%s", cp.sdk.I18n(k), v)})
	}
	return list
}

func (cp *ComponentEventOverviewInfo) getProps(alertEvent *monitorpb.AlertEventItem) Props {
	levelColor := ""
	levelRenderType := ""
	if alertEvent.AlertLevel == "FATAL" {
		levelColor = "red"
		levelRenderType = "highlightText"
	}

	return Props{
		RequestIgnore: []string{"data"},
		ColumnNum:     4,
		Fields: []Field{
			{Label: cp.sdk.I18n("Related Alert Rule"), ValueKey: "relatedRuleName"},
			{Label: cp.sdk.I18n("Related Alert"), ValueKey: "relatedAlertName"},
			{
				Label:      cp.sdk.I18n("Alert Level"),
				ValueKey:   "alertLevel",
				RenderType: levelRenderType,
				Color:      levelColor,
			},
			{Label: cp.sdk.I18n("Alert Source"), ValueKey: "alertSource"},
			{
				Label:      cp.sdk.I18n("Alert Subject"),
				ValueKey:   "alertSubject",
				RenderType: "tagsRow",
			},
			{Label: cp.sdk.I18n("Alert Count"), ValueKey: "alertCount"},
			{Label: cp.sdk.I18n("First Trigger Time"), ValueKey: "firstTriggerTime"},
			{Label: cp.sdk.I18n("Last Trigger Time"), ValueKey: "lastTriggerTime"},
		},
	}
}

func init() {
	name := fmt.Sprintf("component-protocol.components.%s.%s", common.ScenarioKey, common.ComponentNameEventOverviewInfo)
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator(common.ScenarioKey, common.ComponentNameEventOverviewInfo, func() servicehub.Provider {
		return &ComponentEventOverviewInfo{}
	})
}
