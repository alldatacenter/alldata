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

package model

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
)

func TestDataFilter_Selected(t *testing.T) {
	type fields struct {
		cfg FilterConfig
	}
	type args struct {
		od odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			name: "keypass",
			fields: fields{cfg: FilterConfig{
				Keypass: map[string][]string{"name": {"ab*"}},
			}},
			args: args{od: &metric.Metric{
				Name:      "abcd",
				Timestamp: 0,
			}},
			want: true,
		},
		{
			fields: fields{cfg: FilterConfig{
				Keypass: map[string][]string{"name": {".*"}},
			}},
			args: args{od: &metric.Metric{
				Timestamp: 0,
			}},
			want: false,
		},
		{
			name: "keyinclude",
			fields: fields{cfg: FilterConfig{
				Keyinclude: []string{"name", "abc"},
			}},
			args: args{od: &metric.Metric{
				Name:      "abcd",
				Timestamp: 0,
			}},
			want: false,
		},
		{
			name: "keyexclude",
			fields: fields{cfg: FilterConfig{
				Keyexclude: []string{"tags.abc"},
			}},
			args: args{od: &metric.Metric{
				Name:      "abcd",
				Timestamp: 0,
				Tags: map[string]string{
					"abc": "hello",
				},
			}},
			want: false,
		},
		{
			name: "keydrop",
			fields: fields{cfg: FilterConfig{
				Keydrop: map[string][]string{"tags.container": {"POD"}},
			}},
			args: args{od: &metric.Metric{
				Name: "abcd",
				Tags: map[string]string{
					"container": "POD",
				},
			}},
			want: false,
		},
		{
			name: "composite",
			fields: fields{cfg: FilterConfig{
				Keypass:    map[string][]string{"name": {"abcd"}},
				Keydrop:    map[string][]string{"tags.container": {"POD"}},
				Keyinclude: []string{"name", "fields.container_cpu_usage_seconds_total", "tags.cluster_name", "tags.id"},
			}},
			args: args{od: &metric.Metric{
				Name: "abcd",
				Fields: map[string]interface{}{
					"container_cpu_usage_seconds_total": 500,
				},
				Tags: map[string]string{
					"cluster_name": "xxx",
					"id":           "aaa",
				},
			}},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			df, err := NewDataFilter(tt.fields.cfg)
			assert.Nil(t, err)
			if got := df.Selected(tt.args.od); got != tt.want {
				t.Errorf("Selected() = %v, want %v", got, tt.want)
			}
		})
	}
}
