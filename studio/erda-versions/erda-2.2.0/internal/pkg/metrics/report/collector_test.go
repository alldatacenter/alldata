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

package report

import (
	"fmt"
	"net/http"
	"testing"
)

func Test_reportClient_serialize(t *testing.T) {
	type fields struct {
		cfg        *config
		httpClient *http.Client
	}
	type args struct {
		group *NamedMetrics
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test_serialize",
			fields: fields{
				cfg: &config{
					ReportConfig: ReportConfig{
						Collector: CollectorConfig{
							Addr:     "collector.default.svc.cluster.local:7076",
							UserName: "admin",
							Password: "Cqq",
							Retry:    2,
						},
					},
				},
				httpClient: new(http.Client),
			},
			args: args{
				group: &NamedMetrics{
					Name: "_metric_meta",
					Metrics: Metrics{
						&Metric{
							Name:      "_metric_meta",
							Timestamp: 1614583470000,
							Tags: map[string]string{
								"cluster_name": "terminus-dev",
								"meta":         "true",
								"metric_name":  "application_db",
							},
							Fields: map[string]interface{}{
								"fields": []string{"value:number"},
								"tags":   []string{"is_edge", "org_id"},
							},
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &ReportClient{
				CFG:        tt.fields.cfg,
				HttpClient: tt.fields.httpClient,
			}
			got, err := c.serialize(tt.args.group)
			if (err != nil) != tt.wantErr {
				t.Errorf("serialize() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			fmt.Println(got)
		})
	}
}

func Test_reportClient_group(t *testing.T) {
	type fields struct {
		cfg        *config
		httpClient *http.Client
	}
	type args struct {
		in []*Metric
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		{
			name: "test_group",
			fields: fields{
				cfg: &config{
					ReportConfig: ReportConfig{
						Collector: CollectorConfig{
							Addr:     "collector.default.svc.cluster.local:7076",
							UserName: "admin",
							Password: "Cqq",
							Retry:    2,
						},
					},
				},
				httpClient: new(http.Client),
			},
			args: args{
				in: Metrics{
					&Metric{
						Name:      "span",
						Timestamp: 1614583470000,
						Tags: map[string]string{
							"cluster_name": "terminus-dev",
							"meta":         "true",
							"metric_name":  "application_db",
						},
						Fields: map[string]interface{}{
							"fields": []string{"value:number"},
							"tags":   []string{"is_edge", "org_id"},
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &ReportClient{
				CFG:        tt.fields.cfg,
				HttpClient: tt.fields.httpClient,
			}
			if got := c.group(tt.args.in); got != nil {
				for _, v := range got {
					g := v
					fmt.Printf("%+v\n", *g)
				}
			}
		})
	}
}
