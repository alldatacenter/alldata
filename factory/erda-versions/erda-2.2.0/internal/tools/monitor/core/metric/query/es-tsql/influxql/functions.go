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

package esinfluxql

import (
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/influxdata/influxql"
	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/md5x"

	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
)

// Context .
type Context struct {
	now              *time.Time
	calls            map[*influxql.Call]string
	dimensions       map[string]bool
	start, end       int64 // always nanosecond
	originalTimeUnit tsql.TimeUnit
	targetTimeUnit   tsql.TimeUnit
	timeKey          string
	maxTimePoints    int64 // By time interval dimension
	interval         int64 // The actual time interval used
	scopes           map[string]map[string]*scopeField
	aggregations     elastic.Aggregations
	row              int64
	params           map[string]interface{}
	attributesCache  map[string]interface{}
}

type scopeField struct {
	call    *influxql.Call
	handler AggHandler
}

// Now .
func (c *Context) Now() time.Time {
	if c.now == nil {
		now := time.Now()
		c.now = &now
	}
	return *c.now
}

// Range .
func (c *Context) Range(conv bool) (int64, int64) {
	if conv && c.originalTimeUnit != tsql.UnsetTimeUnit {
		return c.start / int64(c.originalTimeUnit), c.end / int64(c.originalTimeUnit)
	}
	return c.start, c.end
}

// OriginalTimeUnit .
func (c *Context) OriginalTimeUnit() tsql.TimeUnit { return c.originalTimeUnit }

// TargetTimeUnit .
func (c *Context) TargetTimeUnit() tsql.TimeUnit { return c.targetTimeUnit }

// TimeKey .
func (c *Context) TimeKey() string { return c.timeKey }

// Interval .
func (c *Context) Interval() int64 { return c.interval }

// Aggregations .
func (c *Context) Aggregations() elastic.Aggregations { return c.aggregations }

// GetFuncID .
func (c *Context) GetFuncID(call *influxql.Call, deftyp influxql.DataType) string {
	if c.calls == nil {
		c.calls = make(map[*influxql.Call]string)
	}
	id, ok := c.calls[call]
	if !ok {
		id = getCallHash(call, deftyp)
		c.calls[call] = id
		return id
	}
	return id
}

// RowNum .
func (c *Context) RowNum() int64 {
	return c.row
}

// AttributeCache .
func (c *Context) AttributesCache() map[string]interface{} {
	if c.attributesCache == nil {
		c.attributesCache = make(map[string]interface{})
	}
	return c.attributesCache
}

func getCallHash(call *influxql.Call, deftyp influxql.DataType) string {
	str, _ := getExprStringAndFlag(call, deftyp)
	return md5x.SumString(str).String16()
}

// HandleScopeAgg .
func (c *Context) HandleScopeAgg(scope string, aggs elastic.Aggregations, expr influxql.Expr) (interface{}, error) {
	fields := c.scopes[scope]
	if fields != nil {
		call, ok := expr.(*influxql.Call)
		if ok {
			id := c.GetFuncID(call, influxql.AnyField)
			f := fields[id]
			if f != nil {
				v, e := f.handler.Handle(aggs)
				return v, e
			}
		}
	}
	return nil, fmt.Errorf("not found scope '%s'", scope)
}

func mustCallArgsNum(call *influxql.Call, num int) error {
	return tsql.MustFuncArgsNum(call.Name, len(call.Args), num)
}

func mustCallArgsMinNum(call *influxql.Call, num int) error {
	return tsql.MustFuncArgsMinNum(call.Name, len(call.Args), num)
}

// FuncFlag .
type FuncFlag int32

// FuncFlag .
const (
	FuncFlagNone   = FuncFlag(0)
	FuncFlagSelect = FuncFlag(1 << (iota - 1))
	FuncFlagWhere
	FuncFlagOrderBy
)

// AggHandler .
type AggHandler interface {
	Aggregations(aggs map[string]elastic.Aggregation, flags ...FuncFlag) error
	Handle(aggs elastic.Aggregations) (interface{}, error)
}

// IsAggFunction .
func IsAggFunction(name string) bool {
	_, ok := AggFunctions[name]
	if ok {
		return true
	}
	return false
}

// IsFunction .
func IsFunction(name string) bool {
	ok := IsAggFunction(name)
	if ok {
		return true
	}
	return tsql.IsFunction(name)
}

