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

	"github.com/ahmetb/go-linq/v3"
	"google.golang.org/protobuf/types/known/structpb"

	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/pkg/common/errors"
)

type HttpCodeChart struct {
	*BaseChart
}

func (httpCode *HttpCodeChart) GetChart(ctx context.Context) (*pb.ServiceChart, error) {
	if linq.From(httpCode.Layers).AnyWith(func(i interface{}) bool {
		return i.(common.TransactionLayerType) != common.TransactionLayerHttp &&
			i.(common.TransactionLayerType) != common.TransactionLayerRpc
	}) {
		return nil, fmt.Errorf("not supported transaction type")
	}

	statement := fmt.Sprintf("SELECT sum(http_status_code_count::field),http_status_code::tag "+
		"FROM %s "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"%s "+
		"AND span_kind::tag=$kind "+
		"%s "+
		"GROUP BY time(%s),http_status_code::tag",
		common.GetDataSourceNames(httpCode.Layers...),
		common.BuildServerSideServiceIdFilterSql("$service_id", httpCode.Layers...),
		common.BuildLayerPathFilterSql(httpCode.LayerPath, "$layer_path", httpCode.FuzzyPath, httpCode.Layers...),
		httpCode.Interval)

	var layerPathParam *structpb.Value
	if httpCode.FuzzyPath {
		layerPathParam = common.NewStructValue(map[string]interface{}{"regex": ".*" + httpCode.LayerPath + ".*"})
	} else {
		layerPathParam = structpb.NewStringValue(httpCode.LayerPath)
	}

	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(httpCode.TenantId),
		"service_id":   structpb.NewStringValue(httpCode.ServiceId),
		"kind":         structpb.NewStringValue("server"),
		"layer_path":   layerPathParam,
	}
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(httpCode.StartTime, 10),
		End:       strconv.FormatInt(httpCode.EndTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := httpCode.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	httpCodeCharts := make([]*pb.Chart, 0, 10)
	rows := response.Results[0].Series[0].Rows
	dividingLine := map[string]int{}
	dividingLineSort := []int{}
	dimensionTemp := ""
	for i, row := range rows {

		httpCodeChart := new(pb.Chart)
		date := row.Values[0].GetStringValue()
		parse, err := time.ParseInLocation(Layout, date, time.Local)
		if err != nil {
			return nil, err
		}
		timestamp := parse.UnixNano() / int64(time.Millisecond)
		value := row.Values[1].GetNumberValue()
		dimension := row.Values[2].GetStringValue()

		if i != 0 {
			dimensionPreNode := rows[i-1].Values[2].GetStringValue()
			if dimensionPreNode != "" {
				dimensionTemp = dimensionPreNode
			}
			if dimensionPreNode == "" {
				dimensionPreNode = dimensionTemp
			}

			datePreNode := rows[i-1].Values[0].GetStringValue()
			parse, err := time.ParseInLocation(Layout, datePreNode, time.Local)
			if err != nil {
				return nil, err
			}
			timestampPreNode := parse.UnixNano() / int64(time.Millisecond)

			if timestamp < timestampPreNode {
				dividingLine[dimensionPreNode] = i - 1
				dividingLineSort = append(dividingLineSort, i-1)
			}
			if i+1 >= len(rows) {
				if _, ok := dividingLine[dimensionPreNode]; !ok {
					dividingLine[dimensionPreNode] = i
					dividingLineSort = append(dividingLineSort, i)
				}
			}
		}

		httpCodeChart.Timestamp = timestamp
		httpCodeChart.Value = value
		httpCodeChart.Dimension = dimension
		httpCodeCharts = append(httpCodeCharts, httpCodeChart)
	}

	for i, chart := range httpCodeCharts {
		if chart.Dimension == "" {
			chart.Dimension = getDimension(dividingLine, dividingLineSort, i)
		}
	}

	return &pb.ServiceChart{Type: pb.ChartType_HttpCode.String(), View: httpCodeCharts}, err
}

func getDimension(dividingLine map[string]int, dividingLineSort []int, i int) string {
	for _, s := range dividingLineSort {
		for k := range dividingLine {
			if dividingLine[k] == s && i <= s {
				return k
			}
		}
	}
	return ""
}
