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

package apistructs

import "testing"

func TestPipelineStatus_AfterPipelineQueue(t *testing.T) {
	tests := []struct {
		name   string
		status PipelineStatus
		want   bool
	}{
		{
			name:   "analyzed",
			status: PipelineStatusAnalyzed,
			want:   false,
		},
		{
			name:   "queue",
			status: PipelineStatusQueue,
			want:   false,
		},
		{
			name:   "running",
			status: PipelineStatusRunning,
			want:   true,
		},
		{
			name:   "success",
			status: PipelineStatusSuccess,
			want:   true,
		},
		{
			name:   "analyzeFailed",
			status: PipelineStatusAnalyzeFailed,
			want:   true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.status.AfterPipelineQueue(); got != tt.want {
				t.Errorf("AfterPipelineQueue() = %v, want %v", got, tt.want)
			}
		})
	}
}
