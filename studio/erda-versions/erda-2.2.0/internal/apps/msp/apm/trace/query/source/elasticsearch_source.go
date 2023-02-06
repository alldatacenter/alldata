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
	"bytes"
	"context"
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/commom/custom"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/storage"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/math"
)

type ElasticsearchSource struct {
	StorageReader storage.Storage
	Metric        metricpb.MetricServiceServer
	Log           logs.Logger

	CompatibleSource TraceSource
}

func (esc ElasticsearchSource) GetSpans(ctx context.Context, req *pb.GetSpansRequest) []*pb.Span {
	org := req.OrgName
	if len(org) <= 0 {
		org = apis.GetHeader(ctx, "org")
	}
	// do esc query
	elasticsearchSpans, _ := FetchSpanFromES(ctx, esc.StorageReader, storage.Selector{
		TraceId: req.TraceID,
		Hint: storage.QueryHint{
			Scope:     org,
			Timestamp: req.StartTime * 1000000, // convert ms to ns
		},
	}, true, int(req.GetLimit()))
	var spans []*pb.Span
	for _, value := range elasticsearchSpans {
		var span pb.Span
		span.Id = value.SpanId
		span.TraceId = value.TraceId
		span.OperationName = value.OperationName
		span.ParentSpanId = value.ParentSpanId
		span.StartTime = value.StartTime
		span.EndTime = value.EndTime
		span.Tags = value.Tags
		spans = append(spans, &span)
	}
	return spans
}

func (esc ElasticsearchSource) GetTraceReqDistribution(ctx context.Context, model custom.Model) ([]*TraceDistributionItem, error) {
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(model.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(model.EndTime, 10))

	queryParams := make(map[string]*structpb.Value)
	queryParams["terminus_keys"] = structpb.NewStringValue(model.TenantId)

	var where bytes.Buffer
	// trace id condition
	if model.TraceId != "" {
		queryParams["trace_id"] = structpb.NewStringValue(model.TraceId)
		where.WriteString("trace_id::tag=$trace_id AND ")
	}

	if model.ServiceName != "" {
		queryParams["service_names"] = structpb.NewStringValue(model.ServiceName)
		where.WriteString("service_names::field=$service_names AND ")
	}

	if model.RpcMethod != "" {
		queryParams["rpc_methods"] = structpb.NewStringValue(model.RpcMethod)
		where.WriteString("rpc_methods::field=$rpc_methods AND ")
	}

	if model.HttpPath != "" {
		queryParams["http_paths"] = structpb.NewStringValue(model.HttpPath)
		where.WriteString("http_paths::field=$http_paths AND ")
	}

	if model.DurationMin > 0 && model.DurationMax > 0 && model.DurationMin < model.DurationMax {
		queryParams["duration_min"] = structpb.NewNumberValue(float64(model.DurationMin))
		queryParams["duration_max"] = structpb.NewNumberValue(float64(model.DurationMax))
		where.WriteString("trace_duration::field>$duration_min AND trace_duration::field<$duration_max AND ")
	}

	// trace status condition
	where.WriteString(esc.traceStatusConditionStrategy(model.Status))

	statement := fmt.Sprintf("SELECT avg(trace_duration::field),count(trace_id::tag) FROM trace WHERE %s terminus_keys::field=$terminus_keys GROUP BY time()", where.String())

	queryRequest := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(model.StartTime, 10),
		End:       strconv.FormatInt(model.EndTime, 10),
		Statement: statement,
		Params:    queryParams,
	}

	response, err := esc.Metric.QueryWithInfluxFormat(ctx, queryRequest)
	if err != nil {
		esc.Log.Error(err)
		return nil, err
	}
	rows := response.Results[0].Series[0].Rows
	if rows == nil || len(rows) == 0 {
		return nil, err
	}
	items := make([]*TraceDistributionItem, 0, 10)
	for _, row := range rows {
		timeFormat := row.Values[0].GetStringValue()
		timeFormat = strings.ReplaceAll(timeFormat, "T", " ")
		timeFormat = strings.ReplaceAll(timeFormat, "Z", "")
		date := timeFormat
		avgDuration := math.DecimalPlacesWithDigitsNumber(row.Values[1].GetNumberValue(), 2)
		count := row.Values[2].GetNumberValue()
		item := &TraceDistributionItem{Date: date, AvgDuration: avgDuration, Count: uint64(count)}
		items = append(items, item)
	}
	return items, nil
}

