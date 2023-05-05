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

package table

import (
	"context"
	"fmt"
	"strconv"
	"time"

	"github.com/ahmetb/go-linq/v3"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/commodel"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-infra/providers/i18n"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-list/common"
)

type provider struct {
	impl.DefaultTable
	Log                 logs.Logger
	I18n                i18n.Translator              `autowired:"i18n" translator:"msp-alert-event-list"`
	MonitorAlertService monitorpb.AlertServiceServer `autowired:"erda.core.monitor.alert.AlertService"`
	Metric              metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		inParams, err := common.ParseFromCpSdk(sdk)
		if err != nil {
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}
		filters := common.NewConfigurableFilterOptions().GetFromGlobalState(*sdk.GlobalState)
		pageNo, pageSize := common.GetPagingFromGlobalState(*sdk.GlobalState)
		sorts := common.GetSortsFromGlobalState(*sdk.GlobalState)

		data, err := p.queryAlertEvents(sdk, sdk.Ctx, inParams, filters, sorts, pageNo, pageSize)
		if err != nil {
			p.Log.Error(err)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}

		p.StdDataPtr = &table.Data{
			Table: *data,
			Operations: map[cptype.OperationKey]cptype.Operation{
				table.OpTableChangePage{}.OpKey(): cputil.NewOpBuilder().WithServerDataPtr(&table.OpTableChangePageServerData{}).Build(),
				table.OpTableChangeSort{}.OpKey(): cputil.NewOpBuilder().Build(),
			},
		}
		return nil
	}
}

func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *provider) RegisterTablePagingOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		common.SetPagingToGlobalState(*sdk.GlobalState, opData.ClientData)
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterTableChangePageOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		common.SetPagingToGlobalState(*sdk.GlobalState, opData.ClientData)
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterTableSortOp(opData table.OpTableChangeSort) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		common.SetSortsToGlobalState(*sdk.GlobalState, opData.ClientData)
		p.RegisterRenderingOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterBatchRowsHandleOp(opData table.OpBatchRowsHandle) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *provider) RegisterRowSelectOp(opData table.OpRowSelect) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *provider) RegisterRowAddOp(opData table.OpRowAdd) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *provider) RegisterRowEditOp(opData table.OpRowEdit) (opFunc cptype.OperationFunc) {
	return nil
}

func (p *provider) RegisterRowDeleteOp(opData table.OpRowDelete) (opFunc cptype.OperationFunc) {
	return nil
}

// Provide .
func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p
}

