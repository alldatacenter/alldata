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
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/influxdata/influxql"
	"github.com/olivere/elastic"

	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
)

// Parser .
type Parser struct {
	ql     *influxql.Parser
	filter *elastic.BoolQuery
	ctx    *Context
}

// New start and end always nanosecond
func New(start, end int64, stmt string) tsql.Parser {
	return &Parser{
		ql: influxql.NewParser(strings.NewReader(stmt)),
		ctx: &Context{
			start:            start,
			end:              end,
			originalTimeUnit: tsql.Nanosecond,
			targetTimeUnit:   tsql.UnsetTimeUnit,
			timeKey:          tsql.TimestampKey,
			maxTimePoints:    512,
		},
	}
}

func init() {
	tsql.RegisterParser("influxql", New)
}

// SetFilter .
func (p *Parser) SetFilter(filter *elastic.BoolQuery) tsql.Parser {
	p.filter = filter
	return p
}

// SetParams .
func (p *Parser) SetParams(params map[string]interface{}) tsql.Parser {
	if len(params) > 0 {
		p.ql.SetParams(params)
	}
	return p
}

// SetOriginalTimeUnit .
func (p *Parser) SetOriginalTimeUnit(unit tsql.TimeUnit) tsql.Parser {
	p.ctx.originalTimeUnit = unit
	return p
}

// SetTargetTimeUnit .
func (p *Parser) SetTargetTimeUnit(unit tsql.TimeUnit) tsql.Parser {
	p.ctx.targetTimeUnit = unit
	return p
}

// SetTimeKey .
func (p *Parser) SetTimeKey(key string) tsql.Parser {
	p.ctx.timeKey = key
	return p
}

// SetMaxTimePoints .
func (p *Parser) SetMaxTimePoints(points int64) tsql.Parser {
	p.ctx.maxTimePoints = points
	return p
}

// ParseQuery .
func (p *Parser) ParseQuery() ([]tsql.Query, error) {
	q, err := p.ql.ParseQuery()
	if err != nil {
		return nil, err
	}
	var qs []tsql.Query
	for _, stmt := range q.Statements {
		s, ok := stmt.(*influxql.SelectStatement)
		if !ok {
			return nil, tsql.ErrNotSupportNonQueryStatement
		}
		q, err := p.parseSelectStatement(s)
		if err != nil {
			return nil, err
		}
		qs = append(qs, q)
	}
	return qs, nil
}

