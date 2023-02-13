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

package dao

import (
	"reflect"
	"testing"
	"time"
)

func TestGetExpiryStatus(t *testing.T) {
	type args struct {
		planFinishedAt *time.Time
		timeBase       time.Time
	}

	timeBase := time.Date(2021, 9, 1, 0, 0, 0, 0, time.Now().Location())
	before := time.Date(2021, 8, 30, 0, 0, 0, 0, time.Now().Location())
	today := time.Date(2021, 9, 1, 0, 0, 0, 0, time.Now().Location())
	tomorrow := time.Date(2021, 9, 2, 0, 0, 0, 0, time.Now().Location())
	week := time.Date(2021, 9, 7, 0, 0, 0, 0, time.Now().Location())
	month := time.Date(2021, 9, 8, 0, 0, 0, 0, time.Now().Location())
	future := time.Date(2021, 10, 15, 0, 0, 0, 0, time.Now().Location())
	tests := []struct {
		name string
		args args
		want ExpireType
	}{
		{
			name: "N/A",
			args: args{
				planFinishedAt: nil,
			},
			want: ExpireTypeUndefined,
		},
		{
			name: "Expired",
			args: args{
				planFinishedAt: &before,
			},
			want: ExpireTypeExpired,
		},
		{
			name: "Today",
			args: args{
				planFinishedAt: &today,
			},
			want: ExpireTypeExpireIn1Day,
		},
		{
			name: "Tomorrow",
			args: args{
				planFinishedAt: &tomorrow,
			},
			want: ExpireTypeExpireIn2Days,
		},
		{
			name: "This week",
			args: args{
				planFinishedAt: &week,
			},
			want: ExpireTypeExpireIn7Days,
		},
		{
			name: "This mouth",
			args: args{
				planFinishedAt: &month,
			},
			want: ExpireTypeExpireIn30Days,
		},
		{
			name: "Future",
			args: args{
				planFinishedAt: &future,
			},
			want: ExpireTypeExpireInFuture,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetExpiryStatus(tt.args.planFinishedAt, timeBase); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getExpiryStatus() = %v, want %v", got, tt.want)
			}
		})
	}
}
