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
	"testing"
	"time"

	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func Test_inIterationInterval(t *testing.T) {
	type args struct {
		iteration *dao.Iteration
		t         *time.Time
	}
	t1, _ := time.Parse("2006-01-02 15:04:05", "2022-02-01 12:12:12.0")
	t2, _ := time.Parse("2006-01-02 15:04:05", "2022-03-01 18:19:20.0")
	t3, _ := time.Parse("2006-01-02 15:04:05", "2022-03-02 18:19:20.0")
	t4, _ := time.Parse("2006-01-02 15:04:05", "2022-03-01 22:19:20.0")
	t5, _ := time.Parse("2006-01-02 15:04:05", "2022-02-10 22:19:20.0")
	iteration := &dao.Iteration{
		StartedAt:  &t1,
		FinishedAt: &t2,
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			"inclusion valid",
			args{
				iteration,
				&t5,
			},
			true,
		},
		{
			"boundary valid",
			args{
				iteration,
				&t4,
			},
			true,
		},
		{
			"invalid",
			args{
				iteration,
				&t3,
			},
			false,
		},
		{
			"invalid time",
			args{
				iteration,
				nil,
			},
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := inIterationInterval(tt.args.iteration, tt.args.t); got != tt.want {
				t.Errorf("inIterationInterval() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_issueValidator_validateStateWithIteration(t *testing.T) {
	type args struct {
		c *issueValidationConfig
	}
	i1 := dao.Iteration{
		Title: "1",
		State: "FILED",
	}
	s1 := dao.IssueState{}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			args:    args{},
			wantErr: true,
		},
		{
			args:    args{&issueValidationConfig{}},
			wantErr: false,
		},
		{
			args:    args{&issueValidationConfig{&i1, &s1}},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			v := &issueValidator{}
			if err := v.validateStateWithIteration(tt.args.c); (err != nil) != tt.wantErr {
				t.Errorf("issueValidator.validateStateWithIteration() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_issueValidator_validateTimeWithInIteration(t *testing.T) {
	type args struct {
		c    *issueValidationConfig
		time *time.Time
	}
	t1, _ := time.Parse("2006-01-02 15:04:05", "2022-02-01 12:12:12.0")
	t2, _ := time.Parse("2006-01-02 15:04:05", "2022-03-01 18:19:20.0")
	i1 := dao.Iteration{
		Title:      "1",
		StartedAt:  &t1,
		FinishedAt: &t2,
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			wantErr: true,
		},
		{
			args:    args{c: &issueValidationConfig{}},
			wantErr: false,
		},
		{
			args:    args{c: &issueValidationConfig{iteration: &i1}},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			v := &issueValidator{}
			if err := v.validateTimeWithInIteration(tt.args.c, tt.args.time); (err != nil) != tt.wantErr {
				t.Errorf("issueValidator.validateTimeWithInIteration() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
