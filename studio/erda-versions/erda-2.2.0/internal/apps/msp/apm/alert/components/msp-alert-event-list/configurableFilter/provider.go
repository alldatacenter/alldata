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

	"github.com/ahmetb/go-linq/v3"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter/impl"
	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-infra/providers/i18n"
	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-list/common"
)

type provider struct {
	impl.DefaultFilter

	Log     logs.Logger
	I18n    i18n.Translator            `autowired:"i18n" translator:"msp-alert-event-list"`
	Monitor monitor.AlertServiceServer `autowired:"erda.core.monitor.alert.AlertService"`
}

func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		sdk.Tran = p.I18n
		inParams, err := common.ParseFromCpSdk(sdk)
		if err != nil {
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}
		data := &filter.Data{
			Conditions: p.getConfigurableFilterOptions(sdk, inParams),
			HideSave:   true,
			Operations: map[cptype.OperationKey]cptype.Operation{
				filter.OpFilter{}.OpKey(): cputil.NewOpBuilder().Build(),
			},
		}
		return &impl.StdStructuredPtr{StdDataPtr: data}
	}
}

func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
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

func (p *provider) getConfigurableFilterOptions(sdk *cptype.SDK, inParams *common.InParams) []interface{} {
	alerts, _ := p.getAlerts(sdk.Ctx, inParams)
	alertSelectCondition := model.NewSelectCondition("alertIds", sdk.I18n("alert strategy"), common.IdNameValuesToSelectOptions(alerts)).WithPlaceHolder(sdk.I18n("select alert strategy"))

	alertLevels := p.getAlertLevels(sdk, inParams)
	alertLevelSelectCondition := model.NewSelectCondition("alertLevels", sdk.I18n("level"), common.IdNameValuesToSelectOptions(alertLevels)).WithPlaceHolder(sdk.I18n("select level"))

	alertStates := p.getAlertStates(sdk, inParams)
	alertStateSelectCondition := model.NewSelectCondition("alertStates", sdk.I18n("status"), common.IdNameValuesToSelectOptions(alertStates)).WithPlaceHolder(sdk.I18n("select status"))

	alertSources := p.getAlertSources(sdk, inParams)
	alertSourceSelectCondition := model.NewSelectCondition("alertSources", sdk.I18n("event source"), common.IdNameValuesToSelectOptions(alertSources)).WithPlaceHolder(sdk.I18n("select event source"))

	alertTriggerTimeRangeCondition := model.NewDateRangeCondition("lastTriggerTime", sdk.I18n("last trigger time"))
	return []interface{}{alertSelectCondition, alertLevelSelectCondition, alertStateSelectCondition, alertSourceSelectCondition, alertTriggerTimeRangeCondition}
}

func (p *provider) getAlertRules(ctx context.Context, params *common.InParams) ([]*common.IdNameValue, error) {
	resp, err := p.Monitor.QueryAlertRule(ctx, &monitor.QueryAlertRuleRequest{
		Scope:   params.Scope,
		ScopeId: params.ScopeId,
	})
	if err != nil {
		return nil, err
	}

	var values []*common.IdNameValue
	linq.From(resp.Data.AlertTypeRules).SelectManyBy(func(i interface{}) linq.Query {
		return linq.From(i.(*monitor.AlertTypeRule).Rules)
	}, func(inner interface{}, outer interface{}) interface{} {
		return &common.IdNameValue{
			Name: inner.(*monitor.AlertRule).Name,
			Id:   inner.(*monitor.AlertRule).Id,
		}
	}).ToSlice(&values)

	return values, nil
}

func (p *provider) getAlerts(ctx context.Context, params *common.InParams) ([]*common.IdNameValue, error) {
	resp, err := p.Monitor.QueryAlert(ctx, &monitor.QueryAlertRequest{
		Scope:    params.Scope,
		ScopeId:  params.ScopeId,
		PageSize: 100,
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

func (p *provider) getAlertLevels(sdk *cptype.SDK, params *common.InParams) []*common.IdNameValue {
	levels := []string{common.Fatal, common.Critical, common.Warning, common.Notice}

	var values []*common.IdNameValue
	for _, level := range levels {
		values = append(values, &common.IdNameValue{
			Id:   level,
			Name: sdk.I18n(level),
		})
	}

	return values
}

func (p *provider) getAlertStates(sdk *cptype.SDK, params *common.InParams) []*common.IdNameValue {
	states := []string{common.Alert, common.Recover, common.Pause, common.Stop}

	var values []*common.IdNameValue
	for _, state := range states {
		values = append(values, &common.IdNameValue{
			Id:   state,
			Name: sdk.I18n(state),
		})
	}

	return values
}

func (p *provider) getAlertSources(sdk *cptype.SDK, params *common.InParams) []*common.IdNameValue {
	sources := []string{common.System, common.Custom}

	var values []*common.IdNameValue
	for _, source := range sources {
		values = append(values, &common.IdNameValue{
			Id:   source,
			Name: sdk.I18n(source),
		})
	}

	return values
}

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameConfigurableFilter, &provider{})
}
