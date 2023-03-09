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

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_NewPerfCollector(t *testing.T) {
	tempDir := t.TempDir()
	f, _ := os.OpenFile(tempDir, os.O_RDONLY, os.ModeDir)
	cpus := []int{0}
	assert.NotPanics(t, func() {
		_, err := NewPerfCollector(f, cpus)
		if err != nil {
			return
		}
	})
}

func Test_GetAndStartPerfCollectorOnContainer(t *testing.T) {
	tempDir := t.TempDir()
	f, _ := os.OpenFile(tempDir, os.O_RDONLY, os.ModeDir)
	cpus := []int{0}
	assert.NotPanics(t, func() {
		_, err := GetAndStartPerfCollectorOnContainer(f, cpus)
		if err != nil {
			return
		}
	})
}

func Test_GetContainerCyclesAndInstructions(t *testing.T) {
	tempDir := t.TempDir()
	f, _ := os.OpenFile(tempDir, os.O_RDONLY, os.ModeDir)
	cpus := []int{0}
	collector, _ := NewPerfCollector(f, cpus)
	assert.NotPanics(t, func() {
		_, _, err := GetContainerCyclesAndInstructions(collector)
		if err != nil {
			return
		}
	})
}

func Test_stopAndClose(t *testing.T) {
	tempDir := t.TempDir()
	f, _ := os.OpenFile(tempDir, os.O_RDONLY, os.ModeDir)
	cpus := []int{0}
	collector, _ := NewPerfCollector(f, cpus)
	assert.NotPanics(t, func() {
		err := collector.stopAndClose()
		if err != nil {
			return
		}
	})
}

func Test_collect(t *testing.T) {
	tempDir := t.TempDir()
	f, _ := os.OpenFile(tempDir, os.O_RDONLY, os.ModeDir)
	cpus := []int{0}
	collector, _ := NewPerfCollector(f, cpus)
	assert.NotPanics(t, func() {
		_, err := collector.collect()
		if err != nil {
			return
		}
	})
}

func Test_CleanUp(t *testing.T) {
	tempDir := t.TempDir()
	f, _ := os.OpenFile(tempDir, os.O_RDONLY, os.ModeDir)
	cpus := []int{0}
	collector, _ := NewPerfCollector(f, cpus)
	f.Close()
	assert.NotPanics(t, func() {
		err := collector.CleanUp()
		if err != nil {
			return
		}
	})
}
