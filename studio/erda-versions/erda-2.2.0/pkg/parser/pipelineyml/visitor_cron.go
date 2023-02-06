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

package pipelineyml

import (
	"strings"
	"time"

	"github.com/erda-project/erda/pkg/cron"
)

const (
	defaultListNextScheduleCount = 5
	maxListNextScheduleCount     = 1000
)

var DefaultCronCompensator = CronCompensator{
	Enable:               false,
	LatestFirst:          true,
	StopIfLatterExecuted: true,
}

type CronVisitor struct {
	// request
	cronStartTime *time.Time
	cronEndTime   *time.Time
	count         int

	// result
	nextTimes []time.Time
	isCron    bool
}

func NewCronVisitor(ops ...CronVisitorOption) *CronVisitor {
	var v CronVisitor
	v.cronStartTime = nil
	v.cronEndTime = nil
	v.count = defaultListNextScheduleCount

	for _, op := range ops {
		op(&v)
	}

	return &v
}

type CronVisitorOption func(*CronVisitor)

func WithCronStartEndTime(cronStartTime, cronEndTime *time.Time) CronVisitorOption {
	return func(v *CronVisitor) {
		v.cronStartTime = cronStartTime
		v.cronEndTime = cronEndTime
	}
}

func WithListNextScheduleCount(count int) CronVisitorOption {
	return func(v *CronVisitor) {
		v.count = count
	}
}

func (v *CronVisitor) Visit(s *Spec) {
	if s.Cron == "" {
		s.CronCompensator = nil
		v.isCron = false
		return
	}

	v.isCron = true

	var (
		schedule cron.Schedule
		err      error
	)

	switch fields := strings.Fields(s.Cron); len(fields) {
	case 7:
		fieldsWithoutYear := fields[:len(fields)-1]
		s.Cron = strings.Join(fieldsWithoutYear, " ")
		fallthrough
	case 6:
		schedule, err = cron.Parse(s.Cron)
	default:
		schedule, err = cron.ParseStandard(s.Cron)
	}
	if err != nil {
		s.appendError(err)
		return
	}

	now := time.Unix(time.Now().Unix(), 0)
	scheduleFrom := now
	if v.cronStartTime != nil {
		scheduleFrom = *v.cronStartTime
	}

	for {
		if len(v.nextTimes) >= maxListNextScheduleCount {
			break
		}
		if v.count >= 0 && len(v.nextTimes) >= v.count {
			break
		}
		nextTime := schedule.Next(scheduleFrom)
		if v.cronEndTime != nil && (*v.cronEndTime).Before(nextTime) {
			break
		}
		v.nextTimes = append(v.nextTimes, nextTime)
		scheduleFrom = nextTime
	}
}

func ListNextCronTime(cronExpr string, ops ...CronVisitorOption) ([]time.Time, error) {
	s := Spec{Cron: cronExpr}
	v := NewCronVisitor(ops...)
	s.Accept(v)
	return v.nextTimes, s.mergeErrors()
}

func IsCron(s *Spec) bool {
	v := NewCronVisitor(nil, nil)
	s.Accept(v)
	return v.isCron
}
