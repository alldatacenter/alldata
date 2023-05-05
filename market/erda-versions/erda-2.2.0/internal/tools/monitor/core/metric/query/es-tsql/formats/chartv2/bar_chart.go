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
	"strconv"

	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
)

func (f *Formater) formatBarChart(q tsql.Query, rs *tsql.ResultSet, params map[string]interface{}) (interface{}, error) {
	timeIdx, _, dims, vals := getGroupColumns(rs.Columns)
	if timeIdx >= 0 {
		dims = append([]int{timeIdx}, dims...)
	}
	var list []interface{}
	var xdata []interface{}
	groups := make(map[string]map[string]*aggData)
	var data map[string]*aggData
	for _, row := range rs.Rows {
		var tag interface{}
		if len(dims) > 0 {
			key := f.getGroupKey(dims, row)
			group, ok := groups[key]
			if !ok {
				group = make(map[string]*aggData)
				groups[key] = group
				list = append(list, group)
			}
			data, tag = group, key
			xdata = append(xdata, key)
		} else if data == nil {
			data = make(map[string]*aggData)
			list = append(list, data)
		}
		for _, val := range vals {
			key := strconv.Itoa(val)
			agg, ok := data[key]
			if !ok {
				col := rs.Columns[val]
				agg = &aggData{
					Name: col.Name,
					Tag:  tag,
				}
				data[key] = agg
			}
			agg.Data = append(agg.Data, row[val])
		}
	}
	return map[string]interface{}{
		"xData": xdata,
		"results": []interface{}{
			map[string]interface{}{
				"data": list,
			},
		},
	}, nil
}
