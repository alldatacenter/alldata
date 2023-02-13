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

package readable_time

import (
	"fmt"
	"time"
)

type ReadableTime struct {
	Second int64
	Minute int64
	Hour   int64
	Day    int64
	Month  int64
	Year   int64
}

func (t ReadableTime) String() string {
	if t.Year > 1 {
		return fmt.Sprintf("%d years ago", t.Year)
	}
	if t.Year*12+t.Month > 1 {
		return fmt.Sprintf("%d months ago", t.Year*12+t.Month)
	}
	if t.Month*30+t.Day > 1 {
		return fmt.Sprintf("%d days ago", t.Month*30+t.Day)
	}
	if t.Day*24+t.Hour > 1 {
		return fmt.Sprintf("%d hours ago", t.Day*24+t.Hour)
	}
	if t.Hour*60+t.Minute > 1 {
		return fmt.Sprintf("%d minutes ago", t.Hour*60+t.Minute)
	}
	if t.Minute*60+t.Second > 1 {
		return fmt.Sprintf("%d seconds ago", t.Minute*60+t.Second)
	}
	return "just now"
}
func Readable(t time.Time) ReadableTime {
	return readableTime(t, time.Now())
}
func readableTime(t time.Time, now time.Time) ReadableTime {
	dur := now.Sub(t)
	secs := int64(dur.Nanoseconds())
	y := secs / int64(time.Hour) / 24 / 365
	m := (secs - y*365*24*int64(time.Hour)) / int64(time.Hour) / 24 / 30
	d := (secs - y*365*24*int64(time.Hour) - m*30*24*int64(time.Hour)) / int64(time.Hour) / 24
	h := (secs - y*365*24*int64(time.Hour) - m*30*24*int64(time.Hour) - d*24*int64(time.Hour)) / int64(time.Hour)
	mi := (secs - y*365*24*int64(time.Hour) - m*30*24*int64(time.Hour) - d*24*int64(time.Hour) - h*int64(time.Hour)) / int64(time.Minute)
	s := (secs - y*365*24*int64(time.Hour) - m*30*24*int64(time.Hour) - d*24*int64(time.Hour) - h*int64(time.Hour) - mi*int64(time.Minute)) / int64(time.Second)
	return ReadableTime{int64(s), int64(mi), int64(h), int64(d), int64(m), int64(y)}
}
