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

package time

import "testing"

func TestAutomaticConversionUnit(t *testing.T) {
	type args struct {
		v float64
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{"case-ns", args{v: 0.2}, "ns"},
		{"case-ns", args{v: 1}, "ns"},
		{"case-µs", args{v: 1000}, "µs"},
		{"case-ms", args{v: 1000000}, "ms"},
		{"case-s", args{v: 1000000000}, "s"},
		{"case-m", args{v: 60000000000}, "m"},
		{"case-h", args{v: 14400000000000}, "h"},
		{"case-h", args{v: 14400000000000}, "h"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, unit := AutomaticConversionUnit(tt.args.v)
			if unit != tt.want {
				t.Errorf("AutomaticConversionUnit() got1 = %v, want %v", unit, tt.want)
			}
		})
	}
}
