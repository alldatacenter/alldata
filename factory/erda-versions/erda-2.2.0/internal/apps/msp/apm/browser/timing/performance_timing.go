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

package timing

import (
	"strings"
)

// PerformanceTiming .
type PerformanceTiming struct {
	FirstPaintTime             int64
	ConnectEnd                 int64
	ConnectStart               int64
	DomComplete                int64
	DomContentLoadedEventEnd   int64
	DomContentLoadedEventStart int64
	DomInteractive             int64
	DomLoading                 int64
	DomainLookupEnd            int64
	DomainLookupStart          int64
	FetchStart                 int64
	LoadEventEnd               int64
	LoadEventStart             int64
	NavigationStart            int64
	RedirectEnd                int64
	RedirectStart              int64
	RequestStart               int64
	ResponseEnd                int64
	ResponseStart              int64
	SecureConnectionStart      int64
	UnloadEventEnd             int64
	UnloadEventStart           int64
}

// ParsePerformanceTiming .
func ParsePerformanceTiming(pt string) *PerformanceTiming {
	performanceTiming := PerformanceTiming{}
	if pt == "" {
		return &performanceTiming
	}

	times := strings.Split(pt, ",")
	ts := make([]int64, 22)
	for i := 0; i < 22 && i < len(times); i++ {
		ts[i] = parseInt64WithRadix(times[i], 0, 36)
	}
	performanceTiming.FirstPaintTime = ts[0]
	if performanceTiming.FirstPaintTime < 0 {
		performanceTiming.FirstPaintTime = 0
	}

	performanceTiming.ConnectEnd = ts[1]
	performanceTiming.ConnectStart = ts[2]
	performanceTiming.DomComplete = ts[3]
	performanceTiming.DomContentLoadedEventEnd = ts[4]
	performanceTiming.DomContentLoadedEventStart = ts[5]
	performanceTiming.DomInteractive = ts[6]
	performanceTiming.DomLoading = ts[7]
	performanceTiming.DomainLookupEnd = ts[8]
	performanceTiming.DomainLookupStart = ts[9]
	performanceTiming.FetchStart = ts[10]
	performanceTiming.LoadEventEnd = ts[11]
	performanceTiming.LoadEventStart = ts[12]
	performanceTiming.NavigationStart = ts[13]
	performanceTiming.RedirectEnd = ts[14]
	performanceTiming.RedirectStart = ts[15]
	performanceTiming.RequestStart = ts[16]
	performanceTiming.ResponseEnd = ts[17]
	performanceTiming.ResponseStart = ts[18]
	performanceTiming.SecureConnectionStart = ts[19]
	performanceTiming.UnloadEventEnd = ts[20]
	performanceTiming.UnloadEventStart = ts[21]

	return &performanceTiming
}
