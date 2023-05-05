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

package tsql

import (
	"fmt"
	"math"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/influxdata/influxql"
	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/conv"
	"github.com/recallsong/go-utils/lang/size"
)

// Context .
type Context interface {
	Now() time.Time
	Range(conv bool) (int64, int64)
	OriginalTimeUnit() TimeUnit // Represents the field returned by TimeKey (), the original unit.
	TargetTimeUnit() TimeUnit   // If it is necessary to return the time, the time is displayed in that unit.
	TimeKey() string            // Default timestamp
	Aggregations() elastic.Aggregations
	HandleScopeAgg(scope string, aggs elastic.Aggregations, expr influxql.Expr) (interface{}, error)
	RowNum() int64
	AttributesCache() map[string]interface{}
}

var timeLayouts = []string{
	time.RFC3339,
	"2006-01-02 15:04:05",
	"2006-01-02",
}

// BuildInFunctions is custom functions in SELECT
var BuildInFunctions = map[string]func(ctx Context, args ...interface{}) (interface{}, error){
	"time": func(ctx Context, args ...interface{}) (interface{}, error) {
		for _, arg := range args {
			if b, ok := arg.(*elastic.AggregationBucketHistogramItem); ok {
				t := int64(b.Key)
				unit := ctx.TargetTimeUnit()
				if unit == UnsetTimeUnit {
					if ctx.OriginalTimeUnit() != UnsetTimeUnit {
						t *= int64(ctx.OriginalTimeUnit())
					}
					return time.Unix(t/int64(time.Second), t%int64(time.Second)).Format("2006-01-02T15:04:05Z"), nil
				}
				return ConvertTimestamp(t, ctx.OriginalTimeUnit(), unit), nil
			}
		}
		return 0, fmt.Errorf("function 'time' not in group or not found time bucket")
	},
	"timestamp": func(ctx Context, args ...interface{}) (interface{}, error) {
		for _, arg := range args {
			if b, ok := arg.(*elastic.AggregationBucketHistogramItem); ok {
				return ConvertTimestamp(int64(b.Key), ctx.OriginalTimeUnit(), ctx.TargetTimeUnit()), nil
			}
		}
		return 0, fmt.Errorf("function 'timestamp' not in group or not found time bucket")
	},
	"range": func(ctx Context, args ...interface{}) (interface{}, error) {
		for _, arg := range args {
			if b, ok := arg.(*elastic.AggregationBucketRangeItem); ok {
				var from, to string
				if b.From != nil {
					from = strconv.FormatFloat(*b.From, 'f', -1, 64)
				}
				if b.To != nil {
					to = strconv.FormatFloat(*b.To, 'f', -1, 64)
				}
				return from + "-" + to, nil
			}
		}
		return 0, fmt.Errorf("function 'range' not in group or not found range bucket")
	},
	"scope": func(ctx Context, args ...interface{}) (interface{}, error) {
		if len(args) < 2 {
			return nil, fmt.Errorf("invalid args for function 'scope'")
		}
		last := len(args) - 1
		expr, ok := args[last].(influxql.Expr)
		if !ok {
			return 0, fmt.Errorf("invalid args for function 'scope'")
		}
		aggs := ctx.Aggregations()
		scope, ok := args[last-1].(string)
		if !ok {
			return nil, fmt.Errorf("invalid args for function 'scope'")
		}
		if scope == "terms" {
			for _, arg := range args[0:last] {
				if b, ok := arg.(*elastic.AggregationBucketKeyItem); ok {
					aggs = b.Aggregations
					break
				}
			}
		}
		return ctx.HandleScopeAgg(scope, aggs, expr)
	},
	"row_num": func(ctx Context, args ...interface{}) (interface{}, error) {
		return ctx.RowNum(), nil
	},
	"default_value": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("default_value", len(args), 2)
		if err != nil {
			return nil, err
		}
		if args[0] == nil {
			return args[1], nil
		}
		return args[0], nil
	},
	// string
	"format": func(ctx Context, args ...interface{}) (interface{}, error) {
		if len(args) < 1 {
			return nil, fmt.Errorf("function '%s' args must more than %d", "format", 1)
		}
		text, err := getStringArg("format", 0, args[0])
		if err != nil {
			return nil, err
		}
		return fmt.Sprintf(text, args[1:]...), nil
	},
	"format_time": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("format_time", len(args), 2)
		if err != nil {
			return nil, err
		}
		t, err := getTimeArg("format_time", 0, args[0], timeLayouts)
		if err != nil {
			return nil, err
		}
		layout, err := getStringArg("format_time", 1, args[1])
		if err != nil {
			return nil, err
		}
		return t.Format(layout), nil
	},
	"format_date": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("format_date", len(args), 1)
		if err != nil {
			return nil, err
		}
		t, err := getTimeArg("format_date", 0, args[0], timeLayouts)
		if err != nil {
			return nil, err
		}
		return t.Format("2006-01-02"), nil
	},
	"format_bytes": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("format_bytes", len(args), 1)
		if err != nil {
			return nil, err
		}
		return size.FormatBytes(conv.ToInt64(args[0], 0)), nil
	},
	"format_duration": func(ctx Context, args ...interface{}) (interface{}, error) {
		if len(args) < 1 {
			return nil, fmt.Errorf("function '%s' args must more than %d", "format_duration", 1)
		}
		v := conv.ToFloat64(args[0], 0)
		unit := "ns"
		if len(args) > 1 {
			u, err := getStringArg("format_duration", 1, args[1])
			if err != nil {
				return nil, err
			}
			unit = u
		}
		switch unit {
		case "ms":
			v *= float64(time.Millisecond)
		case "s":
			v *= float64(time.Second)
		case "m", "min":
			v *= float64(time.Minute)
		case "h":
			v *= float64(time.Hour)
		case "d":
			v *= 24 * float64(time.Hour)
		}
		if len(args) > 2 {
			return FormatDuration(time.Duration(v), conv.ToInt(args[2], 2)), nil
		}
		return time.Duration(int64(v)).String(), nil
	},
	"map": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsMinNum("map", len(args), 3)
		if err != nil {
			return nil, err
		}
		if len(args)%2 != 1 {
			return nil, fmt.Errorf("invalid key-value pairs")
		}
		var conv func(interface{}) interface{}
		switch args[0].(type) {
		case int, int64:
			conv = func(val interface{}) interface{} {
				switch v := val.(type) {
				case int:
					return int64(v)
				case uint:
					return int64(v)
				case uint64:
					return int64(v)
				case float32:
					return int64(v)
				case float64:
					return int64(v)
				}
				return val
			}
		case uint, uint64:
			conv = func(val interface{}) interface{} {
				switch v := val.(type) {
				case int:
					return uint64(v)
				case int64:
					return uint64(v)
				case uint:
					return uint64(v)
				case float32:
					return uint64(v)
				case float64:
					return uint64(v)
				}
				return val
			}
		case float32, float64:
			conv = func(val interface{}) interface{} {
				switch v := val.(type) {
				case int:
					return float64(v)
				case int64:
					return float64(v)
				case uint:
					return float64(v)
				case uint64:
					return float64(v)
				case float32:
					return float64(v)
				}
				return val
			}
		default:
			conv = func(val interface{}) interface{} { return val }
		}
		for i, l := 1, len(args); i < l; i = i + 2 {
			if conv(args[i]) == args[0] {
				return args[i+1], nil
			}
		}
		return args[0], nil
	},
	"round_float": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("round_float", len(args), 2)
		if err != nil {
			return nil, err
		}
		var v float64
		switch val := args[0].(type) {
		case float32:
			v = float64(val)
		case float64:
			v = val
		default:
			return args[0], nil
		}
		v, _ = strconv.ParseFloat(fmt.Sprintf("%."+strconv.Itoa(conv.ToInt(args[1], 2))+"f", v), 64)
		return v, nil
	},
	"trim": func(ctx Context, args ...interface{}) (interface{}, error) {
		return stringTrim("trim", args, strings.Trim)
	},
	"trim_left": func(ctx Context, args ...interface{}) (interface{}, error) {
		return stringTrim("trim_left", args, strings.TrimLeft)
	},
	"trim_right": func(ctx Context, args ...interface{}) (interface{}, error) {
		return stringTrim("trim_right", args, strings.TrimRight)
	},
	"trim_space": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("trim_space", len(args), 1)
		if err != nil {
			return nil, err
		}
		text, err := getStringArg("trim_space", 0, args[0])
		if err != nil {
			return nil, err
		}
		return strings.TrimSpace(text), nil
	},
	"trim_prefix": func(ctx Context, args ...interface{}) (interface{}, error) {
		return stringTrim("trim_prefix", args, strings.TrimPrefix)
	},
	"trim_suffix": func(ctx Context, args ...interface{}) (interface{}, error) {
		return stringTrim("trim_suffix", args, strings.TrimSuffix)
	},
	// math
	"max_value": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("max_value", len(args), 2)
		if err != nil {
			return nil, err
		}
		v, _ := OperateValues(args[0], LT, args[1])
		if v.(bool) {
			return args[1], nil
		}
		return args[0], nil
	},
	"min_value": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("min_value", len(args), 2)
		if err != nil {
			return nil, err
		}
		v, _ := OperateValues(args[0], GT, args[1])
		if v.(bool) {
			return args[1], nil
		}
		return args[0], nil
	},
	// convert
	"int": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("int", len(args), 1)
		if err != nil {
			return nil, err
		}
		switch val := args[0].(type) {
		case nil:
			return int64(0), nil
		case bool:
			return boolToInt(val), nil
		case int:
			return int64(val), nil
		case int8:
			return int64(val), nil
		case int16:
			return int64(val), nil
		case int32:
			return int64(val), nil
		case int64:
			return int64(val), nil
		case uint:
			return int64(val), nil
		case uint8:
			return int64(val), nil
		case uint16:
			return int64(val), nil
		case uint32:
			return int64(val), nil
		case uint64:
			return int64(val), nil
		case float32:
			return int64(val), nil
		case float64:
			return int64(val), nil
		case string:
			v, err := strconv.ParseInt(val, 10, 64)
			if err == nil {
				return v, nil
			}
		case time.Duration:
			return int64(val), nil
		case time.Time:
			return val.UnixNano(), nil
		}
		return int64(0), fmt.Errorf("can't convert %d to int", reflect.TypeOf(args[0]))
	},
	"bool": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("bool", len(args), 1)
		if err != nil {
			return nil, err
		}
		switch val := args[0].(type) {
		case nil:
			return false, nil
		case bool:
			return val, nil
		case int:
			return val > 0, nil
		case int8:
			return val != 0, nil
		case int16:
			return val != 0, nil
		case int32:
			return val != 0, nil
		case int64:
			return val != 0, nil
		case uint:
			return val != 0, nil
		case uint8:
			return val != 0, nil
		case uint16:
			return val != 0, nil
		case uint32:
			return val != 0, nil
		case uint64:
			return val != 0, nil
		case float32:
			return val != 0, nil
		case float64:
			return val != 0, nil
		case string:
			return len(val) > 0, nil
		case time.Duration:
			return val != 0, nil
		case time.Time:
			return val.UnixNano() != 0, nil
		}
		return args[0] != nil, nil
	},
	"float": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("float", len(args), 1)
		if err != nil {
			return nil, err
		}
		switch val := args[0].(type) {
		case nil:
			return float64(0), nil
		case bool:
			return float64(boolToInt(val)), nil
		case int:
			return float64(val), nil
		case int8:
			return float64(val), nil
		case int16:
			return float64(val), nil
		case int32:
			return float64(val), nil
		case int64:
			return float64(val), nil
		case uint:
			return float64(val), nil
		case uint8:
			return float64(val), nil
		case uint16:
			return float64(val), nil
		case uint32:
			return float64(val), nil
		case uint64:
			return float64(val), nil
		case float32:
			return float64(val), nil
		case float64:
			return float64(val), nil
		case string:
			v, err := strconv.ParseFloat(val, 64)
			if err == nil {
				return v, nil
			}
		case time.Duration:
			return float64(val), nil
		case time.Time:
			return float64(val.UnixNano()), nil
		}
		return float64(0), fmt.Errorf("can't convert %d to float", reflect.TypeOf(args[0]))
	},
	"string": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("duration", len(args), 1)
		if err != nil {
			return nil, err
		}
		return fmt.Sprint(args[0]), nil
	},
	"duration": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("duration", len(args), 1)
		if err != nil {
			return nil, err
		}
		switch val := args[0].(type) {
		case nil:
			return time.Duration(0), nil
		case bool:
			return time.Duration(boolToInt(val)), nil
		case int:
			return time.Duration(val), nil
		case int8:
			return time.Duration(val), nil
		case int16:
			return time.Duration(val), nil
		case int32:
			return time.Duration(val), nil
		case int64:
			return time.Duration(val), nil
		case uint:
			return time.Duration(val), nil
		case uint8:
			return time.Duration(val), nil
		case uint16:
			return time.Duration(val), nil
		case uint32:
			return time.Duration(val), nil
		case uint64:
			return time.Duration(val), nil
		case float32:
			return time.Duration(val), nil
		case float64:
			return time.Duration(val), nil
		case string:
			v, err := time.ParseDuration(val)
			if err == nil {
				return v, nil
			}
			return v, nil
		case time.Duration:
			return time.Duration(val), nil
		}
		return time.Duration(0), fmt.Errorf("can't convert %d to duration", reflect.TypeOf(args[0]))
	},
	"parse_time": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsMinNum("parse_time", len(args), 1)
		if err != nil {
			return nil, err
		}
		layouts := timeLayouts
		if len(args) > 1 {
			layout, ok := args[1].(string)
			if !ok {
				return nil, fmt.Errorf("args[1] is not time layout")
			}
			layouts = []string{layout}
		}
		return getTimeArg("parse_time", 0, args[0], layouts)
	},
	// both painless support
	"substring": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsMinNum("substring", len(args), 2)
		if err != nil {
			return nil, err
		}
		s, ok := args[0].(string)
		if !ok {
			return nil, fmt.Errorf("args[0] is not string")
		}
		start, end := conv.ToInt(args[1], 0), len(s)
		if len(args) > 2 {
			end = conv.ToInt(args[2], end)
		}
		if start > len(s) {
			start = len(s)
		}
		if end > len(s) {
			end = len(s)
		}
		if start > end {
			start = end
		}
		return s[start:end], nil
	},
	"tostring": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("tostring", len(args), 1)
		if err != nil {
			return nil, err
		}
		if args[0] == nil {
			return "", nil
		}
		return fmt.Sprint(args[0]), nil
	},
	"if": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("if", len(args), 3)
		if err != nil {
			return nil, err
		}
		b, ok := args[0].(bool)
		if !ok {
			return nil, fmt.Errorf("args[0] is not boolean")
		}
		if b {
			return args[1], nil
		}
		return args[2], nil
	},
	"eq": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("eq", len(args), 2)
		if err != nil {
			return nil, err
		}
		return args[0] == args[1], nil
	},
	"neq": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("neq", len(args), 2)
		if err != nil {
			return nil, err
		}
		return args[0] != args[1], nil
	},
	// like sql in()
	"include": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsMinNum("include", len(args), 2)
		if err != nil {
			return nil, err
		}
		val := args[0]
		for _, arg := range args[1:] {
			if val == arg {
				return true, nil
			}
		}
		return false, nil
	},
	"gt": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("gt", len(args), 2)
		if err != nil {
			return nil, err
		}
		arrFloat, err := CheckNumerical(args)
		if err != nil {
			return nil, err
		}
		return arrFloat[0] > arrFloat[1], nil
	},
	"gte": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("gte", len(args), 2)
		if err != nil {
			return nil, err
		}
		arrFloat, err := CheckNumerical(args)
		if err != nil {
			return nil, err
		}
		return arrFloat[0] >= arrFloat[1], nil
	},
	"lt": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("lt", len(args), 2)
		if err != nil {
			return nil, err
		}
		arrFloat, err := CheckNumerical(args)
		if err != nil {
			return nil, err
		}
		return arrFloat[0] < arrFloat[1], nil
	},
	"lte": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsNum("lte", len(args), 2)
		if err != nil {
			return nil, err
		}
		arrFloat, err := CheckNumerical(args)
		if err != nil {
			return nil, err
		}
		return arrFloat[0] <= arrFloat[1], nil
	},
	"andf": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsMinNum("andf", len(args), 2)
		if err != nil {
			return nil, err
		}
		for index, val := range args {
			b, ok := val.(bool)
			if !ok {
				return nil, fmt.Errorf("args[%v] is not boolean", index)
			}
			if !b {
				return false, nil
			}
		}
		return true, nil
	},
	"orf": func(ctx Context, args ...interface{}) (interface{}, error) {
		err := MustFuncArgsMinNum("orf", len(args), 2)
		if err != nil {
			return nil, err
		}
		for index, val := range args {
			b, ok := val.(bool)
			if !ok {
				return nil, fmt.Errorf("args[%v] is not boolean", index)
			}
			if b {
				return true, nil
			}
		}
		return false, nil
	},
}