// AggFuncDefine .
type AggFuncDefine struct {
	Flag FuncFlag
	New  func(ctx *Context, id string, call *influxql.Call) (AggHandler, error)
}

// AggFunctions .
var AggFunctions = map[string]*AggFuncDefine{
	"max": {
		Flag: FuncFlagSelect | FuncFlagOrderBy,
		New: newUnaryValueAggFunction(
			"max",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewMaxAggregation().Script(script), nil
				}
				return elastic.NewMaxAggregation().Field(field), nil
			},
			func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Max(id)
			},
		),
	},
	"min": {
		Flag: FuncFlagSelect | FuncFlagOrderBy,
		New: newUnaryValueAggFunction(
			"min",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewMinAggregation().Script(script), nil
				}
				return elastic.NewMinAggregation().Field(field), nil
			},
			func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Min(id)
			},
		),
	},
	"avg": {
		Flag: FuncFlagSelect | FuncFlagOrderBy,
		New: newUnaryValueAggFunction(
			"avg",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewAvgAggregation().Script(script), nil
				}
				return elastic.NewAvgAggregation().Field(field), nil
			},
			func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Avg(id)
			},
		),
	},
	"mean": {
		Flag: FuncFlagSelect | FuncFlagOrderBy,
		New: newUnaryValueAggFunction(
			"mean",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewAvgAggregation().Script(script), nil
				}
				return elastic.NewAvgAggregation().Field(field), nil
			},
			func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Avg(id)
			},
		),
	},
	"sum": {
		Flag: FuncFlagSelect | FuncFlagOrderBy,
		New: newUnaryValueAggFunction(
			"sum",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewSumAggregation().Script(script), nil
				}
				return elastic.NewSumAggregation().Field(field), nil
			},
			func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Sum(id)
			},
		),
	},
	"count": {
		Flag: FuncFlagSelect | FuncFlagOrderBy,
		New: newUnaryValueAggFunction(
			"count",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewValueCountAggregation().Script(script), nil
				}
				return elastic.NewValueCountAggregation().Field(field), nil
			},
			func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.ValueCount(id)
			},
		),
	},
	"distinct": {
		Flag: FuncFlagSelect | FuncFlagOrderBy,
		New: newUnaryValueAggFunction(
			"distinct",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewCardinalityAggregation().Script(script), nil
				}
				return elastic.NewCardinalityAggregation().Field(field), nil
			},
			func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Cardinality(id)
			},
		),
	},
	"median": {
		Flag: FuncFlagSelect,
		New: newUnaryAggFunction(
			"median",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewPercentilesAggregation().Percentiles(50).Script(script), nil
				}
				return elastic.NewPercentilesAggregation().Percentiles(50).Field(field), nil
			},
			func(ctx *Context, id, field string, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool) {
				percents, ok := aggs.Percentiles(id)
				if !ok || percents == nil {
					return nil, false
				}
				for _, v := range percents.Values {
					return v, true
				}
				return nil, true
			},
		),
	},
	"percentiles": {
		Flag: FuncFlagSelect,
		New: newMultivariateAggFunction(
			"percentiles",
			func(ctx *Context, id, field string, params []influxql.Expr, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if len(params) == 0 {
					return nil, fmt.Errorf("not percent data")
				}
				ref, ok, err := getLiteralValue(ctx, params[0])
				if !ok || err != nil {
					return nil, fmt.Errorf("invalid percent type error")
				}
				floatPercent, ok := ref.(float64)
				if !ok {
					return nil, fmt.Errorf("invalid percent type error")
				}
				if floatPercent < 0 || floatPercent > 100 {
					return nil, errors.New("percent was out of range")
				}
				if script != nil {
					return elastic.NewPercentilesAggregation().Percentiles(floatPercent).Script(script), nil
				}
				return elastic.NewPercentilesAggregation().Percentiles(floatPercent).Field(field), nil
			},
			func(ctx *Context, id, field string, params []influxql.Expr, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool) {
				percents, ok := aggs.Percentiles(id)
				if !ok || percents == nil {
					return nil, false
				}
				for _, v := range percents.Values {
					return v, true
				}
				return nil, true
			},
		),
	},
	// Diff is the difference value of the countervalue field, that is, the difference between the upper and lower bucket.
	"diff": {
		Flag: FuncFlagSelect,
		New: newUnaryAggFunction(
			"diff",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewMinAggregation().Script(script), nil
				}
				return elastic.NewMinAggregation().Field(field), nil
			},
			func(ctx *Context, id, field string, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool) {
				if next, ok := ctx.attributesCache["next"]; ok {
					min, _ := aggs.Min(id)
					if min == nil {
						return nil, false
					}
					if min.Value == nil {
						return 0, true
					}
					if next, ok := next.(elastic.Aggregations); ok {
						if next, ok := next.Min(id); ok && next != nil && next.Value != nil {
							return *next.Value - *min.Value, true
						}
					}
				}

				return 0, true
			},
		),
	},
	// Diffps is the rate of the countervalue field, that is, the rate of the difference between the upper and lower bucket per second.
	"diffps": {
		Flag: FuncFlagSelect,
		New: newUnaryAggFunction(
			"diffps",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewMinAggregation().Script(script), nil
				}
				return elastic.NewMinAggregation().Field(field), nil
			},
			func(ctx *Context, id, field string, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool) {
				if next, ok := ctx.attributesCache["next"]; ok {
					min, _ := aggs.Min(id)
					if min == nil {
						return nil, false
					}
					if min.Value == nil {
						return 0, true
					}
					if next, ok := next.(elastic.Aggregations); ok {
						if next, ok := next.Min(id); ok && next != nil && next.Value != nil {
							if ctx.targetTimeUnit == tsql.UnsetTimeUnit {
								ctx.targetTimeUnit = tsql.Nanosecond
							}
							seconds := float64(ctx.interval*int64(ctx.targetTimeUnit)) / float64(tsql.Second)
							return (*next.Value - *min.Value) / seconds, true
						}
					}
				}

				return 0, true
			},
		),
	},
	"rateps": {
		Flag: FuncFlagSelect,
		New: newUnaryAggFunction(
			"rateps",
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return elastic.NewSumAggregation().Script(script), nil
				}
				return elastic.NewSumAggregation().Field(field), nil
			},
			func(ctx *Context, id, field string, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool) {
				sum, _ := aggs.Sum(id)
				if sum == nil {
					return nil, false
				}
				if sum.Value == nil {
					return 0, true
				}
				if ctx.targetTimeUnit == tsql.UnsetTimeUnit {
					ctx.targetTimeUnit = tsql.Nanosecond
				}
				seconds := float64(ctx.interval*int64(ctx.targetTimeUnit)) / float64(tsql.Second)
				return *sum.Value / seconds, true
			},
		),
	},
	"first": newSourceFieldAggFunction("first", tsql.TimestampKey, true),
	"last":  newSourceFieldAggFunction("last", tsql.TimestampKey, false),
	"value": newSourceFieldAggFunction("value", tsql.TimestampKey, false),
}

