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

package metricmeta

import (
	"fmt"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
)

// type names
const (
	NumberType      = "number"
	BoolType        = "bool"
	StringType      = "string"
	NumberArrayType = "number_array"
	StringArrayType = "string_array"
	BoolArrayType   = "bool_array"
)

// AggName .
func (m *Manager) AggName(langCodes i18n.LanguageCodes, text string) string {
	t := m.i18n.Translator("_type_aggregations")
	return t.Text(langCodes, aggNames[text])
}

var aggNames = map[string]string{
	"max":      "Maximum",
	"min":      "Minimum",
	"sum":      "Sum",
	"avg":      "Average",
	"value":    "Last Value",
	"count":    "Count",
	"sumCps":   "Sum Per Second",
	"cps":      "Count Per Second",
	"diff":     "Difference",
	"diffps":   "Difference Per Second",
	"distinct": "Distinct",
	"rateps":   "Rate Per Second",
}

func (m *Manager) getTypeAggDefine(langCodes i18n.LanguageCodes, mode string) (*pb.MetaMode, error) {
	t := m.i18n.Translator("_type_aggregations")
	switch mode {
	case "", "query":
		return &pb.MetaMode{
			Types: map[string]*pb.TypeDefine{
				NumberType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "max",
							Name:        t.Text(langCodes, aggNames["max"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "min",
							Name:        t.Text(langCodes, aggNames["min"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "sum",
							Name:        t.Text(langCodes, aggNames["sum"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "avg",
							Name:        t.Text(langCodes, aggNames["avg"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, aggNames["value"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, aggNames["count"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "sumCps",
							Name:        t.Text(langCodes, aggNames["sumCps"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "cps",
							Name:        t.Text(langCodes, aggNames["cps"]),
							ResultType:  NumberType,
						},
						// {
						// 	Aggregation: "diffps",
						// 	Name:        t.Text(langCodes, aggNames["diffps"]),
						// 	ResultType:  NumberType,
						// },
						// {
						// 	Aggregation: "diff",
						// 	Name:        t.Text(langCodes, aggNames["diff"]),
						// 	ResultType:  NumberType,
						// },
					},
				},
				BoolType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, aggNames["count"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, aggNames["value"]),
							ResultType:  BoolType,
						},
					},
				},
				StringType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "distinct",
							Name:        t.Text(langCodes, aggNames["distinct"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, aggNames["count"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "cps",
							Name:        t.Text(langCodes, aggNames["cps"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, aggNames["value"]),
							ResultType:  StringType,
						},
					},
				},
			},
			Filters: []*pb.Operation{
				{
					Operation: "eq",
					Name:      t.Text(langCodes, "Equal"),
				},
				{
					Operation: "neq",
					Name:      t.Text(langCodes, "Not Equal"),
				},
				{
					Operation: "in",
					Name:      t.Text(langCodes, "In"),
					Multi:     true,
				},
				{
					Operation: "like",
					Name:      t.Text(langCodes, "Include"),
				},
				{
					Operation: "exclude",
					Name:      t.Text(langCodes, "Exclude"),
				},
				{
					Operation: "match",
					Name:      t.Text(langCodes, "Match"),
				},
				{
					Operation: "notMatch",
					Name:      t.Text(langCodes, "Not Match"),
				},
			},
		}, nil
	case "analysis":
		return &pb.MetaMode{
			Types: map[string]*pb.TypeDefine{
				NumberType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "max",
							Name:        t.Text(langCodes, "Maximum"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "min",
							Name:        t.Text(langCodes, "Minimum"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "sum",
							Name:        t.Text(langCodes, "Sum"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "avg",
							Name:        t.Text(langCodes, "Average"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, "Last Value"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "values",
							Name:        t.Text(langCodes, "All Values"),
							ResultType:  NumberArrayType,
						},
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, "Count"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "distinct_count",
							Name:        t.Text(langCodes, "Distinct Count"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "p99",
							Name:        t.Text(langCodes, "99 Percentile"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "p95",
							Name:        t.Text(langCodes, "95 Percentile"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "p90",
							Name:        t.Text(langCodes, "90 Percentile"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "p75",
							Name:        t.Text(langCodes, "75 Percentile"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "p50",
							Name:        t.Text(langCodes, "50 Percentile"),
							ResultType:  NumberType,
						},
					},
					Operations: []*pb.Operation{
						{
							Operation: "eq",
							Name:      t.Text(langCodes, "Equal"),
						},
						{
							Operation: "neq",
							Name:      t.Text(langCodes, "Not Equal"),
						},
						{
							Operation: "gt",
							Name:      t.Text(langCodes, "Greater Than"),
						},
						{
							Operation: "gte",
							Name:      t.Text(langCodes, "Greater Than Or Equal"),
						},
						{
							Operation: "lt",
							Name:      t.Text(langCodes, "Less Than"),
						},
						{
							Operation: "lte",
							Name:      t.Text(langCodes, "Less Than Or Equal"),
						},
						{
							Operation: "contains",
							Name:      t.Text(langCodes, "Contains"),
						},
						{
							Operation: "all",
							Name:      t.Text(langCodes, "All Values Equal"),
						},
					},
				},
				BoolType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, "Count"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, "Last Value"),
							ResultType:  BoolType,
						},
						{
							Aggregation: "values",
							Name:        t.Text(langCodes, "All Values"),
							ResultType:  BoolArrayType,
						},
					},
					Operations: []*pb.Operation{
						{
							Operation: "eq",
							Name:      t.Text(langCodes, "Equal"),
						},
						{
							Operation: "neq",
							Name:      t.Text(langCodes, "Not Equal"),
						},
						{
							Operation: "contains",
							Name:      t.Text(langCodes, "Contains"),
						},
						{
							Operation: "all",
							Name:      t.Text(langCodes, "All Values Equal"),
						},
					},
				},
				StringType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "distinct",
							Name:        t.Text(langCodes, "Distinct"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, "Count"),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, "Last Value"),
							ResultType:  StringType,
						},
						{
							Aggregation: "values",
							Name:        t.Text(langCodes, "All Values"),
							ResultType:  StringArrayType,
						},
					},
					Operations: []*pb.Operation{
						{
							Operation: "eq",
							Name:      t.Text(langCodes, "Equal"),
						},
						{
							Operation: "neq",
							Name:      t.Text(langCodes, "Not Equal"),
						},
						{
							Operation: "contains",
							Name:      t.Text(langCodes, "Contains"),
						},
						{
							Operation: "all",
							Name:      t.Text(langCodes, "All Values Equal"),
						},
						{
							Operation: "like",
							Name:      t.Text(langCodes, "Contains"),
						},
					},
				},
				NumberArrayType: {
					Operations: []*pb.Operation{
						{
							Operation: "contains",
							Name:      t.Text(langCodes, "Contains"),
						},
						{
							Operation: "all",
							Name:      t.Text(langCodes, "All Values Equal"),
						},
					},
				},
				BoolArrayType: {
					Operations: []*pb.Operation{
						{
							Operation: "contains",
							Name:      t.Text(langCodes, "Contains"),
						},
						{
							Operation: "all",
							Name:      t.Text(langCodes, "All Values Equal"),
						},
					},
				},
				StringArrayType: {
					Operations: []*pb.Operation{
						{
							Operation: "contains",
							Name:      t.Text(langCodes, "Contains"),
						},
						{
							Operation: "all",
							Name:      t.Text(langCodes, "All Values Equal"),
						},
					},
				},
			},
			Filters: []*pb.Operation{
				{
					Operation: "eq",
					Name:      t.Text(langCodes, "Equal"),
				},
				{
					Operation: "neq",
					Name:      t.Text(langCodes, "Not Equal"),
				},
				{
					Operation: "like",
					Name:      t.Text(langCodes, "Include"),
				},
				{
					Operation: "match",
					Name:      t.Text(langCodes, "Match"),
				},
				{
					Operation: "notMatch",
					Name:      t.Text(langCodes, "Not Match"),
				},
			},
		}, nil
	}
	return nil, fmt.Errorf("invalid mode '%s'", mode)
}

func (m *Manager) getTypeAggDefineInflux(langCodes i18n.LanguageCodes, mode string) (*pb.MetaMode, error) {
	t := m.i18n.Translator("_type_aggregations")
	switch mode {
	case "", "query":
		return &pb.MetaMode{
			Types: map[string]*pb.TypeDefine{
				NumberType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "max",
							Name:        t.Text(langCodes, aggNames["max"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "min",
							Name:        t.Text(langCodes, aggNames["min"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "sum",
							Name:        t.Text(langCodes, aggNames["sum"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "avg",
							Name:        t.Text(langCodes, aggNames["avg"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, aggNames["count"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, aggNames["value"]),
							ResultType:  NumberType,
						},
					},
					Filters: []*pb.Operation{
						{
							Operation: "=",
							Name:      t.Text(langCodes, "Equal"),
						},
						{
							Operation: "!=",
							Name:      t.Text(langCodes, "Not Equal"),
						},
						{
							Operation: ">",
							Name:      t.Text(langCodes, "Greater Than"),
						},
						{
							Operation: ">=",
							Name:      t.Text(langCodes, "Greater Than Or Equal"),
						},
						{
							Operation: "<",
							Name:      t.Text(langCodes, "Less Than"),
						},
						{
							Operation: "<=",
							Name:      t.Text(langCodes, "Less Than Or Equal"),
						},
					},
				},
				BoolType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, aggNames["count"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, aggNames["value"]),
							ResultType:  BoolType,
						},
					},
					Filters: []*pb.Operation{
						{
							Operation: "=",
							Name:      t.Text(langCodes, "Equal"),
						},
						{
							Operation: "!=",
							Name:      t.Text(langCodes, "Not Equal"),
						},
					},
				},
				StringType: {
					Aggregations: []*pb.Aggregation{
						{
							Aggregation: "distinct",
							Name:        t.Text(langCodes, aggNames["distinct"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "count",
							Name:        t.Text(langCodes, aggNames["count"]),
							ResultType:  NumberType,
						},
						{
							Aggregation: "value",
							Name:        t.Text(langCodes, aggNames["value"]),
							ResultType:  StringType,
						},
					},
					Filters: []*pb.Operation{
						{
							Operation: "=",
							Name:      t.Text(langCodes, "Equal"),
						},
						{
							Operation: "!=",
							Name:      t.Text(langCodes, "Not Equal"),
						},
						{
							Operation: "=~",
							Name:      t.Text(langCodes, "Regular Expression"),
						},
					},
				},
			},
		}, nil
	}
	return nil, fmt.Errorf("invalid mode '%s'", mode)
}

// GetSingleAggregationMeta .
func (m *Manager) GetSingleAggregationMeta(langCodes i18n.LanguageCodes, mode, name string) (agg *pb.Aggregation, err error) {
	data, err := m.getTypeAggDefine(langCodes, mode)
	if err != nil {
		return nil, err
	}
	for _, x := range data.Types {
		for _, y := range x.Aggregations {
			if y.Aggregation == name {
				return y, nil
			}
		}
	}
	return
}
