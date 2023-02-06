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

package utils

import (
	"fmt"
	"strconv"
	"strings"
	"time"
	"unicode"
)

const (
	Seconds = "seconds"
	Minutes = "minutes"
	Hours   = "hours"
)

func ConvertTimeToMS(t time.Time) int64 {
	if t.IsZero() {
		return 0
	}
	return t.UnixNano() / int64(time.Millisecond)
}

func ConvertUnitToMS(value int64, unit string) int64 {
	switch unit {
	case Minutes:
		return value * time.Minute.Nanoseconds() / time.Millisecond.Nanoseconds()
	case Hours:
		return value * time.Hour.Nanoseconds() / time.Millisecond.Nanoseconds()
	default:
		return -1
	}
}

func ConvertMSToUnit(t int64) (value int64, unit string) {
	ns := t * time.Millisecond.Nanoseconds()
	if ns > time.Hour.Nanoseconds() {
		return ns / time.Hour.Nanoseconds(), Hours
	} else if ns > time.Minute.Nanoseconds() {
		return ns / time.Minute.Nanoseconds(), Minutes
	} else {
		return ns / time.Second.Nanoseconds(), Seconds
	}
}

func ConvertStringToMS(value string, now int64) (int64, error) {
	if value == "" {
		return 0, nil
	}

	if now == 0 {
		now = time.Now().UnixNano() / int64(time.Millisecond)
	}
	if len(value) > 0 && unicode.IsDigit([]rune(value)[0]) {
		ts, err := strconv.ParseInt(value, 10, 64)
		if err != nil {
			return 0, fmt.Errorf("invalid timestamp %s", value)
		}
		return ts, nil
	}
	if strings.HasPrefix(value, "before_") {
		d, err := getMillisecond(value[len("before_"):])
		if err != nil {
			return 0, nil
		}
		return now - d, nil
	} else if strings.HasPrefix(value, "after_") {
		d, err := getMillisecond(value[len("after_"):])
		if err != nil {
			return 0, nil
		}
		return now + d, nil
	}
	return now, nil
}

func getMillisecond(value string) (int64, error) {
	d, err := time.ParseDuration(value)
	if err != nil {
		return 0, fmt.Errorf("invalid duration: %s", value)
	}
	return int64(d) / int64(time.Millisecond), nil
}
