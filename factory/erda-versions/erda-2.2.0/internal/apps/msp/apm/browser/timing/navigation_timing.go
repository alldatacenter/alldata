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

// NavigationTiming .
type NavigationTiming struct {
	LoadTime          int64
	ReadyStart        int64
	DomReadyTime      int64
	ScriptExecuteTime int64
	RequestTime       int64
	ResponseTime      int64
	InitDomTreeTime   int64
	LoadEventTime     int64
	UnloadEventTime   int64
	AppCacheTime      int64
	ConnectTime       int64
	LookupDomainTime  int64
	RedirectTime      int64
}

// ParseNavigationTiming .
func ParseNavigationTiming(nt string) *NavigationTiming {
	navigationTiming := NavigationTiming{}
	if nt == "" {
		return &navigationTiming
	}
	times := strings.Split(nt, ",")
	ts := make([]int64, 13)
	t := parseInt64WithRadix(times[0], 0, 36)
	ts[0] = t
	for i := 1; i < 13 && i < len(times); i++ {
		ts[i] = parseInt64WithRadix(times[i], 0, 36)
		if ts[i] > t {
			ts[i] = 0
		}
	}
	navigationTiming.LoadTime = t
	navigationTiming.ReadyStart = ts[1]
	navigationTiming.DomReadyTime = ts[2]
	navigationTiming.ScriptExecuteTime = ts[3]
	navigationTiming.RequestTime = ts[4]
	navigationTiming.ResponseTime = ts[5]
	navigationTiming.InitDomTreeTime = ts[6]
	navigationTiming.LoadEventTime = ts[7]
	navigationTiming.UnloadEventTime = ts[8]
	navigationTiming.AppCacheTime = ts[9]
	navigationTiming.ConnectTime = ts[10]
	navigationTiming.LookupDomainTime = ts[11]
	navigationTiming.RedirectTime = ts[12]
	return &navigationTiming
}