func (p *Parser) parseSelectStatement(s *influxql.SelectStatement) (*Query, error) {
	// from
	sources, err := p.parseQuerySources(s.Sources)
	if err != nil {
		return nil, err
	}

	// where
	searchSource := elastic.NewSearchSource()
	start, end := p.ctx.Range(true)
	query := elastic.NewBoolQuery().
		Filter(elastic.NewRangeQuery(p.ctx.TimeKey()).Gte(start).Lte(end))
	if p.filter != nil {
		query = query.Filter(p.filter)
	}
	if s.Condition != nil {
		bq := elastic.NewBoolQuery()
		err = p.parseQueryCondition(s.Condition, bq)
		if err != nil {
			return nil, err
		}
		query.Filter(bq)
	}
	searchSource.Query(query)

	// setup something
	addAgg := func(name string, agg elastic.Aggregation) {
		searchSource.Aggregation(name, agg)
	}
	columns := make(map[string]bool)
	if s.Limit <= 0 {
		s.Limit = tsql.DefaultLimtSize
	}
	if s.Offset <= 0 {
		s.Offset = 0
	}
	var flag queryFlag

	// select
	aggs, handlers, allCols, err := p.parseQueryFields(s.Fields, columns)
	if err != nil {
		return nil, err
	}

	// Support the ORDER BY case
	// 1、Query raw data: support fields, support expressions.
	// 2、Aggregated, ungrouped: No sorting is required because there will only be one piece of data.
	// 3、Group fields, but not by time, not by range: sort the groups by the results of the aggregate function, or by the number of documents per group.
	// 4、Grouping fields by time: sorting the data within the group, grouping by the result of the aggregate function, or the number of documents.
	// 5、Field grouping, either by value domain: sorting the data within the group, grouping by the result of the aggregate function, or the number of documents.
	// 6、Grouping by time only: temporarily not supported.
	// 7、Grouping by range only: temporarily not supported.
	// 4 and 5 might consider supporting aggregation for each interval or range of values within a group, but for now 3, 4, and 5 are handled uniformly.

	globalAgg := aggs
	// group by and order by
	if len(s.Dimensions) > 0 {
		flag |= queryFlagDimensions
		name, dim, addAggFn, f, err := p.parseQueryDimensions(s.Dimensions, s.SortFields, s.Offset, s.Limit, columns, aggs)
		if err != nil {
			return nil, err
		}
		addAgg(name, dim)
		addAgg = addAggFn
		if f&tsql.ColumnFlagGroupByInterval == tsql.ColumnFlagGroupByInterval {
			flag |= queryFlagGroupByTime
		} else if f&tsql.ColumnFlagGroupByRange == tsql.ColumnFlagGroupByRange {
			flag |= queryFlagGroupByRange
		}
		globalAgg = make(map[string]elastic.Aggregation)
	}
	if len(aggs) > 0 {
		flag |= queryFlagAggs
	}
	if len(columns) > 0 {
		flag |= queryFlagColumns
	}
	if allCols {
		flag |= queryFlagAllColumns
	}

	if p.ctx.scopes != nil && p.ctx.scopes["global"] != nil {
		for id, item := range p.ctx.scopes["global"] {
			setupScopeAggg(p.ctx, id, item, globalAgg, "global")
		}
	}
	if len(s.Dimensions) > 0 {
		for id, agg := range globalAgg {
			searchSource.Aggregation(id, agg)
		}
	}

	// add select agg
	for id, agg := range aggs {
		addAgg(id, agg)
	}

	// Automatically add special columns.
	if flag&queryFlagGroupByTime == queryFlagGroupByTime {
		handlers = append([]*columnHandler{
			{
				col: &tsql.Column{
					Name: "time",
					Flag: tsql.ColumnFlagGroupBy | tsql.ColumnFlagGroupByInterval,
				},
				ctx: p.ctx,
			},
		}, handlers...)
	} else if flag&queryFlagGroupByRange == queryFlagGroupByRange {
		handlers = append([]*columnHandler{
			{
				col: &tsql.Column{
					Name: "range",
					Flag: tsql.ColumnFlagGroupBy | tsql.ColumnFlagGroupByRange,
				},
				ctx: p.ctx,
			},
		}, handlers...)
	}

	// columns
	if flag == queryFlagNone {
		searchSource = nil // not need request to es
	} else if flag&(queryFlagDimensions|queryFlagAggs) != queryFlagNone {
		searchSource.Size(0)
		if flag&queryFlagAllColumns != queryFlagNone {

			addAgg("columns", elastic.NewTopHitsAggregation().Sort(p.ctx.TimeKey(), false).Size(1))
		} else if flag&queryFlagColumns != queryFlagNone {
			var cols []string
			for c := range columns {
				cols = append(cols, c)
			}
			addAgg("columns", elastic.NewTopHitsAggregation().Sort(p.ctx.TimeKey(), false).Size(1).
				FetchSourceContext(elastic.NewFetchSourceContext(true).Include(cols...)))
		}
	} else {
		// order by for raw data
		searchSource.From(s.Offset).Size(s.Limit)
		if len(s.SortFields) <= 0 {
			searchSource.Sort(p.ctx.TimeKey(), false)
		} else {
			for _, f := range s.SortFields {
				if f.Expr == nil { // len(f.Name) == 0
					searchSource.Sort(p.ctx.TimeKey(), f.Ascending)
				} else {
					// expr, err := influxql.ParseExpr(f.Name)
					// if err != nil {
					// 	return nil, err
					// }
					expr := f.Expr
					vf, ok := expr.(*influxql.VarRef)
					if ok {
						key := getKeyName(vf, influxql.AnyField)
						searchSource.Sort(key, f.Ascending)
					} else {
						// Don't check expr first, just hand it to elasticsearch.
						s, err := getScriptExpression(p.ctx, expr, influxql.AnyField, nil)
						if err != nil {
							return nil, err
						}
						searchSource.SortBy(elastic.NewScriptSort(elastic.NewScript(s), "").Order(f.Ascending))
					}
				}
			}
		}
	}

	return &Query{
		sources:      sources,
		searchSource: searchSource,
		boolQuery:    query,
		stmt:         s,
		columns:      handlers,
		flag:         flag,
		aggs:         aggs,
		ctx:          p.ctx,
	}, nil
}

func (p *Parser) parseQuerySources(sources influxql.Sources) (list []*tsql.Source, err error) {
	for _, source := range sources {
		switch s := source.(type) {
		case *influxql.Measurement:
			if s.Regex != nil {
				return nil, fmt.Errorf("only support regex source")
			}
			db := s.Database
			if len(db) <= 0 {
				db = s.RetentionPolicy
			}
			list = append(list, &tsql.Source{
				Database: db,
				Name:     s.Name,
			})
		case *influxql.SubQuery:
			return nil, fmt.Errorf("not support sub query yet")
		default:
			return nil, fmt.Errorf("invalid source: %s", source.String())
		}
	}
	if len(list) <= 0 {
		return nil, fmt.Errorf("sources not found")
	}
	return list, nil
}

func getKeyName(ref *influxql.VarRef, deftyp influxql.DataType) string {
	name, _ := getKeyNameAndFlag(ref, deftyp)
	return name
}

const nameKey = "_" + tsql.NameKey

