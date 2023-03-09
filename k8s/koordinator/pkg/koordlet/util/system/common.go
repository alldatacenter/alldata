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
	"io"
	"os"
	"path"
	"strings"

	"k8s.io/klog/v2"
)

var (
	CommonRootDir = "" // for uni-test
)

func CommonFileRead(file string) (string, error) {
	file = path.Join(CommonRootDir, file)
	klog.V(5).Infof("read %s", file)
	data, err := os.ReadFile(file)
	return strings.Trim(string(data), "\n"), err
}

func CommonFileWriteIfDifferent(file string, value string) error {
	currentValue, err := CommonFileRead(file)
	if err != nil {
		return err
	}
	if value == currentValue {
		klog.Infof("resource currentValue equal newValue, skip update resource! file:%s, value %s", file, value)
		return nil
	}
	return CommonFileWrite(file, value)
}

func CommonFileWrite(file string, data string) error {
	file = path.Join(CommonRootDir, file)
	klog.V(5).Infof("write %s [%s]", file, data)
	return os.WriteFile(file, []byte(data), 0644)
}

// ReadFileNoStat uses io.ReadAll to read contents of entire file.
// This is similar to io.ReadFile but without the call to os.Stat, because
// many files in /proc and /sys report incorrect file sizes (either 0 or 4096).
// Reads a max file size of 512kB.  For files larger than this, a scanner
// should be used.
func ReadFileNoStat(filename string) ([]byte, error) {
	const maxBufferSize = 1024 * 512

	f, err := os.Open(filename)
	if err != nil {
		return nil, err
	}
	defer f.Close()

	reader := io.LimitReader(f, maxBufferSize)
	return io.ReadAll(reader)
}

func FileExists(path string) bool {
	_, err := os.Stat(path)
	return !os.IsNotExist(err)
}

func PathExists(path string) (bool, error) {
	_, err := os.Stat(path)
	if err == nil {
		return true, nil
	}
	if os.IsNotExist(err) {
		return false, nil
	}
	return false, err
}

// ParseKVMap parses a file content into a KV map.
// e.g. `user 100\nsystem 20` -> `{"user": "100", "system": "20"}`
func ParseKVMap(content string) map[string]string {
	m := map[string]string{}
	lines := strings.Split(content, "\n")
	for _, line := range lines {
		lineItems := strings.Fields(line)
		if len(lineItems) < 2 {
			klog.V(6).Infof("parse key-value map failed, got invalid number of items, line %s, content %s",
				line, content)
			continue
		}
		m[lineItems[0]] = lineItems[1]
	}
	return m
}
