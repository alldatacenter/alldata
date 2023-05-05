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

package slow_req_distribution

import (
	"context"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/bubblegraph/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	slow_transaction "github.com/erda-project/erda/internal/apps/msp/apm/service/common/slow-transaction"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/datasources"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
)

type provider struct {
	impl.DefaultBubbleGraph
	slow_transaction.SlowTransactionInParams
	Log        logs.Logger
	I18n       i18n.Translator               `autowired:"i18n" translator:"msp-i18n"`
	Metric     metricpb.MetricServiceServer  `autowired:"erda.core.monitor.metric.MetricService"`
	DataSource datasources.ServiceDataSource `autowired:"component-protocol.components.datasources.msp-service"`
}

// RegisterInitializeOp .
func (p *provider) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		bubble, err := p.DataSource.GetBubbleChart(context.WithValue(context.Background(), common.LangKey, sdk.Lang),
			datasources.BubbleChartSlowReqDistribution,
			60,
			p.InParamsPtr.StartTime,
			p.InParamsPtr.EndTime,
			p.InParamsPtr.TenantId,
			p.InParamsPtr.ServiceId,
			common.TransactionLayerDb,
			p.InParamsPtr.LayerPath)

		if err != nil {
			p.Log.Error(err)
			(*sdk.GlobalState)[string(cptype.GlobalInnerKeyError)] = err.Error()
			return nil
		}

		p.StdDataPtr = bubble
		return nil
	}
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
	cpregister.RegisterProviderComponent("transaction-db-slow", "slowReqDistribution", &provider{})
}
