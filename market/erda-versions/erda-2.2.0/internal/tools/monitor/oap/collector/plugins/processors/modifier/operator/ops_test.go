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
	"reflect"
	"testing"

	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
)

func TestTrimPrefix_Operate(t1 *testing.T) {
	type fields struct {
		cfg ModifierCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   odata.ObservableData
	}{
		{
			name: "",
			fields: fields{
				cfg: ModifierCfg{
					Key:    "kubernetes_",
					Value:  "",
					Action: "trim_prefix",
				},
			},
			args: args{item: &metric.Metric{
				Tags: map[string]string{
					"kubernetes_pod_ip": "1.1.1.1",
				},
			}},
			want: &metric.Metric{
				Tags: map[string]string{
					"pod_ip": "1.1.1.1",
				},
			},
		},
	}
	for _, tt := range tests {
		t1.Run(tt.name, func(t1 *testing.T) {
			t := &TrimPrefix{
				cfg: tt.fields.cfg,
			}
			if got := t.Modify(tt.args.item); !reflect.DeepEqual(got, tt.want) {
				t1.Errorf("Modify() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestAdd_Operate(t *testing.T) {
	type fields struct {
		cfg ModifierCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   odata.ObservableData
	}{
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "tags.aaa",
				Value: "bbb",
			}},
			args: args{item: &log.Log{}},
			want: &log.Log{Tags: map[string]string{"aaa": "bbb"}},
		},
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "tags.aaa",
				Value: "bbb",
			}},
			args: args{item: &log.Log{Tags: map[string]string{"aaa": "ccc"}}},
			want: &log.Log{Tags: map[string]string{"aaa": "ccc"}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			a := &Add{
				cfg: tt.fields.cfg,
			}
			if got := a.Modify(tt.args.item); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Modify() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestCopy_Operate(t *testing.T) {
	type fields struct {
		cfg ModifierCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   odata.ObservableData
	}{
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "tags.aaa",
				Value: "tags.bbb",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{"aaa": "ccc"}}},
			want: &metric.Metric{Tags: map[string]string{"aaa": "ccc", "bbb": "ccc"}},
		},
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "tags.bbb",
				Value: "tags.bbb",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{"aaa": "ccc"}}},
			want: &metric.Metric{Tags: map[string]string{"aaa": "ccc"}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			c := &Copy{
				cfg: tt.fields.cfg,
			}
			if got := c.Modify(tt.args.item); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Modify() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestDrop_Operate(t *testing.T) {
	type fields struct {
		cfg ModifierCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   odata.ObservableData
	}{
		{
			fields: fields{cfg: ModifierCfg{
				Key: "tags.aaa",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{"aaa": "ccc"}}},
			want: &metric.Metric{Tags: map[string]string{}},
		},
		{
			fields: fields{cfg: ModifierCfg{
				Key: "tags.aaa",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{}}},
			want: &metric.Metric{Tags: map[string]string{}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			d := &Drop{
				cfg: tt.fields.cfg,
			}
			if got := d.Modify(tt.args.item); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Modify() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestRename_Operate(t *testing.T) {
	type fields struct {
		cfg ModifierCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   odata.ObservableData
	}{
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "tags.aaa",
				Value: "tags.bbb",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{"aaa": "ccc"}}},
			want: &metric.Metric{Tags: map[string]string{"bbb": "ccc"}},
		},
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "tags.ccc",
				Value: "tags.bbb",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{"aaa": "ccc"}}},
			want: &metric.Metric{Tags: map[string]string{"aaa": "ccc"}},
		},
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "fields.aaa",
				Value: "fields.bbb",
			}},
			args: args{item: &metric.Metric{Fields: map[string]interface{}{"aaa": "ccc"}}},
			want: &metric.Metric{Fields: map[string]interface{}{"bbb": "ccc"}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := &Rename{
				cfg: tt.fields.cfg,
			}
			if got := r.Modify(tt.args.item); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Modify() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestSet_Operate(t *testing.T) {
	type fields struct {
		cfg ModifierCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   odata.ObservableData
	}{
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "tags.aaa",
				Value: "bbb",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{"aaa": "ccc"}}},
			want: &metric.Metric{Tags: map[string]string{"aaa": "bbb"}},
		},
		{
			fields: fields{cfg: ModifierCfg{
				Key:   "tags.aaa",
				Value: "bbb",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{}}},
			want: &metric.Metric{Tags: map[string]string{"aaa": "bbb"}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &Set{
				cfg: tt.fields.cfg,
			}
			if got := s.Modify(tt.args.item); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Modify() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestJoin_Modify(t *testing.T) {
	type fields struct {
		cfg ModifierCfg
	}
	type args struct {
		item odata.ObservableData
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   odata.ObservableData
	}{
		{
			fields: fields{cfg: ModifierCfg{
				Keys:      []string{"tags.aaa", "tags.bbb"},
				Separator: ",",
				TargetKey: "tags.new",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{
				"aaa": "hello",
				"bbb": "world",
			}}},
			want: &metric.Metric{Tags: map[string]string{
				"aaa": "hello",
				"bbb": "world",
				"new": "hello,world",
			}},
		},
		{
			fields: fields{cfg: ModifierCfg{
				Keys:      []string{"tags.aaa", "tags.bbb"},
				Separator: ",",
				TargetKey: "tags.new",
			}},
			args: args{item: &metric.Metric{Tags: map[string]string{
				"aaa": "hello",
			}}},
			want: &metric.Metric{Tags: map[string]string{
				"aaa": "hello",
			}},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			j := &Join{
				cfg: tt.fields.cfg,
			}
			if got := j.Modify(tt.args.item); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Modify() = %v, want %v", got, tt.want)
			}
		})
	}
}