// LiteralFunctions is constant functions in SELECT
var LiteralFunctions = map[string]func(ctx Context, args ...interface{}) (interface{}, error){
	"interval": func(ctx Context, args ...interface{}) (interface{}, error) {
		start, end := ctx.Range(false)
		if start >= end {
			return 1, nil
		}
		interval := end - start
		if len(args) > 0 {
			unit, ok := args[0].(string)
			if !ok {
				return nil, fmt.Errorf("invalid time unit")
			}
			if len(unit) > 0 {
				u, err := ParseTimeUnit(unit)
				if err != nil {
					return nil, err
				}
				return interval / int64(u), nil
			}
		}
		return interval, nil
	},
	// time
	"now": func(ctx Context, args ...interface{}) (interface{}, error) {
		return ctx.Now().UnixNano(), nil
	},
	"now_sec": func(ctx Context, args ...interface{}) (interface{}, error) {
		return ctx.Now().UnixNano() / int64(time.Second), nil
	},
	"now_ms": func(ctx Context, args ...interface{}) (interface{}, error) {
		return ctx.Now().UnixNano() / int64(time.Millisecond), nil
	},
	"unix": func(ctx Context, args ...interface{}) (interface{}, error) {
		return ctx.Now().Unix(), nil
	},
	"unix_ns": func(ctx Context, args ...interface{}) (interface{}, error) {
		return ctx.Now().UnixNano(), nil
	},
	"date": func(ctx Context, args ...interface{}) (interface{}, error) {
		return ctx.Now().Format("2006-01-02"), nil
	},
	// max value
	"max_uint8": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxUint8, nil
	},
	"max_uint16": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxUint16, nil
	},
	"max_uint32": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxUint32, nil
	},
	"max_uint64": func(ctx Context, args ...interface{}) (interface{}, error) {
		return uint64(math.MaxUint64), nil
	},
	"max_int8": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxInt8, nil
	},
	"max_int16": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxInt16, nil
	},
	"max_int32": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxInt32, nil
	},
	"max_int64": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxInt64, nil
	},
	"max_float32": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxFloat32, nil
	},
	"max_float64": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MaxFloat64, nil
	},
	// min value
	"min_int8": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MinInt8, nil
	},
	"min_int16": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MinInt16, nil
	},
	"min_int32": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MinInt32, nil
	},
	"min_int64": func(ctx Context, args ...interface{}) (interface{}, error) {
		return math.MinInt64, nil
	},
}

