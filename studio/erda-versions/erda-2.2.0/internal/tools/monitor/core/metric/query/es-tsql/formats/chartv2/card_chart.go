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

import tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"

func (f *Formater) formatCardChart(q tsql.Query, rs *tsql.ResultSet, params map[string]interface{}) (interface{}, error) {
	var list []map[string]interface{}
	if len(rs.Columns) <= 0 {
		return list, nil
	}
	for _, row := range rs.Rows {
		data := map[string]interface{}{
			"name":  rs.Columns[0].Name,
			"value": row[0],
		}
		list = append(list, data)
	}
	return list, nil
}
