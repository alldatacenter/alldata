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

package filetree

import "testing"

func Test_getBranchStr(t *testing.T) {
	type args struct {
		name string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "test 1",
			args: args{
				name: "1/2/tree/feature/blob/pipeline.yml",
			},
			want: "feature/blob",
		},
		{
			name: "test 2",
			args: args{
				name: "1/2/tree/feature/blob/.dice/pipelines/pipeline.yml",
			},
			want: "feature/blob",
		},
		{
			name: "test 3",
			args: args{
				name: "1/2/tree/pipeline.yml/pipeline.yml",
			},
			want: "pipeline.yml",
		},
		{
			name: "test 4",
			args: args{
				name: "1/2/tree/erda/.erda/pipelines/pipeline.yml",
			},
			want: "erda",
		},
		{
			name: "test 5",
			args: args{
				name: "1/2/tree/feature/.erda/.erda/pipelines/pipeline.yml",
			},
			want: "feature/.erda",
		},
		{
			name: "test 5",
			args: args{
				name: "1/2/tree/feature/pipeline.yml/.erda/pipelines/pipeline.yml",
			},
			want: "feature/pipeline.yml",
		},
		{
			name: "test 6",
			args: args{
				name: "1/2/tree/feature/sad",
			},
			want: "feature/sad",
		},
		{
			name: "test 7",
			args: args{
				name: "1/2/tree/feature/.diced",
			},
			want: "feature/.diced",
		},
		{
			name: "test 8",
			args: args{
				name: "1/2/tree/feature/.dice/.dice",
			},
			want: "feature/.dice",
		},
		{
			name: "test 9",
			args: args{
				name: "1/2/tree/.dice",
			},
			want: ".dice",
		},
		{
			name: "test 10",
			args: args{
				name: "1/2/tree/.dice/.diced/.erda",
			},
			want: ".dice/.diced",
		},
		{
			name: "test 11",
			args: args{
				name: "1/2/tree/develop/.dice",
			},
			want: "develop",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getBranchStr(tt.args.name); got != tt.want {
				t.Errorf("getBranchStr() = %v, want %v", got, tt.want)
			}
		})
	}
}
