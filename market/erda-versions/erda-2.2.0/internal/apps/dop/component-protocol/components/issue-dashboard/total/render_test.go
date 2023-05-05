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

package total

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

// ExpireTypeUndefined      ExpireType = "Undefined"
// 	ExpireTypeExpired        ExpireType = "Expired"
// 	ExpireTypeExpireIn1Day   ExpireType = "ExpireIn1Day"
// 	ExpireTypeExpireIn2Days  ExpireType = "ExpireIn2Days"
// 	ExpireTypeExpireIn7Days  ExpireType = "ExpireIn7Days"
// 	ExpireTypeExpireIn30Days ExpireType = "ExpireIn30Days"
// 	ExpireTypeExpireInFuture ExpireType = "ExpireInFuture"

func TestStatsRetriever(t *testing.T) {
	type args struct {
		issueList []dao.IssueItem
	}
	tests := []struct {
		name string
		args args
		want common.Stats
	}{
		{
			name: "test",
			args: args{
				issueList: []dao.IssueItem{
					{
						ExpiryStatus: dao.ExpireTypeUndefined,
					},
					{
						ExpiryStatus: dao.ExpireTypeExpired,
					},
					{
						ExpiryStatus: dao.ExpireTypeExpireIn1Day,
					},
					{
						ExpiryStatus: dao.ExpireTypeExpireIn2Days,
					},
					{
						ExpiryStatus: dao.ExpireTypeExpireIn7Days,
					},
					{
						ExpiryStatus: dao.ExpireTypeExpireIn30Days,
					},
					{
						ExpiryStatus: dao.ExpireTypeExpireIn30Days,
					},
					{
						ExpiryStatus: dao.ExpireTypeExpireInFuture,
						ReopenCount:  1,
					},
					{
						Belong: "CLOSED",
					},
				},
			},
			want: common.Stats{
				Open:      "8",
				Expire:    "1",
				Today:     "1",
				Tomorrow:  "1",
				Week:      "1",
				Month:     "2",
				Undefined: "1",
				Reopen:    "1",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := StatsRetriever(tt.args.issueList); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("StatsRetriever() = %v, want %v", got, tt.want)
			}
		})
	}
}
