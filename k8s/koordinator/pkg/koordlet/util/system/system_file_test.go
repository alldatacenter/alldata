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
	"strconv"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestProcSysctl(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		helper := NewFileTestUtil(t)

		testProcSysFile := "test_file"
		testProcSysFilepath := filepath.Join(SysctlSubDir, testProcSysFile)
		testContent := "0"
		assert.False(t, FileExists(GetProcSysFilePath(testProcSysFile)))
		helper.WriteProcSubFileContents(testProcSysFilepath, testContent)

		s := NewProcSysctl()
		v, err := s.GetSysctl(testProcSysFile)
		assert.NoError(t, err)
		assert.Equal(t, testContent, strconv.Itoa(v))

		testValue := 1
		err = s.SetSysctl(testProcSysFile, testValue)
		assert.NoError(t, err)
		got := helper.ReadProcSubFileContents(testProcSysFilepath)
		gotV, err := strconv.ParseInt(got, 10, 32)
		assert.NoError(t, err)
		assert.Equal(t, int(gotV), testValue)
	})
}

func TestSetSchedGroupIdentity(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		helper := NewFileTestUtil(t)

		// system not supported
		err := SetSchedGroupIdentity(false)
		assert.Error(t, err)

		// system supported, already disabled
		testProcSysFile := KernelSchedGroupIdentityEnable
		testProcSysFilepath := filepath.Join(SysctlSubDir, testProcSysFile)
		testContent := "0"
		assert.False(t, FileExists(GetProcSysFilePath(testProcSysFile)))
		helper.WriteProcSubFileContents(testProcSysFilepath, testContent)
		err = SetSchedGroupIdentity(false)
		assert.NoError(t, err)
		got := helper.ReadProcSubFileContents(testProcSysFilepath)
		assert.Equal(t, got, testContent)

		// system supported, set enabled
		testContent = "1"
		err = SetSchedGroupIdentity(true)
		assert.NoError(t, err)
		got = helper.ReadProcSubFileContents(testProcSysFilepath)
		assert.Equal(t, got, testContent)
	})
}
