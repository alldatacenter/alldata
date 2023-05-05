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

package service

import (
	"context"
	"strings"

	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/chart"
)

func Selector(viewType string, config *config, baseChart *chart.BaseChart, ctx context.Context) ([]*pb.ServiceChart, error) {

	switch viewType {
	case strings.ToLower(pb.ViewType_SERVICE_OVERVIEW.String()):
		view := GetView(config, strings.ToLower(pb.ViewType_SERVICE_OVERVIEW.String()))
		serviceCharts := make([]*pb.ServiceChart, 0, 3)
		err := getViewData(view.Charts, baseChart, ctx, &serviceCharts)
		if err != nil {
			return nil, err
		}
		return serviceCharts, nil
	case strings.ToLower(pb.ViewType_TOPOLOGY_SERVICE_NODE.String()):
		view := GetView(config, strings.ToLower(pb.ViewType_TOPOLOGY_SERVICE_NODE.String()))
		serviceCharts := make([]*pb.ServiceChart, 0, 4)
		err := getViewData(view.Charts, baseChart, ctx, &serviceCharts)
		if err != nil {
			return nil, err
		}
		return serviceCharts, nil
	case strings.ToLower(pb.ViewType_RPS_Chart.String()):
		view := GetView(config, strings.ToLower(pb.ViewType_RPS_Chart.String()))
		serviceCharts := make([]*pb.ServiceChart, 0, 4)
		err := getViewData(view.Charts, baseChart, ctx, &serviceCharts)
		if err != nil {
			return nil, err
		}
		return serviceCharts, nil
	case strings.ToLower(pb.ViewType_Avg_Duration_Chart.String()):
		view := GetView(config, strings.ToLower(pb.ViewType_Avg_Duration_Chart.String()))
		serviceCharts := make([]*pb.ServiceChart, 0, 4)
		err := getViewData(view.Charts, baseChart, ctx, &serviceCharts)
		if err != nil {
			return nil, err
		}
		return serviceCharts, nil
	case strings.ToLower(pb.ViewType_Error_Rate_Chart.String()):
		view := GetView(config, strings.ToLower(pb.ViewType_Error_Rate_Chart.String()))
		serviceCharts := make([]*pb.ServiceChart, 0, 4)
		err := getViewData(view.Charts, baseChart, ctx, &serviceCharts)
		if err != nil {
			return nil, err
		}
		return serviceCharts, nil
	case strings.ToLower(pb.ViewType_Http_Code_Chart.String()):
		view := GetView(config, strings.ToLower(pb.ViewType_Http_Code_Chart.String()))
		serviceCharts := make([]*pb.ServiceChart, 0, 4)
		err := getViewData(view.Charts, baseChart, ctx, &serviceCharts)
		if err != nil {
			return nil, err
		}
		return serviceCharts, nil
	default:
		return nil, nil
	}
}

func getViewData(charts []string, baseChart *chart.BaseChart, ctx context.Context, serviceCharts *[]*pb.ServiceChart) error {
	for _, c := range charts {
		chartData, err := chart.Selector(strings.ToLower(c), baseChart, ctx)
		if err != nil {
			return err
		}
		*serviceCharts = append(*serviceCharts, chartData)
	}
	return nil
}
