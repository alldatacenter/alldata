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
	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/formats"
)

// Formater .
type Formater struct{}

// Format .
func (f *Formater) Format(q tsql.Query, rs *tsql.ResultSet, params map[string]interface{}) (interface{}, error) {
	typ := "table"
	if t, ok := params["type"].(string); ok {
		typ = t
	}
	switch typ {
	case "pie", "list":
		return f.formatListChart(q, rs, params)
	case "line":
		return f.formatLineChart(q, rs, params)
	case "bar":
		return f.formatBarChart(q, rs, params)
	case "card":
		return f.formatCardChart(q, rs, params)
	case "*", "_":
		return f.formatTableChartV2(q, rs, params)
	}
	return f.formatTableChart(q, rs, params)
}

var formater Formater

func init() {
	formats.RegisterFormater("chartv2", &formater)
}
