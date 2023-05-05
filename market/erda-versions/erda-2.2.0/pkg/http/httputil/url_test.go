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

package httputil

import "testing"

func TestJoinPath(t *testing.T) {
	type args struct {
		appendRoot bool
		segments   []string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "empty",
			args: args{},
			want: "",
		},
		{
			name: "appendRoot",
			args: args{
				appendRoot: true,
			},
			want: "/",
		},
		{
			name: "1",
			args: args{
				segments: []string{
					"path1",
				},
			},
			want: "path1",
		},
		{
			name: "1",
			args: args{
				appendRoot: true,
				segments: []string{
					"path1",
				},
			},
			want: "/path1",
		},
		{
			name: "3",
			args: args{
				appendRoot: true,
				segments: []string{
					"path1",
					"path2",
					"path3",
				},
			},
			want: "/path1/path2/path3",
		},
		{
			name: "escape",
			args: args{
				appendRoot: true,
				segments: []string{
					"path1",
					"/",
					"path2",
				},
			},
			want: "/path1/%2F/path2",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := JoinPath(tt.args.appendRoot, tt.args.segments...); got != tt.want {
				t.Errorf("JoinPath() = %v, want %v", got, tt.want)
			}
		})
	}
}
