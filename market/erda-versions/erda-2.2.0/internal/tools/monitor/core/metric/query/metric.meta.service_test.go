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
	context "context"
	reflect "reflect"
	testing "testing"

	servicehub "github.com/erda-project/erda-infra/base/servicehub"
	pb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
)

func Test_metricMetaService_ListMetricNames(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ListMetricNamesRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ListMetricNamesResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.metric.MetricMetaService",
		// 			`
		// erda.core.monitor.metric:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.ListMetricNamesRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.ListMetricNamesResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
			srv := hub.Service(tt.service).(pb.MetricMetaServiceServer)
			got, err := srv.ListMetricNames(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("metricMetaService.ListMetricNames() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("metricMetaService.ListMetricNames() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_metricMetaService_ListMetricMeta(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ListMetricMetaRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ListMetricMetaResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.metric.MetricMetaService",
		// 			`
		// erda.core.monitor.metric:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.ListMetricMetaRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.ListMetricMetaResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
			srv := hub.Service(tt.service).(pb.MetricMetaServiceServer)
			got, err := srv.ListMetricMeta(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("metricMetaService.ListMetricMeta() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("metricMetaService.ListMetricMeta() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_metricMetaService_ListMetricGroups(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.ListMetricGroupsRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.ListMetricGroupsResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.metric.MetricMetaService",
		// 			`
		// erda.core.monitor.metric:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.ListMetricGroupsRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.ListMetricGroupsResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
			srv := hub.Service(tt.service).(pb.MetricMetaServiceServer)
			got, err := srv.ListMetricGroups(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("metricMetaService.ListMetricGroups() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("metricMetaService.ListMetricGroups() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}

func Test_metricMetaService_GetMetricGroup(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetMetricGroupRequest
	}
	tests := []struct {
		name     string
		service  string
		config   string
		args     args
		wantResp *pb.GetMetricGroupResponse
		wantErr  bool
	}{
		// TODO: Add test cases.
		// 		{
		// 			"case 1",
		// 			"erda.core.monitor.metric.MetricMetaService",
		// 			`
		// erda.core.monitor.metric:
		// `,
		// 			args{
		// 				context.TODO(),
		// 				&pb.GetMetricGroupRequest{
		// 					// TODO: setup fields
		// 				},
		// 			},
		// 			&pb.GetMetricGroupResponse{
		// 				// TODO: setup fields.
		// 			},
		// 			false,
		// 		},
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
			srv := hub.Service(tt.service).(pb.MetricMetaServiceServer)
			got, err := srv.GetMetricGroup(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("metricMetaService.GetMetricGroup() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.wantResp) {
				t.Errorf("metricMetaService.GetMetricGroup() = %v, want %v", got, tt.wantResp)
			}
		})
	}
}