func newSourceFieldAggFunction(name, sort string, ascending bool) *AggFuncDefine {
	return &AggFuncDefine{
		Flag: FuncFlagSelect,
		New: newUnaryAggFunction(
			name,
			func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error) {
				if script != nil {
					return nil, fmt.Errorf("not support script")
				}
				key := tsql.TimestampKey
				if sort == tsql.TimestampKey {
					key = ctx.TimeKey()
				}
				return elastic.NewTopHitsAggregation().Size(1).Sort(key, ascending).
					FetchSourceContext(elastic.NewFetchSourceContext(true).Include(field)), nil
			},
			func(ctx *Context, id, field string, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool) {
				hits, ok := aggs.TopHits(id)
				if !ok {
					return nil, false
				}
				if hits == nil || hits.Hits == nil || len(hits.Hits.Hits) <= 0 || hits.Hits.Hits[0].Source == nil {
					return nil, true
				}
				var out map[string]interface{}
				err := json.Unmarshal([]byte(*hits.Hits.Hits[0].Source), &out)
				if err != nil {
					return nil, true
				}
				return getGetValueFromFlatMap(out, field, "."), true
			},
		),
	}
}

type unaryAggFunction struct {
	name   string
	id     string
	field  string
	call   *influxql.Call
	ctx    *Context
	agg    func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error)
	getter func(ctx *Context, id, field string, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool)
}