func getKeyNameAndFlag(ref *influxql.VarRef, deftyp influxql.DataType) (string, tsql.ColumnFlag) {
	if ref.Type == influxql.Unknown {
		if ref.Val == tsql.TimestampKey || ref.Val == tsql.TimeKey {
			return tsql.TimestampKey, tsql.ColumnFlagTimestamp
		} else if ref.Val == tsql.NameKey || ref.Val == nameKey {
			return tsql.NameKey, tsql.ColumnFlagName
		}
		if deftyp == influxql.Tag {
			return tsql.TagsKey + ref.Val, tsql.ColumnFlagTag
		}
	} else if ref.Type == influxql.Tag {
		return tsql.TagsKey + ref.Val, tsql.ColumnFlagTag
	}
	return tsql.FieldsKey + ref.Val, tsql.ColumnFlagField
}

func (p *Parser) parseQueryCondition(cond influxql.Expr, query *elastic.BoolQuery) (err error) {
	switch expr := cond.(type) {
	case *influxql.BinaryExpr:
		if expr.Op == influxql.AND || expr.Op == influxql.OR {
			left := elastic.NewBoolQuery()
			err = p.parseQueryCondition(expr.LHS, left)
			if err != nil {
				return err
			}
			right := elastic.NewBoolQuery()
			err = p.parseQueryCondition(expr.RHS, right)
			if err != nil {
				return err
			}
			if expr.Op == influxql.AND {
				query.Filter(left)
				query.Filter(right)
			} else {
				query.Should(left)
				query.Should(right)
			}
			return nil
		} else if influxql.EQ <= expr.Op && expr.Op <= influxql.GTE {
			lkey, lok := expr.LHS.(*influxql.VarRef)
			rkey, rok := expr.RHS.(*influxql.VarRef)
			if lok && !rok {
				ok, err := p.parseKeyCondition(lkey, expr.Op, expr.RHS, query)
				if err != nil {
					return err
				}
				if ok {
					return nil
				}
			} else if !lok && rok {
				ok, err := p.parseKeyCondition(rkey, reverseOperator(expr.Op), expr.LHS, query)
				if err != nil {
					return err
				}
				if ok {
					return nil
				}
			}
		}
	case *influxql.ParenExpr:
		return p.parseQueryCondition(expr.Expr, query)
	}
	err = p.parseScriptCondition(cond, query)
	if err != nil {
		return err
	}
	return nil
}

func (p *Parser) parseKeyCondition(ref *influxql.VarRef, op influxql.Token, val influxql.Expr, query *elastic.BoolQuery) (bool, error) {
	value, ok, err := getLiteralValue(p.ctx, val)
	if err != nil {
		return false, err
	}
	if !ok {
		return false, nil
	}
	key := getKeyName(ref, influxql.Tag)
	switch op {
	case influxql.EQ:
		query.Filter(elastic.NewTermQuery(key, value))
	case influxql.NEQ:
		query.MustNot(elastic.NewTermQuery(key, value))
	case influxql.EQREGEX, influxql.NEQREGEX:
		r, ok := value.(*regexp.Regexp)
		if !ok || r == nil {
			return false, fmt.Errorf("invalid regexp '%v'", value)
		}
		reg := strings.Replace(r.String(), `/`, `\/`, -1)
		if op == influxql.EQREGEX {
			query.Filter(elastic.NewRegexpQuery(key, reg))
		} else {
			query.MustNot(elastic.NewRegexpQuery(key, reg))
		}
	case influxql.LT:
		query.Filter(elastic.NewRangeQuery(key).Lt(value))
	case influxql.LTE:
		query.Filter(elastic.NewRangeQuery(key).Lte(value))
	case influxql.GT:
		query.Filter(elastic.NewRangeQuery(key).Gt(value))
	case influxql.GTE:
		query.Filter(elastic.NewRangeQuery(key).Gte(value))
	default:
		return false, fmt.Errorf("not support operater '%s'", op.String())
	}
	return true, nil
}

func (p *Parser) parseScriptCondition(cond influxql.Expr, query *elastic.BoolQuery) error {
	fields := make(map[string]bool)
	s, err := getScriptExpression(p.ctx, cond, influxql.Tag, fields)
	if err != nil {
		return err
	}
	if len(s) > 0 {
		for f := range fields {
			query.Filter(elastic.NewExistsQuery(f))
		}
		query.Filter(elastic.NewScriptQuery(elastic.NewScript(s)))
	}
	return nil
}

func (p *Parser) parseQueryFields(fields influxql.Fields, columns map[string]bool) (map[string]elastic.Aggregation, []*columnHandler, bool, error) {
	aggs := make(map[string]elastic.Aggregation)
	var handlers []*columnHandler
	var allColumns bool
	for _, field := range fields {
		h, err := p.parseField(field, aggs, columns)
		if err != nil {
			return nil, nil, false, err
		}
		if h.AllColumns() {
			allColumns = true
		}
		handlers = append(handlers, h)
	}
	return aggs, handlers, allColumns, nil
}