// IsFunction check function is exist.
func IsFunction(name string) bool {
	if _, ok := LiteralFunctions[name]; ok {
		return true
	}
	_, ok := BuildInFunctions[name]
	return ok
}

// MustFuncArgsNum check whether the number of input parameters meets the equal condition.
func MustFuncArgsNum(name string, args, num int) error {
	if args < num {
		return fmt.Errorf("function '%s' must has %d args", name, num)
	} else if args > num {
		return fmt.Errorf("function '%s' expect %d args, but got %d args", name, num, args)
	}
	return nil
}

// MustFuncArgsMinNum check whether the number of input parameters meets the minimum condition.
func MustFuncArgsMinNum(name string, args, num int) error {
	if args < num {
		return fmt.Errorf("function '%s' args must more than %d", name, num)
	}
	return nil
}

// stringTrim verify both parameters entered are strings.
func stringTrim(name string, args []interface{}, fn func(string, string) string) (interface{}, error) {
	err := MustFuncArgsNum(name, len(args), 2)
	if err != nil {
		return nil, err
	}
	text, err := getStringArg(name, 0, args[0])
	if err != nil {
		return nil, err
	}
	arg, err := getStringArg(name, 1, args[1])
	if err != nil {
		return nil, err
	}
	return fn(text, arg), nil
}

