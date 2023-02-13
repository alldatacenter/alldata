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

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/table/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/common/transaction"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/datasources"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	viewtable "github.com/erda-project/erda/internal/apps/msp/apm/service/view/table"
)

type provider struct {
	impl.DefaultTable
	Log        logs.Logger
	I18n       i18n.Translator               `autowired:"i18n" translator:"msp-i18n"`
	Metric     metricpb.MetricServiceServer  `autowired:"erda.core.monitor.metric.MetricService"`
	DataSource datasources.ServiceDataSource `autowired:"component-protocol.components.datasources.msp-service"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		lang := sdk.Lang
		startTime := int64(p.StdInParamsPtr.Get("startTime").(float64))
		endTime := int64(p.StdInParamsPtr.Get("endTime").(float64))
		tenantId := p.StdInParamsPtr.Get("tenantId").(string)
		serviceId := p.StdInParamsPtr.Get("serviceId").(string)

		layerPath := ""
		if x, ok := (*sdk.GlobalState)[transaction.StateKeyTransactionLayerPathFilter]; ok && x != nil {
			layerPath = x.(string)
		}

		pageNo, pageSize := transaction.GetPagingFromGlobalState(*sdk.GlobalState)
		sorts := transaction.GetSortsFromGlobalState(*sdk.GlobalState)

		data, err := p.DataSource.GetTable(context.WithValue(context.Background(), common.LangKey, lang),
			&viewtable.TransactionTableBuilder{
				BaseBuildParams: &viewtable.BaseBuildParams{
					StartTime: startTime,
					EndTime:   endTime,
					TenantId:  tenantId,
					ServiceId: serviceId,
					Layer:     common.TransactionLayerRpc,
					LayerPath: layerPath,
					FuzzyPath: true,
					OrderBy:   sorts,
					PageNo:    pageNo,
					PageSize:  pageSize,
					Metric:    p.Metric,
				},
			})
		if err != nil {
			p.Log.Error("failed to get table data: %s", err)
			return nil
		}

		p.StdDataPtr = &table.Data{
			Table: *data,
			Operations: map[cptype.OperationKey]cptype.Operation{
				table.OpTableChangePage{}.OpKey(): cputil.NewOpBuilder().WithServerDataPtr(&table.OpTableChangePageServerData{}).Build(),
				table.OpTableChangeSort{}.OpKey(): cputil.NewOpBuilder().Build(),
			}}
		return nil
	}
}

func (p *provider) RegisterTablePagingOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		(*sdk.GlobalState)[transaction.StateKeyTransactionPaging] = opData.ClientData
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterTableChangePageOp(opData table.OpTableChangePage) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		(*sdk.GlobalState)[transaction.StateKeyTransactionPaging] = opData.ClientData
		p.RegisterInitializeOp()(sdk)
		return nil
	}
}

func (p *provider) RegisterTableSortOp(opData table.OpTableChangeSort) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		(*sdk.GlobalState)[transaction.StateKeyTransactionSort] = opData.ClientData
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
	cpregister.RegisterProviderComponent("transaction-rpc-analysis", "table", &provider{})
}
