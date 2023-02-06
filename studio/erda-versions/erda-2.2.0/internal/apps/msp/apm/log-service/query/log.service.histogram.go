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

package log_service

import (
	"context"
	"fmt"
	"time"

	"github.com/ahmetb/go-linq/v3"
	"google.golang.org/protobuf/types/known/anypb"

	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/log-service/pb"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/index/query"
	"github.com/erda-project/erda/pkg/common/apis"
)

func (s *logService) HistogramAggregationFromMonitor(ctx context.Context, req *pb.HistogramAggregationRequest) (*pb.HistogramAggregationResponse, error) {
	if !s.p.Cfg.QueryLogESEnabled {
		return nil, nil
	}

	logKeys, err := s.getLogKeys(req.Addon)
	if err != nil {
		return nil, err
	}

	start, end := req.Start*int64(time.Millisecond), req.End*int64(time.Millisecond)
	var monitorResult *pb.HistogramAggregationResponse
	var interval int64

	expr := req.Query
	if len(expr) > 0 {
		expr = fmt.Sprintf("(%s) AND", expr)
	}
	if start > s.startTime {
		expr = fmt.Sprintf("%s (%s)", expr, logKeys.
			ToESQueryString())
	} else if logKeys.Contains(logServiceKey) {
		expr = fmt.Sprintf("%s (%s)", expr, logKeys.
			Where(func(k LogKeyType, v StringList) bool { return k == logServiceKey }).
			ToESQueryString())
	} else {
		return nil, nil
	}

	options := &monitorpb.HistogramAggOptions{
		MinimumInterval: int64(time.Second),
		PreferredPoints: 60,
	}
	interval = (end - start) / options.PreferredPoints
	// minimum interval limit to minimumInterval, default to 1 second,
	// interval should be multiple of 1 second
	if interval < options.MinimumInterval {
		interval = options.MinimumInterval
	} else {
		interval = interval - interval%options.MinimumInterval
	}
	options.FixedInterval = interval
	pbOptions, _ := anypb.New(options)

	monitorResp, err := s.p.MonitorLogService.LogAggregation(ctx, &monitorpb.LogAggregationRequest{
		Query: &monitorpb.GetLogByExpressionRequest{
			Start:           start,
			End:             end,
			QueryExpression: expr,
			QueryMeta: &monitorpb.QueryMeta{
				OrgName:                 apis.GetHeader(ctx, "Org"),
				IgnoreMaxTimeRangeLimit: true,
			},
			Debug: req.Debug,
			Live:  false,
		},
		Aggregations: []*monitorpb.AggregationDescriptor{
			{
				Name:    "histogram",
				Field:   "timestamp",
				Type:    monitorpb.AggregationType_Histogram,
				Options: pbOptions,
			},
		},
	})
	if err != nil {
		return nil, err
	}

	var times []int64
	linq.From(monitorResp.Aggregations["histogram"].Buckets).Select(func(i interface{}) interface{} {
		return int64(i.(*monitorpb.AggregationBucket).Key.GetNumberValue()) / int64(time.Millisecond)
	}).ToSlice(&times)
	var counts []float64
	linq.From(monitorResp.Aggregations["histogram"].Buckets).Select(func(i interface{}) interface{} {
		return float64(i.(*monitorpb.AggregationBucket).Count)
	}).ToSlice(&counts)
	name := s.p.I18n.Text(apis.Language(ctx), "Count")
	monitorResult = &pb.HistogramAggregationResponse{
		Data: &pb.HistogramAggregationResult{
			Interval: interval / int64(time.Millisecond),
			Title:    name,
			Total:    monitorResp.Total,
			Time:     times,
			Results: []*pb.LogStatisticResult{
				{
					Name: "count",
					Data: []*pb.CountHistogram{
						{
							Count: &pb.ArrayAgg{
								Name:      name,
								ChartType: "line",
								Data:      counts,
							},
						},
					},
				},
			},
		},
	}

	return monitorResult, nil
}

func (s *logService) HistogramAggregationFromLoghub(ctx context.Context, req *pb.HistogramAggregationRequest) (*pb.HistogramAggregationResponse, error) {
	if s.p.Cfg.QueryLogESEnabled && req.Start*int64(time.Millisecond) > s.startTime {
		return nil, nil
	}

	orgId := s.getRequestOrgIDOrDefault(ctx)
	loghubResp, err := s.p.LoghubQuery.StatisticLogs(&query.LogStatisticRequest{
		Interval: int64(time.Second / time.Millisecond),
		Points:   60,
		LogRequest: query.LogRequest{
			OrgID:       orgId,
			ClusterName: req.ClusterName,
			Addon:       req.Addon,
			Start:       req.Start,
			End:         req.End,
			TimeScale:   time.Millisecond,
			Query:       req.Query,
			Debug:       req.Debug,
			Lang:        apis.Language(ctx),
		},
	})
	if err != nil {
		return nil, err
	}
	if loghubResp == nil {
		return nil, nil
	}

	var statList []*pb.LogStatisticResult
	linq.From(loghubResp.Results).Select(func(item interface{}) interface{} {
		r := item.(*query.LogStatisticResult)
		var list []*pb.CountHistogram
		linq.From(r.Data).Select(func(i interface{}) interface{} {
			ch := i.(*query.CountHistogram)
			return &pb.CountHistogram{
				Count: &pb.ArrayAgg{
					Name: ch.Count.Name,
					Data: ch.Count.Data,
				},
			}
		}).ToSlice(&list)
		return &pb.LogStatisticResult{
			Name: r.Name,
			Data: list,
		}
	}).ToSlice(&statList)
	loghubResult := &pb.HistogramAggregationResponse{
		Data: &pb.HistogramAggregationResult{
			Title:   loghubResp.Title,
			Total:   loghubResp.Total,
			Time:    loghubResp.Time,
			Results: statList,
		},
	}
	return loghubResult, nil
}

func (s *logService) mergeHistogramAggregationResponse(m *pb.HistogramAggregationResponse, l *pb.HistogramAggregationResponse) *pb.HistogramAggregationResponse {
	if m == nil {
		return l
	}
	if l == nil {
		return m
	}
	m.Data.Total += l.Data.Total
	mList := m.Data.Results[0].Data[0].Count.Data
	lList := l.Data.Results[0].Data[0].Count.Data
	length := len(mList)
	for i, datum := range lList {
		if i >= length {
			break
		}
		mList[i] += datum
	}
	return m
}