func (p *Parser) parseField(field *influxql.Field, aggs map[string]elastic.Aggregation, cols map[string]bool) (*columnHandler, error) {
	ch := &columnHandler{
		field: field,
		ctx:   p.ctx,
	}
	if ch.AllColumns() {
		return ch, nil
	}
	ch.fns = make(map[string]AggHandler)
	err := p.parseFiledAgg(field.Expr, aggs, ch.fns)
	if err != nil {
		return nil, err
	}
	err = p.parseFiledRef(field.Expr, cols)
	if err != nil {
		return nil, err
	}
	return ch, nil
}

func (p *Parser) parseFiledAgg(expr influxql.Expr, aggs map[string]elastic.Aggregation, fns map[string]AggHandler) error {
	switch expr := expr.(type) {
	case *influxql.Call:
		if expr.Name == "scope" {
			return p.parseScopeAgg(expr)
		}
		if define, ok := AggFunctions[expr.Name]; ok {
			if define.Flag&FuncFlagSelect == 0 {
				return fmt.Errorf("not support function '%s' in select", expr.Name)
			}
			id := p.ctx.GetFuncID(expr, influxql.AnyField)
			if _, ok := fns[id]; !ok {
				fn, err := define.New(p.ctx, id, expr)
				if err != nil {
					return err
				}
				err = fn.Aggregations(aggs, FuncFlagSelect)
				if err != nil {
					return err
				}
				fns[id] = fn
			}
		} else if _, ok := tsql.BuildInFunctions[expr.Name]; ok {
			for _, arg := range expr.Args {
				if err := p.parseFiledAgg(arg, aggs, fns); err != nil {
					return err
				}
			}
		}
	case *influxql.BinaryExpr:
		if err := p.parseFiledAgg(expr.LHS, aggs, fns); err != nil {
			return err
		}
		if err := p.parseFiledAgg(expr.RHS, aggs, fns); err != nil {
			return err
		}
	case *influxql.ParenExpr:
		return p.parseFiledAgg(expr.Expr, aggs, fns)
	}
	return nil
}

func (p *Parser) parseFiledRef(expr influxql.Expr, cols map[string]bool) error {
	switch expr := expr.(type) {
	case *influxql.Call:
		if expr.Name == "scope" {
			return nil
		}
		_, ok := AggFunctions[expr.Name]
		if !ok {
			for _, arg := range expr.Args {
				if err := p.parseFiledRef(arg, cols); err != nil {
					return err
				}
			}
		}
	case *influxql.BinaryExpr:
		if err := p.parseFiledRef(expr.LHS, cols); err != nil {
			return err
		}
		if err := p.parseFiledRef(expr.RHS, cols); err != nil {
			return err
		}
	case *influxql.ParenExpr:
		return p.parseFiledRef(expr.Expr, cols)
	case *influxql.VarRef:
		cols[getKeyName(expr, influxql.AnyField)] = true
	}
	return nil
}

func (p *Parser) parseScopeAgg(call *influxql.Call) error {
	if len(call.Args) <= 0 || len(call.Args) > 2 {
		return fmt.Errorf("invalid scope args")
	}
	inner, ok := call.Args[0].(*influxql.Call)
	if !ok {
		return fmt.Errorf("invalid scope args")
	}
	scope := "terms"
	if len(call.Args) == 2 {
		sl, ok := call.Args[1].(*influxql.StringLiteral)
		if !ok {
			return fmt.Errorf("invalid scope args")
		}
		if len(sl.Val) > 0 {
			scope = sl.Val
		}
	}
	if p.ctx.scopes == nil {
		p.ctx.scopes = make(map[string]map[string]*scopeField)
	}
	funcs := p.ctx.scopes[scope]
	if funcs == nil {
		funcs = make(map[string]*scopeField)
		p.ctx.scopes[scope] = funcs
	}
	id := p.ctx.GetFuncID(inner, influxql.AnyField)
	if funcs[id] == nil {
		funcs[id] = &scopeField{
			call: inner,
		}
	}
	return nil
}

