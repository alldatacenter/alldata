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
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/asaskevich/govalidator"
	uuid "github.com/satori/go.uuid"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/pkg/set"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/core/common"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/core/debug"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/core/query"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/db"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/commom/custom"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/source"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
)

type TraceService struct {
	p                     *provider
	i18n                  i18n.Translator
	traceRequestHistoryDB *db.TraceRequestHistoryDB
	Source                source.TraceSource
	CompatibleSource      source.TraceSource
}

var EventFieldSet = set.NewSet("error", "stack", "event", "message", "error_kind", "error_object")

func (s *TraceService) getDebugStatus(lang i18n.LanguageCodes, statusCode debug.Status) string {
	if lang == nil {
		return ""
	}
	switch statusCode {
	case debug.Init:
		return s.i18n.Text(lang, "waiting_for_tracing_data")
	case debug.Success:
		return s.i18n.Text(lang, "success_get_tracing_data")
	case debug.Fail:
		return s.i18n.Text(lang, "fail_get_tracing_data")
	case debug.Stop:
		return s.i18n.Text(lang, "stop_get_tracing_data")
	default:
		return ""
	}
}

func (s *TraceService) GetSpans(ctx context.Context, req *pb.GetSpansRequest) (*pb.GetSpansResponse, error) {
	if req.TraceID == "" || req.ScopeID == "" {
		return nil, errors.NewMissingParameterError("traceId or scopeId")
	}
	if req.Limit <= 0 || req.Limit > 10000 {
		req.Limit = 10000
	}
	spanTree := make(query.SpanTree)
	spans := s.Source.GetSpans(ctx, req)
	sort.Sort(Spans(spans))
	for _, span := range spans {
		if len(spanTree) >= int(req.GetLimit()) {
			break
		}
		spanTree[span.Id] = span
	}

	response, err := s.handleSpanResponse(spanTree)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return response, nil
}

type Spans []*pb.Span

func (s Spans) Len() int      { return len(s) }
func (s Spans) Swap(i, j int) { s[i], s[j] = s[j], s[i] }
func (s Spans) Less(i, j int) bool {
	return s[i].StartTime < s[j].StartTime
}

func getSpanProcessAnalysisDashboard(metricType string) string {
	switch metricType {
	case query.JavaMemoryMetricName:
		return "span_process_analysis_java"
	case query.NodeJsMemoryMetricName:
		return "span_process_analysis_nodejs"
	default:
		return ""
	}
}

func (s *TraceService) getServiceInstanceType(startTime, endTime int64, tenantId, serviceInstanceId string) (string, error) {

	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()

	for _, metricType := range query.ProcessMetrics {
		statement := fmt.Sprintf("SELECT terminus_key::tag FROM %s WHERE terminus_key=$terminus_key "+
			"AND service_instance_id=$service_instance_id LIMIT 1", metricType)
		queryParams := map[string]*structpb.Value{
			"terminus_key":        structpb.NewStringValue(tenantId),
			"service_instance_id": structpb.NewStringValue(serviceInstanceId),
		}

		request := &metricpb.QueryWithInfluxFormatRequest{
			Start:     strconv.FormatInt(startTime, 10),
			End:       strconv.FormatInt(endTime, 10),
			Statement: statement,
			Params:    queryParams,
		}

		response, err := s.p.Metric.QueryWithInfluxFormat(ctx, request)
		if err != nil {
			return "", errors.NewInternalServerError(err)
		}

		rows := response.Results[0].Series[0].Rows
		if len(rows) == 1 {
			return metricType, nil
		}
	}
	return "", nil
}

func (s *TraceService) getSpanServiceAnalysis(ctx context.Context, req *pb.GetSpanDashboardsRequest) (*pb.SpanAnalysis, error) {
	instanceType, err := s.getServiceInstanceType(req.StartTime, req.EndTime, req.TenantID, req.ServiceInstanceID)
	if err != nil {
		return nil, err
	}
	dashboardId := getSpanProcessAnalysisDashboard(instanceType)

	return &pb.SpanAnalysis{
		DashboardID: dashboardId,
		Conditions:  []string{"service_instance_id"},
	}, nil
}

