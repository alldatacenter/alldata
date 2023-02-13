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

func (f *Formater) isCardReq(ctx *query.Context) bool {
	return len(ctx.Req.GroupBy) == 0
}

func (f *Formater) formatCardChart(ctx *query.Context, chart *chartmeta.ChartMeta) (interface{}, error) {
	aggs := ctx.Resp.Aggregations
	datas := make([]map[string]interface{}, 0)
	for _, col := range ctx.Req.Select {
		value, err := col.Function.Handle(ctx, aggs)
		if err != nil {
			return nil, err
		}
		value = setDefaultValue(ctx, value)
		key := col.FuncName + "." + col.Property.Name
		data := map[string]interface{}{
			"name":  key,
			"value": value,
			// status?: 'rise | fall', // Value icon, optionally rising or falling.
			// color?: 'error | cancel | info | success | warning',  // Value color.
		}
		if chart != nil && chart.Defines != nil {
			meta := chart.Defines[key]
			if meta != nil {
				if meta.Label != nil && len(*meta.Label) > 0 {
					data["name"] = *meta.Label
				}
				if meta.Unit != nil {
					data["unit"] = *meta.Unit
					var OriginalUnit string
					if meta.OriginalUnit != nil {
						OriginalUnit = *meta.OriginalUnit
					}
					data["value"] = units.Convert(OriginalUnit, *meta.Unit, value)
				}
				data["i18n"] = meta.I18n
			}
		}
		datas = append(datas, data)
	}
	return map[string]interface{}{
		"metricData": datas,
	}, nil
}
