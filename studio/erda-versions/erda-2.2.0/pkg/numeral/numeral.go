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

import (
	"math"
	"strconv"
)

// Formalize every readable unit (e.g. 10Mi 100G 30K 1024) to byte
func FormalizeUnitToByte(raw string) (int64, error) {
	var length = len(raw)
	if length == 0 {
		return 0, nil
	}

	var base int64
	var roundPart string
	if raw[length-1:] == "i" {
		base = 1024
		roundPart = raw[:length-1]
		length = length - 1
	} else {
		base = 1000
		roundPart = raw
	}

	var round int
	switch roundPart[length-1:] {
	case "E":
		round = 6
	case "P":
		round = 5
	case "T":
		round = 4
	case "G":
		round = 3
	case "M":
		round = 2
	case "K":
		round = 1
	default:
		round = 0
	}

	var numberPart string
	if round != 0 {
		numberPart = roundPart[:length-1]
		length = length - 1
	} else {
		numberPart = roundPart
	}
	ret, err := strconv.ParseInt(numberPart, 10, 32)
	if err != nil {
		return 0, err
	}
	for i := 0; i < round; i++ {
		ret = ret * base
	}
	return ret, nil
}

// Round 保留小数点计算
func Round(f float64, n int) float64 {
	shift := math.Pow(10, float64(n))
	fv := 0.0000000001 + f //对浮点数产生.xxx999999999 计算不准进行处理

	return math.Floor(fv*shift+.5) / shift
}
