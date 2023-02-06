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
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/units"
)

func (f *Formater) isListReq(ctx *query.Context) bool {
	return len(ctx.Req.GroupBy) == 1 && len(ctx.Req.Select) == 1
}

func (f *Formater) formatListChart(ctx *query.Context, chart *chartmeta.ChartMeta) (interface{}, error) {
	aggs := ctx.Resp.Aggregations
	list := make([]map[string]interface{}, 0)
	terms, ok := aggs.Terms(ctx.Req.GroupBy[0].ID)
	if !ok || terms == nil {
		return map[string]interface{}{
			"metricData": list,
		}, nil
	}
	col := ctx.Req.Select[0]
	key := col.FuncName + "." + col.Property.Name

	var meta *chartmeta.DataMeta
	if chart != nil && chart.Defines != nil {
		meta = chart.Defines[key]
	}
	for _, term := range terms.Buckets {
		value, err := col.Function.Handle(ctx, term.Aggregations)
		if err != nil {
			return nil, err
		}
		value = setDefaultValue(ctx, value)
		data := map[string]interface{}{
			"title": term.Key,
			"value": value,
		}
		if ctx.Req.TransGroup {
			if key, ok := term.Key.(string); ok {
				data["title"] = ctx.T.Text(ctx.Lang, key)
			}
		} else if title, ok := term.Key.(string); ok {
			if chart != nil && chart.Defines != nil && chart.Defines[title] != nil && chart.Defines[title].Label != nil {
				data["title"] = chart.Defines[title].Label
			}
		}
		if meta != nil {
			if meta.Unit != nil {
				data["unit"] = meta.Unit
			}
			if meta.Unit != nil {
				var OriginalUnit string
				if meta.OriginalUnit != nil {
					OriginalUnit = *meta.OriginalUnit
				}
				data["value"] = units.Convert(OriginalUnit, *meta.Unit, value)
			}
			data["i18n"] = meta.I18n
		}
		list = append(list, data)
	}
	return map[string]interface{}{
		"metricData": list,
	}, nil
}
