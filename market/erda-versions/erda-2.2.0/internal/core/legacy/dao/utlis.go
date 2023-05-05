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

package dao

import (
	"math"
	"sort"
)

// Round 保留小数点计算
func Round(f float64, n int) float64 {
	shift := math.Pow(10, float64(n))
	fv := 0.0000000001 + f //对浮点数产生.xxx999999999 计算不准进行处理
	return math.Floor(fv*shift+.5) / shift
}

// Int64Slice 自定义 interface{},用于实现 []int64 的排序
type Int64Slice []int64

func (c Int64Slice) Len() int {
	return len(c)
}

func (c Int64Slice) Swap(i, j int) {
	c[i], c[j] = c[j], c[i]
}

func (c Int64Slice) Less(i, j int) bool {
	return c[i] < c[j]
}

func SortInt64Map(m map[int64]int64) Int64Slice {
	// 对 succMap key 进行排序
	keys := make(Int64Slice, len(m))
	i := 0
	for k := range m {
		keys[i] = k
		i++
	}
	sort.Sort(keys)

	return keys
}
