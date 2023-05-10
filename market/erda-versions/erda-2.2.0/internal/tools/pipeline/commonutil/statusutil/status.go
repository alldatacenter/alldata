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

package statusutil

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

// CalculatePipelineStatusV2
func CalculatePipelineStatusV2(tasks []*spec.PipelineTask) apistructs.PipelineStatus {
	total := len(tasks)
	var successNum int
	var failedNum int
	var pauseNum int
	var runningNum int
	var analyzedNum int
	var stopByUserNum int

	for _, task := range tasks {
		// treat as success
		if task.Status.IsSuccessStatus() || (task.Status.IsFailedStatus() && task.Extra.AllowFailure) || task.Status == apistructs.PipelineStatusDisabled {
			successNum++
			continue
		}
		if task.Status == apistructs.PipelineStatusStopByUser {
			stopByUserNum++
			continue
		}
		if task.Status.IsFailedStatus() {
			failedNum++
			continue
		}
		if task.Status == apistructs.PipelineStatusAnalyzed {
			analyzedNum++
			continue
		}
		if task.Status == apistructs.PipelineStatusPaused {
			pauseNum++
			continue
		}
		runningNum++
	}

	allDone := CalculatePipelineTaskAllDone(tasks)
	if allDone {
		// success
		if successNum == total {
			return apistructs.PipelineStatusSuccess
		}
		// stopByUser
		if stopByUserNum > 0 {
			return apistructs.PipelineStatusStopByUser
		}
		// not success or stopByUser, means failed
		return apistructs.PipelineStatusFailed
	}
	// analyzed
	if analyzedNum == total {
		return apistructs.PipelineStatusAnalyzed
	}
	// pause
	if pauseNum > 0 && runningNum == 0 {
		return apistructs.PipelineStatusPaused
	}
	// running
	return apistructs.PipelineStatusRunning
}

// CalculatePipelineTaskAllDone
// all task was Disabled or EndStatus, return true
func CalculatePipelineTaskAllDone(tasks []*spec.PipelineTask) bool {
	for _, task := range tasks {
		if task.Status == apistructs.PipelineStatusDisabled {
			continue
		}
		if !task.Status.IsEndStatus() {
			return false
		}
	}
	return true
}
