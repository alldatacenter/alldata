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

package system

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_GuessCgroupDriverFromCgroupName(t *testing.T) {
	tests := []struct {
		name     string
		envSetup func(cgroupRoot string)
		want     CgroupDriverType
	}{
		{
			name: "'kubepods' and 'kubepods.slice' both exist",
			envSetup: func(cgroupRoot string) {
				os.MkdirAll(filepath.Join(cgroupRoot, "cpu", "kubepods"), 0755)
				os.MkdirAll(filepath.Join(cgroupRoot, "cpu", "kubepods.slice"), 0755)
			},
			want: "",
		},
		{
			name:     "neither 'kubepods' nor 'kubepods.slice' exists",
			envSetup: func(cgroupRoot string) {},
			want:     "",
		},
		{
			name: "'kubepods.slice' exist",
			envSetup: func(cgroupRoot string) {
				os.MkdirAll(filepath.Join(cgroupRoot, "cpu", "kubepods.slice"), 0755)
			},
			want: Systemd,
		},
		{
			name: "'kubepods' exist",
			envSetup: func(cgroupRoot string) {
				os.MkdirAll(filepath.Join(cgroupRoot, "cpu", "kubepods"), 0755)
			},
			want: Cgroupfs,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tmpCgroupRoot := t.TempDir()
			os.MkdirAll(tmpCgroupRoot, 0555)

			Conf = &Config{
				CgroupRootDir: tmpCgroupRoot,
			}

			tt.envSetup(tmpCgroupRoot)
			got := GuessCgroupDriverFromCgroupName()
			assert.Equal(t, tt.want, got)
		})
	}
}