func (p *Parser) parseQueryDimensions(dimensions influxql.Dimensions,
	sorts influxql.SortFields, offset, limit int,
	columns map[string]bool, aggs map[string]elastic.Aggregation,
) (string, elastic.Aggregation, func(name string, agg elastic.Aggregation), tsql.ColumnFlag, error) {
	var histogram *elastic.HistogramAggregation
	var rng *elastic.RangeAggregation
	var scripts []string
	for _, dim := range dimensions {
		switch expr := dim.Expr.(type) {
		case *influxql.Call:
			if expr.Name == "time" {
				if histogram != nil {
					return "", nil, nil, tsql.ColumnFlagNone, fmt.Errorf("not support multi 'time' function in group by")
				}
				if rng != nil {
					return "", nil, nil, tsql.ColumnFlagNone, fmt.Errorf("'time' and 'range' function conflict in group by")
				}
				var interval int64
				if len(expr.Args) == 1 {
					arg := expr.Args[0]
					d, ok := arg.(*influxql.DurationLiteral)
					if !ok || d.Val < time.Second {
						return "", nil, nil, tsql.ColumnFlagNone, fmt.Errorf("invalid arg '%s' in function '%s'", arg.String(), expr.Name)
					}
					interval = int64(d.Val)
				}
				start, end := p.ctx.Range(true)
				interval = adjustInterval(start, end, interval, p.ctx.maxTimePoints)
				p.ctx.interval = interval
				if p.ctx.OriginalTimeUnit() != tsql.UnsetTimeUnit {
					interval /= int64(p.ctx.OriginalTimeUnit())
				}
				if p.ctx.TargetTimeUnit() != tsql.UnsetTimeUnit {
					p.ctx.interval /= int64(p.ctx.TargetTimeUnit())
				}
				histogram = elastic.NewHistogramAggregation().Field(p.ctx.TimeKey()).
					Interval(float64(interval)).MinDocCount(0).
					Offset(float64(start)).ExtendedBounds(float64(start), float64(end))
				continue
			} else if expr.Name == "range" {
				if rng != nil {
					return "", nil, nil, tsql.ColumnFlagNone, fmt.Errorf("not support multi 'range' function in group by")
				}
				if histogram != nil {
					return "", nil, nil, tsql.ColumnFlagNone, fmt.Errorf("'time' and 'range' function conflict in group by")
				}
				err := mustCallArgsMinNum(expr, 2)
				if err != nil {
					return "", nil, nil, tsql.ColumnFlagNone, err
				}
				arg0, ok := expr.Args[0].(*influxql.VarRef)
				if !ok {
					return "", nil, nil, tsql.ColumnFlagNone, fmt.Errorf("args[0] is not reference in 'range' function")
				}

				key := getKeyName(arg0, influxql.AnyField)
				rng = elastic.NewRangeAggregation().Field(key)
				var from interface{}
				for i, item := range expr.Args[1:] {
					val, ok, err := getLiteralValue(p.ctx, item)
					if err != nil {
						return "", nil, nil, tsql.ColumnFlagNone, err
					}
					if !ok {
						return "", nil, nil, tsql.ColumnFlagNone, fmt.Errorf("args[%d] is literal in 'range' function", i)
					}
					if i%2 == 0 {
						from = val
						continue
					}
					rng = rng.AddRange(from, val)
					from = nil
				}
				if from != nil {
					rng = rng.AddRange(from, nil)
					from = nil
				}
				continue
			}
		}
		script, err := getScriptExpression(p.ctx, dim.Expr, influxql.Tag, nil)
		if err != nil {
			return "", nil, nil, tsql.ColumnFlagNone, err
		}
		scripts = append(scripts, script)
		//  Mark that the expression is used for grouping
		if p.ctx.dimensions == nil {
			p.ctx.dimensions = make(map[string]bool)
		}
		key, _ := getExprStringAndFlag(dim.Expr, influxql.Tag)
		p.ctx.dimensions[key] = true
	}
	var terms *elastic.TermsAggregation
	if len(scripts) > 0 {
		script := strings.Join(scripts, " + '/' + ")
		terms = elastic.NewTermsAggregation().
			Script(elastic.NewScript(script)).Size(offset + limit) // .Size(5000)
		if histogram != nil || rng != nil {
			aggs = make(map[string]elastic.Aggregation) // If you have histogram or range, you don't share the aggregate with select.
		}
		if p.ctx.scopes != nil && p.ctx.scopes["terms"] != nil {
			for id, item := range p.ctx.scopes["terms"] {
				setupScopeAggg(p.ctx, id, item, aggs, "terms")
			}
		}
		for _, sort := range sorts {
			err := setupTermsOrderAgg(p.ctx, sort.Expr, aggs, terms, sort)
			if err != nil {
				return "", nil, nil, tsql.ColumnFlagNone, err
			}
		}
		if histogram != nil || rng != nil {
			for id, agg := range aggs {
				terms.SubAggregation(id, agg)
			}
		}
	}

	if terms == nil && (histogram != nil || rng != nil) && len(sorts) > 0 {
		return "", nil, nil, tsql.ColumnFlagNone, fmt.Errorf("not support order by in this case")
	}
	if terms != nil {
		if histogram != nil {
			terms.SubAggregation("histogram", histogram)
			return "term", terms, func(name string, agg elastic.Aggregation) {
				histogram.SubAggregation(name, agg)
			}, tsql.ColumnFlagGroupBy | tsql.ColumnFlagGroupByInterval, nil
		} else if rng != nil {
			terms.SubAggregation("range", rng) //.Size(5000)
			return "term", terms, func(name string, agg elastic.Aggregation) {
				rng.SubAggregation(name, agg)
			}, tsql.ColumnFlagGroupBy | tsql.ColumnFlagGroupByRange, nil
		}
		return "term", terms, func(name string, agg elastic.Aggregation) {
			terms.SubAggregation(name, agg)
		}, tsql.ColumnFlagGroupBy, nil
	} else if histogram != nil {
		return "histogram", histogram, func(name string, agg elastic.Aggregation) {
			histogram.SubAggregation(name, agg)
		}, tsql.ColumnFlagGroupBy | tsql.ColumnFlagGroupByInterval, nil
	} else if rng != nil {
		return "range", rng, func(name string, agg elastic.Aggregation) {
			rng.SubAggregation(name, agg)
		}, tsql.ColumnFlagGroupBy | tsql.ColumnFlagGroupByRange, nil
	}
	return "", nil, nil, tsql.ColumnFlagNone, nil
}