func (s *TraceService) getSpanCallAnalysis(ctx context.Context, req *pb.GetSpanDashboardsRequest) (*pb.SpanAnalysis, error) {
	switch req.Type {
	case strings.ToLower(pb.SpanType_HTTP_CLIENT.String()):
		return &pb.SpanAnalysis{DashboardID: common.CallAnalysisHttpClient, Conditions: []string{"http_path", "http_method", "source_service_id"}}, nil
	case strings.ToLower(pb.SpanType_HTTP_SERVER.String()):
		return &pb.SpanAnalysis{DashboardID: common.CallAnalysisHttpServer, Conditions: []string{"http_path", "http_method", "target_service_id"}}, nil
	case strings.ToLower(pb.SpanType_RPC_CLIENT.String()):
		return &pb.SpanAnalysis{DashboardID: common.CallAnalysisRpcClient, Conditions: []string{"dubbo_service", "dubbo_method", "source_service_id"}}, nil
	case strings.ToLower(pb.SpanType_RPC_SERVER.String()):
		return &pb.SpanAnalysis{DashboardID: common.CallAnalysisRpcServer, Conditions: []string{"dubbo_service", "dubbo_method", "target_service_id"}}, nil
	case strings.ToLower(pb.SpanType_CACHE_CLIENT.String()):
		return &pb.SpanAnalysis{DashboardID: common.CallAnalysisCacheClient, Conditions: []string{"db_statement", "source_service_id"}}, nil
	case strings.ToLower(pb.SpanType_MQ_PRODUCER.String()):
		return &pb.SpanAnalysis{DashboardID: common.CallAnalysisMqProducer, Conditions: []string{"span_kind", "target_service_id", "message_bus_destination"}}, nil
	case strings.ToLower(pb.SpanType_MQ_CONSUMER.String()):
		return &pb.SpanAnalysis{DashboardID: common.CallAnalysisMqConsumer, Conditions: []string{"span_kind", "source_service_id", "message_bus_destination"}}, nil
	case strings.ToLower(pb.SpanType_INVOKE_LOCAL.String()):
		return &pb.SpanAnalysis{DashboardID: common.CallAnalysisInvokeLocal, Conditions: []string{"service_id"}}, nil
	default:
		return nil, errors.NewNotFoundError(fmt.Sprintf("span type (%s)", req.Type))
	}
}

func (s *TraceService) GetSpanDashboards(ctx context.Context, req *pb.GetSpanDashboardsRequest) (*pb.GetSpanDashboardsResponse, error) {
	// call details analysis
	callAnalysis, err := s.getSpanCallAnalysis(ctx, req)
	if err != nil {
		s.p.Log.Error(err)
	}
	// relate service analysis
	serviceAnalysis, err := s.getSpanServiceAnalysis(ctx, req)
	if err != nil {
		s.p.Log.Error(err)
	}
	return &pb.GetSpanDashboardsResponse{
		CallAnalysis:    callAnalysis,
		ServiceAnalysis: serviceAnalysis,
	}, nil
}

func (s *TraceService) handleSpanResponse(spanTree query.SpanTree) (*pb.GetSpansResponse, error) {
	var (
		spans          []*pb.Span
		traceStartTime int64 = 0
		traceEndTime   int64 = 0
		depth          int64 = 0
	)

	spanCount := int64(len(spanTree))
	if spanCount > 0 {
		depth = 1
	}
	services := map[string]common.Void{}
	for id, span := range spanTree {
		services[span.Tags["service_name"]] = common.Void{}
		if span.ParentSpanId == span.Id {
			span.ParentSpanId = ""
		}
		tempDepth := int64(1)
		tempDepth = calculateDepth(tempDepth, span, spanTree)
		if tempDepth > depth {
			depth = tempDepth
		}
		if traceStartTime == 0 || traceStartTime > span.StartTime {
			traceStartTime = span.StartTime
		}
		if traceEndTime == 0 || traceEndTime < span.EndTime {
			traceEndTime = span.EndTime
		}
		span.Duration = positiveInt64(span.EndTime - span.StartTime)
		span.SelfDuration = positiveInt64(span.Duration - childSpanDuration(id, spanTree))
		spans = append(spans, span)
	}

	serviceCount := int64(len(services))

	return &pb.GetSpansResponse{Spans: spans, ServiceCount: serviceCount, Depth: depth, Duration: positiveInt64(traceEndTime - traceStartTime), SpanCount: spanCount}, nil
}

