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
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
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
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail/common"
	pkgtime "github.com/erda-project/erda/pkg/time"
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
		pageNo, pageSize := common.GetPagingFromGlobalState(*sdk.GlobalState)
		sorts := common.GetSortsFromGlobalState(*sdk.GlobalState)

		data, err := p.queryAlertEvents(sdk, sdk.Ctx, inParams, sorts, pageNo, pageSize)
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

func (p *provider) queryAlertEvents(sdk *cptype.SDK, ctx context.Context, params *common.InParams, sorts []*common.Sort, pageNo int64, pageSize int64) (*table.Table, error) {
	reqParams := map[string]*structpb.Value{
		"eventId": structpb.NewStringValue(params.AlertEventId),
	}

	statement := fmt.Sprintf("SELECT count(timestamp) FROM analyzer_alert " +
		"WHERE family_id::tag=$eventId AND alert_suppressed::tag='false' ")
	resp, err := p.Metric.QueryWithInfluxFormat(ctx, &metricpb.QueryWithInfluxFormatRequest{
		Start:     "0",
		End:       strconv.FormatInt(time.Now().UnixNano()/1e6, 10),
		Statement: statement,
		Params:    reqParams,
	})
	if err != nil {
		return nil, err
	}
	if len(resp.Results[0].Series[0].Rows) == 0 {
		return nil, fmt.Errorf("empty result")
	}
	total := resp.Results[0].Series[0].Rows[0].Values[0].GetNumberValue()

	t := &table.Table{
		Columns: table.ColumnsInfo{
			Orders: []table.ColumnKey{"TriggerTime", "AlertState", "TriggerValues", "TriggerDuration", "Tags"},
			ColumnsMap: map[table.ColumnKey]table.Column{
				"TriggerTime":     {Title: sdk.I18n("Trigger Time"), AscOrder: new(bool)},
				"AlertState":      {Title: sdk.I18n("Alert Status")},
				"TriggerValues":   {Title: sdk.I18n("Trigger Values")},
				"TriggerDuration": {Title: sdk.I18n("Alert Duration")},
				"Tags":            {Title: sdk.I18n("Tags")},
			},
		},
		Total:    uint64(total),
		PageNo:   uint64(pageNo),
		PageSize: uint64(pageSize),
	}

	statement = fmt.Sprintf("SELECT * FROM analyzer_alert "+
		"WHERE family_id::tag=$eventId AND alert_suppressed::tag='false' "+
		"ORDER BY timestamp DESC "+
		"LIMIT %v OFFSET %v ", pageSize, (pageNo-1)*pageSize)
	resp, err = p.Metric.QueryWithInfluxFormat(ctx, &metricpb.QueryWithInfluxFormatRequest{
		Start:     "0",
		End:       strconv.FormatInt(time.Now().UnixNano()/1e6, 10),
		Statement: statement,
		Params:    reqParams,
	})
	if err != nil {
		return nil, err
	}

	colIndexMap := map[string]int{}
	for i, col := range resp.Results[0].Series[0].Columns {
		colIndexMap[col] = i
	}
	for _, item := range resp.Results[0].Series[0].Rows {
		t.Rows = append(t.Rows, table.Row{
			CellsMap: map[table.ColumnKey]table.Cell{
				"TriggerTime":     table.NewTextCell(common.FormatTimeMs(int64(item.Values[colIndexMap["timestamp"]].GetNumberValue()) / 1e6)).Build(),
				"AlertState":      table.NewTextCell(sdk.I18n(item.Values[colIndexMap["trigger::tag"]].GetStringValue())).Build(),
				"TriggerValues":   table.NewLabelsCell(p.getTriggerValueLabels(colIndexMap, item)).Build(),
				"TriggerDuration": table.NewTextCell(p.formatTriggerDuration(colIndexMap, item)).Build(),
				"Tags":            table.NewLabelsCell(p.getTagLabels(colIndexMap, item)).Build(),
			},
		})
	}

	return t, nil
}

func (p *provider) formatTriggerDuration(colMap map[string]int, row *metricpb.Row) string {
	index, ok := colMap["trigger_duration::field"]
	if !ok {
		return "-"
	}

	val, unit := pkgtime.AutomaticConversionUnit(row.Values[index].GetNumberValue() * 1e6)
	return fmt.Sprintf("%v%s", val, unit)
}

func (p *provider) getTriggerValueLabels(colMap map[string]int, row *metricpb.Row) commodel.Labels {
	var labels = commodel.Labels{}
	index, ok := colMap["alert_trigger_functions::field"]
	if !ok {
		return labels
	}

	var exprFunctions []*common.AlertExpressionFunction
	value := row.Values[index].GetStringValue()
	err := json.Unmarshal([]byte(value), &exprFunctions)
	if err != nil {
		p.Log.Errorf("failed to decode the expressionFunction: %v", value)
		return labels
	}

	for _, function := range exprFunctions {
		if function.Operator == nil {
			continue
		}

		thresholdValue, triggerValue := function.Value, function.Value
		var triggerValueFieldName string
		if function.Alias != nil {
			triggerValueFieldName = fmt.Sprintf("%s::field", *function.Alias)
		} else {
			triggerValueFieldName = fmt.Sprintf("%s_%s::field", function.Field, function.Aggregator)
		}
		if val, ok := colMap[triggerValueFieldName]; ok {
			triggerValue = val
		}

		labels.Labels = append(labels.Labels, commodel.Label{
			Title: fmt.Sprintf("%s_%s=%v (%v %v)", function.Field, function.Aggregator, triggerValue, p.formatOperator(*function.Operator), thresholdValue),
		})
	}

	return labels
}

func (p *provider) formatOperator(op string) string {
	switch op {
	case "gte":
		return ">="
	case "gt":
		return ">"
	case "lte":
		return "<"
	case "lt":
		return "<"
	case "eq":
		return "="
	case "neq":
		return "!="
	default:
		return op
	}
}

func (p *provider) getTagLabels(colMap map[string]int, row *metricpb.Row) commodel.Labels {
	var labels = commodel.Labels{}
	var orderedColMap []linq.KeyValue

	linq.From(colMap).OrderBy(func(i interface{}) interface{} {
		return i.(linq.KeyValue).Key
	}).ToSlice(&orderedColMap)

	for _, item := range orderedColMap {
		if !strings.HasSuffix(item.Key.(string), "::tag") {
			continue
		}

		labels.Labels = append(labels.Labels, commodel.Label{
			Title: fmt.Sprintf("%v=%v", item.Key, row.Values[item.Value.(int)].GetStringValue()),
		})
	}
	return labels
}

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameEventHistoryTable, &provider{})
}