func adjustInterval(start, end, interval, points int64) int64 {
	duration := end - start
	if interval == 0 {
		if duration < 2*int64(time.Hour) {
			return int64(time.Minute)
		}
		d := duration / (2 * int64(time.Hour))
		return d * int64(time.Minute)
	}
	if points <= 0 {
		points = 1000
	}
	if interval < (end-start)/points {
		interval = (end - start) / points
	}
	return interval
}

// setupTermsOrderAgg handles aggregate functions for user grouping and sorting.
func setupTermsOrderAgg(ctx *Context, expr influxql.Expr, aggs map[string]elastic.Aggregation,
	terms *elastic.TermsAggregation, sort *influxql.SortField) error {
	switch expr := expr.(type) {
	case *influxql.Call:
		if define, ok := AggFunctions[expr.Name]; ok {
			if define.Flag&FuncFlagOrderBy == 0 {
				return fmt.Errorf("not support function '%s' in order by", expr.Name)
			}
			id := ctx.GetFuncID(expr, influxql.AnyField)
			if _, ok := aggs[id]; !ok {
				fn, err := define.New(ctx, id, expr)
				if err != nil {
					return err
				}
				err = fn.Aggregations(aggs, FuncFlagOrderBy)
				if err != nil {
					return err
				}
			}
			terms.OrderByAggregation(id, sort.Ascending)
			return nil
		}
	case *influxql.ParenExpr:
		return setupTermsOrderAgg(ctx, expr.Expr, aggs, terms, sort)
	}
	return fmt.Errorf("invalid order by expression")
}

// setupScopeAggg .
func setupScopeAggg(ctx *Context, id string, field *scopeField, aggs map[string]elastic.Aggregation, scope string) error {
	if define, ok := AggFunctions[field.call.Name]; ok {
		if define.Flag&FuncFlagSelect == 0 {
			return fmt.Errorf("not support function '%s' in scope '%s'", field.call, scope)
		}
		if _, ok := aggs[id]; !ok {
			fn, err := define.New(ctx, id, field.call)
			if err != nil {
				return err
			}
			field.handler = fn
			err = fn.Aggregations(aggs, FuncFlagSelect)
			if err != nil {
				return err
			}
		}
		return nil
	}
	return fmt.Errorf("invalid expression for scope '%s'", scope)
}

// getLiteralValue try to get constants.
func getLiteralValue(ctx *Context, expr influxql.Expr) (interface{}, bool, error) {
	switch val := expr.(type) {
	case *influxql.IntegerLiteral:
		return val.Val, true, nil
	case *influxql.NumberLiteral:
		return val.Val, true, nil
	case *influxql.UnsignedLiteral:
		return val.Val, true, nil
	case *influxql.BooleanLiteral:
		return val.Val, true, nil
	case *influxql.StringLiteral:
		return val.Val, true, nil
	case *influxql.DurationLiteral:
		return int64(val.Val), true, nil
	case *influxql.TimeLiteral:
		return val.Val.UnixNano(), true, nil
	case *influxql.RegexLiteral:
		return val.Val, true, nil
	case *influxql.NilLiteral:
		return nil, true, nil
	case *influxql.ListLiteral:
		return val.Vals, true, nil
	case *influxql.Call:
		return getLiteralFuncValue(ctx, val)
	case *influxql.BinaryExpr:
		lv, ok, err := getLiteralValue(ctx, val.LHS)
		if err != nil {
			return nil, false, err
		}
		if !ok {
			return nil, false, nil
		}
		rv, ok, err := getLiteralValue(ctx, val.RHS)
		if err != nil {
			return nil, false, err
		}
		if !ok {
			return nil, false, nil
		}
		v, err := tsql.OperateValues(lv, toOperator(val.Op), rv)
		if err != nil {
			return nil, false, err
		}
		return v, true, nil
	case *influxql.ParenExpr:
		return getLiteralValue(ctx, val.Expr)
	}
	return nil, false, nil
}

func getLiteralFuncValue(ctx *Context, call *influxql.Call) (interface{}, bool, error) {
	if fn, ok := tsql.LiteralFunctions[call.Name]; ok {
		var args []interface{}
		for _, arg := range call.Args {
			arg, ok, err := getLiteralValue(ctx, arg)
			if err != nil {
				return nil, false, err
			}
			if !ok {
				return nil, false, fmt.Errorf("invalid args in literal function '%s'", call.Name)
			}
			args = append(args, arg)
		}
		v, err := fn(ctx, args...)
		if err != nil {
			return nil, false, err
		}
		return v, true, nil
	} else if fn, ok := tsql.BuildInFunctions[call.Name]; ok {
		var args []interface{}
		for _, arg := range call.Args {
			arg, ok, err := getLiteralValue(ctx, arg)
			if err != nil {
				return nil, false, err
			}
			if ok {
				return nil, false, nil
			}
			args = append(args, arg)
		}
		v, err := fn(ctx, args...)
		if err != nil {
			return nil, false, err
		}
		return v, true, nil
	}
	return nil, false, nil
}

