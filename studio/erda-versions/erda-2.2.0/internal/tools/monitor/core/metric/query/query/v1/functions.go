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

package queryv1

import (
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"
	"strings"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/conv"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

// Functions .
var Functions = map[string]func(col *Column) Function{
	"max": func(col *Column) Function {
		return &esValueFunction{
			esFunction: esFunction{
				Column:         col,
				supportOrderBy: true,
				reduceSupport:  true,
				AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
					if c.Property.IsScript() {
						return elastic.NewMaxAggregation().Script(elastic.NewScript(c.Property.Script)), nil
					}
					return elastic.NewMaxAggregation().Field(c.Property.Key), nil
				},
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Max(id)
			},
		}
	},
	"min": func(col *Column) Function {
		return &esValueFunction{
			esFunction: esFunction{
				Column:         col,
				supportOrderBy: true,
				reduceSupport:  true,
				AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
					if c.Property.IsScript() {
						return elastic.NewMinAggregation().Script(elastic.NewScript(c.Property.Script)), nil
					}
					return elastic.NewMinAggregation().Field(c.Property.Key), nil
				},
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Min(id)
			},
		}
	},
	"avg": func(col *Column) Function {
		return &esValueFunction{
			esFunction: esFunction{
				Column:         col,
				supportOrderBy: true,
				reduceSupport:  true,
				AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
					if c.Property.IsScript() {
						return elastic.NewAvgAggregation().Script(elastic.NewScript(c.Property.Script)), nil
					}
					return elastic.NewAvgAggregation().Field(c.Property.Key), nil
				},
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Avg(id)
			},
		}
	},
	"sum": func(col *Column) Function {
		return &esValueFunction{
			esFunction: esFunction{
				Column:         col,
				supportOrderBy: true,
				reduceSupport:  true,
				AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
					if c.Property.IsScript() {
						return elastic.NewSumAggregation().Script(elastic.NewScript(c.Property.Script)), nil
					}
					return elastic.NewSumAggregation().Field(c.Property.Key), nil
				},
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Sum(id)
			},
		}
	},
	"count": func(col *Column) Function {
		return &esValueFunction{
			esFunction: esFunction{
				Column:         col,
				supportOrderBy: true,
				reduceSupport:  true,
				AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
					if c.Property.IsScript() {
						return elastic.NewValueCountAggregation().Script(elastic.NewScript(c.Property.Script)), nil
					}
					return elastic.NewValueCountAggregation().Field(c.Property.Key), nil
				},
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.ValueCount(id)
			},
		}
	},
	"cardinality": cardinalityFunction,
	"distinct":    cardinalityFunction,
	"countPercent": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: true,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewValueCountAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewValueCountAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				agg, ok := aggs.ValueCount(id)
				if !ok {
					return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
				}
				if agg.Value == nil {
					return float64(0), nil
				}
				var total int64
				if ctx.Resp != nil && ctx.Resp.Hits != nil {
					total = ctx.Resp.Hits.TotalHits
				}
				if total == 0 {
					return float64(0), nil
				}
				return *agg.Value * float64(100) / float64(total), nil
			},
		}
	},
	"sumPercent": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: true,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if !Flags(flags).IsOrderBy() && len(ctx.Req.GroupBy) > 0 {
					ctx.Source.Aggregation(col.ID, elastic.NewSumAggregation().Field(c.Property.Key))
				}
				if c.Property.IsScript() {
					return elastic.NewSumAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewSumAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				agg, ok := aggs.ValueCount(id)
				if !ok {
					return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
				}
				if agg.Value == nil {
					return float64(0), nil
				}

				var total float64
				if len(ctx.Req.GroupBy) > 0 {
					totalAgg, ok := ctx.Resp.Aggregations.Sum(id)
					if !ok {
						return nil, fmt.Errorf("fail to get global sum aggregation")
					}
					if totalAgg.Value != nil {
						total = *totalAgg.Value
					}
				}
				if total == 0 {
					return float64(0), nil
				}
				return *agg.Value * float64(100) / float64(total), nil
			},
		}
	},
	"range": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: false,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				agg := elastic.NewRangeAggregation()
				if c.Property.IsScript() {
					agg.Script(elastic.NewScript(c.Property.Script))
				} else {
					agg.Field(c.Property.Key)
				}
				for _, item := range c.Params {
					value, ok := item.(*ValueRange)
					if !ok {
						return nil, fmt.Errorf("invalid range params type")
					}
					agg.AddRange(value.From, value.To)
				}
				return agg, nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				rang, ok := aggs.Range(id)
				if !ok || rang == nil {
					return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
				}
				var list []map[string]interface{}
				for _, bucket := range rang.Buckets {
					var percent float64
					if ctx.Resp != nil && ctx.Resp.Hits != nil && ctx.Resp.Hits.TotalHits != 0 {
						percent = float64(bucket.DocCount) * 100 / float64(ctx.Resp.Hits.TotalHits)
					}
					list = append(list, map[string]interface{}{
						"min":     bucket.From,
						"max":     bucket.To,
						"percent": percent,
						"count":   bucket.DocCount,
					})
				}
				return list, nil
			},
		}
	},
	"last":   lastFunction,
	"value":  lastFunction,
	"values": valuesFunction,
	"cols": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: false,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return nil, fmt.Errorf("function 'cols' not support script")
				}
				return elastic.NewTopHitsAggregation().Size(1).Sort(ctx.Req.TimeKey, false).
					FetchSourceContext(elastic.NewFetchSourceContext(true).Include(strings.Split(c.Property.Key, ",")...)), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				hits, ok := aggs.TopHits(id)
				if !ok {
					return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
				}
				if hits == nil || hits.Hits == nil || len(hits.Hits.Hits) <= 0 || hits.Hits.Hits[0].Source == nil {
					return nil, nil
				}
				var out map[string]interface{}
				err := json.Unmarshal([]byte(*hits.Hits.Hits[0].Source), &out)
				if err != nil {
					return nil, fmt.Errorf("tail to Unmarshal TopHits source: %s", err)
				}
				var cols []interface{}
				for _, key := range strings.Split(col.Property.Key, ",") {
					cols = append(cols, getMapValue(key, out))
				}
				return cols, nil
			},
		}
	},
	"cpm": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: true,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewValueCountAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewValueCountAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				count, _ := aggs.ValueCount(id)
				return countPerInterval(col, ctx, count, 60000000000)
			},
		}
	},
	"sumCpm": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: true,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewSumAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewSumAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				sum, _ := aggs.Sum(id)
				return countPerInterval(col, ctx, sum, 60000000000)
			},
		}
	},
	"cps": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: true,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewValueCountAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewValueCountAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				count, _ := aggs.ValueCount(id)
				return countPerInterval(col, ctx, count, 1000000000)
			},
		}
	},
	"sumCps": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: true,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewSumAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewSumAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				sum, _ := aggs.Sum(id)
				return countPerInterval(col, ctx, sum, 1000000000)
			},
		}
	},
	"diff": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: false,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewMinAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewMinAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				if next, ok := ctx.Attributes["next"]; ok {
					min, _ := aggs.Min(id)
					if min == nil {
						return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
					}
					if min.Value == nil {
						return 0, nil
					}
					if next, ok := next.(elastic.Aggregations); ok {
						if next, ok := next.Min(id); ok && next != nil && next.Value != nil {
							return *next.Value - *min.Value, nil
						}
					}
				}
				return 0, nil
			},
		}
	},
	"diffps": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: false,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewMinAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewMinAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				if next, ok := ctx.Attributes["next"]; ok {
					min, _ := aggs.Min(id)
					if min == nil {
						return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
					}
					if min.Value == nil {
						return 0, nil
					}
					if next, ok := next.(elastic.Aggregations); ok {
						if next, ok := next.Min(id); ok && next != nil && next.Value != nil {
							return (*next.Value - *min.Value) / (ctx.Req.Interval / 1000000000), nil
						}
					}
				}
				return 0, nil
			},
		}
	},
	"uprate": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: false,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewMinAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewMinAggregation().Field(c.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				if next, ok := ctx.Attributes["next"]; ok {
					min, _ := aggs.Min(id)
					if min == nil {
						return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
					}
					if min.Value == nil {
						return 0, nil
					}
					if next, ok := next.(elastic.Aggregations); ok {
						if next, ok := next.Min(id); ok && next != nil && next.Value != nil {
							if *next.Value < *min.Value {
								return *next.Value / (ctx.Req.Interval / 1000000000), nil
							}
							return (*next.Value - *min.Value) / (ctx.Req.Interval / 1000000000), nil
						}
					}
				}
				return 0, nil
			},
		}
	},
	"apdex": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: false,
			reduceSupport:  false,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				agg := elastic.NewRangeAggregation()
				if c.Property.IsScript() {
					agg.Script(elastic.NewScript(c.Property.Script))
				} else {
					agg.Field(c.Property.Key)
				}
				if len(c.Params) != 3 {
					return nil, fmt.Errorf("invalid apdex params type")
				}
				for _, item := range c.Params {
					value, ok := item.(*ValueRange)
					if !ok {
						return nil, fmt.Errorf("invalid apdex params type")
					}
					agg.AddRange(value.From, value.To)
				}
				return agg, nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				rang, ok := aggs.Range(id)
				if !ok || rang == nil {
					return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
				}
				var total, satisfied, tolerating int64
				for i, bucket := range rang.Buckets {
					total += bucket.DocCount
					if i == 0 {
						satisfied += bucket.DocCount
					} else if i == 1 {
						tolerating += bucket.DocCount
					}
				}
				return (float64(satisfied) + float64(tolerating)/2) / float64(total), nil
			},
		}
	},
	"maxFieldTimestamp": func(col *Column) Function {
		// deprecated
		return &esFunction{
			Column:         col,
			supportOrderBy: false,
			reduceSupport:  true,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return nil, fmt.Errorf("function '%s' not support script", c.Function)
				}
				return elastic.NewTopHitsAggregation().Size(1).Sort(col.Property.Key, false).
					FetchSourceContext(elastic.NewFetchSourceContext(true).Include(ctx.Req.TimeKey)), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				hits, ok := aggs.TopHits(id)
				if !ok {
					return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
				}
				if hits == nil || hits.Hits == nil || len(hits.Hits.Hits) <= 0 || hits.Hits.Hits[0].Source == nil {
					return nil, nil
				}
				var out map[string]interface{}
				err := json.Unmarshal([]byte(*hits.Hits.Hits[0].Source), &out)
				if err != nil {
					return nil, fmt.Errorf("tail to Unmarshal TopHits source: %s", err)
				}
				val, ok := utils.ConvertInt64(getMapValue(ctx.Req.TimeKey, out))
				if ok {
					return val / 1000000, nil
				}
				return val, nil
			},
		}
	},
	"latestTimestamp": func(col *Column) Function {
		// deprecated, max
		return &esValueFunction{
			esFunction: esFunction{
				Column:         col,
				supportOrderBy: true,
				reduceSupport:  true,
				AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
					return elastic.NewMaxAggregation().Field(ctx.Req.TimeKey), nil
				},
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Max(id)
			},
		}
	},
	"oldestTimestamp": func(col *Column) Function {
		// deprecated, min
		return &esValueFunction{
			esFunction: esFunction{
				Column:         col,
				supportOrderBy: true,
				reduceSupport:  true,
				AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
					return elastic.NewMinAggregation().Field(ctx.Req.TimeKey), nil
				},
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
				return aggs.Min(id)
			},
		}
	},
	"pencentiles": func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: false,
			reduceSupport:  true,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if len(c.Params) <= 0 {
					return nil, fmt.Errorf("pencentiles params must not empty")
				}
				value, ok := utils.ConvertFloat64(c.Params[0])
				if !ok || value > float64(100) {
					return nil, fmt.Errorf("pencentiles params must not empty")
				}
				return elastic.NewPercentilesAggregation().Percentiles(value).Field(col.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				percents, ok := aggs.Percentiles(id)
				if !ok || percents == nil {
					return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
				}
				for _, v := range percents.Values {
					return v, nil
				}
				return nil, nil
			},
		}
	},
	"p99": pencentilesFunction(99),
	"p95": pencentilesFunction(95),
	"p90": pencentilesFunction(90),
	"p85": pencentilesFunction(85),
	"p80": pencentilesFunction(80),
	"p75": pencentilesFunction(75),
	"p70": pencentilesFunction(70),
	"p65": pencentilesFunction(65),
	"p60": pencentilesFunction(60),
	"p55": pencentilesFunction(55),
	"p50": pencentilesFunction(50),
	"p45": pencentilesFunction(45),
	"p40": pencentilesFunction(40),
	"p35": pencentilesFunction(35),
	"p30": pencentilesFunction(30),
	"p25": pencentilesFunction(25),
	"p20": pencentilesFunction(20),
	"p15": pencentilesFunction(15),
	"p10": pencentilesFunction(10),
	"p5":  pencentilesFunction(5),
	"group_reduce": func(col *Column) Function {
		return &groupReduceFunction{
			Column: col,
		}
	},
}

