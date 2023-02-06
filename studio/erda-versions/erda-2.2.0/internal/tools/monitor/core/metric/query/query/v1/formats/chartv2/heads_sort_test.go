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

package chartv2

import (
	"fmt"
	"sort"
	"testing"
)

type hs []map[string]interface{}

// Heads order by column，whern column>0,order by column size.
func (h hs) Len() int      { return len(h) }
func (h hs) Swap(i, j int) { h[i], h[j] = h[j], h[i] }
func (h hs) Less(i, j int) bool {
	if h[i]["column"].(int) < 0 && h[j]["column"].(int) > 0 {
		// need to swap
		return false
	}
	if h[i]["column"].(int) > 0 && h[j]["column"].(int) > 0 {
		return h[i]["column"].(int) < h[j]["column"].(int)
	}

	return true
}

var h1 = hs{
	map[string]interface{}{
		"title":  "第2列",
		"column": 2,
	},
	map[string]interface{}{
		"title":  "第1列",
		"column": 1,
	},
	map[string]interface{}{
		"title":  "随机列1",
		"column": -1,
	},
	map[string]interface{}{
		"title":  "第3列",
		"column": 3,
	},
	map[string]interface{}{
		"title":  "随机列2",
		"column": -1,
	},
	map[string]interface{}{
		"title":  "随机列3",
		"column": -1,
	},
}

func Test_heads_Len(t *testing.T) {
	tests := []struct {
		name string
		h    hs
		want int
	}{
		{name: "test-len-1", h: h1, want: 6},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.h.Len(); got != tt.want {
				t.Errorf("Len() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestHeadsSort(t *testing.T) {

	heads := heads{
		// {"column": 2, "title": "line 2"},
		{"column": 0, "title": "line 0"},
		{"column": 1, "title": "line 1"},
		{"column": -1, "title": "random a"},
		// {"column": -1, "title": "random b"},
	}
	fmt.Println(heads)
	sort.Sort(heads)
	fmt.Println(heads)
}

func Test_heads_Less(t *testing.T) {
	type args struct {
		i int
		j int
	}
	tests := []struct {
		name string
		h    hs
		args args
		want bool
	}{
		{
			// i=0，column=2
			// j=1，column=1
			// need to swap,so want false
			name: "test-less-1",
			h:    h1,
			args: args{
				i: 0,
				j: 1,
			},
			want: false,
		},
		{
			// i=1，column=2
			// j=2，column=-1
			// don't need to swap,so want true
			name: "test-less-2",
			h:    h1,
			args: args{
				i: 1,
				j: 2,
			},
			want: true,
		},
		{
			// i=2，column=-1
			// j=3，column=3
			// need to swap,so want false
			name: "test-less-3",
			h:    h1,
			args: args{
				i: 2,
				j: 3,
			},
			want: false,
		},
		{
			name: "test-less-4",
			h:    h1,
			args: args{
				i: 3,
				j: 4,
			},
			want: true,
		},
		{
			name: "test-less-5",
			h:    h1,
			args: args{
				i: 4,
				j: 5,
			},
			want: true,
		},
	}
	print("before sort:")
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.h.Less(tt.args.i, tt.args.j); got != tt.want {
				t.Errorf("Less() = %v, want %v", got, tt.want)
			}
			if !tt.want {
				tt.h.Swap(tt.args.i, tt.args.j)
			}
		})
	}
	print("after sort:")
}

func Test_heads_Swap(t *testing.T) {
	type args struct {
		i int
		j int
	}
	tests := []struct {
		name string
		h    hs
		args args
	}{
		{
			name: "test-swap-2",
			h:    h1,
			args: args{
				i: 0,
				j: 1,
			},
		},
		{
			name: "test-swap-1",
			h:    h1,
			args: args{
				i: 2,
				j: 3,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tt.h.Swap(tt.args.i, tt.args.j)
		})
	}
}
func print(msg string) {
	fmt.Println(msg)
	for _, v := range h1 {
		fmt.Print(v["title"], "|")
	}
	fmt.Println()
}
