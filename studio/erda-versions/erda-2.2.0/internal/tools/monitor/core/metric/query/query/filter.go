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
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"
	"unicode"

	"github.com/olivere/elastic"
)

// Filter .
type Filter struct {
	Key      string
	Operator string
	Value    interface{}
}

// BuildBoolQuery .
func BuildBoolQuery(filters []*Filter, boolQuery *elastic.BoolQuery) error {
	var or *elastic.BoolQuery
	for _, item := range filters {
		switch item.Operator {
		case "eq", "=", "":
			boolQuery.Filter(elastic.NewTermQuery(item.Key, item.Value))
		case "neq", "!=":
			boolQuery.MustNot(elastic.NewTermQuery(item.Key, item.Value))
		case "gt", ">":
			boolQuery.Filter(elastic.NewRangeQuery(item.Key).Gt(item.Value))
		case "gte", ">=":
			boolQuery.Filter(elastic.NewRangeQuery(item.Key).Gte(item.Value))
		case "lt", "<":
			boolQuery.Filter(elastic.NewRangeQuery(item.Key).Lt(item.Value))
		case "lte", "<=":
			boolQuery.Filter(elastic.NewRangeQuery(item.Key).Lte(item.Value))
		case "in":
			if values, ok := item.Value.([]interface{}); ok {
				boolQuery.Filter(elastic.NewTermsQuery(item.Key, values...))
			}
		case "match":
			boolQuery.Filter(elastic.NewWildcardQuery(item.Key, fmt.Sprint(item.Value)))
		case "nmatch":
			boolQuery.MustNot(elastic.NewWildcardQuery(item.Key, fmt.Sprint(item.Value)))
		case "or_eq":
			if or == nil {
				or = elastic.NewBoolQuery()
			}
			or.Should(elastic.NewTermQuery(item.Key, item.Value))
		case "or_in":
			if values, ok := item.Value.([]interface{}); ok {
				if or == nil {
					or = elastic.NewBoolQuery()
				}
				or.Should(elastic.NewTermsQuery(item.Key, values...))
			}
		default:
			return fmt.Errorf("not support filter operator %s", item.Operator)
		}
	}
	if or != nil {
		boolQuery.Must(or)
	}
	return nil
}

