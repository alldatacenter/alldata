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
	"testing"

	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/chart"
)

func TestSelector(t *testing.T) {
	type args struct {
		viewType  string
		config    *config
		baseChart *chart.BaseChart
		ctx       context.Context
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{viewType: "service-overview", config: &config{View: []*View{{ViewType: "service-overview", Charts: []string{"overview"}}}}, baseChart: &chart.BaseChart{}, ctx: context.Background()}, false},
		{"case2", args{viewType: "topology_service_node", config: &config{View: []*View{{ViewType: "topology_service_node", Charts: []string{"topology_service_node"}}}}, baseChart: &chart.BaseChart{}, ctx: context.Background()}, false},
		{"case3", args{viewType: "rps_chart", config: &config{View: []*View{{ViewType: "rps_chart", Charts: []string{"rps_chart"}}}}, baseChart: &chart.BaseChart{}, ctx: context.Background()}, false},
		{"case4", args{viewType: "avg_duration_chart", config: &config{View: []*View{{ViewType: "avg_duration_chart", Charts: []string{"avg_duration_chart"}}}}, baseChart: &chart.BaseChart{}, ctx: context.Background()}, false},
		{"case5", args{viewType: "error_rate_chart", config: &config{View: []*View{{ViewType: "error_rate_chart", Charts: []string{"error_rate_chart"}}}}, baseChart: &chart.BaseChart{}, ctx: context.Background()}, false},
		{"case6", args{viewType: "http_code_chart", config: &config{View: []*View{{ViewType: "http_code_chart", Charts: []string{"http_code_chart"}}}}, baseChart: &chart.BaseChart{}, ctx: context.Background()}, false},
	}
	for _, tt := range tests {

		t.Run(tt.name, func(t *testing.T) {

			_, err := Selector(tt.args.viewType, tt.args.config, tt.args.baseChart, tt.args.ctx)
			if (err != nil) != tt.wantErr {
				t.Errorf("Selector() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
