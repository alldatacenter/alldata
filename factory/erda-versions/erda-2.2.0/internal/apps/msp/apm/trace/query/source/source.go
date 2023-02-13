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

	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/commom/custom"
	"github.com/erda-project/erda/pkg/common/apis"
)

type TraceSource interface {
	GetSpans(ctx context.Context, req *pb.GetSpansRequest) []*pb.Span
	GetSpanCount(ctx context.Context, traceID string) int64
	GetTraceReqDistribution(ctx context.Context, model custom.Model) ([]*TraceDistributionItem, error)
	GetTraces(ctx context.Context, req *pb.GetTracesRequest) (*pb.GetTracesResponse, error)
}

type (
	TraceDistributionItem struct {
		Date        string  `ch:"date"`
		AvgDuration float64 `ch:"avg_duration"`
		Count       uint64  `ch:"trace_count"`
	}
)

func getOrgName(ctx context.Context) string {
	return apis.GetHeader(ctx, "org")
}
