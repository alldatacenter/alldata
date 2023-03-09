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
	"path"
	"testing"

	"github.com/stretchr/testify/assert"
)

const (
	msgResourceSupportedForTesting = "resource is set supported for testing"
)

var (
	defaultAnolisOSResourcesForTesting = []Resource{
		CPUBurst,
		CPUBVTWarpNs,
		MemoryWmarkRatio,
		MemoryWmarkScaleFactor,
		MemoryWmarkMinAdj,
		MemoryMin,
		MemoryLow,
		MemoryHigh,
		MemoryPriority,
		MemoryUsePriorityOom,
		MemoryOomGroup,
	}
)

type FileTestUtil struct {
	// Temporary directory to store mock cgroup filesystem.
	TempDir string

	t *testing.T
}

// NewFileTestUtil creates a new test util for the specified subsystem.
// NOTE: this function should be called only for testing purposes.
func NewFileTestUtil(t *testing.T) *FileTestUtil {
	// NOTE: When $TMPDIR is not set, `t.TempDir()` can use different base directory on Mac OS X and Linux, which may
	// generates too long paths to test unix socket.
	t.Setenv("TMPDIR", "/tmp")
	tempDir := t.TempDir()
	HostSystemInfo.IsAnolisOS = true

	Conf.ProcRootDir = path.Join(tempDir, "proc")
	err := os.MkdirAll(Conf.ProcRootDir, 0777)
	assert.NoError(t, err)
	Conf.CgroupRootDir = tempDir

	return &FileTestUtil{TempDir: tempDir, t: t}
}

func (c *FileTestUtil) Cleanup() {
	if c.TempDir != "" {
		err := os.RemoveAll(c.TempDir)
		assert.NoError(c.t, err)
	}
	initCgroupsVersion()
}

func (c *FileTestUtil) SetResourcesSupported(supported bool, resources ...Resource) {
	for _, r := range resources {
		r.WithSupported(supported, msgResourceSupportedForTesting)
	}
}

func (c *FileTestUtil) SetAnolisOSResourcesSupported(supported bool) {
	c.SetResourcesSupported(supported, defaultAnolisOSResourcesForTesting...)
}

func (c *FileTestUtil) SetCgroupsV2(useCgroupsV2 bool) {
	UseCgroupsV2 = useCgroupsV2
}

func (c *FileTestUtil) MkDirAll(dirRelativePath string) {
	dir := path.Join(c.TempDir, dirRelativePath)
	if err := os.MkdirAll(dir, 0777); err != nil {
		c.t.Fatal(err)
	}
}

func (c *FileTestUtil) CreateFile(fileRelativePath string) {
	filePath := path.Join(c.TempDir, fileRelativePath)
	dir, _ := path.Split(filePath)
	if err := os.MkdirAll(dir, 0777); err != nil {
		c.t.Fatal(err)
	}
	if _, err := os.Create(filePath); err != nil {
		c.t.Fatal(err)
	}
}

func (c *FileTestUtil) WriteFileContents(fileRelativePath, contents string) {
	filePath := path.Join(c.TempDir, fileRelativePath)
	if !FileExists(filePath) {
		c.CreateFile(fileRelativePath)
	}
	err := os.WriteFile(filePath, []byte(contents), 0644)
	if err != nil {
		c.t.Fatal(err)
	}
}

func (c *FileTestUtil) ReadFileContents(fileRelativePath string) string {
	filePath := path.Join(c.TempDir, fileRelativePath)
	contents, err := os.ReadFile(filePath)
	if err != nil {
		c.t.Fatal(err)
	}
	return string(contents)
}

func (c *FileTestUtil) CreateProcSubFile(fileRelativePath string) {
	file := path.Join(Conf.ProcRootDir, fileRelativePath)
	dir, _ := path.Split(file)
	if err := os.MkdirAll(dir, 0777); err != nil {
		c.t.Fatal(err)
	}
	if _, err := os.Create(file); err != nil {
		c.t.Fatal(err)
	}
}

func (c *FileTestUtil) WriteProcSubFileContents(relativeFilePath string, contents string) {
	file := path.Join(Conf.ProcRootDir, relativeFilePath)
	if !FileExists(file) {
		c.CreateProcSubFile(relativeFilePath)
	}
	err := os.WriteFile(file, []byte(contents), 0644)
	if err != nil {
		c.t.Fatal(err)
	}
}

func (c *FileTestUtil) ReadProcSubFileContents(relativeFilePath string) string {
	file := path.Join(Conf.ProcRootDir, relativeFilePath)
	contents, err := os.ReadFile(file)
	if err != nil {
		c.t.Fatal(err)
	}
	return string(contents)
}

func (c *FileTestUtil) CreateCgroupFile(taskDir string, r Resource) {
	filePath := GetCgroupFilePath(taskDir, r)
	dir, _ := path.Split(filePath)
	if err := os.MkdirAll(dir, 0777); err != nil {
		c.t.Fatal(err)
	}
	if _, err := os.Create(filePath); err != nil {
		c.t.Fatal(err)
	}
}

func (c *FileTestUtil) WriteCgroupFileContents(taskDir string, r Resource, contents string) {
	filePath := GetCgroupFilePath(taskDir, r)
	if !FileExists(filePath) {
		c.CreateCgroupFile(taskDir, r)
	}
	err := CgroupFileWrite(taskDir, r, contents)
	if err != nil {
		c.t.Fatal(err)
	}
}

func (c *FileTestUtil) ReadCgroupFileContents(taskDir string, r Resource) string {
	contents, err := CgroupFileRead(taskDir, r)
	if err != nil {
		c.t.Fatal(err)
	}
	return contents
}