func newUnaryAggFunction(
	name string,
	agg func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error),
	getter func(ctx *Context, id, field string, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool),
) func(ctx *Context, id string, call *influxql.Call) (AggHandler, error) {
	return func(ctx *Context, id string, call *influxql.Call) (AggHandler, error) {
		return &unaryAggFunction{
			id:     id,
			name:   name,
			ctx:    ctx,
			call:   call,
			agg:    agg,
			getter: getter,
		}, nil
	}
}

func (f *unaryAggFunction) Aggregations(aggs map[string]elastic.Aggregation, flags ...FuncFlag) error {
	call := f.call
	if err := mustCallArgsNum(call, 1); err != nil {
		return err
	}
	arg := call.Args[0]
	f.id = f.ctx.GetFuncID(call, influxql.AnyField)
	if _, ok := aggs[f.id]; ok {
		return nil
	}
	if ref, ok := arg.(*influxql.VarRef); ok {
		f.field = getKeyName(ref, influxql.AnyField)
		id := f.id
		a, err := f.agg(f.ctx, id, f.field, nil, flags...)
		if err != nil {
			return err
		}
		aggs[f.id] = a
	} else {
		script, err := getScriptExpression(f.ctx, arg, influxql.AnyField, nil)
		if err != nil {
			return err
		}
		a, err := f.agg(f.ctx, f.id, "", elastic.NewScript(script), flags...)
		if err != nil {
			return err
		}
		aggs[f.id] = a
	}
	return nil
}

func (f *unaryAggFunction) Handle(aggs elastic.Aggregations) (interface{}, error) {
	val, ok := f.getter(f.ctx, f.id, f.field, f.call, aggs)
	if !ok {
		return nil, fmt.Errorf("invalid %s Aggregation %s", f.name, f.id)
	}
	return val, nil
}

func newUnaryValueAggFunction(
	name string,
	agg func(ctx *Context, id, field string, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error),
	getter func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool),
) func(ctx *Context, id string, call *influxql.Call) (AggHandler, error) {
	return func(ctx *Context, id string, call *influxql.Call) (AggHandler, error) {
		return &unaryAggFunction{
			id:   id,
			name: name,
			ctx:  ctx,
			call: call,
			agg:  agg,
			getter: func(ctx *Context, id, field string, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool) {
				val, _ := getter(ctx, id, aggs)
				if val == nil || val.Value == nil {
					return float64(0), true
				}
				return *val.Value, true
			},
		}, nil
	}
}

type multivariateAggFunction struct {
	name   string
	id     string
	field  string
	params []influxql.Expr
	call   *influxql.Call
	ctx    *Context
	agg    func(ctx *Context, id, field string, params []influxql.Expr, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error)
	getter func(ctx *Context, id, field string, params []influxql.Expr, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool)
}

func newMultivariateAggFunction(
	name string,
	agg func(ctx *Context, id, field string, params []influxql.Expr, script *elastic.Script, flags ...FuncFlag) (elastic.Aggregation, error),
	getter func(ctx *Context, id, field string, params []influxql.Expr, call *influxql.Call, aggs elastic.Aggregations) (interface{}, bool),
) func(ctx *Context, id string, call *influxql.Call) (AggHandler, error) {
	return func(ctx *Context, id string, call *influxql.Call) (AggHandler, error) {
		return &multivariateAggFunction{
			id:     id,
			name:   name,
			ctx:    ctx,
			call:   call,
			agg:    agg,
			getter: getter,
		}, nil
	}
}

func (f *multivariateAggFunction) Aggregations(aggs map[string]elastic.Aggregation, flags ...FuncFlag) error {
	call := f.call
	if err := mustCallArgsMinNum(call, 1); err != nil {
		return err
	}
	params := call.Args[1:]
	arg := call.Args[0]
	f.id = f.ctx.GetFuncID(call, influxql.AnyField)
	if _, ok := aggs[f.id]; ok {
		return nil
	}
	if ref, ok := arg.(*influxql.VarRef); ok {
		f.field = getKeyName(ref, influxql.AnyField)
		id := f.id

		a, err := f.agg(f.ctx, id, f.field, params, nil, flags...)
		if err != nil {
			return err
		}
		aggs[f.id] = a
	} else {
		script, err := getScriptExpression(f.ctx, arg, influxql.AnyField, nil)
		if err != nil {
			return nil
		}
		a, err := f.agg(f.ctx, f.id, "", params, elastic.NewScript(script), flags...)
		if err != nil {
			return err
		}
		aggs[f.id] = a
	}
	return nil
}

