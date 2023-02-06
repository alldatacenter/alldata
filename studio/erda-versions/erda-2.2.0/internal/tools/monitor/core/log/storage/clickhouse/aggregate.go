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
	"context"
	"fmt"

	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
)

func (p *provider) Aggregate(ctx context.Context, req *storage.Aggregation) (*storage.AggregationResponse, error) {
	expr, tableMeta, err := p.buildSqlFromTablePart(req.Selector)
	if err != nil {
		return nil, err
	}

	expr, _, err = p.appendSqlWherePart(expr, tableMeta, req.Selector)
	if err != nil {
		return nil, err
	}

	if len(req.Aggs) != 1 {
		return nil, fmt.Errorf("do not support multi aggregations: %d", len(req.Aggs))
	}
	agg := req.Aggs[0]
	expr, err = p.appendSqlAggregatePart(expr, req.Start, req.End, agg)
	if err != nil {
		return nil, err
	}

	switch agg.Typ {
	case storage.AggregationHistogram:
		var results []struct {
			Key   int64  `ch:"agg_field"`
			Value uint64 `ch:"count"`
		}
		sql, _, err := expr.ToSQL()
		if req.Debug {
			p.Log.Infof("clickhouse aggregate sql: %s", sql)
		}
		err = p.clickhouse.Client().Select(ctx, &results, sql)
		if err != nil {
			return nil, fmt.Errorf("failed to execute err: %s \n expression: %s", err, sql)
		}
		aggResult := &storage.AggregationResponse{
			Aggregations: map[string]*storage.AggregationResult{
				agg.Name: {
					Buckets: []*storage.AggregationBucket{},
				},
			},
		}
		var total int64
		for _, result := range results {
			aggResult.Aggregations[agg.Name].Buckets = append(aggResult.Aggregations[agg.Name].Buckets, &storage.AggregationBucket{
				Key:   result.Key,
				Count: int64(result.Value),
			})
			total += int64(result.Value)
		}
		aggResult.Total = total
		return aggResult, nil
	case storage.AggregationTerms:
		var results []struct {
			Key   string `ch:"agg_field"`
			Value uint64 `ch:"count"`
		}
		sql, _, err := expr.ToSQL()
		if req.Debug {
			p.Log.Infof("clickhouse aggregate sql: %s", sql)
		}
		err = p.clickhouse.Client().Select(ctx, &results, sql)
		if err != nil {
			return nil, fmt.Errorf("failed to execute err: %s \n expression: %s", err, sql)
		}
		aggResult := &storage.AggregationResponse{
			Aggregations: map[string]*storage.AggregationResult{
				agg.Name: {
					Buckets: []*storage.AggregationBucket{},
				},
			},
		}
		var total int64
		for _, result := range results {
			aggResult.Aggregations[agg.Name].Buckets = append(aggResult.Aggregations[agg.Name].Buckets, &storage.AggregationBucket{
				Key:   result.Key,
				Count: int64(result.Value),
			})
			total += int64(result.Value)
		}
		aggResult.Total = total
		return aggResult, nil
	default:
		return nil, fmt.Errorf("not supported aggregation type: %s", agg.Typ)
	}
}
