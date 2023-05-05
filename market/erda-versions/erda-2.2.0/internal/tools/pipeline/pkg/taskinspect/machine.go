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

type PipelineTaskMachineStat struct {
	Host PipelineTaskMachineHostStat `json:"host,omitempty"`
	Pod  PipelineTaskMachinePodStat  `json:"pod,omitempty"`
	Load PipelineTaskMachineLoadStat `json:"load,omitempty"`
	Mem  PipelineTaskMachineMemStat  `json:"mem,omitempty"`
	Swap PipelineTaskMachineSwapStat `json:"swap,omitempty"`
}
type PipelineTaskMachineHostStat struct {
	HostIP          string `json:"hostIP,omitempty"`
	Hostname        string `json:"hostname,omitempty"`
	UptimeSec       uint64 `json:"uptimeSec,omitempty"`
	BootTimeSec     uint64 `json:"bootTimeSec,omitempty"`
	OS              string `json:"os,omitempty"`
	Platform        string `json:"platform,omitempty"`
	PlatformVersion string `json:"platformVersion,omitempty"`
	KernelVersion   string `json:"kernelVersion,omitempty"`
	KernelArch      string `json:"kernelArch,omitempty"`
}
type PipelineTaskMachinePodStat struct {
	PodIP string `json:"podIP,omitempty"`
}
type PipelineTaskMachineLoadStat struct {
	Load1  float64 `json:"load1,omitempty"`
	Load5  float64 `json:"load5,omitempty"`
	Load15 float64 `json:"load15,omitempty"`
}
type PipelineTaskMachineMemStat struct { // all byte
	Total       uint64  `json:"total,omitempty"`
	Available   uint64  `json:"available,omitempty"`
	Used        uint64  `json:"used,omitempty"`
	Free        uint64  `json:"free,omitempty"`
	UsedPercent float64 `json:"usedPercent,omitempty"`
	Buffers     uint64  `json:"buffers,omitempty"`
	Cached      uint64  `json:"cached,omitempty"`
}
type PipelineTaskMachineSwapStat struct { // all byte
	Total       uint64  `json:"total,omitempty"`
	Used        uint64  `json:"used,omitempty"`
	Free        uint64  `json:"free,omitempty"`
	UsedPercent float64 `json:"usedPercent,omitempty"`
}
