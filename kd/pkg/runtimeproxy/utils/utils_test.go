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

package utils

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestMergeMap(t *testing.T) {
	type args struct {
		a map[string]string
		b map[string]string
	}
	tests := []struct {
		name string
		args args
		want map[string]string
	}{
		{
			name: "a is null case",
			args: args{
				a: nil,
				b: map[string]string{},
			},
			want: map[string]string{},
		},
		{
			name: "b is nil",
			args: args{
				a: map[string]string{},
				b: nil,
			},
			want: map[string]string{},
		},
		{
			name: "b overwrites a with same key",
			args: args{
				a: map[string]string{
					"key1": "value1",
				},
				b: map[string]string{
					"key1": "value2",
				},
			},
			want: map[string]string{
				"key1": "value2",
			},
		},
		{
			name: "get union of a and b when no common keys exist",
			args: args{
				a: map[string]string{
					"key1": "value1",
				},
				b: map[string]string{
					"key2": "value2",
				},
			},
			want: map[string]string{
				"key1": "value1",
				"key2": "value2",
			},
		},
	}
	for _, tt := range tests {
		result := MergeMap(tt.args.a, tt.args.b)
		assert.Equal(t, tt.want, result)
	}
}
