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

package burnoutChart

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func TestSumWorkTime(t *testing.T) {
	tt := []struct {
		issue dao.IssueItem
		want  int
	}{
		{dao.IssueItem{ManHour: ""}, 0},
		{dao.IssueItem{ManHour: "{\"estimateTime\":120,\"thisElapsedTime\":120,\"elapsedTime\":120,\"remainingTime\":120,\"startTime\":\"\",\"workContent\":\"\",\"isModifiedRemainingTime\":true}"}, 120},
	}
	for _, v := range tt {
		workTime, err := sumWorkTime(v.issue)
		if err != nil {
			t.Error(err)
		}
		assert.Equal(t, v.want, workTime)
	}
}

func TestGetWeekDays(t *testing.T) {
	tt := []struct {
		intervals []int
		want      int
	}{
		{intervals: []int{2, 3}, want: 0},
		{intervals: []int{2, 8}, want: 5},
		{intervals: []int{2, 9}, want: 5},
		{intervals: []int{2, 10}, want: 5},
		{intervals: []int{2, 11}, want: 6},
		{intervals: []int{1, 30}, want: 21},
	}
	for i := range tt {
		dates := make([]time.Time, 0)
		for j := tt[i].intervals[0]; j <= tt[i].intervals[1]; j++ {
			dates = append(dates, time.Date(2022, 4, j, 0, 0, 0, 0, time.Local))
		}
		assert.Equal(t, tt[i].want, getWeekDays(dates))
	}
}