func positiveInt64(v int64) int64 {
	if v > 0 {
		return v
	}
	return 0
}

func childSpanDuration(id string, spanTree query.SpanTree) int64 {
	duration := int64(0)
	for _, span := range spanTree {
		if span.ParentSpanId == id {
			duration += positiveInt64(span.EndTime - span.StartTime)
		}
	}
	return duration
}

func calculateDepth(depth int64, span *pb.Span, spanTree query.SpanTree) int64 {
	if span.ParentSpanId == span.Id {
		return 0
	}
	if span.ParentSpanId != "" && spanTree[span.ParentSpanId] != nil {
		depth += 1
		depth = calculateDepth(depth, spanTree[span.ParentSpanId], spanTree)
	}
	return depth
}

func (s *TraceService) GetSpanCount(ctx context.Context, traceID string) (int64, error) {
	count := s.Source.GetSpanCount(ctx, traceID)
	return count, nil
}

func (s *TraceService) GetTracesCount(ctx context.Context, startTime, endTime int64, params map[string]*structpb.Value, statement string) (int64, error) {
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(startTime, 10),
		End:       strconv.FormatInt(endTime, 10),
		Statement: statement,
		Params:    params,
	}

	response, err := s.p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return 0, err
	}
	count := response.Results[0].Series[0].Rows[0].Values[0].GetNumberValue()
	return int64(count), nil
}

func (s *TraceService) GetTraces(ctx context.Context, req *pb.GetTracesRequest) (*pb.GetTracesResponse, error) {
	if req.TenantID == "" {
		return nil, errors.NewMissingParameterError("tenantId")
	}
	if req.PageNo <= 1 {
		req.PageNo = 1
	}
	if req.PageSize <= 0 {
		req.PageSize = 10
	}
	if req.PageSize > 100 {
		req.PageSize = 100
	}
	if (req.DurationMin != 0 && req.DurationMax == 0) || (req.DurationMax != 0 && req.DurationMin == 0) {
		return nil, errors.NewInvalidParameterError("duration", "missing min or max duration")
	}
	if req.DurationMax != 0 && req.DurationMin != 0 && req.DurationMax <= req.DurationMin {
		return nil, errors.NewInvalidParameterError("duration", "duration min <= duration max")
	}
	if req.EndTime <= 0 || req.StartTime <= 0 {
		req.EndTime = time.Now().UnixNano() / 1e6
		h, _ := time.ParseDuration("-1h")
		req.StartTime = time.Now().Add(h).UnixNano() / 1e6
	}
	return s.Source.GetTraces(ctx, req)
}

func (s *TraceService) handleTracesResponse(rows []*metricpb.Row) []*pb.Trace {
	traces := make([]*pb.Trace, 0, len(rows))
	for _, row := range rows {
		var t pb.Trace
		values := row.Values
		t.Id = values[0].GetStringValue()
		t.Duration = values[1].GetNumberValue()
		t.StartTime = int64(values[2].GetNumberValue())
		t.SpanCount = int64(values[3].GetNumberValue())
		for _, serviceName := range values[4].GetListValue().Values {
			t.Services = append(t.Services, serviceName.GetStringValue())
		}
		traces = append(traces, &t)
	}
	return traces
}

