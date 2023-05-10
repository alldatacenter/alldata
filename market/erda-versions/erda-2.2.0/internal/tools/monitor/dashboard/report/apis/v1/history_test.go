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

package reportapisv1

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-proto-go/tools/monitor/dashboard/report/pb"
)

func Test_constructHistoryDTOs(t *testing.T) {
	type args struct {
		histories []reportHistory
	}
	tests := []struct {
		name string
		args args
		want []*pb.ReportHistoryDTO
	}{
		{
			args: args{histories: []reportHistory{
				{
					ID:          1,
					Scope:       "a",
					ScopeID:     "b",
					TaskId:      1,
					DashboardId: "aa",
					Start:       1655457064000,
					End:         1655457065000,
				},
			}},
			want: []*pb.ReportHistoryDTO{
				{
					Id:      1,
					Scope:   "a",
					ScopeId: "b",
					Start:   1655457064000,
				},
			},
		},
		{
			args: args{histories: []reportHistory{
				{
					ID:          1,
					Scope:       "a",
					ScopeID:     "b",
					TaskId:      1,
					DashboardId: "aa",
					Start:       1655222400000,
					End:         1655457065000,
				},
			}},
			want: []*pb.ReportHistoryDTO{
				{
					Id:      1,
					Scope:   "a",
					ScopeId: "b",
					Start:   1655222400000,
					End:     1655457065000},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, tt.want, constructHistoryDTOs(tt.args.histories))
		})
	}
}

func Test_setHistoryTime(t *testing.T) {
	type args struct {
		history *reportHistory
		report  *reportTask
	}
	tests := []struct {
		name string
		args args
		want int64
	}{
		{
			args: args{
				history: &reportHistory{
					End: 1655457064000,
				},
				report: &reportTask{
					Type: daily,
				},
			},
			want: 1655457064000,
		},
		{
			args: args{
				history: &reportHistory{
					End: 1655457064000,
				},
				report: &reportTask{
					Type: weekly,
				},
			},
			want: 1654852264000,
		},
		{
			args: args{
				history: &reportHistory{
					End: 1655457064000,
				},
				report: &reportTask{
					Type: monthly,
				},
			},
			want: 1652778664000,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			setHistoryTime(tt.args.history, tt.args.report)
			assert.Equal(t, tt.want, tt.args.history.Start)
		})
	}
}

func Test_getListHistoriesQuery(t *testing.T) {
	type args struct {
		request *pb.ListHistoriesRequest
	}
	tests := []struct {
		name string
		args args
		want *reportHistoryQuery
	}{
		{
			args: args{request: &pb.ListHistoriesRequest{
				Scope:   "org",
				ScopeId: "erda",
				Start:   1655222400000,
				End:     1655457064000,
			}},
			want: &reportHistoryQuery{
				TaskId:        pointerUint64(0),
				Scope:         "org",
				ScopeID:       "erda",
				StartTime:     pointerInt64(1655222400000),
				EndTime:       pointerInt64(1655481600000),
				CreatedAtDesc: true,
				PreLoadTask:   false,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, tt.want, getListHistoriesQuery(tt.args.request))
		})
	}
}
