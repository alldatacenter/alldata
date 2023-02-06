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
	"github.com/shopspring/decimal"
)

func AbsInt(x int) int {
	y := x >> 31
	return (x ^ y) - y
}

func AbsInt32(x int32) int32 {
	y := x >> 31
	return (x ^ y) - y
}

func AbsInt64(x int64) int64 {
	y := x >> 63
	return (x ^ y) - y
}

// DecimalPlacesWithDigitsNumber Round to decimal places by number of digits
func DecimalPlacesWithDigitsNumber(value float64, digitsNumber int) float64 {
	f, _ := decimal.NewFromFloat(value).Round(int32(digitsNumber)).Float64()
	return f
}
