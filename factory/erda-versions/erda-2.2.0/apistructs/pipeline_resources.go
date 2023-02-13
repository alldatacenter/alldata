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

// PipelineAppliedResources represents multi-kind applied resources.
type PipelineAppliedResources struct {

	// Limits is the minimal enough resource to run pipeline
	// calculate minResource
	// 1 2 (2)
	// 2 3 (3)
	// 4   (4)
	// => max(1,2,2,3,4) = 4
	Limits PipelineAppliedResource `json:"limits"`

	// Requests is the minimal resource to run pipeline
	// calculate maxResource
	// 1 2 (3)
	// 2 3 (5)
	// 4   (4)
	// => max((1+2), (2+3), (4)) = 5
	Requests PipelineAppliedResource `json:"requests"`
}

// PipelineAppliedResource represents one kind of applied resources.
type PipelineAppliedResource struct {
	CPU      float64 `json:"cpu"`
	MemoryMB float64 `json:"memoryMB"`
}

// PipelineOverSoldResource represents one kind of over sold resource.
type PipelineOverSoldResource struct {
	CPURate uint8   `json:"cpuRate"`
	MaxCPU  float64 `json:"maxCPU"`
}
