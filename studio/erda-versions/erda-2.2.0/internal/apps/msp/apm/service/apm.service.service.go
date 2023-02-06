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

package service

import (
	"context"
	"fmt"
	"sort"
	"strconv"
	"strings"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	servicecommon "github.com/erda-project/erda/internal/apps/msp/apm/service/common"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/chart"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/math"
)

type apmServiceService struct {
	p *provider
}

func (s *apmServiceService) GetServiceLanguage(ctx context.Context, req *pb.GetServiceLanguageRequest) (*pb.GetServiceLanguageResponse, error) {
	if req.TenantId == "" {
		return nil, errors.NewMissingParameterError("tenantId")
	}
	if req.ServiceId == "" {
		return nil, errors.NewMissingParameterError("serviceId")
	}
	startTime, endTime := TimeRange("-1h")
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(req.TenantId),
		"service_id":   structpb.NewStringValue(req.ServiceId),
	}
	sql := "SELECT distinct(service_id::tag) FROM %s WHERE terminus_key=$terminus_key AND service_id=$service_id LIMIT 1"
	for key, language := range servicecommon.ProcessTypes {
		statement := fmt.Sprintf(sql, key)
		request := &metricpb.QueryWithInfluxFormatRequest{
			Start:     strconv.FormatInt(startTime, 10),
			End:       strconv.FormatInt(endTime, 10),
			Statement: statement,
			Params:    queryParams,
		}
		response, err := s.p.Metric.QueryWithInfluxFormat(ctx, request)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		if err != nil {
			return nil, err
		}
		count := response.Results[0].Series[0].Rows[0].Values[0].GetNumberValue()
		if count == 1 {
			return &pb.GetServiceLanguageResponse{Language: language}, err
		}
	}
	return nil, nil
}

func (s *apmServiceService) GetServices(ctx context.Context, req *pb.GetServicesRequest) (*pb.GetServicesResponse, error) {
	if req.TenantId == "" {
		return nil, errors.NewMissingParameterError("tenantId")
	}
	if req.PageNo <= 0 {
		req.PageNo = 1
	}
	if req.PageSize < 10 {
		req.PageSize = 10
	}
	if req.PageSize > 100 {
		req.PageSize = 100
	}

	// default get time: 1 day.
	start, end := TimeRange("-24h")

	// get services list
	statement := fmt.Sprintf("SELECT service_id::tag,service_name::tag,service_agent_platform::tag,max(timestamp) FROM application_service_node "+
		"WHERE $condition GROUP BY service_id::tag ORDER BY max(timestamp) DESC LIMIT %v OFFSET %v", req.PageSize, (req.PageNo-1)*req.PageSize)
	condition := " terminus_key::tag=$terminus_key "
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(req.TenantId),
	}
	condition, err := HandleCondition(ctx, req, s, condition)
	if req.ServiceStatus == pb.Status_hasError.String() && !strings.Contains(condition, "include") {
		return &pb.GetServicesResponse{PageNo: req.PageNo, PageSize: req.PageSize}, nil
	}

	if err != nil {
		return nil, err
	}
	statement = strings.ReplaceAll(statement, "$condition", condition)
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := s.p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	services := make([]*pb.Service, 0, 10)
	rows := response.Results[0].Series[0].Rows

	for _, row := range rows {
		service := new(pb.Service)
		service.Id = row.Values[0].GetStringValue()
		service.Name = row.Values[1].GetStringValue()
		service.Language = parseLanguage(row.Values[2].GetStringValue())
		service.LastHeartbeat = time.Unix(0, int64(row.Values[3].GetNumberValue())).Format("2006-01-02 15:04:05")
		service.AggregateMetric = &pb.AggregateMetric{}
		services = append(services, service)
	}

	// calculate total Count
	statement = "SELECT DISTINCT(service_id::tag) FROM application_service_node WHERE $condition"
	statement = strings.ReplaceAll(statement, "$condition", condition)
	countRequest := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	countResponse, err := s.p.Metric.QueryWithInfluxFormat(ctx, countRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	// service total Count
	total := int64(countResponse.Results[0].Series[0].Rows[0].GetValues()[0].GetNumberValue())

	if rows == nil || len(rows) == 0 {
		return &pb.GetServicesResponse{PageNo: req.PageNo, PageSize: req.PageSize, Total: total, List: services}, nil
	}

	sortStrategy, err := s.aggregateMetric(req.ServiceStatus, req.TenantId, &services, ctx)
	if err != nil {
		return nil, err
	}

	sort.SliceStable(services, func(i, j int) bool {
		switch sortStrategy {
		case SortStrategyErrorRate:
			if services[i].AggregateMetric.ErrorRate > services[j].AggregateMetric.ErrorRate {
				return true
			}
			return false
		case SortStrategyAvgDuration:
			if services[i].AggregateMetric.AvgDuration > services[j].AggregateMetric.AvgDuration {
				return true
			}
			return false
		default:
			if services[i].AggregateMetric.AvgRps > services[j].AggregateMetric.AvgRps {
				return true
			}
			return false
		}
	})

	return &pb.GetServicesResponse{PageNo: req.PageNo, PageSize: req.PageSize, Total: total, List: services}, nil
}

