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

type RpsChart struct {
	*BaseChart
}

func (rps *RpsChart) GetChart(ctx context.Context) (*pb.ServiceChart, error) {

	statement := fmt.Sprintf("SELECT rateps(elapsed_count::field) "+
		"FROM %s "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"%s "+
		"%s "+
		"GROUP BY time(%s)",
		common.GetDataSourceNames(rps.Layers...),
		common.BuildServerSideServiceIdFilterSql("$service_id", rps.Layers...),
		common.BuildLayerPathFilterSql(rps.LayerPath, "$layer_path", rps.FuzzyPath, rps.Layers...),
		rps.Interval)

	var layerPathParam *structpb.Value
	if rps.FuzzyPath {
		layerPathParam = common.NewStructValue(map[string]interface{}{"regex": ".*" + rps.LayerPath + ".*"})
	} else {
		layerPathParam = structpb.NewStringValue(rps.LayerPath)
	}

	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(rps.TenantId),
		"service_id":   structpb.NewStringValue(rps.ServiceId),
		"layer_path":   layerPathParam,
	}
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(rps.StartTime, 10),
		End:       strconv.FormatInt(rps.EndTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := rps.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	rpsCharts := make([]*pb.Chart, 0, 10)

	rows := response.Results[0].Series[0].Rows
	maxValue := float64(0)
	for i, row := range rows {
		rpsChart := new(pb.Chart)
		timestamp := int64(0)
		if i == 0 {
			date := row.Values[0].GetStringValue()
			parse, err := time.ParseInLocation(Layout, date, time.Local)
			if err != nil {
				return nil, err
			}
			timestamp = parse.UnixNano() / int64(time.Millisecond)
		} else {
			timestampNano := row.Values[0].GetNumberValue()
			timestamp = int64(timestampNano) / int64(time.Millisecond)
		}

		rpsChart.Timestamp = timestamp
		rpsChart.Value = math.DecimalPlacesWithDigitsNumber(row.Values[1].GetNumberValue(), 2)
		rpsChart.Dimension = "RPS"

		if maxValue < rpsChart.Value {
			maxValue = rpsChart.Value
		}

		rpsCharts = append(rpsCharts, rpsChart)
	}
	return &pb.ServiceChart{Type: pb.ChartType_RPS.String(), MaxValue: maxValue, View: rpsCharts}, err
}
