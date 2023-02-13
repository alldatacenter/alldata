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

package numeral

import "testing"

func TestSubFloat64(t *testing.T) {
	type args struct {
		f1 float64
		f2 float64
	}
	tests := []struct {
		name string
		args args
		want float64
	}{
		{
			name: "0.41-0.4=0.01",
			args: args{
				f1: 0.41,
				f2: 0.4,
			},
			want: 0.01,
		},
		{
			name: "0.41-0.41=0",
			args: args{
				f1: 0.41,
				f2: 0.41,
			},
			want: 0,
		},
		{
			name: "0.4123-0.00001=0.41229",
			args: args{
				f1: 0.4123,
				f2: 0.00001,
			},
			want: 0.41229,
		},
		{
			name: "1-0.4=0.6",
			args: args{
				f1: 1,
				f2: 0.4,
			},
			want: 0.6,
		},
		{
			name: "3-1=2",
			args: args{
				f1: 3,
				f2: 1,
			},
			want: 2,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := SubFloat64(tt.args.f1, tt.args.f2); got != tt.want {
				t.Errorf("SubFloat64() = %v, want %v", got, tt.want)
			}
		})
	}
}
