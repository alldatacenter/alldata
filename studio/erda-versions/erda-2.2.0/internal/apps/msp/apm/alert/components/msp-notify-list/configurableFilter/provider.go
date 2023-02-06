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

package configurableFilter

import (
	"context"
	"math"
	"strings"

	"github.com/ahmetb/go-linq/v3"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter/impl"
	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-infra/providers/i18n"
	messenger "github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-notify-list/common"
)

type provider struct {
	impl.DefaultFilter

	Log       logs.Logger
	I18n      i18n.Translator               `autowired:"i18n" translator:"msp-notify-list"`
	Messenger messenger.NotifyServiceServer `autowired:"erda.core.messenger.notify.NotifyService"`
	Monitor   monitor.AlertServiceServer    `autowired:"erda.core.monitor.alert.AlertService"`
}

func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		sdk.Tran = p.I18n
		inParams, err := common.ParseFromCpSdk(sdk)
		if err != nil {
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}
		p.StdDataPtr = &filter.Data{
			HideSave:   true,
			Conditions: p.getConfigurableFilterOptions(sdk, inParams),
			Operations: map[cptype.OperationKey]cptype.Operation{
				filter.OpFilter{}.OpKey(): cputil.NewOpBuilder().Build(),
			},
		}
		return nil
	}
}

func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *provider) getConfigurableFilterOptions(sdk *cptype.SDK, inParams *common.InParams) []interface{} {
	alerts, _ := p.getAlerts(sdk.Ctx, inParams)
	alertSelectCondition := model.NewSelectCondition("alertId", sdk.I18n("LinkedStrategy"), common.IdNameValuesToSelectOptions(alerts)).WithPlaceHolder(sdk.I18n("LinkedStrategy")).WithMode("single")
	notifyStatus := p.getNotifyStatus(sdk, inParams)
	notifyStatusCondition := model.NewSelectCondition("status", sdk.I18n("Status"), common.IdNameValuesToSelectOptions(notifyStatus)).WithPlaceHolder(sdk.I18n("SelectStatus")).WithMode("single")
	notifyChannel := p.getNotifyChannel(sdk, inParams)
	alertChannelSelectCondition := model.NewSelectCondition("channel", sdk.I18n("NotifyWay"), common.IdNameValuesToSelectOptions(notifyChannel)).WithPlaceHolder(sdk.I18n("SelectNotifyWay")).WithMode("single")
	notifySendTimeCondition := model.NewDateRangeCondition("sendTime", sdk.I18n("SendTime"))
	searchName := model.NewInputCondition("notifyName", sdk.I18n("NotifyName"))
	searchName.Placeholder = sdk.I18n("SearchName")
	searchName.Outside = true
	return []interface{}{alertSelectCondition, notifyStatusCondition, alertChannelSelectCondition, notifySendTimeCondition, searchName}
}

func (p *provider) getNotifyChannel(sdk *cptype.SDK, params *common.InParams) []*common.IdNameValue {
	channels := []string{"dingding", "webhook", "email", "mbox", "vms", "sms", "dingtalk_work_notice"}
	var values []*common.IdNameValue
	for _, channel := range channels {
		values = append(values, &common.IdNameValue{
			Id:   channel,
			Name: sdk.I18n(channel),
		})
	}

	return values
}

func (p *provider) getNotifyStatus(sdk *cptype.SDK, params *common.InParams) []*common.IdNameValue {
	status := []string{"success", "failed"}
	var values []*common.IdNameValue
	for _, state := range status {
		values = append(values, &common.IdNameValue{
			Id:   state,
			Name: sdk.I18n(state),
		})
	}

	return values
}

func (p *provider) getAlerts(ctx context.Context, params *common.InParams) ([]*common.IdNameValue, error) {
	resp, err := p.Monitor.QueryAlert(ctx, &monitor.QueryAlertRequest{
		Scope:    params.Scope,
		ScopeId:  params.ScopeID,
		PageSize: math.MaxInt64,
		PageNo:   1,
	})
	if err != nil {
		return nil, err
	}

	var values []*common.IdNameValue
	linq.From(resp.Data.List).Select(func(i interface{}) interface{} {
		return &common.IdNameValue{
			Name: i.(*monitor.Alert).Name,
			Id:   i.(*monitor.Alert).Id,
		}
	}).ToSlice(&values)

	return values, nil
}

func (p *provider) getNotifies(ctx context.Context, params *common.InParams) ([]*common.IdNameValue, error) {
	resp, err := p.Messenger.QueryAlertNotifyHistories(ctx, &messenger.QueryAlertNotifyHistoriesRequest{
		ScopeType: params.Scope,
		ScopeID:   params.ScopeID,
		PageNo:    1,
		PageSize:  math.MaxInt64,
	})
	if err != nil {
		return nil, err
	}
	var values []*common.IdNameValue
	linq.From(resp.Data.List).Select(func(i interface{}) interface{} {
		str := strings.TrimLeft(i.(*messenger.NotifyHistory).NotifyItemDisplayName, "【")
		str = strings.TrimRight(str, "】")
		return &common.IdNameValue{
			Id:   i.(*messenger.NotifyHistory).Id,
			Name: str,
		}
	}).ToSlice(&values)
	return values, nil
}

func (p *provider) RegisterFilterOp(opData filter.OpFilter) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		common.NewConfigurableFilterOptions().
			DecodeFromClientData(opData.ClientData).
			SetToGlobalState(*sdk.GlobalState)
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterFilterItemSaveOp(opData filter.OpFilterItemSave) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (p *provider) RegisterFilterItemDeleteOp(opData filter.OpFilterItemDelete) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameConfigurableFilter, &provider{})
}