func HandleCondition(ctx context.Context, req *pb.GetServicesRequest, s *apmServiceService, condition string) (string, error) {
	if req.ServiceName != "" {
		condition += " AND service_name::tag=~/.*" + req.ServiceName + ".*/ "
	}
	if req.ServiceStatus == pb.Status_hasError.String() {
		startTime, endTime := TimeRange("-1h")
		_, hasErrorServiceIds, err := s.GetHasErrorService(ctx, req.TenantId, startTime, endTime)
		if err != nil {
			return "", err
		}
		includeIds := ""
		if len(hasErrorServiceIds) > 0 {
			for _, serviceId := range hasErrorServiceIds {
				includeIds += "'" + serviceId + "',"
			}
			includeIds = includeIds[:len(includeIds)-1]
		}
		if len(includeIds) > 0 {
			condition += fmt.Sprintf(" AND include(service_id::tag, %s)", includeIds)
		}
	}
	if req.ServiceStatus == pb.Status_withoutRequest.String() {
		startTime, endTime := TimeRange("-1h")
		_, withRequestServiceIds, err := s.GetWithRequestService(ctx, req.TenantId, startTime, endTime)
		if err != nil {
			return "", err
		}
		includeIds := ""
		if len(withRequestServiceIds) > 0 {
			for _, serviceId := range withRequestServiceIds {
				includeIds += "'" + serviceId + "',"
			}
			includeIds = includeIds[:len(includeIds)-1]
		}
		if len(includeIds) > 0 {
			condition += fmt.Sprintf(" AND not_include(service_id::tag, %s)", includeIds)
		}
	}
	return condition, nil
}

const (
	SortStrategyErrorRate   = "ErrorRateStrategy"
	SortStrategyAvgDuration = "AvgDurationStrategy"
	SortStrategyRPS         = "RpsRateStrategy"
)

func TimeRange(s string) (start int64, end int64) {
	d, _ := time.ParseDuration(s)
	start = time.Now().Add(d).UnixNano() / 1e6
	end = time.Now().UnixNano() / 1e6
	return
}

func (s *apmServiceService) aggregateMetric(serviceStatus, tenantId string, services *[]*pb.Service, ctx context.Context) (sortStrategy string, err error) {
	start, end := TimeRange("-1h")

	includeIds := ""
	serviceMap := make(map[string]*pb.Service)
	for _, service := range *services {
		includeIds += "'" + service.Id + "',"
		serviceMap[service.Id] = service
	}
	includeIds = includeIds[:len(includeIds)-1]

	statement := fmt.Sprintf("SELECT target_service_id::tag,sum(elapsed_count::field)/(60*60),avg(elapsed_mean::field),sum(if(eq(error::tag, 'true'),elapsed_count::field,0))/sum(elapsed_count::field) "+
		"FROM application_http,application_rpc "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"AND include(target_service_id::tag, %s) "+
		"GROUP BY target_service_id::tag", includeIds)
	condition := " terminus_key::tag=$terminus_key "

	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
	}

	statement = strings.ReplaceAll(statement, "$condition", condition)
	request := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	response, err := s.p.Metric.QueryWithInfluxFormat(ctx, request)
	if err != nil {
		return "", err
	}

	var (
		errorRateSortSign   float64
		avgDurationSortSign float64
		rpcSortSign         float64
	)
	if response != nil {
		rows := response.Results[0].Series[0].Rows
		for _, row := range rows {
			serviceId := row.Values[0].GetStringValue()
			if service, ok := serviceMap[serviceId]; ok {
				rps := row.Values[1].GetNumberValue()
				avgDuration := row.Values[2].GetNumberValue()
				errorRate := row.Values[3].GetNumberValue() * 100 // to %

				aggregateMetric := &pb.AggregateMetric{
					AvgRps:      math.DecimalPlacesWithDigitsNumber(rps, 2),
					AvgDuration: math.DecimalPlacesWithDigitsNumber(avgDuration, 2),
					ErrorRate:   math.DecimalPlacesWithDigitsNumber(errorRate, 2),
				}
				service.AggregateMetric = aggregateMetric

				avgDurationSortSign += aggregateMetric.AvgDuration
				rpcSortSign += aggregateMetric.AvgRps
				errorRateSortSign += aggregateMetric.ErrorRate

			}
		}
	}

	if errorRateSortSign > 0 {
		return SortStrategyErrorRate, nil
	} else if avgDurationSortSign > 0 {
		return SortStrategyAvgDuration, nil
	}
	return SortStrategyRPS, nil
}

