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

package influxdb

import (
	"strings"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/formats"
	"github.com/erda-project/erda/pkg/common/errors"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

// Formater .
type Formater struct{}

// Response .
type Response struct {
	Results []*pb.Result `json:"results,omitempty"`
	Error   error        `json:"error,omitempty"`
}

// Format .
func (f *Formater) Format(q tsql.Query, rs *tsql.ResultSet, params map[string]interface{}) (interface{}, error) {
	var columns []string
	for _, c := range rs.Columns {
		columns = append(columns, c.Name)
	}
	rows := make([]*pb.Row, len(rs.Rows))
	for i, values := range rs.Rows {
		vals := make([]*structpb.Value, len(values))
		for i, val := range values {
			if val != nil {
				val, err := structpb.NewValue(val)
				if err != nil {
					return nil, errors.NewInternalServerError(err)
				}
				vals[i] = val
			}
		}
		rows[i] = &pb.Row{Values: vals}
	}
	return api.SuccessRaw(&Response{
		Results: []*pb.Result{
			{
				StatementId: 0,
				Series: []*pb.Serie{
					{
						Name:    getSourceName(q),
						Columns: columns,
						Rows:    rows,
					},
				},
			},
		},
	}), nil
}

func getSourceName(q tsql.Query) string {
	var list []string
	for _, s := range q.Sources() {
		if len(s.Name) > 0 {
			list = append(list, s.Name)
		}
	}
	return strings.Join(list, ",")
}

var formater Formater

func init() {
	formats.RegisterFormater("influxdb", &formater)
}
