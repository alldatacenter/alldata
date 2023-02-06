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

package aggregator

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

func Test_provider_Init(t *testing.T) {
	type fields struct {
		Cfg *config
	}
	tests := []struct {
		name    string
		fields  fields
		wantErr bool
	}{
		{
			fields: fields{
				Cfg: &config{
					Rules: []RuleConfig{
						{
							Func:      "rate",
							Args:      []interface{}{"counter_a"},
							TargetKey: "rate_a",
						},
					},
				},
			},
			wantErr: false,
		},
		{
			fields: fields{
				Cfg: &config{
					Rules: []RuleConfig{
						{
							Func:      "rate",
							Args:      []interface{}{map[string]string{"a": "a"}},
							TargetKey: "rate_a",
						},
					},
				},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				Cfg: tt.fields.Cfg,
			}

			if err := p.Init(nil); (err != nil) != tt.wantErr {
				t.Errorf("Init() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_provider_add(t *testing.T) {
	type fields struct {
		Cfg *config
	}
	type args struct {
		in []*metric.Metric
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   []*metric.Metric
	}{
		{
			fields: fields{
				Cfg: &config{
					Rules: []RuleConfig{
						{
							Func:      "rate",
							Args:      []interface{}{"counter_a"},
							TargetKey: "rate_a",
						},
						{
							Func:      "*",
							Args:      []interface{}{"rate_a", 100},
							TargetKey: "rate_a",
						},
					},
				},
			},
			args: args{
				in: []*metric.Metric{
					{
						Name:      "name",
						Timestamp: int64(time.Second * 1649397600),
						Fields: map[string]interface{}{
							"counter_a": float64(1),
						},
					},
					{
						Name:      "name",
						Timestamp: int64(time.Second * 1649397601),
						Fields: map[string]interface{}{
							"counter_a": float64(2),
						},
					},
					{
						Name:      "name",
						Timestamp: int64(time.Second * 1649397602),
						Fields: map[string]interface{}{
							"counter_a": float64(3),
						},
					},
				},
			},
			want: []*metric.Metric{
				{
					Name:      "name",
					Timestamp: int64(1649397600 * time.Second),
					Fields: map[string]interface{}{
						"counter_a": float64(1),
					},
				},
				{
					Name:      "name",
					Timestamp: int64(1649397601 * time.Second),
					Fields: map[string]interface{}{
						"counter_a": float64(2),
						"rate_a":    float64(100),
					},
				},
				{
					Name:      "name",
					Timestamp: int64(1649397602 * time.Second),
					Fields: map[string]interface{}{
						"counter_a": float64(3),
						"rate_a":    float64(100),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				Cfg: tt.fields.Cfg,
			}
			assert.Nil(t, p.Init(nil))
			for i := 0; i < len(tt.args.in); i++ {
				assert.Equal(t, tt.want[i], p.add(tt.args.in[i]))
			}
		})
	}
}
