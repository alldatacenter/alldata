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
	"google.golang.org/protobuf/types/known/structpb"

	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/log-service/pb"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/index/query"
	"github.com/erda-project/erda/pkg/common/apis"
)

func (s *logService) TermsAggregationFromMonitor(ctx context.Context, req *pb.BucketAggregationRequest) (*pb.BucketAggregationResponse, error) {
	if !s.p.Cfg.QueryLogESEnabled {
		return nil, nil
	}

	logKeys, err := s.getLogKeys(req.Addon)
	if err != nil {
		return nil, err
	}

	start, end := req.Start*int64(time.Millisecond), req.End*int64(time.Millisecond)
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

	missing, _ := structpb.NewValue("null")
	options := &monitorpb.TermsAggOptions{
		Size:    20,
		Missing: missing,
	}
	pbOptions, _ := anypb.New(options)

	var aggregations []*monitorpb.AggregationDescriptor
	linq.From(req.AggFields).Select(func(aggField interface{}) interface{} {
		return &monitorpb.AggregationDescriptor{
			Name:    aggField.(string),
			Field:   aggField.(string),
			Type:    monitorpb.AggregationType_Terms,
			Options: pbOptions,
		}
	}).ToSlice(&aggregations)

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
		Aggregations: aggregations,
	})
	if err != nil {
		return nil, err
	}

	monitorResult := &pb.BucketAggregationResponse{
		Data: &pb.LogFieldsAggregationResult{
			AggFields: map[string]*pb.LogFieldBucket{},
			Total:     monitorResp.Total,
		},
	}

	for _, aggregation := range aggregations {
		buckets, ok := monitorResp.Aggregations[aggregation.Name]
		if !ok {
			continue
		}
		bks := &pb.LogFieldBucket{}
		for _, bucket := range buckets.Buckets {
			bks.Buckets = append(bks.Buckets, &pb.BucketAgg{
				Key:   fmt.Sprint(bucket.Key.AsInterface()),
				Count: bucket.Count,
			})
		}
		monitorResult.Data.AggFields[aggregation.Name] = bks
	}

	return monitorResult, nil
}

func (s *logService) TermsAggregationFromLoghub(ctx context.Context, req *pb.BucketAggregationRequest) (*pb.BucketAggregationResponse, error) {
	if s.p.Cfg.QueryLogESEnabled && req.Start*int64(time.Millisecond) > s.startTime {
		return nil, nil
	}

	orgId := s.getRequestOrgIDOrDefault(ctx)
	loghubResp, err := s.p.LoghubQuery.AggregateLogFields(&query.LogFieldsAggregationRequest{
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
		AggFields: req.AggFields,
		TermsSize: 20,
	})
	if err != nil {
		return nil, err
	}
	if loghubResp == nil {
		return nil, nil
	}

	loghubResult := &pb.BucketAggregationResponse{
		Data: &pb.LogFieldsAggregationResult{
			Total:     loghubResp.Total,
			AggFields: map[string]*pb.LogFieldBucket{},
		},
	}

	for _, aggField := range req.AggFields {
		buckets, ok := loghubResp.AggFields[aggField]
		if !ok {
			continue
		}
		bks := &pb.LogFieldBucket{}
		for _, bucket := range buckets.Buckets {
			bks.Buckets = append(bks.Buckets, &pb.BucketAgg{
				Key:   bucket.Key,
				Count: bucket.Count,
			})
		}
		loghubResult.Data.AggFields[aggField] = bks
	}
	return loghubResult, nil
}

func (s *logService) mergeTermAggregationResponse(m *pb.BucketAggregationResponse, l *pb.BucketAggregationResponse) *pb.BucketAggregationResponse {
	if m == nil {
		return l
	}
	if l == nil {
		return m
	}
	m.Data.Total += l.Data.Total

	for name, lAgg := range l.Data.AggFields {
		mAgg, ok := m.Data.AggFields[name]
		if !ok {
			m.Data.AggFields[name] = lAgg
			continue
		}
		var buckets []*pb.BucketAgg
		linq.From(mAgg.Buckets).
			Concat(linq.From(lAgg.Buckets)).
			GroupBy(
				func(i interface{}) interface{} { return i.(*pb.BucketAgg).Key },
				func(i interface{}) interface{} { return i.(*pb.BucketAgg).Count }).
			Select(func(i interface{}) interface{} {
				group := i.(linq.Group)
				return &pb.BucketAgg{
					Key:   group.Key.(string),
					Count: linq.From(group.Group).SumInts(),
				}
			}).
			OrderByDescending(func(i interface{}) interface{} {
				return i.(*pb.BucketAgg).Count
			}).ToSlice(&buckets)
		mAgg.Buckets = buckets
	}
	return m
}
