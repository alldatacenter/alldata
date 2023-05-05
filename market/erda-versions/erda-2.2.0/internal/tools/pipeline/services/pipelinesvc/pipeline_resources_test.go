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

package pipelinesvc

import (
	"reflect"
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func genTaskWithLimitCPUMem(limitCPU, limitMem float64) *apistructs.PipelineAppliedResources {
	return &apistructs.PipelineAppliedResources{
		Limits: apistructs.PipelineAppliedResource{
			CPU:      limitCPU,
			MemoryMB: limitMem,
		},
	}
}

func genTaskWithRequestCPUMem(limitCPU, limitMem float64) *apistructs.PipelineAppliedResources {
	return &apistructs.PipelineAppliedResources{
		Requests: apistructs.PipelineAppliedResource{
			CPU:      limitCPU,
			MemoryMB: limitMem,
		},
	}
}

func Test_calculatePipelineLimitResource(t *testing.T) {
	type args struct {
		allStagedTasksResources [][]*apistructs.PipelineAppliedResources
	}
	tests := []struct {
		name string
		args args
		want apistructs.PipelineAppliedResource
	}{
		{
			// cpu
			// 1 2 (3)
			// 2 3 (5)
			// 4   (4)
			// => max((1+2), (2+3), (4)) = 5
			// mem
			// 5 4 (9)
			// 1 3 (4)
			// 8   (8)
			// => max((5+4), (1+3), (8)) = 9
			name: "enough cpu",
			args: args{
				allStagedTasksResources: [][]*apistructs.PipelineAppliedResources{
					{genTaskWithLimitCPUMem(1, 5), genTaskWithLimitCPUMem(2, 4)},
					{genTaskWithLimitCPUMem(2, 1), genTaskWithLimitCPUMem(3, 3)},
					{genTaskWithLimitCPUMem(4, 8)},
				},
			},
			want: apistructs.PipelineAppliedResource{
				CPU:      5,
				MemoryMB: 9,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := calculatePipelineLimitResource(tt.args.allStagedTasksResources); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("calculatePipelineLimitResource() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_calculatePipelineRequestResource(t *testing.T) {
	type args struct {
		allStagedTasksResources [][]*apistructs.PipelineAppliedResources
	}
	tests := []struct {
		name string
		args args
		want apistructs.PipelineAppliedResource
	}{
		{
			// calculate minResource
			// cpu
			// 1 2 (2)
			// 2 3 (3)
			// 4   (4)
			// => max(1,2,2,3,4) = 4
			// mem
			// 5 4 (5)
			// 4 1 (4)
			// 7   (7)
			// => max(5,4,4,1,7) = 7
			name: "minimal resource",
			args: args{
				allStagedTasksResources: [][]*apistructs.PipelineAppliedResources{
					{genTaskWithRequestCPUMem(1, 5), genTaskWithRequestCPUMem(2, 4)},
					{genTaskWithRequestCPUMem(2, 4), genTaskWithRequestCPUMem(3, 1)},
					{genTaskWithRequestCPUMem(4, 7)},
				},
			},
			want: apistructs.PipelineAppliedResource{
				CPU:      4,
				MemoryMB: 7,
			},
		},
		{
			name: "test",
			args: args{
				allStagedTasksResources: [][]*apistructs.PipelineAppliedResources{
					{genTaskWithRequestCPUMem(1, 1024), genTaskWithRequestCPUMem(1, 1024)},
					{genTaskWithRequestCPUMem(2, 2049)},
				},
			},
			want: apistructs.PipelineAppliedResource{
				CPU:      2,
				MemoryMB: 2049,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := calculatePipelineRequestResource(tt.args.allStagedTasksResources); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("calculatePipelineRequestResource() = %v, want %v", got, tt.want)
			}
		})
	}
}
