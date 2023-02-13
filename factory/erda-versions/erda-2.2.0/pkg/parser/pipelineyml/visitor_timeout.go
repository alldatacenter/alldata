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
	"github.com/pkg/errors"
)

const (
	TimeoutDuration4Forever = -1
)

type TimeoutVisitor struct{}

func NewTimeoutVisitor() *TimeoutVisitor {
	return &TimeoutVisitor{}
}

func (v *TimeoutVisitor) Visit(s *Spec) {
	for stageIndex, stage := range s.Stages {
		for _, typedActionMap := range stage.Actions {
			for _, action := range typedActionMap {
				if action.Timeout < TimeoutDuration4Forever {
					s.appendError(errors.Errorf("invalid timeout: %d (only %d means forever)", action.Timeout, TimeoutDuration4Forever),
						stageIndex, action.Alias)
				}
			}
		}
	}
}
