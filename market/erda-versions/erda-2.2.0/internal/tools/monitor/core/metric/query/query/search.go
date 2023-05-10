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
	"context"
	"fmt"
	"net/url"
	"strings"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"

	"github.com/erda-project/erda-infra/providers/i18n"
	tsql "github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/es-tsql/formats"
)

type queryer struct {
	index IndexLoader
}

// New .
func New(index IndexLoader) Queryer {
	return &queryer{
		index: index,
	}
}

const hourms = int64(time.Hour) / int64(time.Millisecond)

func (q *queryer) buildTSQLParser(ql, statement string, params map[string]interface{}, filters []*Filter, options url.Values) (
	parser tsql.Parser, start, end int64, others map[string]interface{}, err error) {
	idx := strings.Index(ql, ":")
	if idx > 0 {
		if ql[idx+1:] == "ast" && ql[0:idx] == "influxql" {
			statement, err = ConvertAstToStatement(statement)
			if err != nil {
				return nil, 0, 0, nil, err
			}
		}
		ql = ql[0:idx]
	}
	if ql != "influxql" {
		return nil, 0, 0, nil, fmt.Errorf("not support tsql '%s'", ql)
	}
	start, end, err = ParseTimeRange(options.Get("start"), options.Get("end"), options.Get("timestamp"), options.Get("latest"))
	if err != nil {
		return nil, 0, 0, nil, err
	}
	if end < hourms {
		end = hourms
	}
	if start < 0 || start >= end {
		start = end - hourms
	}
	fs, others := ParseFilters(options)
	filters = append(fs, filters...)
	var boolQuery *elastic.BoolQuery
	if len(filters) > 0 {
		boolQuery = elastic.NewBoolQuery()
		err := BuildBoolQuery(filters, boolQuery)
		if err != nil {
			return nil, 0, 0, nil, err
		}
	}
	parser = tsql.New(start*int64(time.Millisecond), end*int64(time.Millisecond), ql, statement)
	if parser == nil {
		return nil, 0, 0, nil, fmt.Errorf("not support tsql '%s'", ql)
	}
	parser = parser.SetFilter(boolQuery)
	if params == nil {
		params = others
	}
	parser = parser.SetParams(params)
	unit := options.Get("epoch") // Keep the same parameters as the influxdb.
	if len(unit) > 0 {
		unit, err := tsql.ParseTimeUnit(unit)
		if err != nil {
			return nil, 0, 0, nil, err
		}
		parser.SetTargetTimeUnit(unit)
	}
	tf := options.Get("time_field")
	idx = strings.Index(tf, "::")
	if len(tf) > 4 && idx > 0 {
		tf = tf[idx+2:] + "s." + tf[0:idx]
	}
	if len(tf) > 0 {
		parser.SetTimeKey(tf)
		if tf == tsql.TimestampKey {
			parser.SetOriginalTimeUnit(tsql.Nanosecond)
		} else {
			tu := options.Get("time_unit")
			if len(tu) > 0 {
				unit, err := tsql.ParseTimeUnit(tu)
				if err != nil {
					return nil, 0, 0, nil, err
				}
				parser.SetOriginalTimeUnit(unit)
			}
		}
	}
	return parser, start, end, others, nil
}

