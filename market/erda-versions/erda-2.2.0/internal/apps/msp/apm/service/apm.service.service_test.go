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
	"reflect"
	"strings"
	"testing"

	"bou.ke/monkey"
	"github.com/pkg/errors"
	"google.golang.org/protobuf/types/known/structpb"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/service/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/chart"
)

func Test_parseLanguage(t *testing.T) {
	type args struct {
		platform string
	}
	tests := []struct {
		name         string
		args         args
		wantLanguage commonpb.Language
	}{
		{"case1", args{platform: "unknown 1.2.3"}, commonpb.Language_unknown},
		{"case2", args{platform: "JDK 1.2.3"}, commonpb.Language_java},
		{"case3", args{platform: "NODEJS 1.2.3"}, commonpb.Language_nodejs},
		{"case4", args{platform: "PYTHON 1.2.3"}, commonpb.Language_python},
		{"case5", args{platform: "c 1.2.3"}, commonpb.Language_c},
		{"case6", args{platform: "c++ 1.2.3"}, commonpb.Language_cpp},
		{"case7", args{platform: "c# 1.2.3"}, commonpb.Language_csharp},
		{"case8", args{platform: "go 1.2.3"}, commonpb.Language_golang},
		{"case9", args{platform: "php 1.2.3"}, commonpb.Language_php},
		{"case10", args{platform: ".net 1.2.3"}, commonpb.Language_dotnet},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if gotLanguage := parseLanguage(tt.args.platform); gotLanguage != tt.wantLanguage {
				t.Errorf("parseLanguage() = %v, want %v", gotLanguage, tt.wantLanguage)
			}
		})
	}
}

func Test_apmServiceService_GetServices(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetServicesRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{ctx: nil, req: &pb.GetServicesRequest{}}, true},
		{"case2", args{ctx: nil, req: &pb.GetServicesRequest{TenantId: "test-error", ServiceName: "test-service"}}, true},
		{"case3", args{ctx: nil, req: &pb.GetServicesRequest{PageSize: 100, TenantId: "test-tenantId", ServiceName: "test-service"}}, false},
		{"case4", args{ctx: nil, req: &pb.GetServicesRequest{PageSize: 100, TenantId: "test-tenantId", ServiceName: "test-service", ServiceStatus: pb.Status_hasError.String()}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var msc *metricpb.UnimplementedMetricServiceServer
			QueryWithInfluxFormat := monkey.PatchInstanceMethod(reflect.TypeOf(msc), "QueryWithInfluxFormat",
				func(un *metricpb.UnimplementedMetricServiceServer, ctx context.Context, req *metricpb.QueryWithInfluxFormatRequest) (*metricpb.QueryWithInfluxFormatResponse, error) {
					if v, ok := req.Params["terminus_key"]; ok && v.GetStringValue() == "test-error" {
						return nil, errors.New("error")
					}
					if strings.Contains(req.Statement, "DISTINCT") {
						return &metricpb.QueryWithInfluxFormatResponse{
							Results: []*metricpb.Result{
								{Series: []*metricpb.Serie{
									{Rows: []*metricpb.Row{
										{
											Values: []*structpb.Value{
												{Kind: &structpb.Value_NumberValue{NumberValue: 4}},
											},
										},
									}},
								}},
							},
						}, nil
					}

					return &metricpb.QueryWithInfluxFormatResponse{
						Results: []*metricpb.Result{
							{Series: []*metricpb.Serie{
								{Rows: []*metricpb.Row{
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-id"}},
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-name"}},
											{Kind: &structpb.Value_StringValue{StringValue: "jdk 1.2.3"}},
											{Kind: &structpb.Value_NumberValue{NumberValue: 1638770074000}},
										},
									},
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-id2"}},
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-name2"}},
											{Kind: &structpb.Value_StringValue{StringValue: "py 1.2.3"}},
											{Kind: &structpb.Value_NumberValue{NumberValue: 1638770074000}},
										},
									},
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-id3"}},
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-name3"}},
											{Kind: &structpb.Value_StringValue{StringValue: "golang 1.2.3"}},
											{Kind: &structpb.Value_NumberValue{NumberValue: 1638770074000}},
										},
									},
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-id4"}},
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-name4"}},
											{Kind: &structpb.Value_StringValue{StringValue: "nodejs 1.2.3"}},
											{Kind: &structpb.Value_NumberValue{NumberValue: 1638770074000}},
										},
									},
								}},
							}},
						},
					}, nil
				})
			defer QueryWithInfluxFormat.Unpatch()

			monkey.Patch(HandleCondition, func(ctx context.Context, req *pb.GetServicesRequest, s *apmServiceService, condition string) (string, error) {
				return condition, nil
			})

			s := &apmServiceService{
				p: &provider{Metric: msc},
			}
			_, err := s.GetServices(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetServices() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_apmServiceService_GetServiceAnalyzerOverview(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetServiceAnalyzerOverviewRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{req: &pb.GetServiceAnalyzerOverviewRequest{}}, true},
		{"case2", args{req: &pb.GetServiceAnalyzerOverviewRequest{TenantId: "test_tenant_id"}}, true},
		{"case3", args{req: &pb.GetServiceAnalyzerOverviewRequest{TenantId: "test_tenant_id_error", ServiceIds: []string{"test_service_id"}}}, true},
		{"case4", args{req: &pb.GetServiceAnalyzerOverviewRequest{TenantId: "test_tenant_id_TopologyChart", ServiceIds: []string{"test_service_id"}, View: "topology_service_node"}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var msc *metricpb.UnimplementedMetricServiceServer
			Selector := monkey.Patch(Selector, func(viewType string, config *config, baseChart *chart.BaseChart, ctx context.Context) ([]*pb.ServiceChart, error) {
				if baseChart.TenantId == "test_tenant_id_error" {
					return nil, errors.New("error")
				}
				return []*pb.ServiceChart{
					{
						Type: "HttpCode",
						View: []*pb.Chart{
							{
								Timestamp: 1638509880000,
								Value:     1.0,
								Dimension: "200",
							},
						},
					},
					{
						Type: "Rps",
						View: []*pb.Chart{
							{
								Timestamp: 1638509880000,
								Value:     1.0,
							},
						},
					},
					{
						Type: "AvgDuration",
						View: []*pb.Chart{
							{
								Timestamp: 1638509880000,
								Value:     1.0,
							},
						},
					},
					{
						Type: "ErrorRate",
						View: []*pb.Chart{
							{
								Timestamp: 1638509880000,
								Value:     1.0,
							},
						},
					},
				}, nil
			})
			defer Selector.Unpatch()

			s := &apmServiceService{
				p: &provider{Metric: msc},
			}
			_, err := s.GetServiceAnalyzerOverview(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetServiceAnalyzerOverview() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_apmServiceService_GetTotalCount(t *testing.T) {
	type args struct {
		ctx      context.Context
		tenantId string
		start    int64
		end      int64
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{ctx: context.Background(), tenantId: "test"}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var msc *metricpb.UnimplementedMetricServiceServer
			QueryWithInfluxFormat := monkey.PatchInstanceMethod(reflect.TypeOf(msc), "QueryWithInfluxFormat",
				func(un *metricpb.UnimplementedMetricServiceServer, ctx context.Context, req *metricpb.QueryWithInfluxFormatRequest) (*metricpb.QueryWithInfluxFormatResponse, error) {

					return &metricpb.QueryWithInfluxFormatResponse{
						Results: []*metricpb.Result{
							{Series: []*metricpb.Serie{
								{Rows: []*metricpb.Row{
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_NumberValue{NumberValue: 3}},
										},
									},
								}},
							}},
						},
					}, nil
				})
			defer QueryWithInfluxFormat.Unpatch()

			s := &apmServiceService{p: &provider{Metric: msc}}
			_, err := s.GetTotalCount(tt.args.ctx, tt.args.tenantId, tt.args.start, tt.args.end)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetTotalCount() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_apmServiceService_GetHasErrorCount(t *testing.T) {
	type args struct {
		ctx      context.Context
		tenantId string
		start    int64
		end      int64
	}
	tests := []struct {
		name    string
		args    args
		want    int64
		wantErr bool
	}{
		{"case1", args{ctx: context.Background()}, 1, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var msc *metricpb.UnimplementedMetricServiceServer
			QueryWithInfluxFormat := monkey.PatchInstanceMethod(reflect.TypeOf(msc), "QueryWithInfluxFormat",
				func(un *metricpb.UnimplementedMetricServiceServer, ctx context.Context, req *metricpb.QueryWithInfluxFormatRequest) (*metricpb.QueryWithInfluxFormatResponse, error) {

					return &metricpb.QueryWithInfluxFormatResponse{
						Results: []*metricpb.Result{
							{Series: []*metricpb.Serie{
								{Rows: []*metricpb.Row{
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-id"}},
											{Kind: &structpb.Value_BoolValue{BoolValue: true}},
										},
									},
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-id2"}},
											{Kind: &structpb.Value_BoolValue{BoolValue: false}},
										},
									},
								}},
							}},
						},
					}, nil
				})
			defer QueryWithInfluxFormat.Unpatch()

			s := &apmServiceService{p: &provider{Metric: msc}}

			got, _, err := s.GetHasErrorService(tt.args.ctx, tt.args.tenantId, tt.args.start, tt.args.end)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetHasErrorService() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GetHasErrorService() got = %v, want %v", got, tt.want)
				return
			}
		})
	}
}

