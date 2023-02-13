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

package source

import (
	"context"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/providers/cassandra"
	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/commom/custom"
)

type CassandraSource struct {
	CassandraSession *cassandra.Session
	Log              logs.Logger

	CompatibleSource TraceSource
}

func (cs *CassandraSource) GetTraceReqDistribution(ctx context.Context, model custom.Model) ([]*TraceDistributionItem, error) {
	if cs.CompatibleSource != nil {
		return cs.CompatibleSource.GetTraceReqDistribution(ctx, model)
	}
	return []*TraceDistributionItem{}, nil
}

func (cs *CassandraSource) GetTraces(ctx context.Context, req *pb.GetTracesRequest) (*pb.GetTracesResponse, error) {
	if cs.CompatibleSource != nil {
		return cs.CompatibleSource.GetTraces(ctx, req)
	}
	return &pb.GetTracesResponse{}, nil
}

func (cs *CassandraSource) GetSpans(ctx context.Context, req *pb.GetSpansRequest) []*pb.Span {
	iter := cs.CassandraSession.Session().Query("SELECT * FROM spans WHERE trace_id = ? limit ?", req.TraceID, req.Limit).Iter()
	var items []*pb.Span
	for {
		row := make(map[string]interface{})
		if !iter.MapScan(row) {
			break
		}
		var span pb.Span
		span.Id = row["span_id"].(string)
		span.TraceId = row["trace_id"].(string)
		span.OperationName = row["operation_name"].(string)
		span.ParentSpanId = row["parent_span_id"].(string)
		span.StartTime = row["start_time"].(int64)
		span.EndTime = row["end_time"].(int64)
		span.Tags = row["tags"].(map[string]string)
		items = append(items, &span)
	}
	return items
}

func (cs *CassandraSource) GetSpanCount(ctx context.Context, traceID string) int64 {
	var count int64
	cs.CassandraSession.Session().Query("SELECT COUNT(trace_id) FROM spans WHERE trace_id = ?", traceID).Iter().Scan(&count)
	return count
}
