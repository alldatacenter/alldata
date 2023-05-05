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
	"reflect"
	"testing"
	"time"

	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func Test_issueCreateAdjuster_planStarted(t *testing.T) {
	type args struct {
		match    condition
		finished *time.Time
	}
	t1, _ := time.Parse("2006-01-02 15:04:05", "2022-02-01 12:12:12.0")
	t2, _ := time.Parse("2006-01-02 15:04:05", "2022-02-01 00:00:00.0")
	tests := []struct {
		name string
		args args
		want *time.Time
	}{
		{
			args: args{
				func() bool {
					return true
				},
				&t1,
			},
			want: &t2,
		},
		{
			args: args{
				func() bool {
					return true
				},
				nil,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			i := &issueCreateAdjuster{}
			if got := i.planStarted(tt.args.match, tt.args.finished); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("issueCreateAdjuster.planStarted() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_issueCreateAdjuster_planFinished(t *testing.T) {
	type fields struct {
		curTime *time.Time
	}
	t1, _ := time.Parse("2006-01-02 15:04:05", "2022-02-01 12:12:12.0")
	t2, _ := time.Parse("2006-01-02 15:04:05", "2022-01-20 00:00:00.0")
	curt, _ := time.Parse("2006-01-02 15:04:05", "2022-01-22 00:00:00.0")
	type args struct {
		match     condition
		iteration *dao.Iteration
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   *time.Time
	}{
		{
			args: args{
				match: func() bool {
					return true
				},
				iteration: &dao.Iteration{
					FinishedAt: &t1,
					StartedAt:  &t2,
				},
			},
			want: &t2,
		},
		{
			args: args{
				match: func() bool {
					return true
				},
			},
			want: nil,
		},
		{
			args: args{
				match: func() bool {
					return false
				},
			},
			want: nil,
		},
		{
			fields: fields{curTime: &curt},
			args: args{
				match: func() bool {
					return true
				},
				iteration: &dao.Iteration{
					FinishedAt: &t1,
					StartedAt:  &t2,
				},
			},
			want: &curt,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			i := &issueCreateAdjuster{
				curTime: tt.fields.curTime,
			}
			if got := i.planFinished(tt.args.match, tt.args.iteration); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("issueCreateAdjuster.planFinished() = %v, want %v", got, tt.want)
			}
		})
	}
}
