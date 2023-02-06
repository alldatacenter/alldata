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

package util

import (
	"reflect"
	"testing"
)

func TestUniqStringSlice(t *testing.T) {
	type args struct {
		slice []string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			"normal",
			args{[]string{"e", "a", "c", "c", "a", "d", "b", "d", "b"}},
			[]string{"a", "b", "c", "d", "e"},
		},
		{
			"edge1",
			args{[]string{}},
			[]string{},
		},
		{
			"edge2",
			args{[]string{"a"}},
			[]string{"a"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := UniqStringSlice(tt.args.slice); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("UniqStringSlice() = %v, want %v", got, tt.want)
			}
		})
	}
}
