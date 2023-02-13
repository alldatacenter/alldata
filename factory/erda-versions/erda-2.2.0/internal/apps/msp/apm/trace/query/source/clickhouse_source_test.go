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
	"time"

	"github.com/ClickHouse/clickhouse-go/v2/lib/driver"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/providers/clickhouse"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/commom/custom"

	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
)

func Test_mergeAsSpan(t *testing.T) {
	type args struct {
		cs  trace.Series
		sms []trace.Meta
	}
	tests := []struct {
		name string
		args args
		want *pb.Span
	}{
		{
			args: args{
				cs: trace.Series{
					SpanId:       "aaa",
					TraceId:      "bbb",
					StartTime:    2,
					EndTime:      2,
					ParentSpanId: "ppp",
					Tags: map[string]string{
						"db_statement": "select * from abc where id=aaa",
					},
				},
				sms: []trace.Meta{
					{
						Key:   "operation_name",
						Value: "query",
					},
					{
						Key:   "org_name",
						Value: "erda",
					},
				},
			},
			want: &pb.Span{
				Id:            "aaa",
				TraceId:       "bbb",
				ParentSpanId:  "ppp",
				StartTime:     2,
				EndTime:       2,
				OperationName: "query",
				Tags: map[string]string{
					"db_statement":   "select * from abc where id=aaa",
					"org_name":       "erda",
					"operation_name": "query",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := mergeAsSpan(tt.args.cs, tt.args.sms); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("mergeAsSpan() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_GetInterval(t *testing.T) {
	type args struct {
		duration int64
	}
	tests := []struct {
		name string
		args args
		want int64
	}{
		{"case1", args{duration: time.Hour.Milliseconds()}, 1},
		{"case2", args{duration: time.Hour.Milliseconds() * 3}, 3},
		{"case3", args{duration: time.Hour.Milliseconds() * 6}, 6},
		{"case4", args{duration: time.Hour.Milliseconds() * 12}, 12},
		{"case5", args{duration: time.Hour.Milliseconds() * 24}, 24},
		{"case6", args{duration: time.Hour.Milliseconds() * 72}, 72},
		{"case7", args{duration: time.Hour.Milliseconds() * 168}, 168},
		{"case8", args{duration: time.Hour.Milliseconds() * 168 * 3}, 12},
		{"case9", args{duration: time.Hour.Milliseconds() * 168 * 5}, 36},
		{"case10", args{duration: time.Hour.Milliseconds() * 168 * 10}, 36},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			got, _, _ := GetInterval(tt.args.duration)
			if got != tt.want {
				t.Errorf("GetInterval() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestClickhouseSource_sortConditionStrategy(t *testing.T) {
	type args struct {
		sort string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{"case1", args{sort: ""}, "ORDER BY min_start_time DESC"},
		{"case2", args{sort: "TRACE_TIME_DESC"}, "ORDER BY min_start_time DESC"},
		{"case3", args{sort: "TRACE_TIME_ASC"}, "ORDER BY min_start_time ASC"},
		{"case4", args{sort: "TRACE_DURATION_DESC"}, "ORDER BY duration DESC"},
		{"case5", args{sort: "TRACE_DURATION_ASC"}, "ORDER BY duration ASC"},
		{"case6", args{sort: "SPAN_COUNT_DESC"}, "ORDER BY span_count DESC"},
		{"case7", args{sort: "SPAN_COUNT_ASC"}, "ORDER BY span_count ASC"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			chs := &ClickhouseSource{}
			if got := chs.sortConditionStrategy(tt.args.sort); got != tt.want {
				t.Errorf("sortConditionStrategy() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestClickhouseSource_composeFilter(t *testing.T) {
	type args struct {
		req *pb.GetTracesRequest
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{"case1", args{req: &pb.GetTracesRequest{TenantID: "test_tenant"}}, "SELECT distinct(series_id) FROM monitor.spans_meta_all WHERE (series_id in (select distinct(series_id) from monitor.spans_meta where (key = 'terminus_key' AND value = 'test_tenant'))) "},
		{"case2", args{req: &pb.GetTracesRequest{TenantID: "test_tenant", ServiceName: "test_service_name"}}, "SELECT distinct(series_id) FROM monitor.spans_meta_all WHERE (series_id in (select distinct(series_id) from monitor.spans_meta where (key = 'terminus_key' AND value = 'test_tenant'))) AND (series_id in (select distinct(series_id) from monitor.spans_meta where (key='service_name' AND value LIKE concat('%','test_service_name','%')))) "},
		{"case3", args{req: &pb.GetTracesRequest{TenantID: "test_tenant", RpcMethod: "hello()"}}, "SELECT distinct(series_id) FROM monitor.spans_meta_all WHERE (series_id in (select distinct(series_id) from monitor.spans_meta where (key = 'terminus_key' AND value = 'test_tenant'))) AND (series_id in (select distinct(series_id) from monitor.spans_meta where (key='rpc_method' AND value LIKE concat('%','hello()','%')))) "},
		// {"case4", args{req: &pb.GetTracesRequest{TenantID: "test_tenant", HttpPath: "/hello"}}, "SELECT distinct(series_id) FROM monitor.spans_meta_all WHERE (series_id in (select distinct(series_id) from monitor.spans_meta where (key = 'terminus_key' AND value = 'test_tenant'))) AND (series_id in (select distinct(series_id) from monitor.spans_meta where (key='http_path' AND value LIKE concat('%','/hello','%')))) "},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			chs := &ClickhouseSource{}
			assert.Equal(t, tt.want, chs.composeFilter(tt.args.req))
		})
	}
}

func TestClickhouseSource_sortConditionStrategy1(t *testing.T) {
	type args struct {
		sort string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			args: args{sort: "span_count_desc"},
			want: "ORDER BY span_count DESC",
		},
		{
			args: args{sort: "span_count_asc"},
			want: "ORDER BY span_count ASC",
		},
		{
			args: args{sort: "trace_duration_desc"},
			want: "ORDER BY duration DESC",
		},
		{
			args: args{sort: "trace_duration_asc"},
			want: "ORDER BY duration ASC",
		},
		{
			args: args{sort: "trace_time_desc"},
			want: "ORDER BY min_start_time DESC",
		},
		{
			args: args{sort: "trace_time_asc"},
			want: "ORDER BY min_start_time ASC",
		},
		{
			args: args{sort: "xxx"},
			want: "ORDER BY min_start_time DESC",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			chs := &ClickhouseSource{}
			assert.Equal(t, tt.want, chs.sortConditionStrategy(tt.args.sort))
		})
	}
}

func Test_convertToMetas(t *testing.T) {
	type args struct {
		kvs keysValues
	}
	tests := []struct {
		name string
		args args
		want []trace.Meta
	}{
		{
			args: args{kvs: keysValues{
				Keys:     []string{"hello", "hello2"},
				Values:   []string{"world", "world2"},
				SeriesID: 1024,
			}},
			want: []trace.Meta{
				{
					Key:   "hello",
					Value: "world",
				},
				{
					Key:   "hello2",
					Value: "world2",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := convertToMetas(tt.args.kvs); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("convertToMetas() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestClickhouseSource_GetSpans(t *testing.T) {
	chs := ClickhouseSource{
		CompatibleSource: &mockCS{},
		Clickhouse:       &mockclickhouseInf{cli: &mockconn{}},
	}
	spans := chs.GetSpans(context.TODO(), &pb.GetSpansRequest{})
	ass := assert.New(t)
	ass.Equal(1, len(spans))

	chs = ClickhouseSource{
		Clickhouse: &mockclickhouseInf{cli: &mockconn{}},
	}
	spans = chs.GetSpans(context.TODO(), &pb.GetSpansRequest{})
	ass.Equal(0, len(spans))
}

type mockCS struct {
}

func (m *mockCS) GetSpans(ctx context.Context, req *pb.GetSpansRequest) []*pb.Span {
	return []*pb.Span{{}}
}

func (m *mockCS) GetSpanCount(ctx context.Context, traceID string) int64 {
	return 0
}

func (m *mockCS) GetTraceReqDistribution(ctx context.Context, model custom.Model) ([]*TraceDistributionItem, error) {
	return nil, nil
}

func (m *mockCS) GetTraces(ctx context.Context, req *pb.GetTracesRequest) (*pb.GetTracesResponse, error) {
	return nil, nil
}

type mockclickhouseInf struct {
	cli driver.Conn
}

func (m *mockclickhouseInf) Client() driver.Conn {
	return m.cli
}

func (m mockclickhouseInf) NewWriter(opts *clickhouse.WriterOptions) *clickhouse.Writer {
	return nil
}

type mockconn struct {
}

func (m mockconn) Contributors() []string {
	return nil
}

func (m mockconn) ServerVersion() (*driver.ServerVersion, error) {
	return nil, nil
}

func (m mockconn) Select(ctx context.Context, dest interface{}, query string, args ...interface{}) error {
	return nil
}

type mockRows struct {
}

func (m mockRows) Next() bool {
	return false
}

func (m mockRows) Scan(dest ...interface{}) error {
	return nil
}

func (m mockRows) ScanStruct(dest interface{}) error {
	return nil
}

func (m mockRows) ColumnTypes() []driver.ColumnType {
	return nil
}

func (m mockRows) Totals(dest ...interface{}) error {
	return nil
}

func (m mockRows) Columns() []string {
	return nil
}

func (m mockRows) Close() error {
	return nil
}

func (m mockRows) Err() error {
	return nil
}

func (m mockconn) Query(ctx context.Context, query string, args ...interface{}) (driver.Rows, error) {
	return &mockRows{}, nil
}

func (m mockconn) QueryRow(ctx context.Context, query string, args ...interface{}) driver.Row {
	return nil
}

func (m mockconn) PrepareBatch(ctx context.Context, query string) (driver.Batch, error) {
	return nil, nil
}

func (m mockconn) Exec(ctx context.Context, query string, args ...interface{}) error {
	return nil
}

func (m mockconn) AsyncInsert(ctx context.Context, query string, wait bool) error {
	return nil
}

func (m mockconn) Ping(ctx context.Context) error {
	return nil
}

func (m mockconn) Stats() driver.Stats {
	return driver.Stats{}
}

func (m mockconn) Close() error {
	return nil
}
