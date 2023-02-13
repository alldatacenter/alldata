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

package dict

import (
	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/formats"
)

// Formater .
type Formater struct{}

// Format .
func (f *Formater) Format(q tsql.Query, rs *tsql.ResultSet, params map[string]interface{}) (interface{}, error) {
	res := make([]map[string]interface{}, len(rs.Rows))
	for i := 0; i < len(rs.Rows); i++ {
		for j := 0; j < len(rs.Columns); j++ {
			key, val := rs.Columns[j].Name, rs.Rows[i][j]
			if res[i] == nil {
				res[i] = map[string]interface{}{
					key: val,
				}
			} else {
				res[i][key] = val
			}
		}
	}
	return res, nil
}

var formater Formater

func init() {
	formats.RegisterFormater("dict", &formater)
}
