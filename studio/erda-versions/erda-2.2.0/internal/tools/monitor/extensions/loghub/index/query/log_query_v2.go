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
	"strconv"
	"time"

	"github.com/olivere/elastic"

	logs "github.com/erda-project/erda/internal/tools/monitor/core/log"
)

func (c *ESClient) getBoolQueryV2(req *LogRequest) *elastic.BoolQuery {
	boolQuery := c.getTagsBoolQuery(req)
	start := req.Start * int64(req.TimeScale)
	end := req.End * int64(req.TimeScale)
	timeRangeQuery := elastic.NewRangeQuery("timestamp")
	if start > 0 {
		timeRangeQuery.Gte(start)
	}
	if end > 0 {
		timeRangeQuery.Lte(end)
	}
	boolQuery = boolQuery.Filter(timeRangeQuery)
	if len(req.Query) > 0 {
		//byts, _ := json.Marshal(req.Query)
		boolQuery = boolQuery.Filter(elastic.NewQueryStringQuery(req.Query).DefaultField("content").DefaultOperator("AND"))
	}
	return boolQuery
}

func (c *ESClient) searchLogsV2(req *LogSearchRequest, timeout time.Duration) (*LogQueryResponse, error) {
	boolQuery := c.getBoolQueryV2(&req.LogRequest)
	searchSource := c.getSearchSource(req, boolQuery)
	if len(req.Sort) <= 0 {
		searchSource.Sort("timestamp", true).Sort("offset", true)
	}
	if req.Debug {
		c.printSearchSource(searchSource)
	}
	total, hits, err := c.doSearchLogs(req, searchSource, timeout)
	if err != nil {
		return nil, err
	}
	resp := &LogQueryResponse{
		Total: total,
	}
	for _, hit := range hits {
		if hit.Source == nil {
			continue
		}
		var log Log
		err := json.Unmarshal([]byte(*hit.Source), &log)
		if err != nil {
			continue
		}
		c.setModule(&log)
		log.DocId = hit.Id
		log.TimestampNanos = strconv.FormatInt(log.Timestamp, 10)
		log.Timestamp = log.Timestamp / int64(time.Millisecond)
		item := &LogItem{Source: &log, Highlight: map[string][]string(hit.Highlight)}
		if item.Highlight != nil {
			delete(item.Highlight, "tags.dice_org_id")
		}
		resp.Data = append(resp.Data, item)
	}
	return resp, nil
}

func (c *ESClient) statisticLogsV2(req *LogStatisticRequest, timeout time.Duration, name string) (*LogStatisticResponse, error) {
	boolQuery := c.getBoolQueryV2(&req.LogRequest)
	searchSource := elastic.NewSearchSource().Query(boolQuery)
	searchSource.Size(0)
	interval := req.Interval
	if req.Points > 0 {
		interval = (req.End - req.Start) / req.Points
	}

	// minimum interval limit to 1 second
	if interval < 1000 {
		interval = 1000
	} else {
		interval = interval - interval%1000
	}

	intervalMillisecond := interval
	start := req.Start * int64(req.TimeScale)
	end := req.End * int64(req.TimeScale)
	interval = interval * int64(req.TimeScale)
	boundEnd := end - (end-start)%interval
	if boundMod := (end - start) % interval; boundMod == 0 {
		boundEnd = boundEnd - interval
	}
	searchSource = searchSource.Aggregation("timestamp",
		elastic.NewHistogramAggregation().
			Field("timestamp").
			Interval(float64(interval)).
			MinDocCount(0).
			Offset(float64(start%interval)).
			ExtendedBounds(float64(start), float64(boundEnd)),
	)
	if req.Debug {
		c.printSearchSource(searchSource)
	}
	resp, err := c.doRequest(&req.LogRequest, searchSource, timeout)
	if err != nil {
		return nil, err
	}
	result := newLogStatisticResponse(intervalMillisecond, resp.TotalHits(), name)
	if resp.Aggregations == nil {
		return result, nil
	}
	histogram, ok := resp.Aggregations.Histogram("timestamp")
	if !ok {
		return result, nil
	}
	list := result.Results[0].Data[0].Count.Data
	for _, b := range histogram.Buckets {
		//if req.Points > 0 && int64(i+1) > req.Points && len(list) > 0 {
		//	last := len(list) - 1
		//	list[last] = list[last] + float64(b.DocCount)
		//	continue
		//}
		result.Time = append(result.Time, int64(b.Key)/int64(time.Millisecond))
		list = append(list, float64(b.DocCount))
	}
	result.Results[0].Data[0].Count.Data = list
	return result, nil
}

