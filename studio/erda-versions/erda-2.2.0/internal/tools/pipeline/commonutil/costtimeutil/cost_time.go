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

package costtimeutil

import (
	"math"
	"time"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func CalculateTaskCostTimeSec(task *spec.PipelineTask) (cost int64) {
	if task.CostTimeSec >= 0 {
		return task.CostTimeSec
	}
	if task.TimeBegin.IsZero() {
		return -1
	}
	if task.TimeEnd.IsZero() && task.Status.IsEndStatus() { // 终态，但 timeEnd 异常为空
		task.TimeEnd = task.TimeUpdated
	}
	if task.TimeEnd.IsZero() { // 正在运行中
		return int64(time.Now().Sub(task.TimeBegin).Seconds())
	}
	return int64(math.Round(float64(task.TimeEnd.UnixNano()-task.TimeBegin.UnixNano()) / (1000 * 1000 * 1000)))
}

func CalculateTaskQueueTimeSec(task *spec.PipelineTask) (cost int64) {
	defer func() {
		if task.Status.IsEndStatus() && cost < 0 {
			cost = 0
		}
	}()
	if task.QueueTimeSec >= 0 {
		return task.QueueTimeSec
	}
	if task.Extra.TimeBeginQueue.IsZero() {
		return -1
	}
	if task.Extra.TimeEndQueue.IsZero() { // 正在运行中
		return int64(time.Now().Sub(task.Extra.TimeBeginQueue).Seconds())
	}
	return int64(task.Extra.TimeEndQueue.Sub(task.Extra.TimeBeginQueue).Seconds())
}

func CalculatePipelineCostTimeSec(p *spec.Pipeline) (cost int64) {
	defer func() {
		if p.Status.IsEndStatus() && cost < 0 {
			cost = 0
		}
	}()
	if p.CostTimeSec >= 0 {
		return p.CostTimeSec
	}
	// 还没开始
	if p.TimeBegin == nil || p.TimeBegin.IsZero() {
		return -1
	}
	// 正在运行中
	if p.TimeEnd == nil || p.TimeEnd.IsZero() {
		return int64(time.Now().Sub(*p.TimeBegin).Seconds())
	}
	return int64(p.TimeEnd.Sub(*p.TimeBegin).Seconds())
}
