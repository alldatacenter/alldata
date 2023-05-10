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

package eventStatusInfo

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	messenger "github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-notify-detail/common"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/common/apis"
)

func (cp *ComponentEventOverviewInfo) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := cp.GenComponentState(c); err != nil {
		return err
	}
	cp.Messenger = common.GetMessengerServiceFromContext(ctx)
	cp.bdl = common.GetCoreServiceUrlFromContext(ctx)
	cp.ctx = ctx
	cp.sdk = cputil.SDK(ctx)
	inParams, err := common.ParseFromCpSdk(cp.sdk)
	detailCtx := utils.NewContextWithHeader(ctx)
	alertIndex, err := cp.Messenger.GetAlertNotifyDetail(detailCtx, &messenger.GetAlertNotifyDetailRequest{
		Id: inParams.Id,
	})
	if err != nil {
		return fmt.Errorf("failed to gen eventStatusInfo component err: %s", err)
	}
	common.SetNotifyIndexToGlobalState(*gs, alertIndex.Data)
	if alertIndex == nil {
		return errors.Errorf("alertEvent should be exists in globalState")
	}
	status := Status{
		Label: cp.sdk.I18n(alertIndex.Data.Status),
		Color: "green",
	}
	if alertIndex.Data.Status == "failed" {
		status.Color = "red"
	}

	//根据groupid获取通知组名字
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.ParseInt(orgIdStr, 10, 64)
	if err != nil {
		return err
	}
	userIdStr := apis.GetUserID(ctx)
	groupId, err := strconv.Atoi(alertIndex.Data.NotifyGroup)
	if err != nil {
		return err
	}
	notifyGroup, err := cp.bdl.GetNotifyGroupDetail(int64(groupId), orgId, userIdStr)
	if err != nil {
		return err
	}
	sendTime := time.Unix(alertIndex.Data.SendTime.GetSeconds(), int64(alertIndex.Data.SendTime.GetNanos())).Format("2006/01/02 15:04:05")
	data := Data{
		Channel:    cp.sdk.I18n(alertIndex.Data.Channel),
		Status:     status,
		SendTime:   sendTime,
		Group:      notifyGroup.Name,
		LinkedRule: alertIndex.Data.NotifyRule,
	}
	cp.Props = cp.getProps()
	cp.Data = map[string]Data{
		"data": data,
	}
	cp.Transfer(c)
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

func (cp *ComponentEventOverviewInfo) getProps() Props {
	return Props{
		RequestIgnore: []string{"data"},
		ColumnNum:     4,
		Fields: []Field{
			{
				Label:    cp.sdk.I18n("Channel"),
				ValueKey: "channel",
			},
			{
				Label:      cp.sdk.I18n("Status"),
				ValueKey:   "status",
				RenderType: "tagsRow",
			},
			{
				Label:    cp.sdk.I18n("SendTime"),
				ValueKey: "sendTime",
			},
			{
				Label:    cp.sdk.I18n("Group"),
				ValueKey: "group",
			},
			{
				Label:    cp.sdk.I18n("Rule"),
				ValueKey: "linkedRule",
			},
		},
	}
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

func init() {
	name := fmt.Sprintf("component-protocol.components.%s.%s", common.ScenarioKey, common.ComponentNameEventStatusInfo)
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator(common.ScenarioKey, common.ComponentNameEventStatusInfo, func() servicehub.Provider {
		return &ComponentEventOverviewInfo{}
	})
}
