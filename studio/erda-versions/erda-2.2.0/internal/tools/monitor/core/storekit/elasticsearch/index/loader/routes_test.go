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

package loader

import (
	"testing"
)

func Test_getSizeOfTenant(t *testing.T) {
	type args struct {
		ig   *IndexGroup
		size int64
	}
	tests := []struct {
		name string
		args args
		want int64
	}{
		{
			name: "normal",
			args: args{
				ig: &IndexGroup{
					Groups: map[string]*IndexGroup{
						"key2-abc": {
							Groups: map[string]*IndexGroup{
								"key3-abc": {
									List: []*IndexEntry{
										{StoreBytes: 1},
									},
								},
							},
							List: []*IndexEntry{
								{StoreBytes: 1},
								{StoreBytes: 1},
							},
							Fixed: []*IndexEntry{
								{StoreBytes: 2},
							},
						},
					},
				},
				size: 0,
			},
			want: 5,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getSizeOfTenant(tt.args.ig, tt.args.size); got != tt.want {
				t.Errorf("getSizeOfTenant() = %v, want %v", got, tt.want)
			}
		})
	}
}
