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

package slow_count

import (
	"context"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/linegraph/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/datasources"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
)

type provider struct {
	impl.DefaultLineGraph
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
		layerPath := p.StdInParamsPtr.Get("layerPath").(string)

		chart, err := p.DataSource.GetChart(context.WithValue(context.Background(), common.LangKey, lang),
			pb.ChartType_SlowCount,
			startTime,
			endTime,
			tenantId,
			serviceId,
			common.TransactionLayerDb,
			layerPath)

		if err != nil {
			p.Log.Error(err)
			// todo how to throw error?
			return nil
		}

		p.StdDataPtr = chart
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
	cpregister.RegisterProviderComponent("transaction-db-detail", "slowCount", &provider{})
}