func (c *ESClient) aggregateFields(req *LogFieldsAggregationRequest, timeout time.Duration) (*LogFieldsAggregationResponse, error) {
	boolQuery := c.getBoolQueryV2(&req.LogRequest)
	searchSource := elastic.NewSearchSource().Query(boolQuery)
	searchSource.Size(0)
	for _, field := range req.AggFields {
		searchSource.Aggregation(field,
			elastic.NewTermsAggregation().
				Field(field).
				Size(req.TermsSize).
				Missing("null"))
	}
	if req.Debug {
		c.printSearchSource(searchSource)
	}
	resp, err := c.doRequest(&req.LogRequest, searchSource, timeout)
	if err != nil {
		return nil, err
	}
	result := &LogFieldsAggregationResponse{
		Total:     resp.TotalHits(),
		AggFields: map[string]*LogFieldBucket{},
	}
	if resp.Aggregations == nil {
		return result, nil
	}
	for _, field := range req.AggFields {
		termsAgg, ok := resp.Aggregations.Terms(field)
		if !ok {
			return result, nil
		}
		result.AggFields[field] = &LogFieldBucket{
			Buckets: make([]*BucketAgg, len(termsAgg.Buckets)),
		}
		for i, bucket := range termsAgg.Buckets {
			result.AggFields[field].Buckets[i] = &BucketAgg{
				Key:   fmt.Sprint(bucket.Key),
				Count: bucket.DocCount,
			}
		}
	}
	return result, nil
}

func (c *ESClient) downloadLogs(req *LogDownloadRequest, callback func(batchLogs []*Log) error) error {
	boolQuery := c.getBoolQueryV2(&req.LogRequest)
	searchSource := c.getScrollSearchSource(req, boolQuery)
	if len(req.Sort) <= 0 {
		searchSource.Sort("timestamp", true).Sort("offset", true)
	}
	if req.Debug {
		c.printSearchSource(searchSource)
	}

	scrollRequestTimeout := 60 * time.Second
	scrollKeepTime := "1m"
	resp, err := c.doScroll(&req.LogRequest, searchSource, scrollRequestTimeout, scrollKeepTime)
	if err != nil {
		return err
	}

	scrollId := resp.ScrollId
	defer c.clearScroll(&scrollId, scrollRequestTimeout)

	for resp.Hits != nil && len(resp.Hits.Hits) > 0 {
		hits := make([]*Log, len(resp.Hits.Hits))
		for i, hit := range resp.Hits.Hits {
			var log Log
			err = json.Unmarshal([]byte(*hit.Source), &log)
			if err != nil {
				log = Log{Log: &logs.Log{Content: string(*hit.Source)}}
				hits[i] = &log
				continue
			}
			c.setModule(&log)
			log.DocId = hit.Id
			log.Timestamp = log.Timestamp / int64(time.Millisecond)
			hits[i] = &log
		}
		err = callback(hits)
		if err != nil {
			return err
		}

		if len(resp.Hits.Hits) < req.Size {
			return nil
		}

		resp, err = c.scrollNext(scrollId, scrollRequestTimeout, scrollKeepTime)
		if err != nil {
			return err
		}
		scrollId = resp.ScrollId
	}
	return nil
}
