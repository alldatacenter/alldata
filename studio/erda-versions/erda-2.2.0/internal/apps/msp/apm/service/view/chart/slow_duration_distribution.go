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
	"fmt"
	"strconv"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/math"
)

type SlowDurationDistributionChart struct {
	*BaseChart
}

func (avgDuration *SlowDurationDistributionChart) GetChart(ctx context.Context) (*pb.ServiceChart, error) {
	statement := fmt.Sprintf("SELECT sum(elapsed_sum::field)/sum(elapsed_count::field), sum(elapsed_count::field) "+
		"FROM %s_slow "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"%s "+
		"%s "+
		"GROUP BY time(%s)",
		common.GetDataSourceNames(avgDuration.Layers...),
		common.BuildServerSideServiceIdFilterSql("$service_id", avgDuration.Layers...),
		common.BuildLayerPathFilterSql(avgDuration.LayerPath, "$layer_path", avgDuration.FuzzyPath, avgDuration.Layers...),
		avgDuration.Interval)

	var layerPathParam *structpb.Value
	if avgDuration.FuzzyPath {
		layerPathParam = common.NewStructValue(map[string]interface{}{"regex": ".*" + avgDuration.LayerPath + ".*"})
	} else {
		layerPathParam = structpb.NewStringValue(avgDuration.LayerPath)
	}

	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(avgDuration.TenantId),
		"service_id":   structpb.NewStringValue(avgDuration.ServiceId),
		"layer_path":   layerPathParam,
	}
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(avgDuration.StartTime, 10),
		End:       strconv.FormatInt(avgDuration.EndTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := avgDuration.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	avgDurationCharts := make([]*pb.Chart, 0, 10)

	rows := response.Results[0].Series[0].Rows

	maxValue := float64(0)
	for _, row := range rows {
		avgDurationChart := new(pb.Chart)
		date := row.Values[0].GetStringValue()
		parse, err := time.ParseInLocation(Layout, date, time.Local)
		if err != nil {
			return nil, err
		}
		timestamp := parse.UnixNano() / int64(time.Millisecond)

		avgDurationChart.Timestamp = timestamp
		avgDurationChart.Value = math.DecimalPlacesWithDigitsNumber(row.Values[1].GetNumberValue(), 2)
		avgDurationChart.ExtraValues = append(avgDurationChart.ExtraValues, row.Values[2].GetNumberValue())
		avgDurationChart.Dimension = "Avg Duration"

		if maxValue < avgDurationChart.Value {
			maxValue = avgDurationChart.Value
		}

		avgDurationCharts = append(avgDurationCharts, avgDurationChart)
	}
	return &pb.ServiceChart{Type: pb.ChartType_SlowDurationDistribution.String(), MaxValue: maxValue, View: avgDurationCharts}, err
}
