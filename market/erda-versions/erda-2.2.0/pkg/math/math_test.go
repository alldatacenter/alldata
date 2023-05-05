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

package math

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestAbsInt(t *testing.T) {
	x := AbsInt(10)
	assert.Equal(t, 10, x)

	x = AbsInt(0)
	assert.Equal(t, 0, x)

	x = AbsInt(-10)
	assert.Equal(t, 10, x)
}

func TestAbsInt32(t *testing.T) {
	x := AbsInt32(10)
	assert.Equal(t, int32(10), x)

	x = AbsInt32(0)
	assert.Equal(t, int32(0), x)

	x = AbsInt32(-10)
	assert.Equal(t, int32(10), x)
}

func TestAbsInt64(t *testing.T) {
	x := AbsInt64(10)
	assert.Equal(t, int64(10), x)

	x = AbsInt64(0)
	assert.Equal(t, int64(0), x)

	x = AbsInt64(-10)
	assert.Equal(t, int64(10), x)
}

func TestTwoDecimalPlaces(t *testing.T) {
	type args struct {
		value        float64
		digitsNumber int
	}
	tests := []struct {
		name string
		args args
		want float64
	}{
		// Two Decimal Places
		{"case1", args{value: 1.2345, digitsNumber: 2}, 1.23},
		{"case2", args{value: 1.2355, digitsNumber: 2}, 1.24},
		{"case3", args{value: 0.35000000000000003, digitsNumber: 2}, 0.35},
		{"case4", args{value: 0.7000000000000001, digitsNumber: 2}, 0.70},

		// Four Decimal Places
		{"case5", args{value: 1.2345123, digitsNumber: 4}, 1.2345},
		{"case6", args{value: 1.2355567, digitsNumber: 4}, 1.2356},
		{"case7", args{value: 0.35110000000000003, digitsNumber: 4}, 0.3511},
		{"case8", args{value: 0.7011000000000001, digitsNumber: 4}, 0.7011},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := DecimalPlacesWithDigitsNumber(tt.args.value, tt.args.digitsNumber); got != tt.want {
				t.Errorf("DecimalPlacesWithDigitsNumber() = %v, want %v", got, tt.want)
			}
		})
	}
}