func getStringArg(name string, i int, arg interface{}) (string, error) {
	text, ok := arg.(string)
	if !ok {
		return "", fmt.Errorf("function '%s' args[%d] %v is not a string", name, i, arg)
	}
	return text, nil
}

func getTimeArg(name string, i int, arg interface{}, layouts []string) (time.Time, error) {
	formats := timeLayouts
	if len(layouts) > 0 {
		formats = layouts
	}
	t, ok := getTimeValue(arg, formats)
	if ok {
		return t, nil
	}
	return time.Time{}, fmt.Errorf("function '%s' args[%d] %v is not a time", name, i, arg)
}

// getTimeValue.
func getTimeValue(v interface{}, layouts []string) (time.Time, bool) {
	switch val := v.(type) {
	case int:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case int8:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case int16:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case int32:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case int64:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case uint:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case uint8:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case uint16:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case uint32:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case uint64:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case float32:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case float64:
		return time.Unix(int64(val)/int64(time.Second), int64(val)%int64(time.Second)), true
	case string:
		for _, layout := range layouts {
			t, err := time.Parse(layout, val)
			if err == nil {
				return t, true
			}
		}
		v, err := strconv.ParseInt(val, 10, 64)
		if err == nil {
			return time.Unix(v/int64(time.Second), v%int64(time.Second)), true
		}
	case time.Time:
		return val, true
	}
	return time.Time{}, false
}