func getPainlessFuntion(ctx *Context, call *influxql.Call, deftyp influxql.DataType, fields map[string]bool) (string, bool, error) {
	fn, ok := PainlessFunctions[call.Name]
	if !ok {
		return "", false, nil
	}
	if fn.Convert != nil {
		s, err := fn.Convert(ctx, call, deftyp, fields)
		if err != nil {
			return "", false, err
		}
		return s, true, nil
	}
	name, exprs := fn.Name, call.Args
	if fn.Objective {
		if len(call.Args) < 1 {
			return "", false, fmt.Errorf("invalid function")
		}
		obj, err := getScriptExpression(ctx, call.Args[0], deftyp, fields)
		if err != nil {
			return "", false, err
		}
		name, exprs = "("+obj+")"+"."+fn.Name, call.Args[1:]
	}
	var args []string
	for _, arg := range exprs {
		arg, err := getScriptExpression(ctx, arg, deftyp, fields)
		if err != nil {
			return "", false, err
		}
		args = append(args, arg)
	}
	return name + "(" + strings.Join(args, ", ") + ")", true, nil
}

func getScriptExpression(ctx *Context, expr influxql.Expr, deftyp influxql.DataType, fields map[string]bool) (string, error) {
	if expr == nil {
		return "", nil
	}
	switch expr := expr.(type) {
	case *influxql.BinaryExpr:
		left, err := getScriptExpression(ctx, expr.LHS, deftyp, fields)
		if err != nil {
			return "", err
		}
		right, err := getScriptExpression(ctx, expr.RHS, deftyp, fields)
		if err != nil {
			return "", err
		}
		switch expr.Op {
		case influxql.AND:
			return left + " && " + right, nil
		case influxql.OR:
			return left + " || " + right, nil
		case influxql.EQ:
			return left + " == " + right, nil
		case influxql.EQREGEX, influxql.NEQREGEX:
			return "", fmt.Errorf("not support operater '%s'", expr.Op.String())
		}
		return left + " " + expr.Op.String() + " " + right, nil
	case *influxql.Call:
		val, ok, err := getLiteralFuncValue(ctx, expr)
		if err != nil {
			return "", err
		}
		if ok {
			switch v := val.(type) {
			case nil:
				return "null", nil
			case string:
				return "'" + strings.Replace(v, "'", "\\'", -1) + "'", nil
			case []string:
				return "", fmt.Errorf("not support list in script expression")
			case *regexp.Regexp:
				return "", fmt.Errorf("not support regexp in script expression")
			}
			return fmt.Sprint(val), nil
		}
		s, ok, err := getPainlessFuntion(ctx, expr, deftyp, fields)
		if err != nil {
			return "", err
		}
		if ok {
			return s, nil
		}
		return "", fmt.Errorf("not support function '%s' in script expression", expr.Name)
	case *influxql.ParenExpr:
		s, err := getScriptExpression(ctx, expr.Expr, deftyp, fields)
		if err != nil {
			return "", err
		}
		return "(" + s + ")", nil
	case *influxql.IntegerLiteral:
		return strconv.FormatInt(expr.Val, 10), nil
	case *influxql.NumberLiteral:
		return strconv.FormatFloat(expr.Val, 'f', -1, 64), nil
	case *influxql.BooleanLiteral:
		return strconv.FormatBool(expr.Val), nil
	case *influxql.UnsignedLiteral:
		return strconv.FormatUint(expr.Val, 10), nil
	case *influxql.StringLiteral:
		return "'" + strings.Replace(expr.Val, "'", "\\'", -1) + "'", nil
	case *influxql.NilLiteral:
		return "null", nil
	case *influxql.VarRef:
		key := getKeyName(expr, deftyp)
		if fields != nil {
			fields[key] = true
			return "doc['" + key + "'].value", nil
		}
		return "(doc.containsKey('" + key + "')?doc['" + key + "'].value:'')", nil // '' as default
	case *influxql.TimeLiteral:
		return strconv.FormatInt(expr.Val.UnixNano(), 10), nil
	case *influxql.DurationLiteral:
		return strconv.FormatInt(int64(expr.Val), 10), nil
	case *influxql.RegexLiteral:
		return "", fmt.Errorf("not support regexp in script expression")
	case *influxql.ListLiteral:
		return "", fmt.Errorf("not support list in script expression")
	}
	return "", fmt.Errorf("invalid expression")
}

