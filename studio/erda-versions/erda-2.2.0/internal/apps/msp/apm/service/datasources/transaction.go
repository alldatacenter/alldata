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

package datasources

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/ahmetb/go-linq/v3"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/bubblegraph"
	structure "github.com/erda-project/erda-infra/providers/component-protocol/components/commodel/data-structure"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/kv"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/linegraph"
	stdtable "github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/card"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/chart"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/table"
	"github.com/erda-project/erda/pkg/math"
)

func (p *provider) GetChart(ctx context.Context, chartType pb.ChartType, start, end int64, tenantId, serviceId string, layer common.TransactionLayerType, path string) (*linegraph.Data, error) {
	baseChart := &chart.BaseChart{
		StartTime: start,
		EndTime:   end,
		TenantId:  tenantId,
		ServiceId: serviceId,
		Layers:    []common.TransactionLayerType{layer},
		LayerPath: path,
		FuzzyPath: false,
		Metric:    p.Metric,
	}

	data, err := chart.Selector(strings.ToLower(chartType.String()), baseChart, ctx)
	if err != nil {
		return nil, err
	}
	layout := "2006-01-02 15:04:05"

	// convert model
	var xAxis []interface{}
	linq.From(data.View).
		Select(func(i interface{}) interface{} {
			return time.Unix(0, i.(*pb.Chart).Timestamp*1e6).Format(layout)
		}).
		ToSlice(&xAxis)
	dimension := linq.From(data.View).
		Select(func(i interface{}) interface{} { return i.(*pb.Chart).Dimension }).
		First()
	if dimension == nil {
		dimension = data.Type
	}
	var yAxis []interface{}
	linq.From(data.View).
		Select(func(i interface{}) interface{} {
			value := i.(*pb.Chart).Value
			switch strings.ToLower(chartType.String()) {
			case strings.ToLower(pb.ChartType_AvgDuration.String()):
				return math.DecimalPlacesWithDigitsNumber(value/1e6, 2)
			default:
				return value
			}
		}).
		ToSlice(&yAxis)
	yUnitGetter := func(chartType pb.ChartType) (structure.Type, structure.Precision) {
		switch strings.ToLower(chartType.String()) {
		case strings.ToLower(pb.ChartType_AvgDuration.String()):
			return structure.Time, structure.Millisecond
		case strings.ToLower(pb.ChartType_RPS.String()):
			return structure.String, structure.ReqSlashS
		default:
			return structure.Number, ""
		}
	}
	yType, yUnit := yUnitGetter(chartType)

	builder := linegraph.NewDataBuilder().WithTitle(p.I18n.Text(ctx.Value(common.LangKey).(i18n.LanguageCodes), strings.ToLower(data.Type)))
	builder.WithXAxis(xAxis...).WithXOptions(linegraph.NewOptionsBuilder().WithType(structure.String).Build())
	builder.WithYAxis(dimension.(string), yAxis...).WithYOptions(linegraph.NewOptionsBuilder().
		WithDimension(dimension.(string)).WithType(yType).WithPrecision(yUnit).WithEnable(true).Build())
	line := builder.Build()
	line.SubTitle = chart.GetChartUnitDefault(chartType, ctx.Value(common.LangKey).(i18n.LanguageCodes), p.I18n)

	return line, nil
}

