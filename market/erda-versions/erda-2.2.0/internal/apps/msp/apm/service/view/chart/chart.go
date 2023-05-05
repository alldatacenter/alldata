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

package chart

import (
	"context"
	"strings"

	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
)

const Layout = "2006-01-02T15:04:05Z"

type Chart interface {
	GetChart(ctx context.Context) (*pb.ServiceChart, error)
}

type BaseChart struct {
	StartTime int64
	EndTime   int64
	Interval  string
	TenantId  string
	ServiceId string
	Layers    []common.TransactionLayerType
	LayerPath string
	FuzzyPath bool
	Metric    metricpb.MetricServiceServer
}

func GetChartUnitDefault(chartType pb.ChartType, lang i18n.LanguageCodes, i18n i18n.Translator) string {
	switch strings.ToLower(chartType.String()) {
	case strings.ToLower(pb.ChartType_RPS.String()):
		return i18n.Text(lang, "rpsUnit")
	case strings.ToLower(pb.ChartType_AvgDuration.String()),
		strings.ToLower(pb.ChartType_AvgDurationDistribution.String()),
		strings.ToLower(pb.ChartType_SlowDurationDistribution.String()):
		return i18n.Text(lang, "avgDurationUnit")
	case strings.ToLower(pb.ChartType_ErrorRate.String()):
		return i18n.Text(lang, "rateUnit")
	case strings.ToLower(pb.ChartType_ErrorCount.String()):
		return i18n.Text(lang, "countUnit")
	case strings.ToLower(pb.ChartType_SlowCount.String()):
		return i18n.Text(lang, "countUnit")
	default:
		return ""
	}
}

func Selector(chartType string, baseChart *BaseChart, ctx context.Context) (*pb.ServiceChart, error) {
	switch chartType {
	case strings.ToLower(pb.ChartType_HttpCode.String()):
		httpCodeChart := HttpCodeChart{BaseChart: baseChart}
		getChart, err := httpCodeChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	case strings.ToLower(pb.ChartType_RPS.String()):
		rpcChart := RpsChart{BaseChart: baseChart}
		getChart, err := rpcChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	case strings.ToLower(pb.ChartType_AvgDuration.String()):
		avgDurationChart := AvgDurationChart{BaseChart: baseChart}
		getChart, err := avgDurationChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	case strings.ToLower(pb.ChartType_ErrorRate.String()):
		errorChart := ErrorRateChart{BaseChart: baseChart}
		getChart, err := errorChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	case strings.ToLower(pb.ChartType_ErrorCount.String()):
		errCountChart := ErrorCountChart{BaseChart: baseChart}
		getChart, err := errCountChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	case strings.ToLower(pb.ChartType_SlowCount.String()):
		slowCountChart := SlowCountChart{BaseChart: baseChart}
		getChart, err := slowCountChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	case strings.ToLower(pb.ChartType_AvgDurationDistribution.String()):
		addChart := AvgDurationDistributionChart{BaseChart: baseChart}
		getChart, err := addChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	case strings.ToLower(pb.ChartType_SlowDurationDistribution.String()):
		slowDistChart := SlowDurationDistributionChart{BaseChart: baseChart}
		getChart, err := slowDistChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	case strings.ToLower(pb.ChartType_ErrorDurationDistribution.String()):
		errorDistChart := ErrorDurationDistributionChart{BaseChart: baseChart}
		getChart, err := errorDistChart.GetChart(ctx)
		if err != nil {
			return nil, err
		}
		return getChart, err
	default:
		return nil, nil
	}
}
