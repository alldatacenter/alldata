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
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/chartmeta"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricmeta"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/query"
)

type queryer struct {
	index  query.IndexLoader
	charts *chartmeta.Manager
	meta   *metricmeta.Manager
	t      i18n.Translator
}

// New .
func New(index query.IndexLoader, charts *chartmeta.Manager, meta *metricmeta.Manager, t i18n.Translator) Queryer {
	return &queryer{
		index:  index,
		charts: charts,
		meta:   meta,
		t:      t,
	}
}

func (q *queryer) QueryWithFormatV1(qlang, statement, format string, langCode i18n.LanguageCodes) (*Response, error) {
	parser, ok := Parsers[qlang]
	if !ok {
		return nil, fmt.Errorf("invalid query language %s", qlang)
	}
	req, err := parser.Parse(statement)
	if err != nil {
		return nil, err
	}
	err = NormalizeRequest(req)
	if err != nil {
		return nil, err
	}
	return q.doRequest(req, format, langCode)
}

func (q *queryer) doRequest(req *Request, format string, langCodes i18n.LanguageCodes) (*Response, error) {
	cm := q.getChartMeta(req, langCodes)
	boolQuery, err := q.buildBoolQuery(req)
	if err != nil {
		return nil, err
	}
	searchSource := elastic.NewSearchSource().Query(boolQuery)

	showSource := len(req.GroupBy) == 0 && len(req.Select) == 0
	if showSource {
		searchSource.Size(req.Limit[0])
		for _, order := range req.OrderBy {
			if order.FuncName != "" {
				return nil, fmt.Errorf("invalid function '%s' in order by", order.FuncName)
			}
			if order.Property.IsScript() {
				searchSource.SortBy(elastic.NewScript(order.Property.Script))
			} else {
				searchSource.Sort(order.Property.Key, order.Ascending())
			}
		}
	} else {
		searchSource.Size(0)
	}

	ctx := &Context{
		Req:        req,
		Source:     searchSource,
		Attributes: make(map[string]interface{}),
		ChartMeta:  cm,
		T:          q.t,
		Lang:       i18n.LanguageCodes{},
	}
	addAggFn := func(name string, agg elastic.Aggregation) {
		searchSource.Aggregation(name, agg)
	}
	for i, group := range req.GroupBy {
		terms := elastic.NewTermsAggregation().Size(group.Limit)
		if group.Property.IsScript() {
			terms = terms.Script(elastic.NewScript(group.Property.Script))
		} else {
			terms = terms.Field(group.Property.Key)
		}
		group.ColumnAggs = make(map[string]bool) // Used to dedouble the subaggregation of the innermost terms
		if group.Sort != nil {
			if group.Sort.FuncName == "count" && len(group.Sort.Property.Name) <= 0 {
				terms.OrderByCount(group.Sort.Ascending())
			} else {
				if i == (len(req.GroupBy)-1) && req.Aggregate == nil {
					if _, ok := req.Columns[group.Sort.ID]; ok {
						group.ColumnAggs[group.Sort.ID] = true
					}
				}
				col := &Column{
					ID:       group.Sort.ID,
					Property: group.Sort.Property,
					FuncName: group.Sort.FuncName,
					Params:   group.Sort.Params,
				}
				creator, ok := Functions[col.FuncName]
				if !ok {
					return nil, fmt.Errorf("function '%s' not exist", col.FuncName)
				}
				fn := creator(col)
				if fn.SupportOrderBy() {
					if !group.ColumnAggs[group.Sort.ID] {
						aggs, err := fn.Aggregations(ctx, FlagOrderBy)
						if err != nil {
							return nil, fmt.Errorf("function '%s' %s", col.FuncName, err)
						}
						if len(aggs) != 1 {
							return nil, fmt.Errorf("function '%s' not support order by", col.FuncName)
						}
						agg := aggs[0]
						terms.SubAggregation(group.Sort.ID, agg.Aggregation)
					}
					terms.OrderByAggregation(group.Sort.ID, group.Sort.Ascending())
				}
			}
		}
		for _, filter := range group.Filters {
			if i == (len(req.GroupBy)-1) && req.Aggregate == nil {
				if _, ok := req.Columns[filter.ID]; ok {
					group.ColumnAggs[filter.ID] = true
				}
			}
			if !group.ColumnAggs[filter.ID] {
				col := &Column{
					ID:       filter.ID,
					Property: filter.Property,
					FuncName: filter.FuncName,
					Params:   filter.Params,
				}
				creator, ok := Functions[col.FuncName]
				if !ok {
					return nil, fmt.Errorf("function '%s' not exist", col.FuncName)
				}
				fn := creator(col)
				if !fn.SupportOrderBy() {
					return nil, fmt.Errorf("function '%s' not support group filter", col.FuncName)
				}
				aggs, err := fn.Aggregations(ctx, FlagOrderBy)
				if err != nil {
					return nil, fmt.Errorf("function '%s' %s", col.FuncName, err)
				}
				if len(aggs) != 1 {
					return nil, fmt.Errorf("function '%s' not support group filter", col.FuncName)
				}
				agg := aggs[0]
				terms.SubAggregation(filter.ID, agg.Aggregation)
			}
			byts, err := json.Marshal(filter.Value)
			if err != nil {
				return nil, err
			}
			var op string
			switch filter.Operator {
			case "eq", "=", "":
				op = "=="
			case "neq", "!=":
				op = "!="
			case "gt", ">":
				op = ">"
			case "gte", ">=":
				op = ">="
			case "lt", "<":
				op = "<"
			case "lte", "<=":
				op = "<="
			default:
				return nil, fmt.Errorf("not support filter operator %s", filter.Operator)
			}
			terms.SubAggregation(
				fmt.Sprintf("bucket_filter_%s", filter.ID),
				elastic.NewBucketSelectorAggregation().AddBucketsPath("_"+filter.ID, filter.ID).
					Script(elastic.NewScript(fmt.Sprintf("params._%s %s %v", filter.ID, op, string(byts)))))
		}
		addAggFn(group.ID, terms)
		addAggFn = func(name string, agg elastic.Aggregation) {
			terms.SubAggregation(name, agg)
		}
	}

	if req.Aggregate != nil {
		agg := req.Aggregate
		switch agg.FuncName {
		case "histogram":
			start, end := req.Range(true)
			hist := elastic.NewHistogramAggregation().Field(req.TimeKey).
				Interval(req.Interval/float64(req.OriginalTimeUnit)).
				MinDocCount(0).Offset(float64(start)).
				ExtendedBounds(float64(start), float64(end))
			addAggFn(agg.ID, hist)
			addAggFn = func(name string, agg elastic.Aggregation) {
				hist.SubAggregation(name, agg)
			}
		}
	}

	for _, col := range req.Select {
		creator, ok := Functions[col.FuncName]
		if !ok {
			return nil, fmt.Errorf("function '%s' not exist", col.FuncName)
		}
		fn := creator(col)
		col.Function = fn
		aggs, err := fn.Aggregations(ctx, FlagColumnFunc)
		if err != nil {
			return nil, fmt.Errorf("function '%s' %s", col.FuncName, err)
		}
		for _, item := range aggs {
			addAggFn(item.ID, item.Aggregation)
		}
	}

	indices := q.index.GetIndices(req.Metrics, req.ClusterNames, req.Start, req.End)
	if len(indices) == 1 {
		if strings.HasSuffix(indices[0], "-empty") {
			boolQuery.Filter(elastic.NewTermQuery(query.TagKey+".not_exist", "_not_exist"))
		}
	}

	result := &Response{
		Metrics:  req.Metrics,
		Interval: req.Interval,
		req:      req,
	}
	if req.Debug {
		source, err := searchSource.Source()
		if err != nil {
			return nil, fmt.Errorf("invalid search source: %s", err)
		}
		result.details = query.ElasticSearchCURL(q.index.URLs(), indices, source)
		fmt.Println(result.details)
		return result, nil
	}

	now := time.Now()
	context, cancel := context.WithTimeout(context.Background(), q.index.RequestTimeout())
	defer cancel()
	resp, err := q.index.Client().Search(indices...).
		IgnoreUnavailable(true).AllowNoIndices(true).
		SearchSource(searchSource).Do(context)
	if err != nil || (resp != nil && resp.Error != nil) {
		if len(indices) <= 0 || (len(indices) == 1 && strings.HasSuffix(indices[0], "-empty")) {
			return result, nil
		}
		if resp != nil && resp.Error != nil {
			return nil, fmt.Errorf("fail to request storage: %s", jsonx.MarshalAndIndent(resp.Error))
		}
		return nil, fmt.Errorf("fail to request storage: %s", err)
	}
	if resp == nil {
		return result, nil
	}
	ctx.Resp = resp
	result.Total = resp.Hits.TotalHits
	result.Elapsed.Search = time.Now().Sub(now)
	if showSource {
		result.Data = q.showSource(resp)
	} else {
		parts := strings.SplitN(format, ":", 2)
		format = parts[0]
		var param string
		if len(parts) > 1 {
			param = parts[1]
		}
		formater, ok := Formats[format]
		if !ok {
			return nil, fmt.Errorf("invalid formater %s", format)
		}
		result.Data, err = formater.Format(ctx, param)
		if err != nil {
			return nil, fmt.Errorf("fail to format response: %s", err)
		}
	}
	return result, nil
}