func (f *multivariateAggFunction) Handle(aggs elastic.Aggregations) (interface{}, error) {
	val, ok := f.getter(f.ctx, f.id, f.field, f.params, f.call, aggs)
	if !ok {
		return nil, fmt.Errorf("invalid %s Aggregation %s", f.name, f.id)
	}
	return val, nil
}

// PainlessFunction todo .
type PainlessFunction struct {
	Name         string
	Objective    bool
	ObjectType   string
	DefaultValue string
	Convert      func(ctx *Context, call *influxql.Call, deftyp influxql.DataType, fields map[string]bool) (string, error)
}

// PainlessFunctions .
var PainlessFunctions map[string]*PainlessFunction

func init() {
	PainlessFunctions = map[string]*PainlessFunction{
		"substring": {Name: "substring", Objective: true, ObjectType: "string", DefaultValue: "''"},
		"tostring":  {Name: "toString", Objective: true, ObjectType: "object", DefaultValue: "''"},
		"if": {
			Convert: func(ctx *Context, call *influxql.Call, deftyp influxql.DataType, fields map[string]bool) (string, error) {
				err := mustCallArgsNum(call, 3)
				if err != nil {
					return "", err
				}
				cond, err := getScriptExpression(ctx, call.Args[0], deftyp, fields)
				if err != nil {
					return "", err
				}
				trueExpr, err := getScriptExpression(ctx, call.Args[1], deftyp, fields)
				if err != nil {
					return "", err
				}
				falseExpr, err := getScriptExpression(ctx, call.Args[2], deftyp, fields)
				if err != nil {
					return "", err
				}
				return "((" + cond + ")?(" + trueExpr + "):(" + falseExpr + "))", nil
			},
		},
		"eq": {
			Convert: func(ctx *Context, call *influxql.Call, deftyp influxql.DataType, fields map[string]bool) (string, error) {
				err := mustCallArgsNum(call, 2)
				if err != nil {
					return "", err
				}
				left, err := getScriptExpression(ctx, call.Args[0], deftyp, fields)
				if err != nil {
					return "", err
				}
				right, err := getScriptExpression(ctx, call.Args[1], deftyp, fields)
				if err != nil {
					return "", err
				}
				return "((" + left + ")==(" + right + "))", nil
			},
		},
		"gt": {
			Convert: func(ctx *Context, call *influxql.Call, deftyp influxql.DataType, fields map[string]bool) (string, error) {
				err := mustCallArgsNum(call, 2)
				if err != nil {
					return "", err
				}
				left, err := getScriptExpression(ctx, call.Args[0], deftyp, fields)
				if err != nil {
					return "", err
				}
				right, err := getScriptExpression(ctx, call.Args[1], deftyp, fields)
				if err != nil {
					return "", err
				}
				return "((" + left + ")>(" + right + "))", nil
			},
		},
		"include": {
			Convert: func(ctx *Context, call *influxql.Call, deftyp influxql.DataType, fields map[string]bool) (string, error) {
				err := mustCallArgsMinNum(call, 2)
				if err != nil {
					return "", err
				}
				val, err := getScriptExpression(ctx, call.Args[0], deftyp, fields)
				if err != nil {
					return "", err
				}
				var parts []string
				for _, item := range call.Args[1:] {
					s, err := getScriptExpression(ctx, item, deftyp, fields)
					if err != nil {
						return "", err
					}
					parts = append(parts, "("+val+")"+"==("+s+")")
				}
				return "(" + strings.Join(parts, " || ") + ")", nil
			},
		},
		"not_include": {
			Convert: func(ctx *Context, call *influxql.Call, deftyp influxql.DataType, fields map[string]bool) (string, error) {
				err := mustCallArgsMinNum(call, 2)
				if err != nil {
					return "", err
				}
				val, err := getScriptExpression(ctx, call.Args[0], deftyp, fields)
				if err != nil {
					return "", err
				}
				var parts []string
				for _, item := range call.Args[1:] {
					s, err := getScriptExpression(ctx, item, deftyp, fields)
					if err != nil {
						return "", err
					}
					parts = append(parts, "("+val+")"+"!=("+s+")")
				}
				return "(" + strings.Join(parts, " && ") + ")", nil
			},
		},
	}
}
