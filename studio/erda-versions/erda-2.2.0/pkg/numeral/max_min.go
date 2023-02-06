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

// MinFloat64 return the minimal num of nums.
//
// 1, 2, 3, 4, 5, 0 => 0
// 1, 2, 3, 4, 5, 0 (ignoreZero) => 1
func MinFloat64(nums []float64, ignoreZeroOpt ...bool) float64 {
	ignoreZero := false
	if len(ignoreZeroOpt) > 0 {
		ignoreZero = ignoreZeroOpt[0]
	}
	var r float64 = 0
	if len(nums) == 0 {
		return r
	}
	// set r first
	for _, num := range nums {
		if num == 0 && ignoreZero {
			continue
		}
		r = num
		break
	}
	// compare
	for _, num := range nums {
		if num == 0 && ignoreZero {
			continue
		}
		if num < r {
			r = num
			continue
		}
	}
	return r
}

// MaxFloat64 return the maximum num of nums.
//
// 1, 2, 3, 4, 5, 0 => 5
func MaxFloat64(nums []float64) float64 {
	var r float64 = 0
	if len(nums) == 0 {
		return r
	}
	r = nums[0]
	for _, num := range nums {
		if num > r {
			r = num
			continue
		}
	}
	return r
}
