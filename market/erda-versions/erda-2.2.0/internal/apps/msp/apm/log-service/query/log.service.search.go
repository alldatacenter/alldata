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
	"strings"
	"time"

	"github.com/ahmetb/go-linq/v3"

	monitorpb "github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/log-service/pb"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/index/query"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/math"
)

func (s *logService) PagedSearchFromMonitor(ctx context.Context, req *pb.PagedSearchRequest) (*pb.PagedSearchResponse, error) {
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

	// max allowed size limit to 10000
	pageSize := req.PageSize
	if pageSize*req.PageNo > 10000 {
		pageSize = 10000 - (req.PageNo-1)*pageSize
	}

	isDescendingOrder := !StringList(req.Sort).All(func(item string) bool { return strings.HasSuffix(item, " asc") })
	if isDescendingOrder {
		pageSize = -pageSize
	}

	monitorResp, err := s.p.MonitorLogService.GetLogByExpression(ctx, &monitorpb.GetLogByExpressionRequest{
		Start:           start,
		End:             end,
		QueryExpression: expr,
		ExtraFilter: &monitorpb.ExtraFilter{
			PositionOffset: (req.PageNo - 1) * req.PageSize,
		},
		QueryMeta: &monitorpb.QueryMeta{
			Highlight:               req.Highlight,
			OrgName:                 apis.GetHeader(ctx, "Org"),
			IgnoreMaxTimeRangeLimit: true,
			PreferredBufferSize:     int32(math.AbsInt64(pageSize)),
		},
		Count: pageSize,
		Debug: req.Debug,
		Live:  false,
	})
	if err != nil {
		return nil, err
	}

	result := &pb.PagedSearchResponse{
		Data: &pb.LogQueryResult{
			Total: monitorResp.Total,
		},
	}
	for _, line := range monitorResp.Lines {
		result.Data.Data = append(result.Data.Data, &pb.HighlightLog{
			Source: &pb.LogItem{
				DocId:          line.UniqId,
				Id:             line.Id,
				Source:         line.Source,
				Stream:         line.Stream,
				Content:        line.Content,
				Offset:         line.Offset,
				Timestamp:      line.UnixNano / int64(time.Millisecond),
				TimestampNanos: line.Timestamp,
				Tags:           line.Tags,
			},
			Highlight: line.Highlight,
		})
	}

	return result, nil
}

func (s *logService) PagedSearchFromLoghub(ctx context.Context, req *pb.PagedSearchRequest) (*pb.PagedSearchResponse, error) {
	if s.p.Cfg.QueryLogESEnabled && req.Start*int64(time.Millisecond) > s.startTime {
		return nil, nil
	}

	orgId := s.getRequestOrgIDOrDefault(ctx)
	loghubResp, err := s.p.LoghubQuery.SearchLogs(&query.LogSearchRequest{
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
		Page:      req.PageNo,
		Size:      req.PageSize,
		Sort:      req.Sort,
		Highlight: req.Highlight,
	})
	if err != nil {
		return nil, err
	}

	if loghubResp == nil {
		return nil, nil
	}

	result := &pb.PagedSearchResponse{
		Data: &pb.LogQueryResult{
			Total: loghubResp.Total,
		},
	}
	for _, item := range loghubResp.Data {
		result.Data.Data = append(result.Data.Data, &pb.HighlightLog{
			Source: &pb.LogItem{
				DocId:          item.Source.DocId,
				Id:             item.Source.ID,
				Source:         item.Source.Source,
				Stream:         item.Source.Stream,
				Content:        item.Source.Content,
				Offset:         item.Source.Offset,
				Timestamp:      item.Source.Timestamp, //milliseconds
				TimestampNanos: item.Source.TimestampNanos,
				Tags:           item.Source.Tags,
			},
			Highlight: ListMapConverter(item.Highlight).ToPbListMap(),
		})
	}

	return result, nil
}

func (s *logService) SequentialSearchFromMonitor(ctx context.Context, req *pb.SequentialSearchRequest) (*pb.SequentialSearchResponse, error) {
	if !s.p.Cfg.QueryLogESEnabled {
		return nil, nil
	}

	logKeys, err := s.getLogKeys(req.Addon)
	if err != nil {
		return nil, err
	}

	start, end := req.TimestampNanos, int64(0)
	isDescendingOrder := req.Sort == "desc"
	if isDescendingOrder {
		start, end = 0, start
	}
	if req.Start > 0 || req.End > 0 {
		start = req.Start * int64(time.Millisecond)
		end = req.End * int64(time.Millisecond)
	}

	count := req.Count
	if isDescendingOrder {
		count = -count
	}

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

	monitorResp, err := s.p.MonitorLogService.GetLogByExpression(ctx, &monitorpb.GetLogByExpressionRequest{
		Start:           start,
		End:             end,
		QueryExpression: expr,
		ExtraFilter: &monitorpb.ExtraFilter{
			After: &monitorpb.LogUniqueID{
				UnixNano: req.TimestampNanos,
				Id:       req.Id,
				Offset:   req.Offset,
			},
		},
		QueryMeta: &monitorpb.QueryMeta{
			OrgName:                 apis.GetHeader(ctx, "Org"),
			IgnoreMaxTimeRangeLimit: true,
			PreferredBufferSize:     int32(req.Count),
			SkipTotalStat:           true,
		},
		Count: count,
		Debug: req.Debug,
		Live:  false,
	})
	if err != nil {
		return nil, err
	}

	result := &pb.SequentialSearchResponse{
		Data: &pb.LogQueryResult{
			Total: monitorResp.Total,
		},
	}
	for _, line := range monitorResp.Lines {
		result.Data.Data = append(result.Data.Data, &pb.HighlightLog{
			Source: &pb.LogItem{
				DocId:          line.UniqId,
				Id:             line.Id,
				Source:         line.Source,
				Stream:         line.Stream,
				Content:        line.Content,
				Offset:         line.Offset,
				Timestamp:      line.UnixNano / int64(time.Millisecond),
				TimestampNanos: line.Timestamp,
				Tags:           line.Tags,
			},
			Highlight: line.Highlight,
		})
	}

	return result, nil
}