func parseLanguage(platform string) (language commonpb.Language) {
	lowerPlatform := strings.ToLower(platform)
	language = commonpb.Language_unknown
	if strings.Contains(lowerPlatform, "java") || strings.Contains(lowerPlatform, "jdk") {
		language = commonpb.Language_java
	} else if strings.Contains(lowerPlatform, "nodejs") {
		language = commonpb.Language_nodejs
	} else if strings.Contains(lowerPlatform, "python") || strings.Contains(lowerPlatform, "py") {
		language = commonpb.Language_python
	} else if strings.Contains(lowerPlatform, "golang") || strings.Contains(lowerPlatform, "go") {
		language = commonpb.Language_golang
	} else if strings.Contains(lowerPlatform, "c") && !(strings.Contains(lowerPlatform, "cpp") || strings.Contains(lowerPlatform, "c++")) && (!strings.Contains(lowerPlatform, "c#")) {
		language = commonpb.Language_c
	} else if strings.Contains(lowerPlatform, "cpp") || strings.Contains(lowerPlatform, "c++") {
		language = commonpb.Language_cpp
	} else if strings.Contains(lowerPlatform, "php") {
		language = commonpb.Language_php
	} else if strings.Contains(lowerPlatform, ".net") {
		language = commonpb.Language_dotnet
	} else if strings.Contains(lowerPlatform, "c#") {
		language = commonpb.Language_csharp
	}
	return
}

func (s *apmServiceService) GetServiceAnalyzerOverview(ctx context.Context, req *pb.GetServiceAnalyzerOverviewRequest) (*pb.GetServiceAnalyzerOverviewResponse, error) {
	if req.TenantId == "" {
		return nil, errors.NewMissingParameterError("tenantId")
	}
	if req.ServiceIds == nil || len(req.ServiceIds) == 0 {
		return nil, errors.NewMissingParameterError("serviceId")
	}
	if req.View == "" {
		req.View = strings.ToLower(pb.ViewType_SERVICE_OVERVIEW.String())
	}
	interval := ""
	start := req.StartTime
	end := req.EndTime
	if req.StartTime == 0 || req.EndTime == 0 {
		start, end = TimeRange("-1h")
	}

	servicesView := make([]*pb.ServicesView, 0, 10)

	for _, id := range req.ServiceIds {

		baseChart := &chart.BaseChart{
			StartTime: start,
			EndTime:   end,
			Interval:  interval,
			TenantId:  req.TenantId,
			ServiceId: id,
			Metric:    s.p.Metric,
			Layers: []common.TransactionLayerType{
				common.TransactionLayerHttp,
				common.TransactionLayerRpc,
			},
		}

		view, err := Selector(req.View, s.p.Cfg, baseChart, ctx)
		if err != nil {
			return nil, err
		}
		servicesView = append(servicesView, &pb.ServicesView{
			ServiceId: id,
			Views:     view,
		})
	}
	return &pb.GetServiceAnalyzerOverviewResponse{List: servicesView}, nil
}

func (s *apmServiceService) Count(ctx context.Context, tenantId, status string, resp *pb.GetServiceCountResponse) {
	switch status {
	case pb.Status_all.String():
		start, end := TimeRange("-24h")
		count, _ := s.GetTotalCount(ctx, tenantId, start, end)
		resp.TotalCount = count
	case pb.Status_hasError.String():
		start, end := TimeRange("-1h")
		count, _, _ := s.GetHasErrorService(ctx, tenantId, start, end)
		resp.HasErrorCount = count
	case pb.Status_withoutRequest.String():
		start, end := TimeRange("-1h")
		withRequestCount, _, _ := s.GetWithRequestService(ctx, tenantId, start, end)
		resp.WithoutRequestCount = resp.TotalCount - withRequestCount
	}
}

