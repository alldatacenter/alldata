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
	"fmt"
	"math"
	"sort"
	"strconv"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/chartmeta"
	query "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query/v1"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/units"
)

func (f *Formater) isTableReq(ctx *query.Context) bool {
	return len(ctx.Req.GroupBy) == 1 && len(ctx.Req.Select) > 1
}

func (f *Formater) formatTableChart(ctx *query.Context, chart *chartmeta.ChartMeta) (interface{}, error) {
	aggs := ctx.Resp.Aggregations
	h, maxInt := make([]map[string]interface{}, 0), int(math.MaxInt64)
	// columns := make(map[int]bool, 0)
	for _, col := range ctx.Req.Select {
		var column int
		key := col.FuncName + "." + col.Property.Name
		head := map[string]interface{}{
			"dataIndex": key,
		}
		label := key
		if chart != nil && chart.Defines != nil {
			meta := chart.Defines[key]
			if meta != nil {
				head["i18n"] = meta.I18n
			}
			if meta != nil && meta.Label != nil && len(*meta.Label) > 0 {
				label = *meta.Label
			}
			if meta != nil && meta.OriginalUnit != nil && meta.Unit != nil {
				head["unit"] = *meta.Unit
			}
			if meta == nil || meta.Column == nil || *meta.Column == -1 {
				column = maxInt
			} else {
				column = *meta.Column
			}
			// if meta != nil && meta.Column != nil {
			// 	if *meta.Column == -1 {
			// 		column = maxInt
			// 	} else {
			// 		column = *meta.Column
			// 	}
			// if !columns[*meta.Column] {
			// 	column = *meta.Column
			// } else {
			// 	column = *meta.Column + 1
			// }
			// } else {
			//
			// }
		}
		head["title"] = label
		head["column"] = column

		h = append(h, head)
	}

	sort.Sort(heads(h))
	for _, head := range h {
		if _, ok := head["column"]; ok {
			delete(head, "column")
		}
	}
	datas := make([]map[string]interface{}, 0)
	terms, ok := aggs.Terms(ctx.Req.GroupBy[0].ID)
	if !ok || terms == nil {
		return map[string]interface{}{
			"metricData": datas,
			"cols":       h,
		}, nil
	}
	for _, term := range terms.Buckets {
		data := make(map[string]interface{}, len(ctx.Req.Select))
		for _, col := range ctx.Req.Select {
			value, err := col.Function.Handle(ctx, term.Aggregations)
			if err != nil {
				return nil, err
			}
			value = setDefaultValue(ctx, value)
			key := col.FuncName + "." + col.Property.Name
			data[key] = value
			if chart != nil && chart.Defines != nil {
				meta := chart.Defines[key]
				if meta != nil && meta.Unit != nil {
					var OriginalUnit string
					if meta.OriginalUnit != nil {
						OriginalUnit = *meta.OriginalUnit
					}
					data[key] = units.Convert(OriginalUnit, *meta.Unit, value)
				}
			}
			if d, ok := data[key].(float64); ok {
				data[key] = decimal(d)
			}
		}

		datas = append(datas, data)
	}
	return map[string]interface{}{
		"metricData": datas,
		"cols":       h,
	}, nil
}

type heads []map[string]interface{}

func (h heads) Len() int      { return len(h) }
func (h heads) Swap(i, j int) { h[i], h[j] = h[j], h[i] }
func (h heads) Less(i, j int) bool {

	// if _, ok := h[i]["column"]; !ok {
	// 	return true
	// }
	// if _, ok := h[j]["column"]; !ok {
	// 	return true
	// }
	//
	// x, ok := h[i]["column"].(int)
	// if !ok {
	// 	return true
	// }
	// y, ok := h[i]["column"].(int)
	// if !ok {
	// 	return true
	// }
	//
	// if y == -1 {
	// 	return true
	// }
	// if x == -1 && y >= 0 {
	// 	return true
	// }

	// if h[i]["column"].(int) < 0 && h[j]["column"].(int) > 0 {
	// 	return false
	// }
	// if h[i]["column"].(int) > 0 && h[j]["column"].(int) > 0 {
	// 	return h[i]["column"].(int) < h[j]["column"].(int)
	// }
	// return true

	return h[i]["column"].(int) < h[j]["column"].(int)
}

func decimal(value float64) float64 {
	value, _ = strconv.ParseFloat(fmt.Sprintf("%.2f", value), 64)
	return value
}
