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

package nodenumaresource

import (
	"sync"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

// CPUTopologyManager manages the CPU Topology and CPU assignments options.
type CPUTopologyManager interface {
	GetCPUTopologyOptions(nodeName string) CPUTopologyOptions
	UpdateCPUTopologyOptions(nodeName string, updateFn func(options *CPUTopologyOptions))
	Delete(nodeName string)
}

type CPUTopologyOptions struct {
	CPUTopology  *CPUTopology                       `json:"cpuTopology,omitempty"`
	ReservedCPUs cpuset.CPUSet                      `json:"reservedCPUs,omitempty"`
	MaxRefCount  int                                `json:"maxRefCount,omitempty"`
	Policy       *extension.KubeletCPUManagerPolicy `json:"policy,omitempty"`
}

type cpuTopologyManager struct {
	lock            sync.Mutex
	topologyOptions map[string]CPUTopologyOptions
}

func NewCPUTopologyManager() CPUTopologyManager {
	manager := &cpuTopologyManager{
		topologyOptions: map[string]CPUTopologyOptions{},
	}
	return manager
}

func (m *cpuTopologyManager) GetCPUTopologyOptions(nodeName string) CPUTopologyOptions {
	m.lock.Lock()
	defer m.lock.Unlock()
	return m.topologyOptions[nodeName]
}

func (m *cpuTopologyManager) UpdateCPUTopologyOptions(nodeName string, updateFn func(options *CPUTopologyOptions)) {
	m.lock.Lock()
	defer m.lock.Unlock()
	options := m.topologyOptions[nodeName]
	updateFn(&options)
	if options.MaxRefCount == 0 {
		options.MaxRefCount = 1
	}
	m.topologyOptions[nodeName] = options
}

func (m *cpuTopologyManager) Delete(nodeName string) {
	m.lock.Lock()
	defer m.lock.Unlock()
	delete(m.topologyOptions, nodeName)
}
