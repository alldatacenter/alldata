//go:build linux
// +build linux

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

package perf

// todo: add readme

import (
	"fmt"
	"os"

	"github.com/hodgesds/perf-utils"
	"go.uber.org/multierr"
	"golang.org/x/sys/unix"
	"k8s.io/klog/v2"
)

type PerfCollector struct {
	cgroupFile        *os.File
	cpus              []int
	cpuHwProfilersMap map[int]*perf.HardwareProfiler
	// todo: cpuSwProfilers map[int]*perf.SoftwareProfiler
}

func NewPerfCollector(cgroupFile *os.File, cpus []int) (*PerfCollector, error) {
	collector := &PerfCollector{
		cgroupFile:        cgroupFile,
		cpus:              cpus,
		cpuHwProfilersMap: map[int]*perf.HardwareProfiler{},
	}
	for _, cpu := range cpus {
		cpiProfiler, err := perf.NewHardwareProfiler(int(cgroupFile.Fd()), cpu, perf.RefCpuCyclesProfiler|perf.CpuInstrProfiler, unix.PERF_FLAG_PID_CGROUP)
		if err != nil && !cpiProfiler.HasProfilers() {
			return nil, err
		}

		// todo: NewSoftwareProfiler, etc.

		collector.cpuHwProfilersMap[cpu] = &cpiProfiler
	}
	return collector, nil
}

func GetAndStartPerfCollectorOnContainer(cgroupFile *os.File, cpus []int) (*PerfCollector, error) {
	collector, err := NewPerfCollector(cgroupFile, cpus)
	if err != nil {
		return nil, err
	}
	for _, cpu := range collector.cpus {
		go func(cpu int) {
			if newErr := (*collector.cpuHwProfilersMap[cpu]).Start(); newErr != nil {
				err = multierr.Append(err, newErr)
			}
		}(cpu)
		if err != nil {
			return nil, err
		}
	}
	return collector, nil
}

// todo: call collect() to get all metrics at the same time instead of put it inside GetContainerCyclesAndInstructions
func GetContainerCyclesAndInstructions(collector *PerfCollector) (uint64, uint64, error) {
	defer func() {
		stopErr := collector.stopAndClose()
		if stopErr != nil {
			klog.Errorf("stopAndClose perf err %v", stopErr)
		}
	}()
	result, err := collector.collect()
	if err != nil {
		return 0, 0, err
	}
	return result.cycles, result.instructions, nil
}

type collectResult struct {
	cycles       uint64
	instructions uint64

	// todo: context-switches, etc.
}

func (c *PerfCollector) collect() (result collectResult, err error) {
	for _, cpu := range c.cpus {
		// todo: c.swProfile, etc.
		profile, err := c.hwProfileOnSingleCPU(cpu)
		if err != nil {
			return result, err
		}
		// skip not counted cases
		if profile.RefCPUCycles != nil {
			result.cycles += *profile.RefCPUCycles
		}
		if profile.Instructions != nil {
			result.instructions += *profile.Instructions
		}
	}
	return result, err
}

func (c *PerfCollector) hwProfileOnSingleCPU(cpu int) (*perf.HardwareProfile, error) {
	profile := &perf.HardwareProfile{}
	if err := (*c.cpuHwProfilersMap[cpu]).Profile(profile); err != nil {
		return nil, fmt.Errorf("profile err : %v", err)
	}
	return profile, nil
}

func (c *PerfCollector) stopAndClose() (err error) {
	for _, cpu := range c.cpus {
		// todo: c.swProfile, etc.
		newErr := c.stopOnSingleCPU(cpu)
		if newErr != nil {
			err = multierr.Append(err, newErr)
		}
		newErr = c.closeOnSingleCPU(cpu)
		if newErr != nil {
			err = multierr.Append(err, newErr)
		}
	}
	return err
}

func (c *PerfCollector) stopOnSingleCPU(cpu int) error {
	if err := (*c.cpuHwProfilersMap[cpu]).Stop(); err != nil {
		return fmt.Errorf("stop container %v, cpu: %v fd err : %v", int(c.cgroupFile.Fd()), cpu, err)
	}
	return nil
}

func (c *PerfCollector) closeOnSingleCPU(cpu int) error {
	if err := (*c.cpuHwProfilersMap[cpu]).Close(); err != nil {
		return fmt.Errorf("close container %v, cpu: %v fd err : %v", int(c.cgroupFile.Fd()), cpu, err)
	}
	return nil
}

func (c *PerfCollector) CleanUp() error {
	err := c.cgroupFile.Close()
	if err != nil {
		return fmt.Errorf("close cgroupFile %v, err : %v", c.cgroupFile.Name(), err)
	}
	return nil
}
