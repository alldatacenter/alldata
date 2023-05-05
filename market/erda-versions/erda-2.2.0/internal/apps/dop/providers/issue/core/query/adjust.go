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

package query

import (
	"time"

	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

type condition func() bool

type issueAdjuster interface {
	planFinished(condition, *dao.Iteration) *time.Time
}

type issueCreateAdjuster struct {
	curTime *time.Time
}

func (i *issueCreateAdjuster) planFinished(match condition, iteration *dao.Iteration) *time.Time {
	if !match() {
		return nil
	}
	if iteration == nil {
		return nil
	}
	if inIterationInterval(iteration, i.curTime) {
		return i.curTime
	}
	return iteration.StartedAt
}

func (i *issueCreateAdjuster) planStarted(match condition, finished *time.Time) *time.Time {
	if !match() {
		return nil
	}
	if finished == nil {
		return nil
	}
	t := finished.Truncate(24 * time.Hour)
	return &t
}
