/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package cpuset

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_MergeCPUSet(t *testing.T) {
	type args struct {
		old []int32
		new []int32
	}
	tests := []struct {
		name string
		args args
		want []int32
	}{
		{
			name: "do not panic on empty input",
			want: []int32{},
		},
		{
			name: "merge and sort correctly for disjoint input",
			args: args{
				old: []int32{0, 1, 2},
				new: []int32{5, 8, 7},
			},
			want: []int32{0, 1, 2, 5, 7, 8},
		},
		{
			name: "merge and sort correctly for incomplete input",
			args: args{
				new: []int32{1, 0, 2},
			},
			want: []int32{0, 1, 2},
		},
		{
			name: "merge and sort correctly for intersecting input",
			args: args{
				old: []int32{2, 1, 0},
				new: []int32{1, 7, 5},
			},
			want: []int32{0, 1, 2, 5, 7},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := MergeCPUSet(tt.args.old, tt.args.new)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_ParseCPUSetStr(t *testing.T) {
	type args struct {
		cpusetStr string
	}
	tests := []struct {
		name    string
		args    args
		want    []int32
		wantErr bool
	}{
		{
			name: "do not panic on empty input",
			want: []int32{},
		},
		{
			name:    "parse mixed cpuset correctly",
			args:    args{cpusetStr: "0-5,34,46-48"},
			want:    []int32{0, 1, 2, 3, 4, 5, 34, 46, 47, 48},
			wantErr: false,
		},
		{
			name:    "parse empty content",
			args:    args{cpusetStr: "    \n"},
			want:    nil,
			wantErr: true,
		},
		{
			name:    "parse and throw an error for illegal input",
			args:    args{cpusetStr: "   0-5,a,10-13 "},
			want:    nil,
			wantErr: true,
		},
		{
			name:    "parse and throw an error for illegal input 1",
			args:    args{cpusetStr: "   0,1-b,10-11,13 "},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := ParseCPUSetStr(tt.args.cpusetStr)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_GenerateCPUSetStr(t *testing.T) {
	type args struct {
		cpuset []int32
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "generate for empty input",
		},
		{
			name: "generate for single-element input",
			args: args{cpuset: []int32{1}},
			want: "1",
		},
		{
			name: "generate for multi-element input",
			args: args{cpuset: []int32{5, 3, 1, 0}},
			want: "0-1,3,5",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := GenerateCPUSetStr(tt.args.cpuset)
			assert.Equal(t, tt.want, got)
		})
	}
}