type esFunction struct {
	*Column
	reduceSupport  bool
	supportOrderBy bool
	AggGatter      func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error)
	ValueGetter    func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error)
}

func (f *esFunction) Aggregations(ctx *Context, flags ...Flag) ([]*Aggregation, error) {
	aggs, err := f.AggGatter(f.Column, ctx, flags...)
	if err != nil {
		return nil, err
	}
	return []*Aggregation{
		{
			ID:          f.ID,
			Aggregation: aggs,
		},
	}, nil
}

func (f *esFunction) Handle(ctx *Context, aggs elastic.Aggregations) (interface{}, error) {
	return f.ValueGetter(ctx, f.ID, aggs)
}

func (f *esFunction) SupportOrderBy() bool {
	return f.supportOrderBy
}

func (f *esFunction) SupportReduce() bool {
	return f.reduceSupport
}

type esValueFunction struct {
	esFunction
	ValueGetter func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool)
}

func (f *esValueFunction) Handle(ctx *Context, aggs elastic.Aggregations) (interface{}, error) {
	agg, ok := f.ValueGetter(ctx, f.ID, aggs)
	if !ok {
		return nil, fmt.Errorf("invalid %s aggregation type", f.FuncName)
	}
	if agg.Value == nil {
		return float64(0), nil
	}
	return *agg.Value, nil
}