func (q *queryer) doQuery(ql, statement string, params map[string]interface{}, filters []*Filter, options url.Values) (*ResultSet, tsql.Query, map[string]interface{}, error) {
	parser, start, end, others, err := q.buildTSQLParser(ql, statement, params, filters, options)
	if err != nil {
		return nil, nil, nil, err
	}
	querys, err := parser.ParseQuery()
	if err != nil {
		return nil, nil, nil, err
	}
	if len(querys) != 1 {
		return nil, nil, nil, fmt.Errorf("only support one statement")
	}
	query := querys[0]
	metrics, clusters := getMetricsAndClustersFromSources(query.Sources())
	indices := q.index.GetIndices(metrics, clusters, start, end)
	for _, c := range clusters {
		query.BoolQuery().Filter(elastic.NewTermQuery(TagKey+".cluster_name", c))
	}
	if len(indices) == 1 {
		if strings.HasSuffix(indices[0], "-empty") {
			query.BoolQuery().Filter(elastic.NewTermQuery(TagKey+".not_exist", "_not_exist"))
		}
	}
	searchSource := query.SearchSource()
	result := &ResultSet{}
	if _, ok := options["debug"]; ok {
		var source interface{}
		if searchSource != nil {
			source, err = searchSource.Source()
			if err != nil {
				return nil, nil, nil, fmt.Errorf("invalid search source: %s", err)
			}
		}
		result.Details = ElasticSearchCURL(q.index.URLs(), indices, source)
		fmt.Println(result.Details)
		return result, query, nil, nil
	}
	var resp *elastic.SearchResult
	if searchSource != nil {
		now := time.Now()
		resp, err = q.esRequest(indices, searchSource)
		if err != nil {
			return nil, nil, nil, err
		}
		result.Elapsed.Search = time.Now().Sub(now)
	}
	rs, err := query.ParseResult(resp)
	if err != nil {
		return nil, nil, nil, err
	}
	result.ResultSet = rs
	return result, query, others, nil
}

func getMetricsAndClustersFromSources(sources []*tsql.Source) (metrics []string, clusters []string) {
	for _, source := range sources {
		if len(source.Name) > 0 {
			metrics = append(metrics, source.Name)
		}
		if len(source.Database) > 0 {
			clusters = append(clusters, source.Database)
		}
	}
	return metrics, clusters
}

func (q *queryer) esRequest(indices []string, searchSource *elastic.SearchSource) (*elastic.SearchResult, error) {
	context, cancel := context.WithTimeout(context.Background(), q.index.RequestTimeout())
	defer cancel()
	resp, err := q.index.Client().Search(indices...).
		IgnoreUnavailable(true).AllowNoIndices(true).
		SearchSource(searchSource).Do(context)
	if err != nil || (resp != nil && resp.Error != nil) {
		if len(indices) <= 0 || (len(indices) == 1 && strings.HasSuffix(indices[0], "-empty")) {
			return nil, nil
		}
		if resp != nil && resp.Error != nil {
			return nil, fmt.Errorf("fail to request storage: %s", jsonx.MarshalAndIndent(resp.Error))
		}
		return nil, fmt.Errorf("fail to request storage: %s", err)
	}
	return resp, nil
}

// Query .
func (q *queryer) Query(tsql, statement string, params map[string]interface{}, options url.Values) (*ResultSet, error) {
	rs, _, _, err := q.doQuery(tsql, statement, params, nil, options)
	return rs, err
}

// QueryWithFormat .
func (q *queryer) QueryWithFormat(tsql, statement, format string, langCode i18n.LanguageCodes, params map[string]interface{}, filters []*Filter, options url.Values) (*ResultSet, interface{}, error) {
	rs, query, opts, err := q.doQuery(tsql, statement, params, filters, options)
	if err != nil {
		return nil, nil, err
	}
	if rs.Details != nil {
		return rs, nil, err
	}
	data, err := formats.Format(format, query, rs.ResultSet, opts)
	return rs, data, err
}

// QueryRaw .
func (q *queryer) QueryRaw(metrics, clusters []string, start, end int64, searchSource *elastic.SearchSource) (*elastic.SearchResult, error) {
	indices := q.index.GetIndices(metrics, clusters, start, end)
	return q.SearchRaw(indices, searchSource)
}

// SearchRaw .
func (q *queryer) SearchRaw(indices []string, searchSource *elastic.SearchSource) (*elastic.SearchResult, error) {
	context, cancel := context.WithTimeout(context.Background(), q.index.RequestTimeout())
	defer cancel()
	return q.index.Client().Search(indices...).
		IgnoreUnavailable(true).AllowNoIndices(true).
		SearchSource(searchSource).Do(context)
}
