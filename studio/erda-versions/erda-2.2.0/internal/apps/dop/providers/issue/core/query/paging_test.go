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
)

func Test_getRelatedIDs(t *testing.T) {
	type args struct {
		lableRelationIDs []int64
		issueRelationIDs []int64
		isLabel          bool
		isIssue          bool
	}
	tests := []struct {
		name string
		args args
		want []int64
	}{
		{
			args: args{
				[]int64{1, 3},
				[]int64{3, 4},
				true,
				true,
			},
			want: []int64{3},
		},
		{
			args: args{
				[]int64{1, 3},
				[]int64{3, 4},
				false,
				true,
			},
			want: []int64{3, 4},
		},
		{
			args: args{
				[]int64{1, 3},
				[]int64{3, 4},
				true,
				false,
			},
			want: []int64{1, 3},
		},
		{
			args: args{
				[]int64{1, 3},
				[]int64{3, 4},
				false,
				false,
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getRelatedIDs(tt.args.lableRelationIDs, tt.args.issueRelationIDs, tt.args.isLabel, tt.args.isIssue); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getRelatedIDs() = %v, want %v", got, tt.want)
			}
		})
	}
}
