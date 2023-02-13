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

package loop

import (
	"math"
	"time"
)

// CalculateInterval 传入当前循环次数，计算间隔时间
func (l *Loop) CalculateInterval(loopedTimes uint64) time.Duration {
	// if loopedTimes == 0 Means the first execution of the task, the time interval should be 0
	if loopedTimes == 0 {
		return time.Duration(0)
	}
	// 间隔 等于 初始间隔 乘以 衰退比例的 loopedTimes-1 次方
	// change loopedTimes+1 to loopedTimes-1 to make interval is accurate
	interval := time.Duration(float64(l.interval) * math.Pow(l.declineRatio, float64(loopedTimes-1)))
	// 间隔 不能大于衰退限制
	if interval > l.declineLimit {
		interval = l.declineLimit
	}
	// 间隔 不能小于0
	if interval < 0 {
		interval = time.Duration(math.Min(float64(l.declineLimit), float64(l.interval)))
	}
	return interval
}
