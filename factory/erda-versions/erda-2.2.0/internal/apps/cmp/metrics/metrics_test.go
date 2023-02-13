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

package metrics

import (
	"context"
	"encoding/json"
	"fmt"
	"testing"

	"golang.org/x/time/rate"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
)

func Test_isEmptyResponse(t *testing.T) {
	type args struct {
		resp *pb.QueryWithInfluxFormatResponse
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			"case1",
			args{
				resp: &pb.QueryWithInfluxFormatResponse{
					Results: []*pb.Result{
						{
							Series: []*pb.Serie{
								{
									Name:    "host_summary",
									Columns: []string{"last(cpu_cores_usage::field)"},
									Rows: []*pb.Row{
										{
											Values: []*structpb.Value{},
										},
									},
								},
							},
						},
					},
				},
			},
			true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ss, err := json.Marshal(tt.args.resp)
			fmt.Printf("ss: %s, err: %+v, raw: %+v\n", ss, err, tt.args.resp)
			if got := isEmptyResponse(tt.args.resp); got != tt.want {
				t.Errorf("isEmptyResponse() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestToInfluxReq1(t *testing.T) {
	type args struct {
		req  *MetricsRequest
		kind string
	}
	clusterName := "terminus-dev"
	namespace := "default"
	nodeReq := &MetricsRequest{
		UserId:       "1",
		OrgId:        "2",
		Cluster:      clusterName,
		Type:         Memory,
		Kind:         Node,
		NodeRequests: []MetricsReqInterface{&MetricsNodeRequest{Ip: "1.1.1.1"}},
	}
	nodeReq.NodeRequests = []MetricsReqInterface{&MetricsNodeRequest{Ip: "1.1.1.1", MetricsRequest: nodeReq}}
	podReq := &MetricsRequest{
		UserId:  "1",
		OrgId:   "2",
		Cluster: clusterName,
		Type:    Cpu,
		Kind:    Pod,
	}
	podReq.PodRequests = []MetricsReqInterface{&MetricsPodRequest{PodNamespace: namespace, Name: "telegraf-app-00e2f41199-z92wc", MetricsRequest: podReq}}
	tests := []struct {
		name    string
		args    args
		want    map[string]*MetricsReq
		want1   map[string]*MetricsData
		wantErr bool
	}{
		{
			name: "test",
			args: args{
				req:  nodeReq,
				kind: Node,
			},
			want: map[string]*MetricsReq{
				clusterName: {
					rawReq: &pb.QueryWithInfluxFormatRequest{
						Statement: NodeResourceUsageSelectStatement,
						Params:    map[string]*structpb.Value{"cluster_name": structpb.NewStringValue(clusterName)},
						Start:     "before_5m",
						End:       "now",
					},
				},
			},
		},
		{
			name: "test2",
			args: args{
				req:  podReq,
				kind: Pod,
			},
			want: map[string]*MetricsReq{
				namespace: {
					rawReq: &pb.QueryWithInfluxFormatRequest{
						Statement: PodResourceUsageSelectStatement,
						Params:    map[string]*structpb.Value{"cluster_name": structpb.NewStringValue(clusterName)},
						Start:     "before_5m",
						End:       "now",
					},
				},
			},
		},
	}
	m := &Metric{}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, _, err := m.ToInfluxReq(tt.args.req, tt.args.kind)
			if (err != nil) != tt.wantErr {
				t.Errorf("ToInfluxReq() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func TestMetric_Store(t *testing.T) {
	type fields struct {
		ctx           context.Context
		Metricq       pb.MetricServiceServer
		metricReqChan chan []*MetricsReq
		limiter       *rate.Limiter
	}
	type args struct {
		resp       *pb.QueryWithInfluxFormatResponse
		res        map[string]*MetricsData
		metricsReq *MetricsReq
	}

	res := map[string]*MetricsData{}
	res["111cpu"] = &MetricsData{
		Used:       1,
		Unallocate: 0,
		Left:       0,
	}
	res["111memory"] = &MetricsData{
		Used:       2,
		Unallocate: 0,
		Left:       0,
	}
	metricsReq := &pb.QueryWithInfluxFormatRequest{}
	queryReq := &pb.QueryWithInfluxFormatRequest{}
	queryReq.Start = "before_5m"
	queryReq.End = "now"
	queryReq.Statement = NodeResourceUsageSelectStatement
	queryReq.Params = map[string]*structpb.Value{
		"cluster_name": structpb.NewStringValue("1"),
		//"host_ip":      structpb.NewListValue(&structpb.ListValue{Values: []*structpb.Value{structpb.NewStringValue(nreq.IP())}}),
	}
	queryReq.Filters = []*pb.Filter{{
		Key:   "tags.host_ip",
		Op:    "in",
		Value: structpb.NewListValue(&structpb.ListValue{Values: []*structpb.Value{structpb.NewStringValue("1")}}),
	}}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   map[string]*MetricsData
	}{
		{
			name: "1",
			args: args{
				resp: &pb.QueryWithInfluxFormatResponse{
					Results: []*pb.Result{
						{
							Series: []*pb.Serie{
								{
									Name:    "host_summary",
									Columns: []string{"last(cpu_cores_usage::field)"},
									Rows: []*pb.Row{
										{
											Values: []*structpb.Value{structpb.NewNumberValue(1), structpb.NewNumberValue(2)},
										},
									},
								},
							},
						},
					},
				},
				res: res,
				metricsReq: &MetricsReq{
					rawReq:  metricsReq,
					resType: "cpu",
					resKind: "1",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			m := &Metric{
				ctx:     tt.fields.ctx,
				Metricq: tt.fields.Metricq,
				limiter: tt.fields.limiter,
			}
			m.Store(tt.args.resp, tt.args.metricsReq)
		})
	}
}
