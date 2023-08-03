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

package system

import (
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestIsAnolisOS(t *testing.T) {
	tests := []struct {
		name       string
		fs         string
		cgroupFile string
		expect     bool
	}{
		{
			name:       "anolis_bvt_test",
			fs:         CgroupCPUDir,
			cgroupFile: filepath.Join(CgroupCPUDir, CPUBVTWarpNsName),
			expect:     true,
		},
		{
			name:       "anolis_wmark_ratio_systemd_test",
			fs:         CgroupMemDir,
			cgroupFile: filepath.Join(CgroupMemDir, KubeRootNameSystemd, MemoryWmarkRatioName),
			expect:     true,
		},
		{
			name:       "anolis_wmark_ratio_cgroupfs_test",
			fs:         CgroupMemDir,
			cgroupFile: filepath.Join(CgroupMemDir, KubeRootNameCgroupfs, MemoryWmarkRatioName),
			expect:     true,
		},
		{
			name:       "not_anolis_test",
			fs:         CgroupCPUDir,
			cgroupFile: filepath.Join(CgroupCPUDir, CPUCFSQuotaName),
			expect:     false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := NewFileTestUtil(t)
			helper.MkDirAll(tt.fs)
			helper.CreateFile(tt.cgroupFile)
			assert.Equal(t, tt.expect, isAnolisOS())
		})
	}

}