func (s *logService) SequentialSearchFromLoghub(ctx context.Context, req *pb.SequentialSearchRequest) (*pb.SequentialSearchResponse, error) {

	start, end := req.TimestampNanos, int64(0)
	if req.Sort == "desc" {
		start, end = start-int64(7*24*time.Hour), start
	}
	if req.Start > 0 || req.End > 0 {
		start = req.Start * int64(time.Millisecond)
		end = req.End * int64(time.Millisecond)
	}

	if s.p.Cfg.QueryLogESEnabled && start > s.startTime {
		return nil, nil
	}

	orgId := s.getRequestOrgIDOrDefault(ctx)
	sorts := []string{"timestamp " + req.Sort, "id " + req.Sort, "offset " + req.Sort}
	loghubResp, err := s.p.LoghubQuery.SearchLogs(&query.LogSearchRequest{
		LogRequest: query.LogRequest{
			OrgID:       orgId,
			ClusterName: req.ClusterName,
			Addon:       req.Addon,
			Start:       start,
			End:         end,
			TimeScale:   time.Nanosecond,
			Query:       req.Query,
			Debug:       req.Debug,
			Lang:        apis.Language(ctx),
		},
		Page:        1,
		Size:        req.Count,
		Sort:        sorts,
		SearchAfter: []interface{}{req.TimestampNanos, req.Id, req.Offset},
	})
	if err != nil {
		return nil, err
	}

	result := &pb.SequentialSearchResponse{
		Data: &pb.LogQueryResult{
			Total: loghubResp.Total,
		},
	}
	for _, item := range loghubResp.Data {
		result.Data.Data = append(result.Data.Data, &pb.HighlightLog{
			Source: &pb.LogItem{
				DocId:          item.Source.DocId,
				Id:             item.Source.ID,
				Source:         item.Source.Source,
				Stream:         item.Source.Stream,
				Content:        item.Source.Content,
				Offset:         item.Source.Offset,
				Timestamp:      item.Source.Timestamp, //milliseconds
				TimestampNanos: item.Source.TimestampNanos,
				Tags:           item.Source.Tags,
			},
			Highlight: ListMapConverter(item.Highlight).ToPbListMap(),
		})
	}

	return result, nil
}

func (s *logService) mergePagedSearchResponse(m *pb.PagedSearchResponse, l *pb.PagedSearchResponse, ascending bool, limit int) *pb.PagedSearchResponse {
	if m == nil {
		return l
	}
	if l == nil {
		return m
	}

	m.Data.Total += l.Data.Total
	m.Data.Data = s.mergeLogSlices(m.Data.Data, l.Data.Data, ascending, limit)
	return m
}

func (s *logService) mergeSequentialSearchResponse(m *pb.SequentialSearchResponse, l *pb.SequentialSearchResponse, ascending bool, limit int) *pb.SequentialSearchResponse {
	if m == nil {
		return l
	}
	if l == nil {
		return m
	}

	m.Data.Total += l.Data.Total
	m.Data.Data = s.mergeLogSlices(m.Data.Data, l.Data.Data, ascending, limit)
	return m
}

func (s *logService) mergeLogSlices(a []*pb.HighlightLog, b []*pb.HighlightLog, ascending bool, limit int) []*pb.HighlightLog {
	if a == nil {
		return b
	}
	if b == nil {
		return a
	}

	if limit == 0 {
		limit = len(a) + len(b)
	}

	var list []*pb.HighlightLog
	mergedQuery := linq.From(a).Concat(linq.From(b))
	if ascending {
		mergedQuery.OrderBy(func(i interface{}) interface{} {
			return i.(*pb.HighlightLog).Source.TimestampNanos
		}).Take(limit).ToSlice(&list)
	} else {
		mergedQuery.OrderByDescending(func(i interface{}) interface{} {
			return i.(*pb.HighlightLog).Source.TimestampNanos
		}).Take(limit).ToSlice(&list)
	}

	return list
}
