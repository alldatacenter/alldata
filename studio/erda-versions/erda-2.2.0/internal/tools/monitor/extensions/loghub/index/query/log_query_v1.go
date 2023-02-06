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
	"time"

	"github.com/olivere/elastic"

	logs "github.com/erda-project/erda/internal/tools/monitor/core/log"
)

func (c *ESClient) getBoolQueryV1(req *LogRequest) *elastic.BoolQuery {
	boolQuery := c.getTagsBoolQuery(req)
	start := time.Unix(req.Start/1000, req.Start%1000*int64(time.Millisecond))
	end := time.Unix(req.End/1000, req.End%1000*int64(time.Millisecond))
	boolQuery = boolQuery.Filter(elastic.NewRangeQuery("@timestamp").Gte(start).Lte(end))
	if len(req.Query) > 0 {
		//byts, _ := json.Marshal(req.Query)
		boolQuery = boolQuery.Filter(elastic.NewQueryStringQuery("message:" + req.Query))
	}
	return boolQuery
}

// LogV1 .
type LogV1 struct {
	Message   string            `json:"message"`
	Offset    int64             `json:"offset"`
	Timestamp string            `json:"@timestamp"`
	Tags      map[string]string `json:"tags"`
}

// ToLog .
func (l *LogV1) ToLog() *Log {
	log := &Log{
		Log: &logs.Log{
			Content: l.Message,
			Offset:  l.Offset,
			Tags:    l.Tags,
		},
	}
	t, err := time.Parse("2006-01-02T15:04:05.999Z", l.Timestamp)
	if err == nil {
		log.Timestamp = t.UnixNano() / int64(time.Millisecond)
	}
	return log
}

func (c *ESClient) searchLogsV1(req *LogSearchRequest, timeout time.Duration) (*LogQueryResponse, error) {
	boolQuery := c.getBoolQueryV1(&req.LogRequest)
	searchSource := c.getSearchSource(req, boolQuery)
	if len(req.Sort) <= 0 {
		searchSource.Sort("@timestamp", true).Sort("offset", true)
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
		// 	"tags": {
		// 	    "dice_runtime_id": "7587",
		// 	    "dice_service_name": "marketing-batch-server",
		// 	    "dice_project_id": "181",
		// 	    "dice_workspace": "staging",
		// 	    "dice_application_id": "3027",
		// 	    "dice_application_name": "longfor-marketing",
		// 	    "dice_runtime_name": "release/1.0",
		// 	    "stream": "stdout",
		// 	    "container_id": "5a4fa92ecb687d02ed9063155760d37c3a90c06a15e0df0e8b6d5b037aba4b9a",
		// 	    "terminus_log_key": "zcdff177492c8441ebb84ab63f4e297ae",
		// 	    "dice_project_name": "longfor-middle-marketing"
		// 	},
		// 	"offset": 2206380,
		// 	"message": "Hibernate: select activity0_.id as id1_26_, activity0_.approval_id as approval2_26_, activity0_.approval_status as approval3_26_, activity0_.batch_id as batch_id4_26_, activity0_.created_at as created_5_26_, activity0_.description as descript6_26_, activity0_.ext1 as ext7_26_, activity0_.ext2 as ext8_26_, activity0_.ext3 as ext9_26_, activity0_.group_id as group_i10_26_, activity0_.link as link11_26_, activity0_.marketing_mode as marketi12_26_, activity0_.name as name13_26_, activity0_.operator_id as operato14_26_, activity0_.operator_name as operato15_26_, activity0_.status as status16_26_, activity0_.target_code as target_17_26_, activity0_.updated_at as updated18_26_, activity0_.work_flow as work_fl19_26_ from sb_activity activity0_ where activity0_.approval_status=3 and (activity0_.status in (1 , 2))",
		// 	"@timestamp": "2020-07-20T22:21:01.631Z"
		var logv1 LogV1
		err := json.Unmarshal([]byte(*hit.Source), &logv1)
		if err != nil {
			continue
		}
		log := logv1.ToLog()
		c.setModule(log)
		resp.Data = append(resp.Data, &LogItem{Source: log})
	}
	return resp, nil
}

func (c *ESClient) statisticLogsV1(req *LogStatisticRequest, timeout time.Duration, name string) (*LogStatisticResponse, error) {
	boolQuery := c.getBoolQueryV1(&req.LogRequest)
	searchSource := elastic.NewSearchSource().Query(boolQuery)
	searchSource.Size(0)
	interval := req.Interval
	if req.Points > 0 {
		interval = (req.End - req.Start) / req.Points
	}
	searchSource = searchSource.Aggregation("@timestamp",
		elastic.NewDateHistogramAggregation().
			Field("@timestamp").
			Interval(interval).
			MinDocCount(0).
			Offset(req.Start%interval).ExtendedBounds(req.Start, req.End),
	)
	if req.Debug {
		c.printSearchSource(searchSource)
	}
	resp, err := c.doRequest(&req.LogRequest, searchSource, timeout)
	if err != nil {
		return nil, err
	}
	result := newLogStatisticResponse(interval, resp.TotalHits(), name)
	if resp.Aggregations == nil {
		return result, nil
	}
	histogram, ok := resp.Aggregations.DateHistogram("@timestamp")
	if !ok {
		return result, nil
	}
	list := result.Results[0].Data[0].Count.Data
	for i, b := range histogram.Buckets {
		if req.Points > 0 && int64(i+1) > req.Points && len(list) > 0 {
			last := len(list) - 1
			list[last] = list[last] + float64(b.DocCount)
			continue
		}
		result.Time = append(result.Time, int64(b.Key))
		list = append(list, float64(b.DocCount))
	}
	result.Results[0].Data[0].Count.Data = list
	return result, nil
}