// GetTimestampValue .
func GetTimestampValue(v interface{}) (int64, bool) {
	switch val := v.(type) {
	case int:
		return int64(val), true
	case int8:
		return int64(val), true
	case int16:
		return int64(val), true
	case int32:
		return int64(val), true
	case int64:
		return val, true
	case uint:
		return int64(val), true
	case uint8:
		return int64(val), true
	case uint16:
		return int64(val), true
	case uint32:
		return int64(val), true
	case uint64:
		return int64(val), true
	case float32:
		return int64(val), true
	case float64:
		return int64(val), true
	case string:
		for _, layout := range timeLayouts {
			t, err := time.Parse(layout, val)
			if err == nil {
				return t.UnixNano(), true
			}
		}
		v, err := strconv.ParseInt(val, 10, 64)
		if err == nil {
			return v, true
		}
	case time.Time:
		return val.UnixNano(), true
	}
	return 0, false
}

// convertToFloat64 convert interfaces to float64 , return false if not numerical type.
func convertToFloat64(v interface{}) (float64, bool) {
	switch val := v.(type) {
	case int:
		return float64(val), true
	case int8:
		return float64(val), true
	case int16:
		return float64(val), true
	case int32:
		return float64(val), true
	case int64:
		return float64(val), true
	case uint:
		return float64(val), true
	case uint8:
		return float64(val), true
	case uint16:
		return float64(val), true
	case uint32:
		return float64(val), true
	case uint64:
		return float64(val), true
	case float32:
		return float64(val), true
	case float64:
		return val, true
	case time.Duration:
		return float64(val), true
	}
	return 0, false
}

// CheckNumerical Check whether interfaces is a numeric type.
func CheckNumerical(v []interface{}) ([]float64, error) {
	var arrFloat []float64
	for index, val := range v {
		floatVal, ok := convertToFloat64(val)
		if !ok {
			return nil, fmt.Errorf("args[%v] is not numerical type", index)
		}
		arrFloat = append(arrFloat, floatVal)
	}
	return arrFloat, nil
}

// Keep the specified decimal places.
func FormatDuration(d time.Duration, precision int) string {
	val, base := int64(d), int64(time.Nanosecond)
	switch {
	case val <= int64(time.Microsecond):
		return d.String()
	case val <= int64(time.Millisecond):
		base = int64(time.Microsecond)
	case val <= int64(time.Second):
		base = int64(time.Millisecond)
	default:
		base = int64(time.Second)
	}
	for i := 0; i < precision; i++ {
		base /= 10
	}
	if base > 1 {
		if (val % base) >= (base / 2) {
			return time.Duration((val/base + 1) * base).String()
		}
		return time.Duration(val / base * base).String()
	}
	return d.String()
}