func (f *esValueFunction) SupportOrderBy() bool {
	return f.supportOrderBy
}

func countPerInterval(col *Column, ctx *Context, agg *elastic.AggregationValueMetric, interval float64) (interface{}, error) {
	if agg == nil {
		return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
	}
	interval = (ctx.Req.Interval / interval)
	if interval == 0 {
		return 0, nil
	}
	var value float64
	if agg.Value != nil {
		value = *agg.Value
	}
	return value / interval, nil
}

func pencentilesFunction(value int) func(col *Column) Function {
	return func(col *Column) Function {
		return &esFunction{
			Column:         col,
			supportOrderBy: true,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewPercentilesAggregation().Percentiles(float64(value)).Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewPercentilesAggregation().Percentiles(float64(value)).Field(col.Property.Key), nil
			},
			ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
				percents, ok := aggs.Percentiles(id)
				if !ok || percents == nil {
					return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
				}
				for _, v := range percents.Values {
					return v, nil
				}
				return nil, nil
			},
		}
	}
}

func lastFunction(col *Column) Function {
	return &esFunction{
		Column:         col,
		supportOrderBy: false,
		reduceSupport:  true,
		AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
			if c.Property.IsScript() {
				return nil, fmt.Errorf("function 'last' not support script")
			}
			return elastic.NewTopHitsAggregation().Size(1).Sort(ctx.Req.TimeKey, false).
				FetchSourceContext(elastic.NewFetchSourceContext(true).Include(c.Property.Key)), nil
		},
		ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
			hits, ok := aggs.TopHits(id)
			if !ok {
				return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
			}
			if hits == nil || hits.Hits == nil || len(hits.Hits.Hits) <= 0 || hits.Hits.Hits[0].Source == nil {
				return nil, nil
			}
			var out map[string]interface{}
			err := json.Unmarshal([]byte(*hits.Hits.Hits[0].Source), &out)
			if err != nil {
				return nil, fmt.Errorf("tail to Unmarshal TopHits source: %s", err)
			}
			return getMapValue(col.Property.Key, out), nil
		},
	}
}

