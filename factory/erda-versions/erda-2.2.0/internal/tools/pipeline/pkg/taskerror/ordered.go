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
	"fmt"
	"sort"
	"strings"
	"time"
)

type OrderedErrors []*Error

func (o OrderedErrors) Len() int           { return len(o) }
func (o OrderedErrors) Less(i, j int) bool { return o[i].Ctx.EndTime.Before(o[j].Ctx.EndTime) }
func (o OrderedErrors) Swap(i, j int)      { o[i], o[j] = o[j], o[i] }

func (o OrderedErrors) ConvertErrors() {
	for _, response := range o {
		if response.Ctx.Count > 1 {
			response.Msg = fmt.Sprintf("%s\nstartTime: %s\nendTime: %s\ncount: %d",
				response.Msg, response.Ctx.StartTime.Format("2006-01-02 15:04:05"),
				response.Ctx.EndTime.Format("2006-01-02 15:04:05"), response.Ctx.Count)
		}
	}
}

func (o OrderedErrors) AppendError(errs ...*Error) OrderedErrors {
	if len(errs) == 0 {
		return o
	}
	var ordered OrderedErrors
	for _, g := range o {
		ordered = append(ordered, g)
	}

	var newOrderedErrs OrderedErrors
	now := time.Now()
	for index, g := range errs {
		// TODO action agent should add err start time and end time
		if g.Ctx.StartTime.IsZero() {
			g.Ctx.StartTime = now.Add(time.Duration(index) * time.Millisecond)
		}
		if g.Ctx.EndTime.IsZero() {
			g.Ctx.EndTime = now.Add(time.Duration(index) * time.Millisecond)
		}
		if g.Ctx.Count == 0 {
			g.Ctx.Count = 1
		}
		newOrderedErrs = append(newOrderedErrs, g)
	}
	sort.Sort(newOrderedErrs)

	var lastErr *Error
	if len(ordered) != 0 {
		lastErr = ordered[len(ordered)-1]
	}

	for _, g := range newOrderedErrs {
		if lastErr == nil {
			ordered = append(ordered, g)
			lastErr = g
			continue
		}

		if strings.EqualFold(lastErr.Msg, g.Msg) {
			if !g.Ctx.StartTime.IsZero() && g.Ctx.StartTime.Before(lastErr.Ctx.StartTime) {
				lastErr.Ctx.StartTime = g.Ctx.StartTime
			}
			if g.Ctx.EndTime.After(lastErr.Ctx.EndTime) {
				lastErr.Ctx.EndTime = g.Ctx.EndTime
			}
			lastErr.Ctx.Count++
			continue
		} else {
			ordered = append(ordered, g)
			lastErr = g
		}
	}
	return ordered
}
