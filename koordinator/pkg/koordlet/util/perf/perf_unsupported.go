//go:build !linux
// +build !linux

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

import "os"

type PerfCollector struct{}

func NewPerfCollector(cgroupFile *os.File, cpus []int) (*PerfCollector, error) {
	return &PerfCollector{}, nil
}

func GetContainerCyclesAndInstructions(collector *PerfCollector) (uint64, uint64, error) {
	return 0, 0, nil
}

func GetAndStartPerfCollectorOnContainer(cgroupFile *os.File, cpus []int) (*PerfCollector, error) {
	return &PerfCollector{}, nil
}

func (c *PerfCollector) stopAndClose() (err error) {
	return nil
}

func (c *PerfCollector) closeOnSingleCPU(cpu int) error { return nil }

func (c *PerfCollector) stopOnSingleCPU(cpu int) error {
	return nil
}

type HardwareProfile struct{}

func (c *PerfCollector) hwProfileOnSingleCPU(cpu int) (HardwareProfile, error) {
	return HardwareProfile{}, nil
}

type collectResult struct{}

func (c *PerfCollector) collect() (result collectResult, err error) {
	return collectResult{}, err
}

func (c *PerfCollector) CleanUp() error {
	return nil
}
