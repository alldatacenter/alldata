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

	"github.com/olivere/elastic"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/chartmeta"
	query "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query/v1"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/units"
)

// Formater .
type Formater struct{}

// Format .
func (f *Formater) Format(ctx *query.Context, param string) (interface{}, error) {
	var times []int64
	appendTime := true
	chart := ctx.ChartMeta
	if ctx.Req.Aggregate == nil {
		if f.isChartBarReq(ctx) {
			return f.formatChartBarChart(ctx, chart) // 柱状图
		} else if f.isTableReq(ctx) { // 表格
			return f.formatTableChart(ctx, chart)
		} else if f.isListReq(ctx) { // 列表
			return f.formatListChart(ctx, chart)
		} else if f.isCardReq(ctx) { // 卡片图
			return f.formatCardChart(ctx, chart)
		}
	}
	// Other query methods still follow the old chart format.
	var title string
	if chart != nil {
		title = chart.Title
	}
	if len(ctx.Req.GroupBy) == 0 {
		data, err := f.aggData(ctx, ctx.Resp.Aggregations, nil, &times, &appendTime, chart)
		if err != nil {
			return nil, err
		}
		return map[string]interface{}{
			"times": times,
			"data":  data,
			"title": title,
		}, nil
	}
	data, err := f.groupData(0, ctx, ctx.Resp.Aggregations, &times, &appendTime, chart)
	if err != nil {
		return nil, err
	}
	return map[string]interface{}{
		"times": times,
		"data":  data,
		"title": title,
	}, nil
}

func (f *Formater) groupData(depth int, ctx *query.Context, aggs elastic.Aggregations, times *[]int64, appendTime *bool, chart *chartmeta.ChartMeta) (interface{}, error) {
	terms, ok := aggs.Terms(ctx.Req.GroupBy[depth].ID)
	if !ok || terms == nil {
		return nil, fmt.Errorf("invalid terms %s", ctx.Req.GroupBy[depth].ID)
	}
	list := make([]map[string]interface{}, 0)
	last := depth >= len(ctx.Req.GroupBy)-1
	for _, term := range terms.Buckets {
		if last {
			data, err := f.aggData(ctx, term.Aggregations, term.Key, times, appendTime, chart)
			if err != nil {
				return nil, err
			}
			list = append(list, data)
		} else {
			data, err := f.groupData(depth+1, ctx, term.Aggregations, times, appendTime, chart)
			if err != nil {
				return nil, err
			}
			group := make(map[string]interface{}, 3)
			group["tag"] = term.Key
			group["total"] = term.DocCount
			group["data"] = data
			list = append(list, group)
		}
	}
	return list, nil
}

func (f *Formater) aggData(ctx *query.Context, aggs elastic.Aggregations, group interface{},
	times *[]int64, appendTime *bool, chart *chartmeta.ChartMeta) (map[string]interface{}, error) {
	result := make(map[string]interface{})
	if ctx.Req.Aggregate != nil {
		agg := ctx.Req.Aggregate
		switch agg.FuncName {
		case "histogram":
			interval := ctx.Req.Interval
			alignEnd := ctx.Req.AlignEnd
			points := int(ctx.Req.Points)
			histogram, ok := aggs.Histogram(ctx.Req.Aggregate.ID)
			if !ok || histogram == nil {
				return nil, fmt.Errorf("invalid histogram %s", ctx.Req.Aggregate.ID)
			}
			for _, col := range ctx.Req.Select {
				var list []interface{}
				for i, bucket := range histogram.Buckets {
					if i+1 < len(histogram.Buckets) {
						ctx.Attributes["next"] = histogram.Buckets[i+1].Aggregations
					} else {
						delete(ctx.Attributes, "next")
					}
					if i == 0 {
						delete(ctx.Attributes, "previous")
					} else {
						ctx.Attributes["previous"] = histogram.Buckets[i-1].Aggregations
					}
					if i < points {
						if *appendTime {
							var t int64
							if alignEnd {
								t = int64((bucket.Key*float64(ctx.Req.OriginalTimeUnit) + interval) / 1000000)
							} else {
								t = int64(bucket.Key * float64(ctx.Req.OriginalTimeUnit) / 1000000)
							}
							*times = append(*times, t)
						}
						value, err := col.Function.Handle(ctx, bucket.Aggregations)
						if err != nil {
							return nil, err
						}
						value = setDefaultValue(ctx, value)
						list = append(list, value)
					}
				}
				delete(ctx.Attributes, "next")
				delete(ctx.Attributes, "previous")
				putColumnData(col, result, list, group, chart)
				*appendTime = false
			}
		}
	} else {
		err := putAggData(ctx, aggs, result, group, chart)
		if err != nil {
			return nil, err
		}
	}
	return result, nil
}

func setDefaultValue(ctx *query.Context, value interface{}) interface{} {
	if value == nil {
		if ctx.Req.DefaultNullValue != nil {
			return ctx.Req.DefaultNullValue
		}
	}
	return value
}

func putAggData(ctx *query.Context, aggs elastic.Aggregations, out map[string]interface{}, group interface{}, chart *chartmeta.ChartMeta) error {
	if len(ctx.Req.Select) <= 0 && len(ctx.Req.GroupBy) > 0 {
		out["data"] = nil
		out["tag"] = group
		return nil
	}
	for _, col := range ctx.Req.Select {
		value, err := col.Function.Handle(ctx, aggs)
		if err != nil {
			return err
		}
		value = setDefaultValue(ctx, value)
		putColumnData(col, out, value, group, chart)
	}
	return nil
}

func putColumnData(col *query.Column, out map[string]interface{}, value interface{}, group interface{}, chart *chartmeta.ChartMeta) map[string]interface{} {
	// chart info
	key := col.FuncName + "." + col.Property.Name
	var meta *chartmeta.DataMeta
	if chart != nil && chart.Defines != nil {
		meta = chart.Defines[key]
	}
	data := make(map[string]interface{}, 3)
	data["agg"] = col.FuncName
	data["tag"] = group
	if meta != nil {
		data["name"] = meta.Label
		data["unit"] = meta.Unit
		data["unitType"] = meta.UnitType
		data["chartType"] = meta.ChartType
		data["axisIndex"] = meta.AxisIndex
		data["i18n"] = meta.I18n
		if datas, ok := value.([]interface{}); ok {
			var arr []interface{}
			for _, d := range datas {
				if meta.Unit != nil {
					var OriginalUnit string
					if meta.OriginalUnit != nil {
						OriginalUnit = *meta.OriginalUnit
					}
					d = units.Convert(OriginalUnit, *meta.Unit, d)
				}
				arr = append(arr, d)
			}
			data["data"] = arr
		} else {
			if meta.Unit != nil {
				var OriginalUnit string
				if meta.OriginalUnit != nil {
					OriginalUnit = *meta.OriginalUnit
				}
				data["data"] = units.Convert(OriginalUnit, *meta.Unit, value)
			} else {
				data["data"] = value
			}
		}
	} else {
		data["data"] = value
		data["name"] = col.Property.Name
	}
	out[key] = data
	return data
}

var formater Formater

func init() {
	query.RegisterResponseFormater("chartv2", &formater)
}
