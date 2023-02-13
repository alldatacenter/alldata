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

package strutil

import (
	"math"
	"testing"
)

func TestString(t *testing.T) {
	tests := []struct {
		name string
		i    interface{}
		want string
	}{
		{name: "int", i: 1, want: "1"},
		{name: "int8 min", i: int8(math.MinInt8), want: "-128"},
		{name: "int8 max", i: int8(math.MaxInt8), want: "127"},
		{name: "int32 min", i: int32(math.MinInt32), want: "-2147483648"},
		{name: "int32 max", i: int32(math.MaxInt32), want: "2147483647"},
		{name: "int64 min", i: int64(math.MinInt64), want: "-9223372036854775808"},
		{name: "int64 max", i: int64(math.MaxInt64), want: "9223372036854775807"},
		{name: "uint", i: uint(1), want: "1"},
		{name: "uint8 max", i: uint8(math.MaxUint8), want: "255"},
		{name: "uint32 max", i: uint32(math.MaxUint32), want: "4294967295"},
		{name: "uint64 max", i: uint64(math.MaxUint64), want: "18446744073709551615"},
		//{name: "float32", i: float32(1324555555), want: "1324555555"},
		{name: "float64", i: float64(132455555555555.1), want: "132455555555555.1"},
		{name: "[]byte", i: []byte{'a', 'b'}, want: "ab"},
		{name: "string", i: "ab", want: "ab"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := String(tt.i); got != tt.want {
				t.Errorf("string() = %v, want %v", got, tt.want)
			}
		})
	}
}
