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
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_NewDsModeConfig(t *testing.T) {
	expectConfig := &Config{
		CgroupKubePath:        "kubepods/",
		CgroupRootDir:         "/host-cgroup/",
		ProcRootDir:           "/proc/",
		SysRootDir:            "/host-sys/",
		SysFSRootDir:          "/host-sys-fs/",
		VarRunRootDir:         "/host-var-run/",
		RuntimeHooksConfigDir: "/host-etc-hookserver/",
	}
	defaultConfig := NewDsModeConfig()
	assert.Equal(t, expectConfig, defaultConfig)
}

func Test_NewHostModeConfig(t *testing.T) {
	expectConfig := &Config{
		CgroupKubePath:        "kubepods/",
		CgroupRootDir:         "/sys/fs/cgroup/",
		ProcRootDir:           "/proc/",
		SysRootDir:            "/sys/",
		SysFSRootDir:          "/sys/fs/",
		VarRunRootDir:         "/var/run/",
		RuntimeHooksConfigDir: "/etc/runtime/hookserver.d",
	}
	defaultConfig := NewHostModeConfig()
	assert.Equal(t, expectConfig, defaultConfig)
}

func Test_InitFlags(t *testing.T) {
	t.Run("", func(t *testing.T) {
		cfg := NewDsModeConfig()
		assert.NotNil(t, cfg)
	})
}
