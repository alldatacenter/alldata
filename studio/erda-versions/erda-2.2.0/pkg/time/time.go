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

import "github.com/erda-project/erda/pkg/math"

var (
	Nanosecond  float64 = 1                  // ns
	Microsecond         = 1000 * Nanosecond  // µs
	Millisecond         = 1000 * Microsecond // ms
	Second              = 1000 * Millisecond // s
	Minute              = 60 * Second        // m
	Hour                = 60 * Minute        // h
)

func AutomaticConversionUnit(v float64) (float64, string) {
	if v <= 0 {
		return 0, "ns"
	}
	switch {
	case v >= Hour:
		return math.DecimalPlacesWithDigitsNumber(v/Hour, 2), "h"
	case v >= Minute && v < Hour:
		return math.DecimalPlacesWithDigitsNumber(v/Minute, 2), "m"
	case v >= Second && v < Minute:
		return math.DecimalPlacesWithDigitsNumber(v/Second, 2), "s"
	case v >= Millisecond && v < Second:
		return math.DecimalPlacesWithDigitsNumber(v/Millisecond, 2), "ms"
	case v >= Microsecond && v < Millisecond:
		return math.DecimalPlacesWithDigitsNumber(v/Microsecond, 2), "µs"
	case v >= Nanosecond && v < Microsecond:
		return math.DecimalPlacesWithDigitsNumber(v/Nanosecond, 2), "ns"
	default:
		return math.DecimalPlacesWithDigitsNumber(v/Nanosecond, 2), "ns"
	}
}
