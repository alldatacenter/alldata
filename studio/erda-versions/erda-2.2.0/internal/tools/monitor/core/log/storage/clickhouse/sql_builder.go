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

package clickhouse

import (
	"fmt"
	"strings"
	"time"

	"github.com/doug-martin/goqu/v9"

	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage/clickhouse/converter"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage/clickhouse/query_parser"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

func (p *provider) buildSqlFromTablePart(req *storage.Selector) (*goqu.SelectDataset, *loader.TableMeta, error) {
	var orgName string
	for _, name := range req.Meta.OrgNames {
		if len(name) == 0 {
			continue
		}
		if len(orgName) > 0 {
			return nil, nil, fmt.Errorf("do not support multi-org search: %v", req.Meta.OrgNames)
		}
		orgName = name
	}
	table, meta := p.Loader.GetSearchTable(orgName)
	expr := goqu.From(table)
	return expr, meta, nil
}

func (p *provider) appendSqlWherePart(expr *goqu.SelectDataset, tableMeta *loader.TableMeta, req *storage.Selector) (*goqu.SelectDataset, map[string][]string, error) {
	expr = expr.Where(
		goqu.C("timestamp").Gte(goqu.L("fromUnixTimestamp64Nano(cast(?,'Int64'))", req.Start)),
		goqu.C("timestamp").Lt(goqu.L("fromUnixTimestamp64Nano(cast(?,'Int64'))", req.End)),
	)

	if len(req.Meta.OrgNames) > 0 {
		expr = expr.Where(goqu.C("org_name").In(req.Meta.OrgNames))
	}

	// compatibility for source=deploy
	isContainer := true
	for _, filter := range req.Filters {
		if filter.Key != "source" {
			continue
		}
		if val, ok := filter.Value.(string); ok && val != "container" {
			isContainer = false
		}
	}

	var highlightItems map[string][]string
	for _, filter := range req.Filters {
		val, ok := filter.Value.(string)
		if !ok {
			continue
		}
		if len(val) == 0 {
			continue
		}
		// compatibility for source=deploy, ignore tags filters
		if !isContainer && strings.HasPrefix(filter.Key, "tags.") {
			continue
		}

		nameConverter := converter.NewFieldNameConverter(tableMeta, p.Cfg.FieldNameMapper)

		field := nameConverter.Convert(filter.Key)

		switch filter.Op {
		case storage.EQ:
			expr = expr.Where(goqu.L(field).Eq(val))
		case storage.REGEXP:
			expr = expr.Where(goqu.L("match(?,?)", field, val))
		case storage.CONTAINS:
			expr = expr.Where(goqu.L(field).Like(fmt.Sprintf("%%%s%%", val)))
		case storage.EXPRESSION:
			parser := query_parser.NewEsqsParser(nameConverter, "content", "AND", req.Meta.Highlight)
			result := parser.Parse(val)
			if result.Error() != nil {
				return expr, highlightItems, fmt.Errorf("wrong search expression: %s", result.Error())
			}
			sql := result.Sql()
			if len(sql) > 0 {
				expr = expr.Where(goqu.L(sql))
				highlightItems = result.HighlightItems()
			}
		}
	}
	return expr, highlightItems, nil
}

func (p *provider) appendSqlAggregatePart(expr *goqu.SelectDataset, startTime, endTime int64, agg *storage.AggregationDescriptor) (*goqu.SelectDataset, error) {
	switch agg.Typ {
	case storage.AggregationHistogram:
		options := storage.HistogramAggOptions{
			MinimumInterval: int64(time.Second),
			PreferredPoints: 60,
		}
		if opt, ok := agg.Options.(storage.HistogramAggOptions); ok {
			if opt.PreferredPoints > 0 {
				options.PreferredPoints = opt.PreferredPoints
			}
			if opt.MinimumInterval > 0 {
				options.MinimumInterval = opt.MinimumInterval
			}
			options.FixedInterval = opt.FixedInterval
		}
		interval := options.FixedInterval
		if interval == 0 {
			interval = (endTime - startTime) / options.PreferredPoints
			// minimum interval limit to minimumInterval, default to 1 second,
			// interval should be multiple of 1 second
			if interval < options.MinimumInterval {
				interval = options.MinimumInterval
			} else {
				interval = interval - interval%options.MinimumInterval
			}
		}
		expr = expr.
			Select(
				goqu.L("toUnixTimestamp64Nano(timestamp) - mod(toUnixTimestamp64Nano(timestamp),?)", interval).As("agg_field"),
				goqu.L("count(*)").As("count"),
			).
			GroupBy(goqu.C("agg_field")).Order(goqu.C("agg_field").Asc())
		return expr, nil
	case storage.AggregationTerms:
		options := storage.TermsAggOptions{
			Missing: "null",
			Size:    20,
		}
		if opt, ok := agg.Options.(storage.TermsAggOptions); ok {
			options.Missing = opt.Missing
			if opt.Size > 0 {
				options.Size = opt.Size
			}
		}
		expr = expr.
			Select(
				goqu.C(agg.Field).As("agg_field"),
				goqu.L("count(*)").As("count")).
			GroupBy(goqu.C("agg_field")).
			Order(goqu.C("count").Desc()).
			Limit(uint(options.Size))
		return expr, nil
	default:
		return nil, fmt.Errorf("not supported aggregation type: %s", agg.Typ)
	}
}