func cardinalityFunction(col *Column) Function {
	return &esValueFunction{
		esFunction: esFunction{
			Column:         col,
			supportOrderBy: true,
			reduceSupport:  true,
			AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
				if c.Property.IsScript() {
					return elastic.NewCardinalityAggregation().Script(elastic.NewScript(c.Property.Script)), nil
				}
				return elastic.NewCardinalityAggregation().Field(c.Property.Key), nil
			},
		},
		ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (*elastic.AggregationValueMetric, bool) {
			return aggs.Cardinality(id)
		},
	}
}

// Example: SELECT fieldA From xxx LIMIT 10
func valuesFunction(col *Column) Function {
	return &esFunction{
		Column:         col,
		supportOrderBy: true,
		reduceSupport:  true,
		AggGatter: func(c *Column, ctx *Context, flags ...Flag) (elastic.Aggregation, error) {
			var err error
			key, size := c.Property.Key, 1
			idx := strings.Index(c.Property.Key, ":")
			if idx > 0 {
				key = c.Property.Key[:idx]
				size, err = strconv.Atoi(c.Property.Key[idx+1:])
				if err != nil {
					return nil, err
				}
			}
			c.Property.Key = key
			return elastic.NewTopHitsAggregation().Size(size).Sort(ctx.Req.TimeKey, false).
				FetchSourceContext(elastic.NewFetchSourceContext(true).Include(c.Property.Key)), nil
		},
		ValueGetter: func(ctx *Context, id string, aggs elastic.Aggregations) (interface{}, error) {
			hits, ok := aggs.TopHits(id)
			if !ok {
				return nil, fmt.Errorf("invalid %s aggregation type", col.FuncName)
			}
			if hits == nil || hits.Hits == nil || len(hits.Hits.Hits) <= 0 {
				return nil, nil
			}

			res := []interface{}{}
			for _, item := range hits.Hits.Hits {
				var data map[string]interface{}
				err := json.Unmarshal(*item.Source, &data)
				if err != nil {
					return nil, fmt.Errorf("tail to Unmarshal TopHits source: %s", err)
				}
				res = append(res, getMapValue(col.Property.Key, data))
			}
			return res, nil
		},
	}
}

