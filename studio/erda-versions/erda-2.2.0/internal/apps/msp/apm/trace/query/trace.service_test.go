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
	"context"
	"encoding/json"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/bmizerany/assert"
	uuid "github.com/satori/go.uuid"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/core/common"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/core/debug"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/core/query"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/db"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/source"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/storage"
)

//go:generate mockgen -destination=./mock_storage.go -package query -source=../storage/storage.go Storage
func Test_traceService_GetSpans(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetSpansRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetSpansResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.msp.apm.trace.TraceService",
		//			`
		//erda.msp.apm.trace:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.GetSpansRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.GetSpansResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.TraceServiceServer)
			got, err := srv.GetSpans(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("TraceService.GetSpans() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("TraceService.GetSpans() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_traceService_fetchSpanFromES(t *testing.T) {
	s1 := &trace.Span{
		TraceId:       "s1TraceId",
		SpanId:        "s1SpanId",
		ParentSpanId:  "s1ParentSpanId",
		OperationName: "s1OperationName",
		StartTime:     1,
		EndTime:       1,
		Tags:          map[string]string{"tagk.s1a": "tagv.s1a", "tagk.s1b": "tagv.s1b"},
	}
	ss := &listStorage{
		span: s1,
	}

	tests := []struct {
		name    string
		ctx     context.Context
		storage storage.Storage
		sel     storage.Selector
		forward bool
		limit   int
		want    []*trace.Span
	}{{
		"case 1",
		context.TODO(),
		ss,
		storage.Selector{TraceId: "s1TraceId"},
		true,
		1,
		[]*trace.Span{s1},
	},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got, err := source.FetchSpanFromES(tt.ctx, ss, tt.sel, tt.forward, tt.limit); !reflect.DeepEqual(got, tt.want) || err != nil {
				t.Errorf("fetchSpanFromES() = %v, want %v", got, tt.want)
			}
		})
	}

}

func Test_traceService_GetTraces(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetTracesRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetTracesResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.msp.apm.trace.TraceService",
		//			`
		//erda.msp.apm.trace:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.GetTracesRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.GetTracesResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.TraceServiceServer)
			got, err := srv.GetTraces(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("TraceService.GetTraces() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("TraceService.GetTraces() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_traceService_GetTraceQueryConditions(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetTraceQueryConditionsRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetTraceQueryConditionsResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.msp.apm.trace.TraceService",
		//			`
		//erda.msp.apm.trace:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.GetTraceQueryConditionsRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.GetTraceQueryConditionsResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.TraceServiceServer)
			got, err := srv.GetTraceQueryConditions(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("TraceService.GetTraceQueryConditions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("TraceService.GetTraceQueryConditions() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_traceService_GetTraceDebugHistories(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetTraceDebugHistoriesRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetTraceDebugHistoriesResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.msp.apm.trace.TraceService",
		//			`
		//erda.msp.apm.trace:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.GetTraceDebugHistoriesRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.GetTraceDebugHistoriesResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.TraceServiceServer)
			got, err := srv.GetTraceDebugHistories(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("TraceService.GetTraceDebugHistories() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("TraceService.GetTraceDebugHistories() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_traceService_GetTraceDebugByRequestID(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetTraceDebugRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetTraceDebugResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.msp.apm.trace.TraceService",
		//			`
		//erda.msp.apm.trace:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.GetTraceDebugRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.GetTraceDebugResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.TraceServiceServer)
			got, err := srv.GetTraceDebugByRequestID(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("TraceService.GetTraceDebugByRequestID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("TraceService.GetTraceDebugByRequestID() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_traceService_StopTraceDebug(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.StopTraceDebugRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.StopTraceDebugResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.msp.apm.trace.TraceService",
		//			`
		//erda.msp.apm.trace:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.StopTraceDebugRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.StopTraceDebugResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.TraceServiceServer)
			got, err := srv.StopTraceDebug(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("TraceService.StopTraceDebug() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("TraceService.StopTraceDebug() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_traceService_GetTraceDebugHistoryStatusByRequestID(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetTraceDebugStatusByRequestIDRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetTraceDebugStatusByRequestIDResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		//		{
		//			"case 1",
		//			"erda.msp.apm.trace.TraceService",
		//			`
		//erda.msp.apm.trace:
		//`,
		//			args{
		//				context.TODO(),
		//				&pb.GetTraceDebugStatusByRequestIDRequest{
		//					// TODO: setup fields
		//				},
		//			},
		//			&pb.GetTraceDebugStatusByRequestIDResponse{
		//				// TODO: setup fields.
		//			},
		//			false,
		//		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			hub := servicehub.New()
			events := hub.Events()
			go func() {
				hub.RunWithOptions(&servicehub.RunOptions{Content: tt.config})
			}()
			err := <-events.Started()
			if err != nil {
				t.Error(err)
				return
			}
			srv := hub.Service(tt.service).(pb.TraceServiceServer)
			got, err := srv.GetTraceDebugHistoryStatusByRequestID(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("TraceService.GetTraceDebugHistoryStatusByRequestID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("TraceService.GetTraceDebugHistoryStatusByRequestID() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_composeTraceRequestHistory(t *testing.T) {
	key := uuid.NewV4().String()
	req := pb.CreateTraceDebugRequest{
		Method:    "GET",
		Url:       "http://erda.cloud",
		Body:      "",
		Query:     map[string]string{},
		Header:    map[string]string{},
		ScopeID:   key,
		ProjectID: "1",
	}

	queryString, err := json.Marshal(req.Query)
	if err != nil {
		return
	}
	headerString, err := json.Marshal(req.Header)
	if err != nil {
		return
	}
	bodyValid := json.Valid([]byte(req.Body))
	if req.Body != "" && !bodyValid {
		return
	}
	if req.CreateTime == "" || req.UpdateTime == "" {
		req.CreateTime = time.Now().Format(common.Layout)
		req.UpdateTime = time.Now().Format(common.Layout)
	}
	createTime, err := time.ParseInLocation(common.Layout, req.CreateTime, time.Local)
	if err != nil {
		return
	}
	updateTime, err := time.ParseInLocation(common.Layout, req.UpdateTime, time.Local)
	if err != nil {
		return
	}

	key2 := uuid.NewV4().String()
	req2 := pb.CreateTraceDebugRequest{
		Method:    "GET",
		Url:       "http://erda.cloud",
		Body:      "{'name':'test'}",
		Query:     map[string]string{},
		Header:    map[string]string{},
		ScopeID:   key2,
		ProjectID: "1",
	}

	queryString2, err := json.Marshal(req2.Query)
	if err != nil {
		return
	}
	headerString2, err := json.Marshal(req2.Header)
	if err != nil {
		return
	}
	bodyValid2 := json.Valid([]byte(req2.Body))
	if req2.Body != "" && !bodyValid2 {
		return
	}
	if req2.CreateTime == "" || req2.UpdateTime == "" {
		req2.CreateTime = time.Now().Format(common.Layout)
		req2.UpdateTime = time.Now().Format(common.Layout)
	}
	createTime2, err := time.ParseInLocation(common.Layout, req2.CreateTime, time.Local)
	if err != nil {
		return
	}
	updateTime2, err := time.ParseInLocation(common.Layout, req2.UpdateTime, time.Local)
	if err != nil {
		return
	}

	key3 := uuid.NewV4().String()
	req3 := pb.CreateTraceDebugRequest{
		Method:    "GET",
		Url:       "http://erda.cloud",
		Body:      "{fd'namdfasdfe'fasdx:fadsf'test'ad}",
		Query:     map[string]string{},
		Header:    map[string]string{},
		ScopeID:   key3,
		ProjectID: "1",
	}

	queryString3, err := json.Marshal(req3.Query)
	if err != nil {
		return
	}
	headerString3, err := json.Marshal(req3.Header)
	if err != nil {
		return
	}
	bodyValid3 := json.Valid([]byte(req3.Body))
	if req3.Body != "" && !bodyValid3 {
		return
	}
	if req3.CreateTime == "" || req3.UpdateTime == "" {
		req3.CreateTime = time.Now().Format(common.Layout)
		req3.UpdateTime = time.Now().Format(common.Layout)
	}
	createTime3, err := time.ParseInLocation(common.Layout, req3.CreateTime, time.Local)
	if err != nil {
		return
	}
	updateTime3, err := time.ParseInLocation(common.Layout, req3.UpdateTime, time.Local)
	if err != nil {
		return
	}
	h := &db.TraceRequestHistory{
		TerminusKey:    req.ScopeID,
		Url:            req.Url,
		QueryString:    string(queryString),
		Header:         string(headerString),
		Body:           req.Body,
		Method:         req.Method,
		Status:         int(req.Status),
		ResponseBody:   req.ResponseBody,
		ResponseStatus: int(req.ResponseCode),
		CreateTime:     createTime,
		UpdateTime:     updateTime,
	}

	h2 := &db.TraceRequestHistory{
		TerminusKey:    req2.ScopeID,
		Url:            req2.Url,
		QueryString:    string(queryString2),
		Header:         string(headerString2),
		Body:           req2.Body,
		Method:         req2.Method,
		Status:         int(req2.Status),
		ResponseBody:   req2.ResponseBody,
		ResponseStatus: int(req2.ResponseCode),
		CreateTime:     createTime2,
		UpdateTime:     updateTime2,
	}

	h3 := &db.TraceRequestHistory{
		TerminusKey:    req3.ScopeID,
		Url:            req3.Url,
		QueryString:    string(queryString3),
		Header:         string(headerString3),
		Body:           req3.Body,
		Method:         req3.Method,
		Status:         int(req3.Status),
		ResponseBody:   req3.ResponseBody,
		ResponseStatus: int(req3.ResponseCode),
		CreateTime:     createTime3,
		UpdateTime:     updateTime3,
	}
	type args struct {
		req *pb.CreateTraceDebugRequest
	}
	tests := []struct {
		name    string
		args    args
		want    *db.TraceRequestHistory
		wantErr bool
	}{
		{"case-1", args{&req}, h, false},
		{"case-2", args{&req2}, h2, false},
		{"case-3", args{&req3}, h3, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := composeTraceRequestHistory(tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("composeTraceRequestHistory() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got == nil {
				t.Errorf("composeTraceRequestHistory() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			tt.want.RequestId = got.RequestId
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("composeTraceRequestHistory() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_bodyCheck(t *testing.T) {
	type args struct {
		body string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{"case1", args{body: ""}, true},
		{"case2", args{body: "{\"test\":\"test\"}"}, true},
		{"case3", args{body: "s{ss}sss"}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := bodyCheck(tt.args.body); got != tt.want {
				t.Errorf("bodyCheck() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_traceService_getDebugStatus(t *testing.T) {
	type fields struct {
		p                     *provider
		i18n                  i18n.Translator
		traceRequestHistoryDB *db.TraceRequestHistoryDB
	}
	type args struct {
		lang       i18n.LanguageCodes
		statusCode debug.Status
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   string
	}{
		{"case1", fields{p: nil, i18n: nil, traceRequestHistoryDB: nil}, args{lang: nil, statusCode: debug.Success}, ""},
		{"case2", fields{p: nil, i18n: nil, traceRequestHistoryDB: nil}, args{lang: nil, statusCode: debug.Init}, ""},
		{"case3", fields{p: nil, i18n: nil, traceRequestHistoryDB: nil}, args{lang: nil, statusCode: debug.Fail}, ""},
		{"case4", fields{p: nil, i18n: nil, traceRequestHistoryDB: nil}, args{lang: nil, statusCode: debug.Stop}, ""},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &TraceService{
				p:                     tt.fields.p,
				i18n:                  tt.fields.i18n,
				traceRequestHistoryDB: tt.fields.traceRequestHistoryDB,
			}
			if got := s.getDebugStatus(tt.args.lang, tt.args.statusCode); got != tt.want {
				t.Errorf("getDebugStatus() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getSpanProcessAnalysisDashboard(t *testing.T) {
	type args struct {
		metricType string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{"case1", args{metricType: "jvm_memory"}, "span_process_analysis_java"},
		{"case2", args{metricType: "nodejs_memory"}, "span_process_analysis_nodejs"},
		{"case3", args{metricType: "xxx"}, ""},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getSpanProcessAnalysisDashboard(tt.args.metricType); got != tt.want {
				t.Errorf("getSpanProcessAnalysisDashboard() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_traceService_getSpanCallAnalysis(t *testing.T) {
	type fields struct {
		p                     *provider
		i18n                  i18n.Translator
		traceRequestHistoryDB *db.TraceRequestHistoryDB
	}
	type args struct {
		ctx context.Context
		req *pb.GetSpanDashboardsRequest
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   string
	}{
		{"case1", fields{}, args{req: &pb.GetSpanDashboardsRequest{Type: "http_client"}}, "span_call_analysis_http_client"},
		{"case2", fields{}, args{req: &pb.GetSpanDashboardsRequest{Type: "http_server"}}, "span_call_analysis_http_client"},
		{"case3", fields{}, args{req: &pb.GetSpanDashboardsRequest{Type: "rpc_client"}}, "span_call_analysis_rpc_client"},
		{"case4", fields{}, args{req: &pb.GetSpanDashboardsRequest{Type: "rpc_server"}}, "span_call_analysis_rpc_server"},
		{"case5", fields{}, args{req: &pb.GetSpanDashboardsRequest{Type: "mq_producer"}}, "span_call_analysis_mq_producer"},
		{"case6", fields{}, args{req: &pb.GetSpanDashboardsRequest{Type: "mq_consumer"}}, "span_call_analysis_mq_consumer"},
		{"case7", fields{}, args{req: &pb.GetSpanDashboardsRequest{Type: "cache_client"}}, "span_call_analysis_cache_client"},
		{"case8", fields{}, args{req: &pb.GetSpanDashboardsRequest{Type: "invoke_local"}}, "span_call_analysis_invoke_local"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &TraceService{
				p:                     tt.fields.p,
				i18n:                  tt.fields.i18n,
				traceRequestHistoryDB: tt.fields.traceRequestHistoryDB,
			}
			got, err := s.getSpanCallAnalysis(tt.args.ctx, tt.args.req)
			if (err != nil) && got.DashboardID != tt.want {
				t.Errorf("getSpanCallAnalysis() error = %v, wantErr %v", err, tt.want)
				return
			}
		})
	}
}

func Test_traceService_handleSpanResponse(t *testing.T) {
	spans := []*pb.Span{
		{
			Id:            "34035630-b97a-4f6e-b9e8-a1ca6ec32127",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Producer/Sync/apm-demo-topic-sync-parallel",
			StartTime:     1631515503925000000,
			EndTime:       1631515503927000000,
			ParentSpanId:  "5f3fd322-e4bd-4117-8449-5027cf49ef56",
		},
		{
			Id:            "fc494cf9-2811-453f-bd9e-99b1d865b7c2",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Producer/Async/apm-demo-topic-async-parallel",
			StartTime:     1631515503928000000,
			EndTime:       1631515503932000000,
			ParentSpanId:  "5f3fd322-e4bd-4117-8449-5027cf49ef56",
		},
		{
			Id:            "14eaee91-8064-41e7-9bcb-f9e69f92d1df",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Producer/Sync/apm-demo-topic-sync-order",
			StartTime:     1631515503927000000,
			EndTime:       1631515503928000000,
			ParentSpanId:  "5f3fd322-e4bd-4117-8449-5027cf49ef56",
		},
		{
			Id:            "173e9b4f-8cc9-4d26-9914-637d8a12ba83",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Consumer/apm-demo-topic",
			StartTime:     1631515503934000000,
			EndTime:       1631515503934000000,
			ParentSpanId:  "9548c236-90b0-48ac-8fb5-75100c76e129",
		},
		{
			Id:            "fe925605-78a9-409d-a506-2c6c8aecdfc2",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Consumer/apm-demo-topic-sync-parallel",
			StartTime:     1631515503930000000,
			EndTime:       1631515503930000000,
			ParentSpanId:  "34035630-b97a-4f6e-b9e8-a1ca6ec32127",
		},
		{
			Id:            "97a6d79b-c7d8-4818-83de-47fad413dcf4",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Producer/Async/apm-demo-topic-async-order",
			StartTime:     1631515503929000000,
			EndTime:       1631515503938000000,
			ParentSpanId:  "5f3fd322-e4bd-4117-8449-5027cf49ef56",
		},
		{
			Id:            "9548c236-90b0-48ac-8fb5-75100c76e129",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Producer/Sync/apm-demo-topic",
			StartTime:     1631515503923000000,
			EndTime:       1631515503925000000,
			ParentSpanId:  "5f3fd322-e4bd-4117-8449-5027cf49ef56",
		}, {

			Id:            "5f3fd322-e4bd-4117-8449-5027cf49ef56",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "GET http://ss-api.test.terminus.io/api/rocketmq/send",
			StartTime:     1631515503922000000,
			EndTime:       1631515503929000000,
			ParentSpanId:  "",
		},
		{
			Id:            "7aa7a7fc-9756-4b3a-b0b2-ecac8230242f",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Consumer/apm-demo-topic-async-order",
			StartTime:     1631515503940000000,
			EndTime:       1631515503940000000,
			ParentSpanId:  "97a6d79b-c7d8-4818-83de-47fad413dcf4",
		},
		{
			Id:            "564acae7-f22f-40ed-9049-a5c6a62d59d2",
			TraceId:       "95606e4b-92da-4f9e-8f64-4e241e30157f",
			OperationName: "RocketMQ/Consumer/apm-demo-topic-async-parallel",
			StartTime:     1631515503935000000,
			EndTime:       1631515503935000000,
			ParentSpanId:  "fc494cf9-2811-453f-bd9e-99b1d865b7c2",
		},
	}

	tree := map[string]*pb.Span{}
	for _, v := range spans {
		tree[v.Id] = v
	}
	type fields struct {
		p                     *provider
		i18n                  i18n.Translator
		traceRequestHistoryDB *db.TraceRequestHistoryDB
	}
	type args struct {
		spanTree query.SpanTree
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   *pb.GetSpansResponse
	}{
		{"case1", fields{}, args{spanTree: tree}, nil},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &TraceService{
				p:                     tt.fields.p,
				i18n:                  tt.fields.i18n,
				traceRequestHistoryDB: tt.fields.traceRequestHistoryDB,
			}
			got, _ := s.handleSpanResponse(tt.args.spanTree)
			if got.ServiceCount != 1 {
				t.Errorf("handleSpanResponse() got = %v, want %v", got.ServiceCount, 1)
			}

			for _, span := range got.Spans {
				if span.SelfDuration < 0 || span.Duration < 0 {
					t.Errorf("duration (%v) or selfDuration (%v) < 0", span.Duration, span.SelfDuration)
				}
			}
		})
	}
}

func Test_traceService_GetSpanEvents(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.SpanEventRequest
	}
	tests := []struct {
		name     string
		args     args
		wantResp *pb.SpanEventResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		{
			"case 1",
			args{
				context.TODO(),
				&pb.SpanEventRequest{
					// TODO: setup fields
				},
			},
			&pb.SpanEventResponse{
				SpanEvents: []*pb.SpanEvent{
					{
						Timestamp: 1634875807541,
						Events: map[string]string{
							"event": "server send",
						},
					},
				},
			},
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var metricq *metricpb.UnimplementedMetricServiceServer
			monkey.PatchInstanceMethod(reflect.TypeOf(metricq), "QueryWithTableFormat", func(*metricpb.UnimplementedMetricServiceServer, context.Context, *metricpb.QueryWithTableFormatRequest) (*metricpb.QueryWithTableFormatResponse, error) {
				return &metricpb.QueryWithTableFormatResponse{
					Data: &metricpb.TableResult{
						Cols: []*metricpb.TableColumn{
							{Key: "event::tag", Name: "tags.event", Flag: "tag"},
							{Key: "service::tag", Name: "tags.service", Flag: "tag"},
							{Key: "timestamp", Name: "timestamp", Flag: "timestamp"},
						},
						Data: []*metricpb.TableRow{
							{
								Values: map[string]*structpb.Value{
									"event::tag":   structpb.NewStringValue("server send"),
									"service::tag": structpb.NewStringValue("service1"),
									"timestamp":    structpb.NewNumberValue(1634875807541),
								},
							},
						},
					},
				}, nil
			})
			s := &TraceService{
				p:                     &provider{Metric: metricq},
				i18n:                  nil,
				traceRequestHistoryDB: nil,
			}
			got, err := s.GetSpanEvents(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("TraceService.GetSpanEvents() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("TraceService.GetSpanEvents() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_traceService_EventFieldSet(t *testing.T) {
	tests := []struct {
		name     string
		tag      string
		wantResp bool
	}{
		{
			"event",
			"event",
			true,
		},
		{
			"other",
			"service",
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			has := EventFieldSet.Contains(tt.tag)
			assert.Equal(t, has, tt.wantResp)
		})
	}
}

func Test_traceService_getSpanEventQueryTime(t *testing.T) {
	type args struct {
		req *pb.SpanEventRequest
	}
	now := time.Now()
	tests := []struct {
		name          string
		service       string
		args          args
		wantStartTime int64
		wantEndTime   int64
	}{
		{
			"startTime 0",
			"erda.msp.apm.trace.TraceService",
			args{
				&pb.SpanEventRequest{StartTime: now.UnixNano() / 1e6, SpanID: ""},
			},
			now.Add(-time.Minute*15).UnixNano() / 1e6,
			now.Add(time.Minute*15).UnixNano() / 1e6,
		},
		{
			"startTime 1634875807541",
			"erda.msp.apm.trace.TraceService",
			args{
				&pb.SpanEventRequest{StartTime: 1634875807541, SpanID: ""},
			},
			1634875807541 - int64((time.Minute*15)/time.Millisecond),
			1634875807541 + int64((time.Minute*15)/time.Millisecond),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &TraceService{
				p:                     nil,
				i18n:                  nil,
				traceRequestHistoryDB: nil,
			}
			start, end := s.getSpanEventQueryTime(tt.args.req)
			assert.Equal(t, start, tt.wantStartTime)
			assert.Equal(t, end, tt.wantEndTime)
		})
	}
}

func Test_traceService_handleSpanEventResponse(t *testing.T) {
	table := &metricpb.TableResult{
		Cols: []*metricpb.TableColumn{
			{Key: "event::tag", Name: "tags.event", Flag: "tag"},
			{Key: "service::tag", Name: "tags.service", Flag: "tag"},
			{Key: "timestamp", Name: "timestamp", Flag: "timestamp"},
		},
		Data: []*metricpb.TableRow{
			{
				Values: map[string]*structpb.Value{
					"event::tag":   structpb.NewStringValue("server send"),
					"service::tag": structpb.NewStringValue("service1"),
					"timestamp":    structpb.NewNumberValue(1634875807541),
				},
			},
		},
	}
	type args struct {
		req *metricpb.TableResult
	}
	tests := []struct {
		name     string
		args     args
		wantResp []*pb.SpanEvent
	}{
		{
			"case1",
			args{
				table,
			},
			[]*pb.SpanEvent{
				{
					Timestamp: 1634875807541,
					Events: map[string]string{
						"event": "server send",
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &TraceService{
				p:                     nil,
				i18n:                  nil,
				traceRequestHistoryDB: nil,
			}
			events := s.handleSpanEventResponse(tt.args.req)
			assert.Equal(t, len(events), len(tt.wantResp))
			assert.Equal(t, events[0].Timestamp, tt.wantResp[0].Timestamp)
		})
	}
}

func Test_traceService_CreateTraceDebug(t *testing.T) {

	type args struct {
		ctx context.Context
		req *pb.CreateTraceDebugRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.CreateTraceDebugRequest{}}, true},
		{"case2", args{req: &pb.CreateTraceDebugRequest{Url: "http://localhost:8080"}}, true},
		{"case3", args{req: &pb.CreateTraceDebugRequest{Url: "http://localhost:8080", ScopeID: "xx"}}, true},
		{"case4", args{req: &pb.CreateTraceDebugRequest{Url: "xxxxx", ScopeID: "xx", Method: "GET"}}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &TraceService{}
			_, err := s.CreateTraceDebug(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("CreateTraceDebug() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_calculateDepth(t *testing.T) {
	type args struct {
		spanTree query.SpanTree
	}
	tests := []struct {
		name string
		args args
		want int64
	}{
		{name: "case1", args: args{spanTree: query.SpanTree{
			"4f861481-0c15-4664-bce1-1be574270b8c": &pb.Span{Id: "4f861481-0c15-4664-bce1-1be574270b8c", ParentSpanId: "8b3a6fd6-5089-41c6-a4af-ce71e0b2a64f"},
			"682cddc0-2b87-4927-8ee1-8cde1aa0c65c": &pb.Span{Id: "682cddc0-2b87-4927-8ee1-8cde1aa0c65c", ParentSpanId: "3cf58797-d350-4ad8-8486-2f19fb1a0bf5"},
			"1ca09d00-c6c1-4f9a-ae0b-a633b623a8c4": &pb.Span{Id: "1ca09d00-c6c1-4f9a-ae0b-a633b623a8c4", ParentSpanId: "498bb75f-8edd-4a79-a3dd-31ebeaa60ee4"},
			"8b3a6fd6-5089-41c6-a4af-ce71e0b2a64f": &pb.Span{Id: "8b3a6fd6-5089-41c6-a4af-ce71e0b2a64f", ParentSpanId: "498bb75f-8edd-4a79-a3dd-31ebeaa60ee4"},
			"8cb8602c-26d7-4c6d-96a1-8a620479c21c": &pb.Span{Id: "8cb8602c-26d7-4c6d-96a1-8a620479c21c", ParentSpanId: "498bb75f-8edd-4a79-a3dd-31ebeaa60ee4"},
			"498bb75f-8edd-4a79-a3dd-31ebeaa60ee4": &pb.Span{Id: "498bb75f-8edd-4a79-a3dd-31ebeaa60ee4", ParentSpanId: "                                    "},
			"86d28c98-1399-4bf6-b214-dc3c8e1f413f": &pb.Span{Id: "86d28c98-1399-4bf6-b214-dc3c8e1f413f", ParentSpanId: "610b655f-ba97-4725-8c3f-e51f8430ac02"},
			"52e75fdf-6d2c-48af-954c-c6b05b2c6cf2": &pb.Span{Id: "52e75fdf-6d2c-48af-954c-c6b05b2c6cf2", ParentSpanId: "566630e3-b0b1-4f76-ae6d-c53c62a5aa23"},
			"9008cddd-cade-489b-a731-550bbfe50cff": &pb.Span{Id: "9008cddd-cade-489b-a731-550bbfe50cff", ParentSpanId: "6f7f569a-592f-43e7-be69-aaf8a1a2fa63"},
			"566630e3-b0b1-4f76-ae6d-c53c62a5aa23": &pb.Span{Id: "566630e3-b0b1-4f76-ae6d-c53c62a5aa23", ParentSpanId: "6f7f569a-592f-43e7-be69-aaf8a1a2fa63"},
			"6f7f569a-592f-43e7-be69-aaf8a1a2fa63": &pb.Span{Id: "6f7f569a-592f-43e7-be69-aaf8a1a2fa63", ParentSpanId: "                                    "},
		}}, want: 3},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			depth := int64(0)
			if len(tt.args.spanTree) > 0 {
				depth = int64(1)
			}
			for _, span := range tt.args.spanTree {
				if span.ParentSpanId == span.Id {
					span.ParentSpanId = ""
				}
				tempDepth := int64(1)
				tempDepth = calculateDepth(tempDepth, span, tt.args.spanTree)
				if tempDepth > depth {
					depth = tempDepth
				}
			}
			if depth != tt.want {
				t.Errorf("This trace depth is %v, want %v", depth, tt.want)
				return
			}
		})
	}
}

func TestTraceService_GetSpanCount(t *testing.T) {
	type fields struct {
		p                     *provider
		i18n                  i18n.Translator
		traceRequestHistoryDB *db.TraceRequestHistoryDB
		Source                source.TraceSource
	}
	type args struct {
		ctx     context.Context
		traceID string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    int64
		wantErr bool
	}{
		{"case1", fields{Source: &source.ElasticsearchSource{}}, args{}, 1, false},
		{"case2", fields{Source: &source.CassandraSource{}}, args{}, 1, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			monkey.UnpatchAll()
			s := &TraceService{Source: tt.fields.Source}
			esc := &source.ElasticsearchSource{}
			monkey.PatchInstanceMethod(reflect.TypeOf(esc), "GetSpanCount", func(ecs *source.ElasticsearchSource, ctx context.Context, traceID string) int64 {
				return 1
			})

			cs := &source.CassandraSource{}
			monkey.PatchInstanceMethod(reflect.TypeOf(cs), "GetSpanCount", func(ecs *source.CassandraSource, ctx context.Context, traceID string) int64 {
				return 1
			})

			got, err := s.GetSpanCount(tt.args.ctx, tt.args.traceID)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetSpanCount() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GetSpanCount() got = %v, want %v", got, tt.want)
			}
		})
	}
}
