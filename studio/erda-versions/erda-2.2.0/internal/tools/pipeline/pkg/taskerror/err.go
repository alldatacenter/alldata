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

package taskerror

import (
	"time"
)

type Error struct {
	Code string       `json:"code"`
	Msg  string       `json:"msg"`
	Ctx  ErrorContext `json:"ctx"`
}

type ErrorContext struct {
	StartTime time.Time `json:"startTime"`
	EndTime   time.Time `json:"endTime"`
	Count     uint64    `json:"count"`
}

func (c *ErrorContext) CalculateFrequencyPerHour() uint64 {
	if c.StartTime.IsZero() || c.EndTime.IsZero() || c.EndTime.Sub(c.StartTime) <= time.Hour {
		return c.Count
	}
	return uint64(float64(c.Count) / c.EndTime.Sub(c.StartTime).Hours())
}
