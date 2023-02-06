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

package chartv2

import (
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/chartmeta"
	query "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query/v1"
)

func (f *Formater) isChartBarReq(ctx *query.Context) bool {
	return ctx.Req.ChartType == "chart:bar" && len(ctx.Req.GroupBy) == 1
}

func (f *Formater) formatChartBarChart(ctx *query.Context, chart *chartmeta.ChartMeta) (interface{}, error) {
	aggs := ctx.Resp.Aggregations
	var title string
	if chart != nil {
		title = chart.Title
	}
	var xdata []interface{}
	terms, ok := aggs.Terms(ctx.Req.GroupBy[0].ID)
	if !ok || terms == nil {
		return map[string]interface{}{
			"xdata": xdata,
			"data": []interface{}{
				make(map[string]interface{}, len(ctx.Req.Select)),
			},
			"title": title,
		}, nil
	}
	values := make([][]interface{}, len(ctx.Req.Select))
	for _, term := range terms.Buckets {
		xdata = append(xdata, term.Key)
		for i, col := range ctx.Req.Select {
			value, err := col.Function.Handle(ctx, term.Aggregations)
			if err != nil {
				return nil, err
			}
			value = setDefaultValue(ctx, value)
			values[i] = append(values[i], value)
		}
	}
	data := make(map[string]interface{}, len(ctx.Req.Select))
	for i, col := range ctx.Req.Select {
		putColumnData(col, data, values[i], "", chart)
	}
	return map[string]interface{}{
		"xdata": xdata,
		"data": []interface{}{
			data,
		},
		"title": title,
	}, nil
}