func (s *apmServiceService) GetServiceCount(ctx context.Context, req *pb.GetServiceCountRequest) (*pb.GetServiceCountResponse, error) {
	if req.TenantId == "" {
		return nil, errors.NewMissingParameterError("tenantId")
	}
	var ss = []string{pb.Status_all.String(), pb.Status_hasError.String(), pb.Status_withoutRequest.String()}
	response := &pb.GetServiceCountResponse{}
	for _, status := range ss {
		s.Count(ctx, req.TenantId, status, response)
	}
	return response, nil
}

func (s *apmServiceService) GetWithRequestService(ctx context.Context, tenantId string, start int64, end int64) (int64, []string, error) {
	// withoutRequest Count
	statement := "SELECT target_service_id::tag,if(gt(sum(elapsed_sum::field),0),true,false) FROM application_http_service,application_rpc_service WHERE $condition GROUP BY target_service_id::tag "
	withoutRequestCondition := "target_terminus_key::tag=$target_terminus_key "
	statement = strings.ReplaceAll(statement, "$condition", withoutRequestCondition)
	queryParams := map[string]*structpb.Value{
		"target_terminus_key": structpb.NewStringValue(tenantId),
	}
	countRequest := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	countResponse, err := s.p.Metric.QueryWithInfluxFormat(ctx, countRequest)
	if err != nil {
		return 0, nil, errors.NewInternalServerError(err)
	}
	withoutRequestCount := int64(0)
	var serviceIds []string

	rows := countResponse.Results[0].Series[0].Rows
	for _, row := range rows {
		if row.GetValues()[1].GetBoolValue() {
			withoutRequestCount += 1
			serviceId := row.GetValues()[0].GetStringValue()
			serviceIds = append(serviceIds, serviceId)
		}
	}
	return withoutRequestCount, serviceIds, nil
}

func (s *apmServiceService) GetHasErrorService(ctx context.Context, tenantId string, start int64, end int64) (int64, []string, error) {
	// hasError Count
	statement := "SELECT target_service_id::tag,if(gt(sum(errors_sum::field),0),true,false) FROM application_http_service,application_rpc_service WHERE $condition GROUP BY target_service_id::tag "
	unhealthyCondition := " target_terminus_key::tag=$target_terminus_key AND errors_sum::field>0 "
	statement = strings.ReplaceAll(statement, "$condition", unhealthyCondition)

	queryParams := map[string]*structpb.Value{
		"target_terminus_key": structpb.NewStringValue(tenantId),
	}
	countRequest := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	countResponse, err := s.p.Metric.QueryWithInfluxFormat(ctx, countRequest)
	if err != nil {
		return 0, nil, errors.NewInternalServerError(err)
	}
	hasErrorCount := int64(0)
	var serviceIds []string
	rows := countResponse.Results[0].Series[0].Rows
	for _, row := range rows {
		if row.GetValues()[1].GetBoolValue() {
			hasErrorCount += 1
			serviceId := row.GetValues()[0].GetStringValue()
			serviceIds = append(serviceIds, serviceId)
		}
	}
	return hasErrorCount, serviceIds, nil
}

func (s *apmServiceService) GetTotalCount(ctx context.Context, tenantId string, start int64, end int64) (int64, error) {
	// calculate total Count
	condition := " terminus_key::tag=$terminus_key "
	statement := "SELECT DISTINCT(service_id::tag) FROM application_service_node WHERE $condition"
	statement = strings.ReplaceAll(statement, "$condition", condition)
	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(tenantId),
	}
	countRequest := &metricpb.QueryWithInfluxFormatRequest{
		Start:     strconv.FormatInt(start, 10),
		End:       strconv.FormatInt(end, 10),
		Statement: statement,
		Params:    queryParams,
	}
	countResponse, err := s.p.Metric.QueryWithInfluxFormat(ctx, countRequest)
	if err != nil {
		return 0, errors.NewInternalServerError(err)
	}
	total := int64(countResponse.Results[0].Series[0].Rows[0].GetValues()[0].GetNumberValue())
	return total, nil
}
