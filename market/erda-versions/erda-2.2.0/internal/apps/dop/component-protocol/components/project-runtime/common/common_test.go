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

package common

import "testing"

func TestExitsWithoutCase(t *testing.T) {
	type args struct {
		s   string
		sub string
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "1",
			args: args{
				s:   "AbCdefg",
				sub: "bc",
			},
			want: true,
		},
		{
			name: "2",
			args: args{
				s:   "AbCdefg",
				sub: "abcdefg",
			},
			want: true,
		},
		{
			name: "3",
			args: args{
				s:   "",
				sub: "abcdefg",
			},
			want: false,
		},
		{
			name: "4",
			args: args{
				s:   "AbCdefg",
				sub: "",
			},
			want: true,
		},
		{
			name: "4",
			args: args{
				s:   "AbCdefg",
				sub: "hijk",
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := ExitsWithoutCase(tt.args.s, tt.args.sub); got != tt.want {
				t.Errorf("ExitsWithoutCase() = %v, want %v", got, tt.want)
			}
		})
	}
}
