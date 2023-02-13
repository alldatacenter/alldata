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
	"net/url"
	"strconv"
	"strings"
	"time"
)

type TimeAlignCondition string

type FuncCondition struct {
	Name, Field string
}

type GroupCondition string

type FilterCondition struct {
	Key, Op, Val string
}

type FieldCondition struct {
	key, op, val string
	req          *MetricQueryRequest
}

type RangeCondition struct {
	Name   string
	Ranges []struct {
		From int
		To   int
	}

	Split     int
	RangeSize float64
}

type MetricQueryRequest struct {
	diagram    string
	scope      string
	format     string
	start, end time.Time
	point      int
	align      TimeAlignCondition
	filters    []FilterCondition
	fieldMaps  []FieldCondition
	groups     []GroupCondition
	limits     []int
	functions  []FuncCondition
	sorts      []string
	range_     *RangeCondition
}

func CreateQueryRequest(metricName string) *MetricQueryRequest {
	now := time.Now()
	start := now.Add(-time.Hour * 1)
	req := &MetricQueryRequest{
		scope:     metricName,
		start:     start,
		end:       now,
		point:     10,
		filters:   make([]FilterCondition, 0),
		groups:    make([]GroupCondition, 0),
		functions: make([]FuncCondition, 0),
		sorts:     make([]string, 0),
		range_:    nil,
	}
	return req
}

func (req *MetricQueryRequest) StartFrom(start time.Time) *MetricQueryRequest {
	req.start = start
	return req
}

func (req *MetricQueryRequest) EndWith(end time.Time) *MetricQueryRequest {
	req.end = end
	return req
}

// chart:
// chartv2:
func (req *MetricQueryRequest) FormatAs(format string) *MetricQueryRequest {
	req.format = format
	return req
}

func (req *MetricQueryRequest) SetDiagram(diagram string) *MetricQueryRequest {
	req.diagram = diagram
	return req
}

func (req *MetricQueryRequest) Filter(key, val string) *MetricQueryRequest {
	req.filters = append(req.filters, FilterCondition{key, "filter", val})
	return req
}

func (req *MetricQueryRequest) Match(key, val string) *MetricQueryRequest {
	req.filters = append(req.filters, FilterCondition{key, "match", val})
	return req
}

func (req *MetricQueryRequest) Field(key string) *FieldCondition {
	return &FieldCondition{
		req: req,
		key: key,
	}
}

func (req *MetricQueryRequest) In(key string, values []string) *MetricQueryRequest {
	for _, val := range values {
		req.filters = append(req.filters, FilterCondition{key, "in", val})
	}
	return req
}

func (req *MetricQueryRequest) LimitPoint(point int) *MetricQueryRequest {
	req.point = point
	return req
}

// see：https://yuque.antfin.com/spot/develop-docs/hr2c1y#f14b2b31
func (req *MetricQueryRequest) Apply(funcName, field string) *MetricQueryRequest {
	req.functions = append(req.functions, FuncCondition{funcName, field})
	return req
}

func (req *MetricQueryRequest) GroupBy(groups []string) *MetricQueryRequest {
	for _, g := range groups {
		req.groups = append(req.groups, GroupCondition(g))
	}
	return req
}

func (req *MetricQueryRequest) LimitGroup(limit int) *MetricQueryRequest {
	req.limits = append(req.limits, limit)
	return req
}

func (req *MetricQueryRequest) Sort(field string) *MetricQueryRequest {
	req.sorts = append(req.sorts, field)
	return req
}

func (req *MetricQueryRequest) Align(align TimeAlignCondition) *MetricQueryRequest {
	req.align = align
	return req
}

func (req *MetricQueryRequest) ConstructParam() *url.Values {
	param := url.Values{}
	if req.point > 0 {
		param.Add("points", strconv.Itoa(req.point))
	}
	if req.format != "" {
		param.Add("format", req.format)
	}
	// mile second
	param.Add("start", strconv.Itoa(int(req.start.Unix())*1000))
	param.Add("end", strconv.Itoa(int(req.end.Unix())*1000))
	for _, f := range req.filters {
		param.Add(strings.Join([]string{f.Op, f.Key}, "_"), f.Val)
	}
	for _, f := range req.functions {
		param.Add(f.Name, f.Field)
	}

	for _, g := range req.groups {
		param.Add("group", string(g))
	}
	for _, v := range req.limits {
		param.Add("limit", strconv.Itoa(v))
	}

	for _, fie := range req.fieldMaps {
		param.Add(strings.Join([]string{"field", fie.op, fie.key}, "_"), fie.val)
	}

	if req.range_ != nil {
		// todo
	}

	return &param
}
