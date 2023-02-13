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

package elasticsearch

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/ahmetb/go-linq/v3"
	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"

	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

func (p *provider) Aggregate(ctx context.Context, req *storage.Aggregation) (*storage.AggregationResponse, error) {
	var keyPaths []loader.KeyPath
	for _, orgName := range req.Meta.OrgNames {
		keyPaths = append(keyPaths, loader.KeyPath{
			Keys:      []string{orgName},
			Recursive: true,
		})
	}
	indices := p.Loader.Indices(ctx, req.Start, req.End, keyPaths...)
	searchSource, err := getAggregateSearchSource(req)
	if err != nil {
		return nil, err
	}
	if req.Debug {
		source, _ := searchSource.Source()
		fmt.Printf("indices: %v\nsearchSource: %s\n", strings.Join(indices, ","), jsonx.MarshalAndIndent(source))
	}
	ctx, cancel := context.WithTimeout(ctx, p.Cfg.QueryTimeout)
	defer cancel()
	resp, err := p.client.Search(indices...).
		IgnoreUnavailable(true).
		AllowNoIndices(true).
		TimeoutInMillis(int(p.Cfg.QueryTimeout / time.Millisecond)).
		SearchSource(searchSource).
		Do(ctx)
	if err != nil {
		return nil, err
	}
	if resp.Error != nil {
		return nil, fmt.Errorf(resp.Error.Reason)
	}
	result := parseAggregateResult(req, resp)
	return result, nil
}

func parseAggregateResult(req *storage.Aggregation, resp *elastic.SearchResult) *storage.AggregationResponse {
	result := &storage.AggregationResponse{
		Total:        resp.TotalHits(),
		Aggregations: map[string]*storage.AggregationResult{},
	}
	for _, descriptor := range req.Aggs {
		var buckets []*storage.AggregationBucket
		switch descriptor.Typ {
		case storage.AggregationHistogram:
			histogram, ok := resp.Aggregations.Histogram(descriptor.Name)
			if !ok {
				continue
			}
			linq.From(histogram.Buckets).Select(func(item interface{}) interface{} {
				it := item.(*elastic.AggregationBucketHistogramItem)
				return &storage.AggregationBucket{
					Key:   int64(it.Key),
					Count: it.DocCount,
				}
			}).ToSlice(&buckets)

		case storage.AggregationTerms:
			terms, ok := resp.Aggregations.Terms(descriptor.Name)
			if !ok {
				continue
			}
			linq.From(terms.Buckets).Select(func(item interface{}) interface{} {
				it := item.(*elastic.AggregationBucketKeyItem)
				return &storage.AggregationBucket{
					Key:   it.Key,
					Count: it.DocCount,
				}
			}).ToSlice(&buckets)
		}
		result.Aggregations[descriptor.Name] = &storage.AggregationResult{
			Buckets: buckets,
		}
	}
	return result
}

func getAggregateSearchSource(req *storage.Aggregation) (*elastic.SearchSource, error) {
	searchSource := getSearchSource(req.Start, req.End, req.Selector)

	for _, agg := range req.Aggs {
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
				interval = (req.End - req.Start) / options.PreferredPoints
				// minimum interval limit to minimumInterval, default to 1 second,
				// interval should be multiple of 1 second
				if interval < options.MinimumInterval {
					interval = options.MinimumInterval
				} else {
					interval = interval - interval%options.MinimumInterval
				}
			}
			boundEnd := req.End - (req.End-req.Start)%interval
			if (req.End-req.Start)%interval == 0 {
				boundEnd = boundEnd - interval
			}
			searchSource.Aggregation(agg.Name, elastic.NewHistogramAggregation().
				Field(agg.Field).
				Interval(float64(interval)).
				MinDocCount(0).
				Offset(float64(req.Start%interval)).
				ExtendedBounds(float64(req.Start), float64(boundEnd)))
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
			searchSource.Aggregation(agg.Name, elastic.NewTermsAggregation().
				Field(agg.Field).
				Size(int(options.Size)).
				Missing(options.Missing))
		default:
			return nil, fmt.Errorf("do not support aggregation type: %+v", agg.Typ)
		}
	}

	return searchSource, nil
}
