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

package task_uuid

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func MakeJobIDSliceWithLoopedTimes(action *spec.PipelineTask) []string {
	var JobIDSlice []string
	if isLoop(action) {
		for i := apistructs.TaskLoopTimeBegin; i <= int(action.Extra.LoopOptions.LoopedTimes); i++ {
			JobIDSlice = append(JobIDSlice, parseUUID(action.Extra.UUID, i))
		}
		return JobIDSlice
	}
	JobIDSlice = append(JobIDSlice, action.Extra.UUID)
	return JobIDSlice
}

func parseUUID(uuid string, index int) string {
	return fmt.Sprintf("%s-loop-%d", uuid, index)
}

func MakeJobID(action *spec.PipelineTask) string {
	if isLoop(action) {
		return parseUUID(action.Extra.UUID, int(action.Extra.LoopOptions.LoopedTimes))
	}
	return action.Extra.UUID
}

func isLoop(action *spec.PipelineTask) bool {
	return action.Extra.LoopOptions != nil && action.Extra.LoopOptions.CalculatedLoop != nil && action.Extra.LoopOptions.CalculatedLoop.Strategy.MaxTimes > 0
}
