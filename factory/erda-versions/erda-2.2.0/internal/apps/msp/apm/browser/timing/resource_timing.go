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
	"fmt"
	"strings"

	jsoniter "github.com/json-iterator/go"
)

var (
	json          = jsoniter.ConfigCompatibleWithStandardLibrary
	initiatorType = map[string]string{
		"0": "other",
		"1": "img",
		"2": "link",
		"3": "script",
		"4": "css",
		"5": "xmlhttprequest",
		"6": "iframe",
		"7": "image",
	}
)

// ResourceTiming .
type ResourceTiming struct {
	Name                  string
	InitiatorType         int64
	StartTime             int64
	ResponseEnd           int64
	ResponseStart         int64
	RequestStart          int64
	ConnectEnd            int64
	SecureConnectionStart int64
	ConnectStart          int64
	DomainLookupEnd       int64
	DomainLookupStart     int64
	RedirectEnd           int64
	RedirectStart         int64
}

// ResourceTimingList .
type ResourceTimingList []ResourceTiming

// Timing .
func (rt ResourceTimingList) Timing() int64 {
	if len(rt) == 0 {
		return 0
	}
	var start, end int64
	for _, val := range rt {
		if start == 0 || val.StartTime < start {
			start = val.StartTime
		}
		if end == 0 || val.ResponseEnd > end {
			end = val.ResponseEnd
		}
	}
	rlt := end - start
	if rlt < 0 {
		rlt = 0
	}
	return rlt
}

// DNSCount .
func (rt ResourceTimingList) DNSCount() int64 {
	if len(rt) == 0 {
		return 0
	}
	var count int64
	for _, val := range rt {
		if val.DomainLookupEnd-val.DomainLookupStart > 0 {
			count++
		}
	}
	return count
}

// ParseResourceTiming .
func ParseResourceTiming(s string) (ResourceTimingList, error) {
	var rts ResourceTimingList
	if s == "" {
		return rts, nil
	}

	var m = map[string]interface{}{}
	if err := json.Unmarshal([]byte(s), &m); err != nil {
		return rts, fmt.Errorf("fail to parse ResourceTiming, data: %s", s)
	}

	var resMap = map[string]string{}
	decodeResource(m, resMap, "")

	for key, value := range resMap {
		if value == "" || len(value) < 2 {
			continue
		}
		typeKey := value[0:1]
		if _, ok := initiatorType[typeKey]; !ok {
			continue
		}

		timing := value[1:]
		times := resTimingDecode(timing)

		rt := ResourceTiming{
			Name:                  key,
			InitiatorType:         parseInt64(typeKey, 0),
			StartTime:             times[0],
			ResponseEnd:           times[1],
			ResponseStart:         times[2],
			RequestStart:          times[3],
			ConnectEnd:            times[4],
			SecureConnectionStart: times[5],
			ConnectStart:          times[6],
			DomainLookupEnd:       times[7],
			DomainLookupStart:     times[8],
			RedirectEnd:           times[9],
			RedirectStart:         times[10],
		}
		rts = append(rts, rt)
	}
	return rts, nil
}

func decodeResource(input map[string]interface{}, output map[string]string, preKey string) {
	for key, value := range input {
		if m, ok := value.(map[string]interface{}); ok {
			decodeResource(m, output, preKey+key)
		} else {
			if v, ok := value.(string); ok {
				output[preKey+key] = v
			}
		}
	}
}

func resTimingDecode(timing string) [11]int64 {
	var times [11]int64
	parts := strings.Split(timing, ",")
	t := parseInt64WithRadix(parts[0], 0, 36)
	times[0] = t
	for i := 1; i < 11 && i < len(parts); i++ {
		times[i] = t + parseInt64WithRadix(parts[i], 0, 36)
	}
	return times
}
