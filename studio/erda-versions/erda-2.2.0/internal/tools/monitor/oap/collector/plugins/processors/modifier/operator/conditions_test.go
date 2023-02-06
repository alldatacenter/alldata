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

package operator

import (
	"testing"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
)

func TestKeyExist_Match(t *testing.T) {
	type fields struct {
		cfg ConditionCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			fields: fields{cfg: ConditionCfg{
				Key: "tags.aaa",
				Op:  "key_exist",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{"aaa": ""}}},
			want: true,
		},
		{
			fields: fields{cfg: ConditionCfg{
				Key: "tags.aaa",
				Op:  "key_exist",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{"bbb": ""}}},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			k := NewKeyExist(tt.fields.cfg)
			if got := k.Match(tt.args.item); got != tt.want {
				t.Errorf("Match() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestValueMatch_Match(t *testing.T) {
	type fields struct {
		cfg ConditionCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			fields: fields{
				cfg: ConditionCfg{
					Key:   "tags.pod_namespace",
					Value: "addon(.*?)",
				},
			},
			args: args{item: &metric.Metric{Tags: map[string]string{
				"pod_namespace": "group-addon-rocketmq--w28b7279764b64180843611a3dfbe0f4b",
			}}},
			want: true,
		},
		{
			fields: fields{
				cfg: ConditionCfg{
					Key:   "tags.pod_namespace",
					Value: "addon(.*?)",
				},
			},
			args: args{item: &metric.Metric{Tags: map[string]string{
				"pod_namespace": "addon-redis--yaf54cd190f484ddba84c681e4a9eba69",
			}}},
			want: true,
		},
		{
			fields: fields{
				cfg: ConditionCfg{
					Key:   "tags.pod_namespace",
					Value: "addon(.*?)",
				},
			},
			args: args{item: &metric.Metric{Tags: map[string]string{
				"pod_namespace": "app_name",
			}}},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			v := NewValueMatch(tt.fields.cfg)
			if got := v.Match(tt.args.item); got != tt.want {
				t.Errorf("Match() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestValueEmpty_Match(t *testing.T) {
	type fields struct {
		cfg ConditionCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			fields: fields{
				cfg: ConditionCfg{
					Key: "tags.aaa",
				},
			},
			args: args{
				item: &metric.Metric{Tags: map[string]string{"aaa": ""}},
			},
			want: true,
		},
		{
			fields: fields{
				cfg: ConditionCfg{
					Key: "tags.aaa",
				},
			},
			args: args{
				item: &metric.Metric{Tags: map[string]string{"aaa": "bbb"}},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ve := NewValueEmpty(tt.fields.cfg)
			if got := ve.Match(tt.args.item); got != tt.want {
				t.Errorf("Match() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestNoopCondition_Match(t *testing.T) {
	type fields struct {
		cfg ConditionCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			fields: fields{
				cfg: ConditionCfg{
					Key: "tags.aaa",
				},
			},
			args: args{
				item: &metric.Metric{Tags: map[string]string{"aaa": ""}},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			n := NewNoopCondition(tt.fields.cfg)
			if got := n.Match(tt.args.item); got != tt.want {
				t.Errorf("Match() = %v, want %v", got, tt.want)
			}
		})
	}
}
