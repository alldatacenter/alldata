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
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/commodel"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/commom/custom"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/commom/trace"
	pkgtime "github.com/erda-project/erda/pkg/time"
)

type provider struct {
	impl.DefaultTable
	custom.TraceInParams
	Log          logs.Logger
	I18n         i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	TraceService *query.TraceService          `autowired:"erda.msp.apm.trace.TraceService"`
	Metric       metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

func (p *provider) getSort(sort common.Sort) string {
	switch sort.FieldKey {
	case string(trace.ColumnDuration):
		if sort.Ascending {
			return pb.SortCondition_TRACE_DURATION_ASC.String()
		}
		return pb.SortCondition_TRACE_DURATION_DESC.String()
	case string(trace.ColumnStartTime):
		if sort.Ascending {
			return pb.SortCondition_TRACE_TIME_ASC.String()
		}
		return pb.SortCondition_TRACE_TIME_DESC.String()
	case string(trace.ColumnSpanCount):
		if sort.Ascending {
			return pb.SortCondition_SPAN_COUNT_ASC.String()
		}
		return pb.SortCondition_SPAN_COUNT_DESC.String()
	default:
		return pb.SortCondition_TRACE_TIME_DESC.String()
	}
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		params := p.TraceInParams.InParamsPtr
		pageNo, pageSize := trace.GetPagingFromGlobalState(*sdk.GlobalState)
		order := p.getSort(trace.GetSortsFromGlobalState(*sdk.GlobalState))
		traces, err := p.TraceService.GetTraces(sdk.Ctx, &pb.GetTracesRequest{
			TenantID:    params.TenantId,
			Status:      params.Status,
			StartTime:   params.StartTime,
			EndTime:     params.EndTime,
			Limit:       params.Limit,
			TraceID:     params.TraceId,
			DurationMin: params.DurationMin,
			DurationMax: params.DurationMax,
			Sort:        order,
			ServiceName: params.ServiceName,
			RpcMethod:   params.RpcMethod,
			HttpPath:    params.HttpPath,
			PageNo:      int64(pageNo),
			PageSize:    int64(pageSize),
		})
		tt := trace.InitTable(sdk.Lang, p.I18n)
		tt.Total = uint64(traces.Total)
		tt.PageSize = uint64(traces.PageSize)
		tt.PageNo = uint64(traces.PageNo)

		for _, t := range traces.Data {
			row := table.Row{CellsMap: map[table.ColumnKey]table.Cell{}}
			row.CellsMap[trace.ColumnTraceId] = table.NewCompleteTextCell(commodel.Text{Text: t.Id, EnableCopy: true}).Build()
			v, unit := pkgtime.AutomaticConversionUnit(t.Duration)

			row.CellsMap[trace.ColumnDuration] = table.NewTextCell(fmt.Sprintf("%v %s", v, unit)).Build()
			row.CellsMap[trace.ColumnStartTime] = table.NewTextCell(time.Unix(0, t.StartTime).Format("2006-01-02 15:04:05")).Build()
			row.CellsMap[trace.ColumnSpanCount] = table.NewTextCell(fmt.Sprintf("%v", t.SpanCount)).Build()
			var labels []commodel.Label
			for _, service := range t.Services {
				labels = append(labels, commodel.Label{ID: service})
			}
			row.CellsMap[trace.ColumnServices] = table.NewLabelsCell(commodel.Labels{Labels: labels}).Build()
			tt.Rows = append(tt.Rows, row)
		}

		if err != nil {
			p.Log.Error(err)
			return nil
		}
		p.StdDataPtr = &table.Data{
			Table: tt,
			Operations: map[cptype.OperationKey]cptype.Operation{
				table.OpTableChangePage{}.OpKey(): cputil.NewOpBuilder().WithServerDataPtr(&table.OpTableChangePageServerData{}).Build(),
				table.OpTableChangeSort{}.OpKey(): cputil.NewOpBuilder().Build(),
			},
		}
		return nil
	}
}

func (p *provider) RegisterTablePagingOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		(*sdk.GlobalState)[trace.StateKeyTracePaging] = opData.ClientData
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterTableChangePageOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		(*sdk.GlobalState)[trace.StateKeyTracePaging] = opData.ClientData
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterTableSortOp(opData table.OpTableChangeSort) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		(*sdk.GlobalState)[trace.StateKeyTraceSort] = opData.ClientData
		p.RegisterInitializeOp()(sdk)
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

// RegisterRenderingOp .
func (p *provider) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

// Provide .
func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return p
}

func init() {
	cpregister.RegisterProviderComponent("trace-query", "table", &provider{})
}