func (s *TraceService) GetTraceDebugHistories(ctx context.Context, req *pb.GetTraceDebugHistoriesRequest) (*pb.GetTraceDebugHistoriesResponse, error) {
	if req.Limit <= 0 {
		req.Limit = 20
	}
	if req.Limit > 200 {
		req.Limit = 200
	}
	if req.ScopeID == "" {
		return nil, errors.NewMissingParameterError("scopeId")
	}

	histories, err := s.traceRequestHistoryDB.QueryHistoriesByScopeID(req.ScopeID, time.Now(), req.Limit)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	td := pb.TraceDebug{}
	var traceDebugHistories []*pb.TraceDebugHistory
	for _, history := range histories {
		debugHistory, err := s.convertToTraceDebugHistory(ctx, history)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		traceDebugHistories = append(traceDebugHistories, debugHistory)
	}
	td.History = traceDebugHistories
	td.Limit = int32(req.Limit)
	count, err := s.traceRequestHistoryDB.QueryCountByScopeID(req.ScopeID)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	td.Total = count
	return &pb.GetTraceDebugHistoriesResponse{Data: &td}, nil
}

func (s *TraceService) GetTraceReqDistribution(ctx context.Context, model custom.Model) ([]*source.TraceDistributionItem, error) {
	return s.Source.GetTraceReqDistribution(ctx, model)
}

func (s *TraceService) GetTraceQueryConditions(ctx context.Context, req *pb.GetTraceQueryConditionsRequest) (*pb.GetTraceQueryConditionsResponse, error) {
	return &pb.GetTraceQueryConditionsResponse{Data: s.translateConditions(apis.Language(ctx))}, nil
}

func (s *TraceService) translateConditions(lang i18n.LanguageCodes) *pb.TraceQueryConditions {
	conditions := query.DepthCopyQueryConditions()

	for _, condition := range conditions.Sort {
		condition.DisplayName = query.TranslateCondition(s.i18n, lang, condition.Key)
	}

	for _, condition := range conditions.TraceStatus {
		condition.DisplayName = query.TranslateCondition(s.i18n, lang, condition.Key)
	}

	for _, condition := range conditions.Others {
		condition.DisplayName = query.TranslateCondition(s.i18n, lang, condition.Key)
	}
	return conditions
}

func (s *TraceService) GetTraceDebugByRequestID(ctx context.Context, req *pb.GetTraceDebugRequest) (*pb.GetTraceDebugResponse, error) {
	dbHistory, err := s.traceRequestHistoryDB.QueryHistoryByRequestID(req.ScopeID, req.RequestID)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	debugHistory, err := s.convertToTraceDebugHistory(ctx, dbHistory)
	return &pb.GetTraceDebugResponse{Data: debugHistory}, nil
}

func (s *TraceService) CreateTraceDebug(ctx context.Context, req *pb.CreateTraceDebugRequest) (*pb.CreateTraceDebugResponse, error) {
	if req.Url == "" {
		return nil, errors.NewMissingParameterError("Url")
	}
	if req.ScopeID == "" {
		return nil, errors.NewMissingParameterError("TerminusKey")
	}
	if req.Method == "" {
		return nil, errors.NewMissingParameterError("Method")
	}
	if !govalidator.IsURL(req.Url) {
		return nil, errors.NewParameterTypeError("Url invalid")
	}

	bodyValid := bodyCheck(req.Body)
	if !bodyValid {
		return nil, errors.NewParameterTypeError("body")
	}
	if req.Name == "" {
		req.Name = "no name"
	}

	history, err := composeTraceRequestHistory(req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	insertHistory, err := s.traceRequestHistoryDB.InsertHistory(*history)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	statusInfo := pb.TraceDebugStatus{
		RequestID:  insertHistory.RequestId,
		Status:     int32(insertHistory.Status),
		StatusName: s.getDebugStatus(apis.Language(ctx), debug.Status(insertHistory.Status)),
		ScopeID:    insertHistory.TerminusKey,
	}

	response, err := s.sendHTTPRequest(err, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	responseCode := response.StatusCode
	responseBody, err := ioutil.ReadAll(response.Body)

	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			s.p.Log.Error("http response close fail.")
		}
	}(response.Body)

	_, err = s.traceRequestHistoryDB.UpdateDebugResponseByRequestID(req.ScopeID, req.RequestID, responseCode, string(responseBody))
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.CreateTraceDebugResponse{Data: &statusInfo}, nil
}

