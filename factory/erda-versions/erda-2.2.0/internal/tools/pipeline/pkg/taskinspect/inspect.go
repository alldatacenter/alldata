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

package taskinspect

import (
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
)

const (
	PipelineTaskMaxErrorPerHour = 180
)

type Inspect struct {
	Inspect     string                   `json:"inspect,omitempty"`
	Events      string                   `json:"events,omitempty"`
	MachineStat *PipelineTaskMachineStat `json:"machineStat,omitempty"`

	// Errors stores from pipeline internal, not callback(like action-agent).
	// For external errors, use taskresult.Result.Errors.
	Errors taskerror.OrderedErrors `json:"errors,omitempty"`
}

func (t *Inspect) IsErrorsExceed() (bool, *taskerror.Error) {
	for _, g := range t.Errors {
		if g.Ctx.CalculateFrequencyPerHour() > PipelineTaskMaxErrorPerHour {
			return true, g
		}
	}
	return false, nil
}