// ParseFilters .
func ParseFilters(params url.Values) (filters []*Filter, ps map[string]interface{}) {
	ps = make(map[string]interface{})
	for key, vals := range params {
		switch {
		case strings.HasPrefix(key, "filter_"):
			filters = append(filters, getFilters("filter_", "=", key, vals, nil)...)
		case strings.HasPrefix(key, "eq_"):
			filters = append(filters, getFilters("eq_", "=", key, vals, nil)...)
		case strings.HasPrefix(key, "nfilter_"):
			filters = append(filters, getFilters("nfilter_", "!=", key, vals, nil)...)
		case strings.HasPrefix(key, "notFilter_"): // deprecated
			filters = append(filters, getFilters("notFilter_", "!=", key, vals, nil)...)
		case strings.HasPrefix(key, "neq_"):
			filters = append(filters, getFilters("neq_", "!=", key, vals, nil)...)
		case strings.HasPrefix(key, "match_"):
			filters = append(filters, getFilters("match_", "match", key, vals, nil)...)
		case strings.HasPrefix(key, "nmatch_"):
			filters = append(filters, getFilters("nmatch_", "nmatch", key, vals, nil)...)
		case strings.HasPrefix(key, "notMatch_"): // deprecated
			filters = append(filters, getFilters("notMatch_", "nmatch", key, vals, nil)...)
		case strings.HasPrefix(key, "include_"):
			filters = append(filters, getFilters("include_", "match", key, vals, warpWildcard)...)
		case strings.HasPrefix(key, "like_"):
			filters = append(filters, getFilters("like_", "match", key, vals, warpWildcard)...)
		case strings.HasPrefix(key, "exclude_"):
			filters = append(filters, getFilters("exclude_", "nmatch", key, vals, warpWildcard)...)
		case strings.HasPrefix(key, "nlike"):
			filters = append(filters, getFilters("nlike_", "nmatch", key, vals, warpWildcard)...)
		case strings.HasPrefix(key, "in_"):
			key = key[len("in_"):]
			if key != "" {
				values := toInterfaceSlice(vals)
				filters = append(filters, &Filter{
					Key:      getKeyWithType(key, TagKey),
					Operator: "in",
					Value:    values,
				})
			}
		case strings.HasPrefix(key, "or_eq_"):
			filters = append(filters, getFilters("or_eq_", "or_eq", key, vals, nil)...)
		case strings.HasPrefix(key, "or_in_"):
			key = key[len("or_in_"):]
			if key != "" {
				values := toInterfaceSlice(vals)
				filters = append(filters, &Filter{
					Key:      getKeyWithType(key, TagKey),
					Operator: "or_in",
					Value:    values,
				})
			}
		case strings.HasPrefix(key, "lt_"):
			key = key[len("lt_"):]
			filters = append(filters, getFilters("", "<", getKeyWithType(key, FieldKey), vals, parseValue)...)
		case strings.HasPrefix(key, "lte_"):
			key = key[len("lte_"):]
			filters = append(filters, getFilters("", "<=", getKeyWithType(key, FieldKey), vals, parseValue)...)
		case strings.HasPrefix(key, "gt_"):
			key = key[len("gt_"):]
			filters = append(filters, getFilters("", ">", getKeyWithType(key, FieldKey), vals, parseValue)...)
		case strings.HasPrefix(key, "gte_"):
			key = key[len("gte_"):]
			filters = append(filters, getFilters("", ">=", getKeyWithType(key, FieldKey), vals, parseValue)...)
		case strings.HasPrefix(key, "field_"): // deprecated
			key = key[len("field_"):]
			if key != "" {
				idx := strings.Index(key, "_")
				if idx > 0 {
					field := key[idx+1:]
					if len(field) == 0 {
						continue
					}
					for _, val := range vals {
						if idx := strings.LastIndex(val, ":"); idx > 0 && idx < len(val)-1 {
							val = val[idx+1:]
						}
						filters = append(filters, &Filter{
							Key:      getKeyWithType(field, FieldKey),
							Operator: key[:idx],
							Value:    parseValue(val),
						})
					}
				}
			}
		default:
			if len(vals) == 1 {
				ps[key] = vals[0]
			} else {
				ps[key] = vals
			}
		}
	}
	return filters, ps
}

func getFilters(prefix, op, key string, vals []string, fn func(string) interface{}) (filters []*Filter) {
	key = key[len(prefix):]
	if key != "" {
		for _, val := range vals {
			f := &Filter{
				Key:      getKeyWithType(key, TagKey),
				Operator: op,
				Value:    val,
			}
			if fn != nil {
				f.Value = fn(val)
			}
			filters = append(filters, f)
		}
	}
	return filters
}

func warpWildcard(v string) interface{} { return "*" + v + "*" }

func parseValue(v string) interface{} {
	if strings.HasPrefix(v, "string(") && strings.HasSuffix(v, ")") {
		return v[len("string(") : len(v)-1]
	}
	var value interface{}
	err := json.Unmarshal([]byte(v), &value)
	if err != nil {
		return v
	}
	return value
}

func getKeyWithType(key, typ string) string {
	if strings.Contains(key, ".") || len(typ) == 0 {
		if key[0] == '.' {
			return key[1:]
		}
		return key
	}
	return typ + "." + key
}

func toInterfaceSlice(vals []string) []interface{} {
	values := make([]interface{}, 0, len(vals))
	for _, val := range vals {
		values = append(values, val)
	}
	return values
}

