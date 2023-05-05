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

package formats

import (
	"fmt"

	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
)

// Formater response formater
type Formater interface {
	Format(q tsql.Query, rs *tsql.ResultSet, params map[string]interface{}) (interface{}, error)
}

// Formats .
var Formats = map[string]Formater{}

// RegisterFormater .
func RegisterFormater(name string, formater Formater) {
	Formats[name] = formater
}

// Format .
func Format(name string, q tsql.Query, rs *tsql.ResultSet, params map[string]interface{}) (interface{}, error) {
	f, ok := Formats[name]
	if !ok {
		return nil, fmt.Errorf("invalid formater '%s'", name)
	}
	return f.Format(q, rs, params)
}
