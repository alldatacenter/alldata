/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package handler

import "time"

const (
	// unixProtocol is the network protocol of unix socket.
	unixProtocol             = "unix"
	defaultConnectionTimeout = 5 * time.Second
)

type ContainerRuntimeHandler interface {
	StopContainer(containerID string, timeout int64) error
	UpdateContainerResources(containerID string, opts UpdateOptions) error
}

type UpdateOptions struct {
	// CPU CFS (Completely Fair Scheduler) period. Default: 0 (not specified).
	CPUPeriod int64
	// CPU CFS (Completely Fair Scheduler) quota. Default: 0 (not specified).
	CPUQuota int64
	// CPU shares (relative weight vs. other containers). Default: 0 (not specified).
	CPUShares int64
	// Memory limit in bytes. Default: 0 (not specified).
	MemoryLimitInBytes int64
	// OOMScoreAdj adjusts the oom-killer score. Default: 0 (not specified).
	OomScoreAdj int64
	// CpuSetCpus constrains the allowed set of logical CPUs. Default: "" (not specified).
	CpusetCpus string
	// CpuSetMems constrains the allowed set of memory nodes. Default: "" (not specified).
	CpusetMems string
	// SpecAnnotationFilePath is the file path to specAnnotations. In JSON|Yaml format.
	SpecAnnotationFilePath string
}
