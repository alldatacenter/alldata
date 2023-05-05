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

package logic

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

func TransferStatus(status string) apistructs.PipelineStatus {
	switch status {

	case string(apistructs.StatusError):
		return apistructs.PipelineStatusError

	case string(apistructs.StatusUnknown):
		return apistructs.PipelineStatusUnknown

	case string(apistructs.StatusCreated):
		return apistructs.PipelineStatusCreated

	case string(apistructs.StatusUnschedulable), "INITIAL":
		return apistructs.PipelineStatusQueue

	case string(apistructs.StatusRunning), "ACTIVE":
		return apistructs.PipelineStatusRunning

	case string(apistructs.StatusStoppedOnOK), string(apistructs.StatusFinished), string(apistructs.StatusStopped):
		return apistructs.PipelineStatusSuccess

	case string(apistructs.StatusStoppedOnFailed), string(apistructs.StatusFailed):
		return apistructs.PipelineStatusFailed

	case string(apistructs.StatusStoppedByKilled):
		return apistructs.PipelineStatusStopByUser

	case string(apistructs.StatusNotFoundInCluster):
		// this is status is for compatibility with scheduler executor
		// func-exist will transfer to started false
		return apistructs.PipelineStatusStartError
	}

	return apistructs.PipelineStatusUnknown
}

func JudgeExistedByStatus(statusDesc apistructs.PipelineStatusDesc) (created bool, started bool, err error) {
	created = true
	switch statusDesc.Status {
	// err
	case apistructs.PipelineStatusError, apistructs.PipelineStatusUnknown:
		err = errors.Errorf("failed to judge job exist or not, detail: %s", statusDesc)
	// not started
	case apistructs.PipelineStatusCreated, apistructs.PipelineStatusStartError:
		started = false
	// started
	case apistructs.PipelineStatusQueue, apistructs.PipelineStatusRunning,
		apistructs.PipelineStatusSuccess, apistructs.PipelineStatusFailed,
		apistructs.PipelineStatusStopByUser:
		started = true

	// default
	default:
		started = false
	}
	return
}