type groupReduceFunction struct {
	*Column
	supportOrderBy bool
	interCol       *Column
	innerFn        Function
	reduce         string
}

func (f *groupReduceFunction) Aggregations(ctx *Context, flags ...Flag) ([]*Aggregation, error) {
	exp := f.Property.GetExpression()
	params, err := url.ParseQuery(exp)
	if err != nil {
		return nil, fmt.Errorf("invalid group_reduce expression: %s", err)
	}
	group := params.Get("group")
	reduce := params.Get("reduce")
	if len(reduce) <= 0 || len(group) <= 0 {
		return nil, fmt.Errorf("group_reduce reduce or group missing")
	}
	limit := params.Get("limit")
	var size = 5000
	if len(limit) > 0 {
		s, err := strconv.Atoi(limit)
		if err != nil {
			return nil, fmt.Errorf("invalid group_reduce limit format: %s", err)
		}
		size = s
	}
	terms := elastic.NewTermsAggregation().Size(size).Field(group)
	for key, vals := range params {
		if creator, ok := Functions[key]; ok && len(vals) > 0 {
			col := &Column{
				Property: Property{Name: vals[0]},
				FuncName: key,
			}
			col.Property.Normalize(query.FieldKey)
			col.ID = NormalizeID(col.FuncName, &col.Property)
			fn := creator(col)
			if !fn.SupportReduce() {
				return nil, fmt.Errorf("function '%s' not support group reduce", col.FuncName)
			}
			aggs, err := fn.Aggregations(ctx, FlagReduce)
			if err != nil {
				return nil, fmt.Errorf("function '%s' %s", col.FuncName, err)
			}
			if len(aggs) != 1 {
				return nil, fmt.Errorf("function '%s' not support group reduce", col.FuncName)
			}
			f.reduce = reduce
			f.innerFn = fn
			f.interCol = col
			return []*Aggregation{
				{
					ID:          f.ID,
					Aggregation: terms.SubAggregation(aggs[0].ID, aggs[0].Aggregation),
				},
			}, nil
		}
	}
	return nil, fmt.Errorf("group_reduce not found one agg")
}

func (f *groupReduceFunction) Handle(ctx *Context, aggs elastic.Aggregations) (interface{}, error) {
	terms, ok := aggs.Terms(f.ID)
	if !ok || terms == nil {
		return nil, fmt.Errorf("invalid %s aggregation type", f.FuncName)
	}
	var list []float64
	for _, item := range terms.Buckets {
		val, err := f.innerFn.Handle(ctx, item.Aggregations)
		if err != nil {
			return nil, fmt.Errorf("fail to get group_reduce value: %s", err)
		}
		list = append(list, conv.ToFloat64(val, 0))
	}
	switch f.reduce {
	case "max":
		var max float64
		for _, v := range list {
			if v > max {
				max = v
			}
		}
		return max, nil
	case "min":
		var min float64
		for _, v := range list {
			if v < min {
				min = v
			}
		}
		return min, nil
	case "sum":
		var sum float64
		for _, v := range list {
			sum += v
		}
		return sum, nil
	case "avg":
		var sum float64
		for _, v := range list {
			sum += v
		}
		if len(list) == 0 {
			return 0, nil
		}
		return sum / float64(len(list)), nil
	}
	return nil, fmt.Errorf("invalid reduce %s", f.reduce)
}

func (f *groupReduceFunction) SupportOrderBy() bool {
	return false
}

func (f *groupReduceFunction) SupportReduce() bool {
	return false
}
