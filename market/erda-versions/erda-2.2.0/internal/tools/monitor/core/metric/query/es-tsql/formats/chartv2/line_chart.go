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
	"strconv"
	"strings"

	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
)

type aggData struct {
	Name string        `json:"name"`
	Tag  interface{}   `json:"tag"`
	Data []interface{} `json:"data"`
}

func (f *Formater) formatLineChart(q tsql.Query, rs *tsql.ResultSet, params map[string]interface{}) (interface{}, error) {
	timeIdx, _, dims, vals := getGroupColumns(rs.Columns)
	var list []interface{}
	var times []int64
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
		} else if data == nil {
			data = make(map[string]*aggData)
			list = append(list, data)
		}
		if timeIdx >= 0 && len(groups) <= 1 {
			ts, _ := tsql.GetTimestampValue(row[timeIdx])
			ts = tsql.ConvertTimestamp(ts, q.Context().TargetTimeUnit(), tsql.Millisecond) // 强制转换成 ms
			times = append(times, ts)
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
		"time": times,
		"results": []interface{}{
			map[string]interface{}{
				"data": list,
			},
		},
	}, nil
}

func (f *Formater) getGroupKey(idxs []int, row []interface{}) string {
	var group []string
	for _, i := range idxs {
		v := row[i]
		if v == nil {
			group = append(group, "")
		} else {
			group = append(group, fmt.Sprint(v))
		}
	}
	return strings.Join(group, ",")
}

func getGroupColumns(cols []*tsql.Column) (ti, rngs int, dims, vals []int) {
	rngs, ti = -1, -1
	for i, c := range cols {
		if c.Flag&tsql.ColumnFlagGroupBy == tsql.ColumnFlagGroupBy {
			if c.Flag&tsql.ColumnFlagGroupByInterval == tsql.ColumnFlagGroupByInterval {
				ti = i
			} else if c.Flag&tsql.ColumnFlagGroupByRange == tsql.ColumnFlagGroupByRange {
				rngs = i
			} else {
				dims = append(dims, i)
			}
		} else {
			vals = append(vals, i)
		}
	}
	return
}