func getExprStringAndFlag(expr influxql.Expr, deftyp influxql.DataType) (key string, flag tsql.ColumnFlag) {
	if expr == nil {
		return "", tsql.ColumnFlagNone
	}
	switch expr := expr.(type) {
	case *influxql.BinaryExpr:
		left, lf := getExprStringAndFlag(expr.LHS, deftyp)
		right, rf := getExprStringAndFlag(expr.RHS, deftyp)
		return left + expr.Op.String() + right, lf | rf
	case *influxql.Call:
		flag |= tsql.ColumnFlagFunc
		if expr.Name == "time" || expr.Name == "timestamp" {
			flag |= tsql.ColumnFlagGroupByInterval
		} else if expr.Name == "range" {
			flag |= tsql.ColumnFlagGroupByRange
		}
		var args []string
		for _, arg := range expr.Args {
			k, f := getExprStringAndFlag(arg, deftyp)
			args = append(args, k)
			flag |= f
		}
		return expr.Name + "(" + strings.Join(args, ",") + ")", flag
	case *influxql.ParenExpr:
		key, flag = getExprStringAndFlag(expr.Expr, deftyp)
		return key, flag
	case *influxql.IntegerLiteral:
		return strconv.FormatInt(expr.Val, 10), tsql.ColumnFlagLiteral
	case *influxql.NumberLiteral:
		return strconv.FormatFloat(expr.Val, 'f', -1, 64), tsql.ColumnFlagLiteral
	case *influxql.BooleanLiteral:
		return strconv.FormatBool(expr.Val), tsql.ColumnFlagLiteral
	case *influxql.UnsignedLiteral:
		return strconv.FormatUint(expr.Val, 10), tsql.ColumnFlagLiteral
	case *influxql.StringLiteral, *influxql.NilLiteral, *influxql.TimeLiteral, *influxql.DurationLiteral, *influxql.RegexLiteral, *influxql.ListLiteral:
		return expr.String(), tsql.ColumnFlagLiteral
	case *influxql.VarRef:
		return getKeyNameAndFlag(expr, deftyp)
	}
	return expr.String(), tsql.ColumnFlagNone
}

// ParseRawQuery .
func (p *Parser) ParseRawQuery() ([]*tsql.Source, *elastic.BoolQuery, *elastic.SearchSource, error) {
	q, err := p.ql.ParseQuery()
	if err != nil {
		return nil, nil, nil, err
	}
	for _, stmt := range q.Statements {
		s, ok := stmt.(*influxql.SelectStatement)
		if !ok {
			return nil, nil, nil, tsql.ErrNotSupportNonQueryStatement
		}
		source, query, search, err := p.parseRawSelectStatement(s)
		if err != nil {
			return nil, nil, nil, err
		}
		return source, query, search, err
	}
	return nil, nil, nil, fmt.Errorf("not found query statements")
}

// parseRawSelectStatement only handle from、where、sort.
func (p *Parser) parseRawSelectStatement(s *influxql.SelectStatement) ([]*tsql.Source, *elastic.BoolQuery, *elastic.SearchSource, error) {
	// from
	sources, err := p.parseQuerySources(s.Sources)
	if err != nil {
		return nil, nil, nil, err
	}
	// where
	searchSource := elastic.NewSearchSource()
	start, end := p.ctx.Range(true)
	query := elastic.NewBoolQuery().
		Filter(elastic.NewRangeQuery(p.ctx.TimeKey()).Gte(start).Lte(end))
	if p.filter != nil {
		query = query.Filter(p.filter)
	}
	if s.Condition != nil {
		bq := elastic.NewBoolQuery()
		err = p.parseQueryCondition(s.Condition, bq)
		if err != nil {
			return nil, nil, nil, err
		}
		query.Filter(bq)
	}
	searchSource.Query(query)
	if s.Limit <= 0 {
		s.Limit = tsql.DefaultLimtSize
	}
	if s.Offset <= 0 {
		s.Offset = 0
	}
	searchSource.From(s.Offset).Size(s.Limit)
	// sort
	if len(s.SortFields) <= 0 {
		searchSource.Sort(p.ctx.TimeKey(), false)
	} else {
		for _, f := range s.SortFields {
			if f.Expr == nil { // len(f.Name) == 0
				searchSource.Sort(p.ctx.TimeKey(), f.Ascending)
			} else {
				// expr, err := influxql.ParseExpr(f.Name)
				// if err != nil {
				// 	return nil, err
				// }
				expr := f.Expr
				vf, ok := expr.(*influxql.VarRef)
				if ok {
					key := getKeyName(vf, influxql.AnyField)
					searchSource.Sort(key, f.Ascending)
				} else {
					// 先不校验 expr, 直接交给 es 处理
					s, err := getScriptExpression(p.ctx, expr, influxql.AnyField, nil)
					if err != nil {
						return nil, nil, nil, err
					}
					searchSource.SortBy(elastic.NewScriptSort(elastic.NewScript(s), "").Order(f.Ascending))
				}
			}
		}
	}
	return sources, query, searchSource, nil
}
