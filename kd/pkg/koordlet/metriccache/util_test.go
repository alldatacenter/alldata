/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package metriccache

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func Test_fieldPercentileOfMetricList(t *testing.T) {
	type args struct {
		metricsList interface{}
		fieldName   string
		percentile  float32
	}
	tests := []struct {
		name    string
		args    args
		want    float64
		wantErr bool
	}{
		{
			name: "do not panic",
			args: args{
				metricsList: 1,
				fieldName:   "v",
				percentile:  0.5,
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "trow error for illegal percentile",
			args: args{
				metricsList: []struct {
					v float32
				}{},
				fieldName:  "v",
				percentile: -1,
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "trow error for illegal list length",
			args: args{
				metricsList: []struct {
					v float32
				}{},
				fieldName:  "v",
				percentile: 0.5,
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "trow error for illegal element type",
			args: args{
				metricsList: []struct {
					v int
				}{
					{v: 1000},
				},
				fieldName:  "v",
				percentile: 0.5,
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "calculate single-element list",
			args: args{
				metricsList: []struct {
					v float32
				}{
					{v: 1000},
				},
				fieldName:  "v",
				percentile: 0.5,
			},
			want:    1000,
			wantErr: false,
		},
		{
			name: "calculate multi-element list of p90",
			args: args{
				metricsList: []struct {
					v float32
				}{
					{v: 800},
					{v: 100},
					{v: 200},
					{v: 600},
					{v: 700},
					{v: 0},
					{v: 500},
					{v: 400},
					{v: 300},
				},
				fieldName:  "v",
				percentile: 0.9,
			},
			want:    700,
			wantErr: false,
		},
		{
			name: "calculate multi-element list of p50",
			args: args{
				metricsList: []struct {
					v float32
				}{
					{v: 800},
					{v: 100},
					{v: 200},
					{v: 600},
					{v: 700},
					{v: 0},
					{v: 500},
					{v: 400},
					{v: 300},
				},
				fieldName:  "v",
				percentile: 0.5,
			},
			want:    300,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := fieldPercentileOfMetricList(tt.args.metricsList, AggregateParam{ValueFieldName: tt.args.fieldName}, tt.args.percentile)
			assert.Equal(t, true, tt.wantErr == (err != nil))
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_fieldLastOfMetricList(t *testing.T) {
	type args struct {
		metricsList interface{}
		param       AggregateParam
	}
	tests := []struct {
		name    string
		args    args
		want    float64
		wantErr bool
	}{
		{
			name: "do not panic for invalide metrics",
			args: args{
				metricsList: 1,
				param:       AggregateParam{ValueFieldName: "v", TimeFieldName: "T"},
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "do not panic for invalid ValueFieldName",
			args: args{
				metricsList: []struct {
					v float32
					T time.Time
				}{
					{v: 1.0, T: time.Now().Add(-5 * time.Second)},
					{v: 2.0, T: time.Now()},
				},
				param: AggregateParam{TimeFieldName: "T"},
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "do not panic for invalid TimeFieldName",
			args: args{
				metricsList: []struct {
					v float32
					t time.Time
				}{
					{v: 1.0, t: time.Now().Add(-5 * time.Second)},
					{v: 2.0, t: time.Now()},
				},
				param: AggregateParam{ValueFieldName: "v"},
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "trow error for illegal time",
			args: args{
				metricsList: []struct {
					v float32
					t time.Time
				}{
					{v: 1.0, t: time.Now().Add(-5 * time.Second)},
					{v: 2.0, t: time.Now().Add(-3 * time.Second)},
					{v: 3.0, t: time.Now()},
				},
				param: AggregateParam{ValueFieldName: "v", TimeFieldName: "t"},
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "trow error for illegal list length",
			args: args{
				metricsList: []struct {
					v float32
					T time.Time
				}{},
				param: AggregateParam{ValueFieldName: "v", TimeFieldName: "T"},
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "calculate single-element list",
			args: args{
				metricsList: []struct {
					v float32
					T time.Time
				}{
					{v: 3.0, T: time.Now()},
				},
				param: AggregateParam{ValueFieldName: "v", TimeFieldName: "T"},
			},
			want:    3.0,
			wantErr: false,
		},
		{
			name: "calculate multi-element list",
			args: args{
				metricsList: []struct {
					v float32
					T time.Time
				}{
					{v: 1.0, T: time.Now().Add(-5 * time.Second)},
					{v: 2.0, T: time.Now().Add(-3 * time.Second)},
					{v: 3.0, T: time.Now()},
				},
				param: AggregateParam{ValueFieldName: "v", TimeFieldName: "T"},
			},
			want:    3.0,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := fieldLastOfMetricList(tt.args.metricsList, tt.args.param)
			assert.Equal(t, true, tt.wantErr == (err != nil))
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_fieldLastOfMetricListBool(t *testing.T) {
	type args struct {
		metricsList interface{}
		param       AggregateParam
	}
	tests := []struct {
		name    string
		args    args
		want    bool
		wantErr bool
	}{
		{
			name: "do not panic for invalide metrics",
			args: args{
				metricsList: 1,
				param:       AggregateParam{ValueFieldName: "v", TimeFieldName: "T"},
			},
			want:    false,
			wantErr: true,
		},
		{
			name: "do not panic for invalid ValueFieldName",
			args: args{
				metricsList: []struct {
					v bool
					T time.Time
				}{
					{v: true, T: time.Now().Add(-5 * time.Second)},
					{v: false, T: time.Now()},
				},
				param: AggregateParam{TimeFieldName: "T"},
			},
			want:    false,
			wantErr: true,
		},
		{
			name: "do not panic for invalid TimeFieldName",
			args: args{
				metricsList: []struct {
					v bool
					t time.Time
				}{
					{v: true, t: time.Now().Add(-5 * time.Second)},
					{v: false, t: time.Now()},
				},
				param: AggregateParam{ValueFieldName: "v"},
			},
			want:    false,
			wantErr: true,
		},
		{
			name: "trow error for illegal time",
			args: args{
				metricsList: []struct {
					v bool
					t time.Time
				}{
					{v: true, t: time.Now().Add(-5 * time.Second)},
					{v: true, t: time.Now().Add(-3 * time.Second)},
					{v: true, t: time.Now()},
				},
				param: AggregateParam{ValueFieldName: "v", TimeFieldName: "t"},
			},
			want:    false,
			wantErr: true,
		},
		{
			name: "trow error for illegal list length",
			args: args{
				metricsList: []struct {
					v bool
					T time.Time
				}{},
				param: AggregateParam{ValueFieldName: "v", TimeFieldName: "T"},
			},
			want:    false,
			wantErr: true,
		},
		{
			name: "calculate single-element list",
			args: args{
				metricsList: []struct {
					v bool
					T time.Time
				}{
					{v: true, T: time.Now()},
				},
				param: AggregateParam{ValueFieldName: "v", TimeFieldName: "T"},
			},
			want:    true,
			wantErr: false,
		},
		{
			name: "calculate multi-element list",
			args: args{
				metricsList: []struct {
					v bool
					T time.Time
				}{
					{v: false, T: time.Now().Add(-5 * time.Second)},
					{v: false, T: time.Now().Add(-3 * time.Second)},
					{v: true, T: time.Now()},
				},
				param: AggregateParam{ValueFieldName: "v", TimeFieldName: "T"},
			},
			want:    true,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := fieldLastOfMetricListBool(tt.args.metricsList, tt.args.param)
			assert.Equal(t, true, tt.wantErr == (err != nil))
			assert.Equal(t, tt.want, got)
		})
	}
}