func (q *queryer) buildBoolQuery(req *Request) (*elastic.BoolQuery, error) {
	start, end := req.Range(true)
	boolQuery := elastic.NewBoolQuery().Filter(elastic.NewRangeQuery(req.TimeKey).Gte(start).Lte(end + req.EndOffset/int64(req.OriginalTimeUnit)))
	err := query.BuildBoolQuery(req.Where, boolQuery)
	if err != nil {
		return nil, err
	}
	return boolQuery, nil
}

func (q *queryer) showSource(resp *elastic.SearchResult) interface{} {
	var list []map[string]interface{}
	if resp.Hits == nil {
		return list
	}
	for _, item := range resp.Hits.Hits {
		var source map[string]interface{}
		json.Unmarshal([]byte(*item.Source), &source)
		list = append(list, source)
	}
	return list
}

func (q *queryer) getChartMeta(req *Request, langCodes i18n.LanguageCodes) *chartmeta.ChartMeta {
	if req.Trans {
		metas, err := q.meta.MetricMeta(langCodes, "org,dice,micro_service", "", req.Name)
		if err == nil && len(metas) > 0 {
			meta := metas[0]
			cm := &chartmeta.ChartMeta{
				Defines:     make(map[string]*chartmeta.DataMeta),
				MetricNames: req.Name,
				Name:        req.Name,
			}
			for _, col := range req.Select {
				key := col.FuncName + "." + col.Property.Name
				if strings.HasPrefix(col.Property.Key, "tags.") {
					k := col.Property.Key[len("tags."):]
					tag := meta.Tags[k]
					if tag != nil && len(tag.Name) > 0 {
						name := tag.Name + " " + q.meta.AggName(langCodes, col.FuncName)
						cm.Defines[key] = &chartmeta.DataMeta{Label: &name}
					}
				} else if strings.HasPrefix(col.Property.Key, "fields.") {
					k := col.Property.Key[len("fields."):]
					field := meta.Fields[k]
					if field != nil && len(field.Name) > 0 {
						name := field.Name + " " + q.meta.AggName(langCodes, col.FuncName)
						cm.Defines[key] = &chartmeta.DataMeta{Label: &name}
					}
				} else if col.Property.Key == "name" && len(meta.Name.Name) > 0 {
					name := meta.Name.Name + " " + q.meta.AggName(langCodes, col.FuncName)
					cm.Defines[key] = &chartmeta.DataMeta{Label: &name}
				}
			}
			return cm
		}
	} else if req.LegendMap != nil && len(req.LegendMap) > 0 { // user custom
		// def := make(map[string]*chartmeta.DataMeta)
		// for k, v := range req.LegendMap {
		// 	def[k] = &v
		// }
		cm := &chartmeta.ChartMeta{
			Defines:     req.LegendMap,
			MetricNames: req.Name,
			Name:        req.Name,
		}
		return cm
	}

	cm := q.charts.ChartMeta(langCodes, req.Name)
	if cm != nil {
		req.Metrics = strings.Split(cm.MetricNames, ",")
	}
	return cm
}