func bodyCheck(body string) bool {
	if body == "" {
		return true
	}
	return json.Valid([]byte(body))
}

func composeTraceRequestHistory(req *pb.CreateTraceDebugRequest) (*db.TraceRequestHistory, error) {
	req.RequestID = uuid.NewV4().String()

	queryString, err := json.Marshal(req.Query)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	headerString, err := json.Marshal(req.Header)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if req.CreateTime == "" {
		req.CreateTime = time.Now().Format(common.Layout)
	}
	createTime, err := time.ParseInLocation(common.Layout, req.CreateTime, time.Local)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	req.UpdateTime = time.Now().Format(common.Layout)
	updateTime, err := time.ParseInLocation(common.Layout, req.UpdateTime, time.Local)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	history := &db.TraceRequestHistory{
		Name:           req.Name,
		RequestId:      req.RequestID,
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
	return history, nil
}

func (s *TraceService) sendHTTPRequest(err error, req *pb.CreateTraceDebugRequest) (*http.Response, error) {
	client := &http.Client{}
	params := ioutil.NopCloser(strings.NewReader(req.Body))
	request, err := http.NewRequest(req.Method, req.Url, params)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	for k, v := range req.Header {
		request.Header.Set(k, v)
	}
	s.tracing(request, req)
	response, err := client.Do(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return response, nil
}

func (s *TraceService) tracing(request *http.Request, req *pb.CreateTraceDebugRequest) {
	request.Header.Set("terminus-request-id", req.RequestID)
	request.Header.Set("terminus-request-sampled", "true")
}

func (s *TraceService) StopTraceDebug(ctx context.Context, req *pb.StopTraceDebugRequest) (*pb.StopTraceDebugResponse, error) {
	_, err := s.traceRequestHistoryDB.UpdateDebugStatusByRequestID(req.ScopeID, req.RequestID, int(debug.Fail))
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return nil, nil
}

func (s *TraceService) isExistSpan(ctx context.Context, requestID string) (bool, error) {
	count, err := s.GetSpanCount(ctx, requestID)
	if err != nil {
		return false, errors.NewInternalServerError(err)
	}
	if count < 0 {
		return false, errors.NewInternalServerError(err)
	}
	if count == 0 {
		return false, nil
	}
	return true, nil
}

func (s *TraceService) GetTraceDebugHistoryStatusByRequestID(ctx context.Context, req *pb.GetTraceDebugStatusByRequestIDRequest) (*pb.GetTraceDebugStatusByRequestIDResponse, error) {
	dbHistory, err := s.traceRequestHistoryDB.QueryHistoryByRequestID(req.ScopeID, req.RequestID)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if dbHistory == nil {
		return nil, errors.NewNotFoundError("trace debug history not found.")
	}

	statusInfo := pb.TraceDebugStatus{
		RequestID:  dbHistory.RequestId,
		Status:     int32(dbHistory.Status),
		StatusName: s.getDebugStatus(apis.Language(ctx), debug.Status(dbHistory.Status)),
		ScopeID:    dbHistory.TerminusKey,
	}

	if debug.Status(dbHistory.Status) == debug.Init {
		exist, err := s.isExistSpan(ctx, req.RequestID)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		if exist {
			info, err := s.traceRequestHistoryDB.UpdateDebugStatusByRequestID(dbHistory.TerminusKey, dbHistory.RequestId, int(debug.Success))
			if err != nil {
				return nil, errors.NewInternalServerError(err)
			}
			statusInfo.Status = int32(info.Status)
			statusInfo.StatusName = s.getDebugStatus(apis.Language(ctx), debug.Status(info.Status))
		} else {
			// If trace data is not obtained within 20 minutes, it is considered that the writing of trace data has failed.
			if (time.Now().UnixNano()-dbHistory.UpdateTime.UnixNano())/1e9 > 20*60 {
				info, err := s.traceRequestHistoryDB.UpdateDebugStatusByRequestID(dbHistory.TerminusKey, dbHistory.RequestId, int(debug.Fail))
				if err != nil {
					return nil, errors.NewInternalServerError(err)
				}
				statusInfo.Status = int32(info.Status)
				statusInfo.StatusName = s.getDebugStatus(apis.Language(ctx), debug.Status(info.Status))
			}
		}
	}

	return &pb.GetTraceDebugStatusByRequestIDResponse{Data: &statusInfo}, nil
}

func (s *TraceService) convertToTraceDebugHistory(ctx context.Context, dbHistory *db.TraceRequestHistory) (*pb.TraceDebugHistory, error) {
	language := apis.Language(ctx)

	query := make(map[string]string)
	if dbHistory.QueryString != "" {
		err := json.Unmarshal([]byte(dbHistory.QueryString), &query)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
	}
	headers := make(map[string]string)
	if dbHistory.Header != "" {
		err := json.Unmarshal([]byte(dbHistory.Header), &headers)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
	}
	return &pb.TraceDebugHistory{
		RequestID:    dbHistory.RequestId,
		ScopeID:      dbHistory.TerminusKey,
		Url:          dbHistory.Url,
		Query:        query,
		Header:       headers,
		Body:         dbHistory.Body,
		Status:       int32(dbHistory.Status),
		StatusName:   s.getDebugStatus(language, debug.Status(dbHistory.Status)),
		ResponseCode: int32(dbHistory.ResponseStatus),
		ResponseBody: dbHistory.ResponseBody,
		Method:       dbHistory.Method,
		CreateTime:   dbHistory.CreateTime.Format(common.Layout),
		UpdateTime:   dbHistory.UpdateTime.Format(common.Layout),
	}, nil
}

func (s *TraceService) GetSpanEvents(ctx context.Context, req *pb.SpanEventRequest) (*pb.SpanEventResponse, error) {
	startTime, endTime := s.getSpanEventQueryTime(req)
	req.StartTime = req.StartTime - int64((time.Minute*15)/time.Millisecond)
	statement := "select * from apm_span_event where span_id = $span_id order by timestamp asc limit 1000"
	queryParams := map[string]*structpb.Value{
		"span_id": structpb.NewStringValue(req.SpanID),
	}
	queryCtx, _ := context.WithTimeout(ctx, time.Minute)
	queryRequest := &metricpb.QueryWithTableFormatRequest{
		Start:     strconv.FormatInt(startTime, 10),
		End:       strconv.FormatInt(endTime, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := s.p.Metric.QueryWithTableFormat(queryCtx, queryRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	table := response.Data
	events := s.handleSpanEventResponse(table)
	return &pb.SpanEventResponse{SpanEvents: events}, nil
}

func (s *TraceService) getSpanEventQueryTime(req *pb.SpanEventRequest) (int64, int64) {
	if req.StartTime <= 0 {
		req.StartTime = time.Now().Add(-time.Minute*15).UnixNano() / 1e6
	}
	return req.StartTime - int64((time.Minute*15)/time.Millisecond), req.StartTime + int64((time.Minute*15)/time.Millisecond)
}

func (s *TraceService) handleSpanEventResponse(table *metricpb.TableResult) []*pb.SpanEvent {
	spanEvents := make([]*pb.SpanEvent, 0)
	eventNames := make(map[string]string)
	for _, col := range table.Cols {
		if col.Flag == "tag" {
			key := strings.Replace(col.Key, "::tag", "", -1)
			if EventFieldSet.Contains(key) {
				eventNames[key] = col.Key
			}
		}
	}
	for _, data := range table.Data {
		timestamp := int64(data.Values["timestamp"].GetNumberValue())
		events := make(map[string]string)
		for key, value := range eventNames {
			events[key] = data.Values[value].GetStringValue()
		}
		event := &pb.SpanEvent{
			Timestamp: timestamp,
			Events:    events,
		}
		spanEvents = append(spanEvents, event)
	}
	return spanEvents
}
