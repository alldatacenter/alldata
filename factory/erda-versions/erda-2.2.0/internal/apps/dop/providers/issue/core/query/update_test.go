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

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func Test_validPlanTime(t *testing.T) {
	type args struct {
		req   *pb.UpdateIssueRequest
		issue *dao.Issue
	}
	timeBase := time.Date(2021, 9, 1, 0, 0, 0, 0, time.Now().Location())
	today := time.Date(2021, 9, 1, 0, 0, 0, 0, time.Now().Location()).Format(time.RFC3339)
	tomorrow := time.Date(2021, 9, 2, 0, 0, 0, 0, time.Now().Location()).Format(time.RFC3339)
	nilTime := time.Unix(0, 0).Format(time.RFC3339)
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			args: args{
				req: &pb.UpdateIssueRequest{},
				issue: &dao.Issue{
					PlanStartedAt:  &timeBase,
					PlanFinishedAt: &timeBase,
				},
			},
		},
		{
			args: args{
				req: &pb.UpdateIssueRequest{
					PlanStartedAt:  &tomorrow,
					PlanFinishedAt: &today,
				},
				issue: &dao.Issue{},
			},
			wantErr: true,
		},
		{
			args: args{
				req: &pb.UpdateIssueRequest{
					PlanStartedAt: &nilTime,
				},
				issue: &dao.Issue{
					PlanStartedAt: &timeBase,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := validPlanTime(tt.args.req, tt.args.issue); (err != nil) != tt.wantErr {
				t.Errorf("validPlanTime() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_asTime(t *testing.T) {
	type args struct {
		s *string
	}
	s1, s2 := "", "1234"
	s3, s4 := "2022-06-24T00:00:00+08:00", "2022-06-23T16:00:00Z"
	t1 := time.Date(2022, 6, 24, 0, 0, 0, 0, time.Now().Location())
	tests := []struct {
		name string
		args args
		want *time.Time
	}{
		{
			args: args{nil},
			want: nil,
		},
		{
			args: args{&s1},
			want: nil,
		},
		{
			args: args{&s2},
			want: nil,
		},
		{
			args: args{&s3},
			want: &t1,
		},
		{
			args: args{&s4},
			want: &t1,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := asTime(tt.args.s); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("asTime() = %v, want %v", got, tt.want)
			}
		})
	}
}