func (p *provider) GetBubbleChart(ctx context.Context, bubbleType BubbleChartType, preferredChartPoints int64, start, end int64, tenantId, serviceId string, layer common.TransactionLayerType, path string) (*bubblegraph.Data, error) {
	var chartType pb.ChartType
	switch bubbleType {
	case BubbleChartReqDistribution:
		chartType = pb.ChartType_AvgDurationDistribution
	case BubbleChartSlowReqDistribution:
		chartType = pb.ChartType_SlowDurationDistribution
	case BubbleChartErrorReqDistribution:
		chartType = pb.ChartType_ErrorDurationDistribution
	default:
		return nil, fmt.Errorf("not supported bubbleChartType: %s", bubbleType)
	}

	interval := getInterval(start, end, time.Second, preferredChartPoints)
	baseChart := &chart.BaseChart{
		StartTime: start,
		EndTime:   end,
		Interval:  interval,
		TenantId:  tenantId,
		ServiceId: serviceId,
		Layers:    []common.TransactionLayerType{layer},
		LayerPath: path,
		FuzzyPath: false,
		Metric:    p.Metric,
	}

	data, err := chart.Selector(strings.ToLower(chartType.String()), baseChart, ctx)
	if err != nil {
		return nil, err
	}

	// convert model
	layout := "2006-01-02 15:04:05"
	yUnitGetter := func(chartType pb.ChartType) (structure.Type, structure.Precision) {
		switch strings.ToLower(chartType.String()) {
		case strings.ToLower(pb.ChartType_AvgDurationDistribution.String()), strings.ToLower(pb.ChartType_SlowDurationDistribution.String()), strings.ToLower(pb.ChartType_ErrorDurationDistribution.String()):
			return structure.Time, structure.Millisecond
		default:
			return structure.Number, ""
		}
	}
	yAxisFormatter := func(value float64) interface{} {
		switch strings.ToLower(chartType.String()) {
		case strings.ToLower(pb.ChartType_AvgDurationDistribution.String()), strings.ToLower(pb.ChartType_SlowDurationDistribution.String()), strings.ToLower(pb.ChartType_ErrorDurationDistribution.String()):
			return math.DecimalPlacesWithDigitsNumber(value/1e6, 2)
		default:
			return value
		}
	}

	dimension := linq.From(data.View).
		Select(func(i interface{}) interface{} { return i.(*pb.Chart).Dimension }).
		First()
	if dimension == nil {
		dimension = data.Type
	}

	yType, yUnit := yUnitGetter(chartType)
	builder := bubblegraph.NewDataBuilder().WithTitle(p.I18n.Text(ctx.Value(common.LangKey).(i18n.LanguageCodes), strings.ToLower(string(bubbleType)))).
		WithXOptions(bubblegraph.NewOptionsBuilder().WithType(structure.String).Build()).
		WithYOptions(bubblegraph.NewOptionsBuilder().WithType(yType).WithPrecision(yUnit).WithDimension(dimension.(string)).WithEnable(true).Build())
	for _, item := range data.View {
		builder.WithBubble(bubblegraph.NewBubbleBuilder().
			WithDimension(dimension.(string)).
			WithValueX(time.Unix(0, item.Timestamp*1e6).Format(layout)).
			WithValueY(yAxisFormatter(item.Value)).
			WithValueSize(item.ExtraValues[0]).
			Build())
	}
	bubble := builder.Build()

	return bubble, nil
}

func (p *provider) GetCard(ctx context.Context, cardType card.CardType, start, end int64, tenantId, serviceId string, layer common.TransactionLayerType, path string) (*kv.KV, error) {
	baseCard := &card.BaseCard{
		StartTime: start,
		EndTime:   end,
		TenantId:  tenantId,
		ServiceId: serviceId,
		Layer:     layer,
		LayerPath: path,
		FuzzyPath: false,
		Metric:    p.Metric,
	}

	data, err := card.GetCard(ctx, cardType, baseCard)
	if err != nil {
		return nil, err
	}

	pair := &kv.KV{
		Key:    p.I18n.Text(ctx.Value(common.LangKey).(i18n.LanguageCodes), strings.ToLower(data.Name)),
		Value:  strconv.FormatFloat(data.Value, 'f', -1, 64),
		SubKey: data.Unit,
	}
	return pair, nil
}

func (p *provider) GetTable(ctx context.Context, builder table.Builder) (*stdtable.Table, error) {
	data, err := builder.GetTable(ctx)
	if err != nil {
		return nil, err
	}

	lang := ctx.Value(common.LangKey).(i18n.LanguageCodes)

	tt := stdtable.Table{
		Columns: stdtable.ColumnsInfo{
			ColumnsMap: map[stdtable.ColumnKey]stdtable.Column{},
		},
		Total:    uint64(data.Total),
		PageSize: uint64(builder.GetBaseBuildParams().PageSize),
		PageNo:   uint64(builder.GetBaseBuildParams().PageNo),
	}

	for _, column := range data.Columns {
		tt.Columns.Orders = append(tt.Columns.Orders, stdtable.ColumnKey(column.Key))
		tt.Columns.ColumnsMap[stdtable.ColumnKey(column.Key)] = stdtable.Column{Title: p.I18n.Text(lang, column.Key), FieldBindToOrder: column.Key, EnableSort: column.Sortable}
	}
	for _, row := range data.Rows {
		stdrow := stdtable.Row{
			CellsMap: map[stdtable.ColumnKey]stdtable.Cell{},
		}
		for _, cell := range row.GetCells() {
			stdrow.CellsMap[stdtable.ColumnKey(cell.Key)] = stdtable.NewTextCell(fmt.Sprintf("%v", cell.Value)).Build()
		}
		tt.Rows = append(tt.Rows, stdrow)
	}

	return &tt, nil
}
