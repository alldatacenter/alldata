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
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/storage"
	"github.com/erda-project/erda/pkg/common/apis"
)

func TestFetchSpanFromES(t *testing.T) {
	type args struct {
		ctx     context.Context
		storage storage.Storage
		sel     storage.Selector
		forward bool
		limit   int
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{limit: 0}, false},
		{"case2", args{limit: -1}, true},
		{"case2", args{limit: 1}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			monkey.UnpatchAll()
			monkey.Patch(FetchSpanFromES, func(ctx context.Context, storage storage.Storage, sel storage.Selector, forward bool, limit int) (list []*trace.Span, err error) {
				if limit == 0 {
					return []*trace.Span{}, nil
				}
				if limit == -1 {
					return nil, errors.New("error")
				}
				return []*trace.Span{
					{TraceId: "test"},
				}, nil
			})
			_, err := FetchSpanFromES(tt.args.ctx, tt.args.storage, tt.args.sel, tt.args.forward, tt.args.limit)
			if (err != nil) != tt.wantErr {
				t.Errorf("FetchSpanFromES() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func TestElasticsearchSource_GetSpans(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetSpansRequest
	}
	tests := []struct {
		name string
		args args
		want []*pb.Span
	}{
		{"case1", args{req: &pb.GetSpansRequest{}}, nil},
		{"case2", args{req: &pb.GetSpansRequest{
			TraceID: "test-one", OrgName: "test", Limit: 1,
		}}, []*pb.Span{{TraceId: "test-one", Id: "test-one" + "-span1"}}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			monkey.UnpatchAll()
			monkey.Patch(FetchSpanFromES, func(ctx context.Context, storage storage.Storage, sel storage.Selector, forward bool, limit int) (list []*trace.Span, err error) {
				if limit == 0 {
					return []*trace.Span{}, nil
				}
				if limit == -1 {
					return nil, errors.New("error")
				}
				return []*trace.Span{
					{TraceId: "test-one", SpanId: "test-one" + "-span1"},
				}, nil
			})

			monkey.Patch(apis.GetHeader, func(ctx context.Context, key string) string {
				return "test"
			})

			esc := &ElasticsearchSource{}
			if got := esc.GetSpans(tt.args.ctx, tt.args.req); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetSpans() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestElasticsearchSource_sortConditionStrategy(t *testing.T) {
	type args struct {
		sort string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{"case1", args{sort: ""}, "ORDER BY start_time::field DESC"},
		{"case2", args{sort: "TRACE_TIME_DESC"}, "ORDER BY start_time::field DESC"},
		{"case3", args{sort: "TRACE_TIME_ASC"}, "ORDER BY start_time::field ASC"},
		{"case4", args{sort: "TRACE_DURATION_DESC"}, "ORDER BY trace_duration::field DESC"},
		{"case5", args{sort: "TRACE_DURATION_ASC"}, "ORDER BY trace_duration::field ASC"},
		{"case6", args{sort: "SPAN_COUNT_DESC"}, "ORDER BY span_count::field DESC"},
		{"case7", args{sort: "SPAN_COUNT_ASC"}, "ORDER BY span_count::field ASC"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			esc := ElasticsearchSource{}
			if got := esc.sortConditionStrategy(tt.args.sort); got != tt.want {
				t.Errorf("sortConditionStrategy() = %v, want %v", got, tt.want)
			}
		})
	}
}
