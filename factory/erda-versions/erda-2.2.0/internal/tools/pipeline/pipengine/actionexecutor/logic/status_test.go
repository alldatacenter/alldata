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
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestJudgeExistedByStatus(t *testing.T) {
	testCases := []struct {
		name          string
		status        apistructs.PipelineStatusDesc
		expectStarted bool
		expectError   bool
	}{
		{
			name: "task is abnormal",
			status: apistructs.PipelineStatusDesc{
				Status: apistructs.PipelineStatusError,
			},
			expectStarted: false,
			expectError:   true,
		},
		{
			name: "status is start error",
			status: apistructs.PipelineStatusDesc{
				Status: apistructs.PipelineStatusStartError,
			},
			expectStarted: false,
			expectError:   false,
		},
		{
			name: "status is running",
			status: apistructs.PipelineStatusDesc{
				Status: apistructs.PipelineStatusRunning,
			},
			expectStarted: true,
			expectError:   false,
		},
		{
			name: "status is unknown",
			status: apistructs.PipelineStatusDesc{
				Status: "unknown",
			},
			expectStarted: false,
			expectError:   false,
		},
	}
	for _, tt := range testCases {
		_, started, err := JudgeExistedByStatus(tt.status)
		actualErr := err != nil
		if actualErr != tt.expectError {
			t.Errorf("%s: expect error: %v, actual error: %v", tt.name, tt.expectError, actualErr)
		}
		if started != tt.expectStarted {
			t.Errorf("%s: expect started: %v, actual started: %v", tt.name, tt.expectStarted, started)
		}
	}
}