func Test_apmServiceService_GetWithoutRequestCount(t *testing.T) {
	type args struct {
		ctx      context.Context
		tenantId string
		start    int64
		end      int64
	}
	tests := []struct {
		name    string
		args    args
		want    int64
		wantErr bool
	}{
		{"case1", args{ctx: context.Background()}, 1, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			var msc *metricpb.UnimplementedMetricServiceServer
			QueryWithInfluxFormat := monkey.PatchInstanceMethod(reflect.TypeOf(msc), "QueryWithInfluxFormat",
				func(un *metricpb.UnimplementedMetricServiceServer, ctx context.Context, req *metricpb.QueryWithInfluxFormatRequest) (*metricpb.QueryWithInfluxFormatResponse, error) {

					return &metricpb.QueryWithInfluxFormatResponse{
						Results: []*metricpb.Result{
							{Series: []*metricpb.Serie{
								{Rows: []*metricpb.Row{
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-id"}},
											{Kind: &structpb.Value_BoolValue{BoolValue: true}},
										},
									},
									{
										Values: []*structpb.Value{
											{Kind: &structpb.Value_StringValue{StringValue: "test-service-id2"}},
											{Kind: &structpb.Value_BoolValue{BoolValue: false}},
										},
									},
								}},
							}},
						},
					}, nil
				})
			defer QueryWithInfluxFormat.Unpatch()

			s := &apmServiceService{p: &provider{Metric: msc}}

			got, _, err := s.GetWithRequestService(tt.args.ctx, tt.args.tenantId, tt.args.start, tt.args.end)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetWithRequestService() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("GetWithRequestService() got = %v, want %v", got, tt.want)
			}
		})
	}
}
