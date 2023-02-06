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

package utils

import (
	"testing"
)

func TestGetMapValueString(t *testing.T) {
	type args struct {
		m   map[string]interface{}
		key []string
	}
	tests := []struct {
		name  string
		args  args
		want  string
		want1 bool
	}{
		{
			args: args{
				m: map[string]interface{}{
					"a":  "b",
					"c":  "d",
					"xa": "b",
				},
				key: []string{"a"},
			},
			want:  "b",
			want1: true,
		},
		{
			args: args{
				m: map[string]interface{}{
					"a":  "b",
					"c":  "d",
					"xa": "b",
				},
				key: []string{"aa", "xa"},
			},
			want:  "b",
			want1: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, got1 := GetMapValueString(tt.args.m, tt.args.key...)
			if got != tt.want {
				t.Errorf("GetMapValueString() got = %v, want %v", got, tt.want)
			}
			if got1 != tt.want1 {
				t.Errorf("GetMapValueString() got1 = %v, want %v", got1, tt.want1)
			}
		})
	}
}
