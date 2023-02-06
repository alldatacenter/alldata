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

package monitor_data_chart

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/ahmetb/go-linq/v3"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	structure "github.com/erda-project/erda-infra/providers/component-protocol/components/commodel/data-structure"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/complexgraph"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/complexgraph/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-alert-event-detail/common"
	"github.com/erda-project/erda/pkg/math"
)

type provider struct {
	impl.DefaultComplexGraph

	Log                 logs.Logger
	I18n                i18n.Translator              `autowired:"i18n" translator:"msp-alert-overview"`
	Metric              metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
	MonitorAlertService monitorpb.AlertServiceServer `autowired:"erda.core.monitor.alert.AlertService"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		sdk.Tran = p.I18n
		data, err := p.getAlertEventChart(sdk)
		if err != nil {
			p.Log.Errorf("failed to render chart: %s", err)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}
		return &impl.StdStructuredPtr{
			StdDataPtr: data,
		}
	}
}

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *provider) getAlertEventChart(sdk *cptype.SDK) (*complexgraph.Data, error) {
	inParams, err := common.ParseFromCpSdk(sdk)
	if err != nil {
		return nil, err
	}
	alertEvent := common.GetAlertEventFromGlobalState(*sdk.GlobalState)
	if alertEvent == nil {
		return nil, fmt.Errorf("alertEvent should be in globalState")
	}

	alertExpr, err := p.MonitorAlertService.GetRawAlertExpression(sdk.Ctx, &monitorpb.GetRawAlertExpressionRequest{
		Id: alertEvent.ExpressionID,
	})
	if err != nil {
		return nil, fmt.Errorf("failed to get alert raw expression: %s", err)
	}

	var expression common.AlertExpression
	err = json.Unmarshal([]byte(alertExpr.Data.Expression), &expression)
	if err != nil {
		return nil, fmt.Errorf("failed to decode raw expression: %s", err)
	}

	return p.queryMetrics(sdk, inParams, alertEvent, &expression)
}

func (p *provider) queryMetrics(sdk *cptype.SDK, inParams *common.InParams, alertEvent *monitorpb.AlertEventItem, alertExpression *common.AlertExpression) (*complexgraph.Data, error) {
	alertSubject, err := p.decodeAlertSubject(alertEvent)
	if err != nil {
		return nil, fmt.Errorf("failed to decode alert subject: %s", err)
	}

	whereSql := bytes.Buffer{}
	params := map[string]*structpb.Value{}
	for k, v := range alertSubject {
		val, err := structpb.NewValue(v)
		if err != nil {
			return nil, fmt.Errorf("failed to convert alertSubject to pbValue: %s, value: %v", err, v)
		}
		params[k] = val
		if whereSql.Len() > 0 {
			whereSql.WriteString("AND ")
		}
		whereSql.WriteString(fmt.Sprintf("%s::tag = $%s ", k, k))
	}

	function, ok := linq.From(alertExpression.Functions).FirstWith(func(i interface{}) bool {
		return i.(*common.AlertExpressionFunction).Operator != nil && len(*i.(*common.AlertExpressionFunction).Operator) > 0
	}).(*common.AlertExpressionFunction)
	if !ok {
		return nil, fmt.Errorf("no trigger function defined")
	}

	var filters []*metricpb.Filter
	for _, filter := range alertExpression.Filters {
		pbVal, err := structpb.NewValue(filter.Value)
		if err != nil {
			return nil, fmt.Errorf("failed to convert to pb value: %s, val: %v", err, filter.Value)
		}
		filters = append(filters, &metricpb.Filter{
			Key:   fmt.Sprintf("tags.%s", filter.Tag),
			Value: pbVal,
			Op:    filter.Operator,
		})
	}

	metric := alertExpression.Metric
	if len(metric) == 0 {
		metric = alertExpression.Metrics[0]
	}

	statement := fmt.Sprintf("SELECT timestamp(), %s(%s::field) FROM %s WHERE %s GROUP BY time(%v)",
		function.Aggregator, function.Field, metric, whereSql.String(), common.GetInterval(inParams.StartTime, inParams.EndTime, time.Second, 60))

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(inParams.StartTime, 10),
		End:       strconv.FormatInt(inParams.EndTime, 10),
		Statement: statement,
		Params:    params,
		Filters:   filters,
	}
	resp, err := p.Metric.QueryWithInfluxFormat(sdk.Ctx, request)
	if err != nil {
		return nil, err
	}
	columns := resp.Results[0].Series[0].Columns
	rows := resp.Results[0].Series[0].Rows

	//build the graph
	xAxisBuilder := complexgraph.NewAxisBuilder().
		WithType(complexgraph.Category).
		WithDataStructure(structure.Timestamp, "", true)
	yAxisBuilder := complexgraph.NewAxisBuilder().
		WithType(complexgraph.Value).
		WithDataStructure(structure.Number, "", true).
		WithDimensions(sdk.I18n(columns[2]))
	sereBuilder := complexgraph.NewSereBuilder().
		WithType(complexgraph.Line).
		WithDimension(sdk.I18n(columns[2]))

	for _, row := range rows {
		xAxisBuilder.WithData(int64(row.Values[1].GetNumberValue()) / 1e6)
		sereBuilder.WithData(math.DecimalPlacesWithDigitsNumber(row.Values[2].GetNumberValue(), 2))
	}

	dataBuilder := complexgraph.NewDataBuilder().
		WithXAxis(xAxisBuilder.Build()).
		WithYAxis(yAxisBuilder.Build()).
		WithDimensions(columns[2]).
		WithSeries(sereBuilder.Build())

	return dataBuilder.Build(), nil
}

func (p *provider) decodeAlertSubject(alertEvent *monitorpb.AlertEventItem) (map[string]interface{}, error) {
	var subjects map[string]interface{}
	err := json.Unmarshal([]byte(alertEvent.AlertSubject), &subjects)
	return subjects, err
}

func init() {
	cpregister.RegisterProviderComponent(common.ScenarioKey, common.ComponentNameMonitorDataChart, &provider{})
}
