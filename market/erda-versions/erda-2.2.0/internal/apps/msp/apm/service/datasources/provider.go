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

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/bubblegraph"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/kv"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/linegraph"
	stdtable "github.com/erda-project/erda-infra/providers/component-protocol/components/table"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/card"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/table"
)

type provider struct {
	Log    logs.Logger
	I18n   i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Metric metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

type BubbleChartType string

const (
	BubbleChartReqDistribution      BubbleChartType = "requestDistribution"
	BubbleChartSlowReqDistribution  BubbleChartType = "slowRequestDistribution"
	BubbleChartErrorReqDistribution BubbleChartType = "errorRequestDistribution"
)

type ServiceDataSource interface {
	GetChart(ctx context.Context, chartType pb.ChartType, start, end int64, tenantId, serviceId string, layer common.TransactionLayerType, path string) (*linegraph.Data, error)
	GetBubbleChart(ctx context.Context, bubbleType BubbleChartType, preferredChartPoints int64, start, end int64, tenantId, serviceId string, layer common.TransactionLayerType, path string) (*bubblegraph.Data, error)
	GetCard(ctx context.Context, cardType card.CardType, start, end int64, tenantId, serviceId string, layer common.TransactionLayerType, path string) (*kv.KV, error)
	GetTable(ctx context.Context, builder table.Builder) (*stdtable.Table, error)
}

func init() {
	servicehub.Register("component-protocol.components.datasources.msp-service", &servicehub.Spec{
		Creator:  func() servicehub.Provider { return &provider{} },
		Services: []string{"component-protocol.components.datasources.msp-service"},
	})
}