func (p *provider) queryAlertEvents(sdk *cptype.SDK, ctx context.Context, params *common.InParams, filters *common.ConfigurableFilterOptions, sorts []*common.Sort, pageNo int64, pageSize int64) (*table.Table, error) {
	var sortDefines []*monitorpb.AlertEventSort
	linq.From(sorts).Select(func(i interface{}) interface{} {
		return &monitorpb.AlertEventSort{
			SortField:  i.(*common.Sort).FieldKey,
			Descending: !i.(*common.Sort).Ascending,
		}
	}).ToSlice(&sortDefines)
	events, err := p.MonitorAlertService.GetAlertEvents(ctx, &monitorpb.GetAlertEventRequest{
		Scope:   params.Scope,
		ScopeId: params.ScopeId,
		Condition: &monitorpb.GetAlertEventRequestCondition{
			Name:                 filters.Name,
			AlertLevels:          filters.AlertLevels,
			AlertIds:             filters.AlertIds,
			AlertStates:          filters.AlertStates,
			AlertSources:         filters.AlertSources,
			LastTriggerTimeMsMin: filters.LastTriggerTimeMin,
			LastTriggerTimeMsMax: filters.LastTriggerTimeMax,
		},
		Sorts:    sortDefines,
		PageNo:   pageNo,
		PageSize: pageSize,
	})
	if err != nil {
		return nil, err
	}

	t := &table.Table{
		Columns: table.ColumnsInfo{
			Orders: []table.ColumnKey{table.ColumnKey(AlertListTableColumnName),
				table.ColumnKey(AlertListTableColumnAlertName),
				table.ColumnKey(AlertListTableColumnTriggerCount),
				table.ColumnKey(AlertListTableColumnAlertLevel),
				table.ColumnKey(AlertListTableColumnAlertState),
				table.ColumnKey(AlertListTableColumnAlertSource),
				table.ColumnKey(AlertListTableColumnLastTriggerTime),
			},
			ColumnsMap: map[table.ColumnKey]table.Column{
				table.ColumnKey(AlertListTableColumnName):            {Title: sdk.I18n(AlertListTableColumnName)},
				table.ColumnKey(AlertListTableColumnAlertName):       {Title: sdk.I18n(AlertListTableColumnAlertName)},
				table.ColumnKey(AlertListTableColumnTriggerCount):    {Title: sdk.I18n(AlertListTableColumnTriggerCount)},
				table.ColumnKey(AlertListTableColumnAlertLevel):      {Title: sdk.I18n(AlertListTableColumnAlertLevel)},
				table.ColumnKey(AlertListTableColumnAlertState):      {Title: sdk.I18n(AlertListTableColumnAlertState)},
				table.ColumnKey(AlertListTableColumnAlertSource):     {Title: sdk.I18n(AlertListTableColumnAlertSource)},
				table.ColumnKey(AlertListTableColumnLastTriggerTime): {Title: sdk.I18n(AlertListTableColumnLastTriggerTime), EnableSort: true, FieldBindToOrder: "LastTriggerTime"},
			},
		},
		Total:    uint64(events.Total),
		PageNo:   uint64(pageNo),
		PageSize: uint64(pageSize),
	}

	for _, item := range events.Items {
		reqParams := map[string]*structpb.Value{
			"eventId": structpb.NewStringValue(item.Id),
		}
		statement := fmt.Sprintf("SELECT count(timestamp) FROM analyzer_alert " +
			"WHERE family_id::tag=$eventId AND alert_suppressed::tag='false' ")
		resp, err := p.Metric.QueryWithInfluxFormat(ctx, &metricpb.QueryWithInfluxFormatRequest{
			Start:     "0",
			End:       strconv.FormatInt(time.Now().UnixNano()/1e6, 10),
			Statement: statement,
			Params:    reqParams,
		})
		triggerCount := float64(0)
		if err != nil {
			p.Log.Errorf("query trigger count error, eventId: %s, err: \n %s", item.Id, err)
		}
		if resp != nil && resp.Results != nil {
			triggerCount = resp.Results[0].Series[0].Rows[0].Values[0].GetNumberValue()
		}
		t.Rows = append(t.Rows, table.Row{
			ID: table.RowID(item.Id),
			CellsMap: map[table.ColumnKey]table.Cell{
				table.ColumnKey(AlertListTableColumnName):            table.NewTextCell(item.Name).Build(),
				table.ColumnKey(AlertListTableColumnAlertName):       table.NewTextCell(item.AlertName).Build(),
				table.ColumnKey(AlertListTableColumnTriggerCount):    table.NewTextCell(strconv.FormatInt(int64(triggerCount), 10)).Build(),
				table.ColumnKey(AlertListTableColumnAlertLevel):      table.NewCompleteTextCell(commodel.Text{Text: sdk.I18n(item.AlertLevel), Status: commodel.UnifiedStatus(common.LevelColors[item.AlertLevel])}).Build(),
				table.ColumnKey(AlertListTableColumnAlertState):      table.NewCompleteTextCell(commodel.Text{Text: sdk.I18n(item.AlertState), Status: commodel.UnifiedStatus(common.StateColors[item.AlertState])}).Build(),
				table.ColumnKey(AlertListTableColumnAlertSource):     table.NewTextCell(sdk.I18n(item.AlertSource)).Build(),
				table.ColumnKey(AlertListTableColumnLastTriggerTime): table.NewTextCell(time.Unix(item.LastTriggerTime/1e3, 0).Format("2006/01/02 15:04:05")).Build(),
			},
		})
	}
	return t, nil
}

const (
	AlertListTableColumnName            string = "alertListTableColumnName"
	AlertListTableColumnAlertName       string = "alertListTableColumnAlertName"
	AlertListTableColumnTriggerCount    string = "alertListTableColumnTriggerCount"
	AlertListTableColumnAlertLevel      string = "alertListTableColumnAlertLevel"
	AlertListTableColumnAlertState      string = "alertListTableColumnAlertState"
	AlertListTableColumnAlertSource     string = "alertListTableColumnAlertSource"
	AlertListTableColumnLastTriggerTime string = "alertListTableColumnLastTriggerTime"
)

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameTable, &provider{})
}