// ParseTimestamp .
func ParseTimestamp(value string, now int64) (int64, error) {
	if now == 0 {
		now = time.Now().UnixNano() / int64(time.Millisecond)
	}
	if len(value) > 0 && unicode.IsDigit([]rune(value)[0]) {
		ts, err := strconv.ParseInt(value, 10, 64)
		if err != nil {
			return 0, fmt.Errorf("invalid timestamp %s", value)
		}
		return ts, nil
	}

	var align, end bool
	var unit string
	if strings.HasPrefix(value, "align_") {
		value, align = value[len("align_"):], true
		if strings.HasPrefix(value, "start_") {
			value = value[len("start_"):]
			idx := strings.Index(value, "_")
			if idx >= 0 {
				unit = value[0:idx]
				value = value[idx+1:]
			}
		} else if strings.HasPrefix(value, "end_") {
			value, end = value[len("end_"):], true
			idx := strings.Index(value, "_")
			if idx >= 0 {
				unit = value[0:idx]
				value = value[idx+1:]
			}
		}
	}
	if len(unit) > 0 {
		_, ok := getTimeUnit(unit)
		if !ok {
			return 0, fmt.Errorf("invalid time unit %s", unit)
		}
	}
	if strings.HasPrefix(value, "before_") {
		d, err := getMillisecond(value[len("before_"):])
		if err != nil {
			return 0, nil
		}
		now = now - d
	} else if strings.HasPrefix(value, "after_") {
		d, err := getMillisecond(value[len("after_"):])
		if err != nil {
			return 0, nil
		}
		now = now + d
	}
	if align {
		now = alignTime(now, unit, end)
	}
	return now, nil
}

func getMillisecond(value string) (int64, error) {
	for _, d := range []string{"day", "days"} {
		if strings.HasSuffix(value, d) {
			val := value[0 : len(value)-len(d)]
			v, err := strconv.ParseInt(val, 10, 64)
			if err != nil {
				return 0, fmt.Errorf("invalid duration: %s", val)
			}
			return v * 24 * int64(time.Hour) / int64(time.Millisecond), nil
		}
	}
	if strings.HasSuffix(value, "d") {
		v, err := strconv.ParseInt(value[0:len(value)-1], 10, 64)
		if err == nil {
			return v * 24 * int64(time.Hour) / int64(time.Millisecond), nil
		}
	}
	d, err := time.ParseDuration(value)
	if err != nil {
		return 0, fmt.Errorf("invalid duration: %s", value)
	}
	return int64(d) / int64(time.Millisecond), nil
}

func alignTime(t int64, unit string, end bool) int64 {
	var offset int64
	switch unit {
	case "d", "day":
		offset = -8 * int64(time.Hour) / int64(time.Millisecond)
	}
	align, _ := getTimeUnit(unit)
	align /= int64(time.Millisecond)
	t = t - t%align + offset
	if end {
		t += align
	}
	return t
}

func getTimeUnit(unit string) (int64, bool) {
	switch unit {
	case "s", "sec", "second":
		return int64(time.Second), true
	case "m", "min", "minute":
		return int64(time.Minute), true
	case "h", "hour":
		return int64(time.Hour), true
	case "d", "day":
		return 24 * int64(time.Hour), true
	}
	return 0, false
}

// ParseTimeRange .
func ParseTimeRange(start, end, timestamp, latest string) (st int64, et int64, err error) {
	var now int64
	if timestamp != "" {
		now, err = strconv.ParseInt(timestamp, 10, 64)
		if err != nil {
			return 0, 0, fmt.Errorf("invalid timestamp: %s", timestamp)
		}
	} else {
		now = time.Now().UnixNano() / int64(time.Millisecond)
	}
	if latest != "" {
		d, err := getMillisecond(latest)
		if err != nil {
			return 0, 0, err
		}
		return now - d, now, nil
	}
	if len(start) > 0 {
		st, err = ParseTimestamp(start, now)
		if err != nil {
			return 0, 0, err
		}
	} else {
		st = 0
	}
	et, err = ParseTimestamp(end, now)
	if err != nil {
		return 0, 0, err
	}
	return st, et, nil
}
