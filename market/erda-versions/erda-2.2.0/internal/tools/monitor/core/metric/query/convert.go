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

package query

import (
	"fmt"
	"net/url"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
)

func convertOptions(start, end string, options map[string]string) url.Values {
	vals := url.Values{}
	for k, v := range options {
		vals.Set(k, v)
	}
	vals.Del("ql")
	vals.Del("q")
	vals.Del("format")
	vals.Set("start", start)
	vals.Set("end", end)
	return vals
}

func convertParams(pvalues map[string]*structpb.Value) map[string]interface{} {
	if len(pvalues) > 0 {
		params := make(map[string]interface{})
		for k, v := range pvalues {
			if v == nil {
				params[k] = nil
			} else {
				params[k] = v.AsInterface()
			}
		}
		return params
	}
	return nil
}

func convertFilters(filters []*pb.Filter) []*query.Filter {
	var list []*query.Filter
	for _, f := range filters {
		if f == nil {
			continue
		}
		filter := &query.Filter{
			Key:      f.Key,
			Operator: f.Op,
		}
		if f.Value != nil {
			filter.Value = f.Value.AsInterface()
		}
		list = append(list, filter)
	}
	return list
}

func parseFilters(filters []*query.Filter) (list []*pb.Filter, err error) {
	for _, item := range filters {
		value, err := structpb.NewValue(item.Value)
		if err != nil {
			return nil, err
		}
		list = append(list, &pb.Filter{
			Key:   item.Key,
			Op:    item.Operator,
			Value: value,
		})

	}
	return list, nil
}

func parseOptions(opts map[string]interface{}) map[string]string {
	options := make(map[string]string)
	for k, v := range opts {
		if val, ok := v.(string); ok {
			options[k] = val
		}
	}
	return options
}

func convertInfluxDBResults(list []*pb.Result) map[string]interface{} {
	results := make([]interface{}, len(list))
	for i, result := range list {
		series := make([]interface{}, len(result.Series))
		for i, serie := range result.Series {
			rows := make([][]interface{}, len(serie.Rows))
			for i, row := range serie.Rows {
				vals := make([]interface{}, len(row.Values))
				for i, val := range row.Values {
					if val != nil {
						vals[i] = val.AsInterface()
					}
				}
				rows[i] = vals
			}
			series[i] = map[string]interface{}{
				"name":    serie.Name,
				"columns": serie.Columns,
				"values":  rows,
			}
		}
		results[i] = map[string]interface{}{
			"statement_id": i,
			"series":       series,
		}
	}
	return map[string]interface{}{"results": results}
}

func parseValuesToParams(values url.Values) map[string]*structpb.Value {
	params := make(map[string]*structpb.Value)
	for k, vals := range values {
		values := make([]interface{}, len(vals))
		for _, val := range vals {
			values = append(values, val)
		}
		list, err := structpb.NewList(values)
		if err == nil {
			params[k] = structpb.NewListValue(list)
		}
	}
	return params
}

func convertParamsToValues(params map[string]*structpb.Value) url.Values {
	values := url.Values{}
	for k, val := range params {
		if val == nil {
			values.Add(k, "")
		} else {
			val := val.AsInterface()
			switch v := val.(type) {
			case []interface{}:
				for _, v := range v {
					if v == nil {
						values.Add(k, "")
					} else {
						values.Add(k, fmt.Sprint(v))
					}
				}
			case string:
				values.Add(k, v)
			}
		}
	}
	return values
}