func (esc ElasticsearchSource) composeTraceQueryConditions(req *pb.GetTracesRequest) (map[string]*structpb.Value, string, string) {
	metricsParams := url.Values{}
	metricsParams.Set("start", strconv.FormatInt(req.StartTime, 10))
	metricsParams.Set("end", strconv.FormatInt(req.EndTime, 10))

	queryParams := make(map[string]*structpb.Value)
	queryParams["terminus_keys"] = structpb.NewStringValue(req.TenantID)

	var where bytes.Buffer
	// trace id condition
	if req.TraceID != "" {
		queryParams["trace_id"] = structpb.NewStringValue(req.TraceID)
		where.WriteString("trace_id::tag=$trace_id AND ")
	}

	if req.ServiceName != "" {
		queryParams["service_names"] = structpb.NewStringValue(req.ServiceName)
		where.WriteString("service_names::field=$service_names AND ")
	}

	if req.RpcMethod != "" {
		queryParams["rpc_methods"] = structpb.NewStringValue(req.RpcMethod)
		where.WriteString("rpc_methods::field=$rpc_methods AND ")
	}

	if req.HttpPath != "" {
		queryParams["http_paths"] = structpb.NewStringValue(req.HttpPath)
		where.WriteString("http_paths::field=$http_paths AND ")
	}

	if req.DurationMin > 0 && req.DurationMax > 0 && req.DurationMin < req.DurationMax {
		queryParams["duration_min"] = structpb.NewNumberValue(float64(req.DurationMin))
		queryParams["duration_max"] = structpb.NewNumberValue(float64(req.DurationMax))
		where.WriteString("trace_duration::field>$duration_min AND trace_duration::field<$duration_max AND ")
	}

	// trace status condition
	where.WriteString(esc.traceStatusConditionStrategy(req.Status))
	// sort condition
	sort := esc.sortConditionStrategy(req.Sort)
	statement := fmt.Sprintf("SELECT trace_id::tag,trace_duration::field,start_time::field,span_count::field,service_names::field "+
		"FROM trace "+
		"WHERE %s terminus_keys::field=$terminus_keys "+
		"%s "+
		"LIMIT %v OFFSET %v", where.String(), sort, req.PageSize, (req.PageNo-1)*req.PageSize)

	statementCount := fmt.Sprintf("SELECT count(trace_id::tag) "+
		"FROM trace "+
		"WHERE %s terminus_keys::field=$terminus_keys ", where.String())
	return queryParams, statement, statementCount
}

func (esc ElasticsearchSource) sortConditionStrategy(sort string) string {
	switch strings.ToLower(sort) {
	case strings.ToLower(pb.SortCondition_TRACE_TIME_DESC.String()):
		return "ORDER BY start_time::field DESC"
	case strings.ToLower(pb.SortCondition_TRACE_TIME_ASC.String()):
		return "ORDER BY start_time::field ASC"
	case strings.ToLower(pb.SortCondition_TRACE_DURATION_DESC.String()):
		return "ORDER BY trace_duration::field DESC"
	case strings.ToLower(pb.SortCondition_TRACE_DURATION_ASC.String()):
		return "ORDER BY trace_duration::field ASC"
	case strings.ToLower(pb.SortCondition_SPAN_COUNT_DESC.String()):
		return "ORDER BY span_count::field DESC"
	case strings.ToLower(pb.SortCondition_SPAN_COUNT_ASC.String()):
		return "ORDER BY span_count::field ASC"
	default:
		return "ORDER BY start_time::field DESC"
	}
}

func (esc ElasticsearchSource) GetTraces(ctx context.Context, req *pb.GetTracesRequest) (*pb.GetTracesResponse, error) {

	queryParams, statement, statementCount := esc.composeTraceQueryConditions(req)

	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()

	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(req.StartTime, 10),
		End:       strconv.FormatInt(req.EndTime, 10),
		Statement: statement,
		Params:    queryParams,
	}

	response, err := esc.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	total, err := esc.GetTracesCount(ctx, req.StartTime, req.EndTime, queryParams, statementCount)
	if err != nil {
		return nil, err
	}

	rows := response.Results[0].Series[0].Rows
	traces := esc.handleTracesResponse(rows)
	return &pb.GetTracesResponse{Data: traces, PageNo: req.PageNo, PageSize: req.PageSize, Total: total}, nil
}

func (esc ElasticsearchSource) GetTracesCount(ctx context.Context, startTime, endTime int64, params map[string]*structpb.Value, statement string) (int64, error) {
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(startTime, 10),
		End:       strconv.FormatInt(endTime, 10),
		Statement: statement,
		Params:    params,
	}

	response, err := esc.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return 0, err
	}
	count := response.Results[0].Series[0].Rows[0].Values[0].GetNumberValue()
	return int64(count), nil
}

func (esc ElasticsearchSource) handleTracesResponse(rows []*metricpb.Row) []*pb.Trace {
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

func (esc *ElasticsearchSource) traceStatusConditionStrategy(traceStatus string) string {
	switch traceStatus {
	case strings.ToLower(pb.TraceStatusCondition_TRACE_SUCCESS.String()):
		return "errors_sum::field=0 AND"
	case strings.ToLower(pb.TraceStatusCondition_TRACE_ALL.String()):
		return "errors_sum::field>=0 AND"
	case strings.ToLower(pb.TraceStatusCondition_TRACE_ERROR.String()):
		return "errors_sum::field>0 AND"
	default:
		return "errors_sum::field>=0 AND"
	}
}

func (esc *ElasticsearchSource) GetSpanCount(ctx context.Context, traceID string) int64 {
	return esc.StorageReader.Count(ctx, traceID)
}

func FetchSpanFromES(ctx context.Context, storage storage.Storage, sel storage.Selector, forward bool, limit int) (list []*trace.Span, err error) {
	it, err := storage.Iterator(ctx, &sel)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	defer it.Close()

	if forward {
		for it.Next() {
			span, ok := it.Value().(*trace.Span)
			if !ok {
				continue
			}
			list = append(list, span)
			if len(list) >= limit {
				break
			}
		}
	} else {
		for it.Prev() {
			span, ok := it.Value().(*trace.Span)
			if !ok {
				continue
			}
			list = append(list, span)
			if len(list) >= limit {
				break
			}
		}
	}
	if it.Error() != nil {
		return nil, errors.NewInternalServerError(err)
	}

	return list, it.Error()
}
