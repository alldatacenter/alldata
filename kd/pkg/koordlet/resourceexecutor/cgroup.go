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

package resourceexecutor

import (
	"fmt"
	"math"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	"k8s.io/klog/v2"

	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

const (
	// CgroupMaxSymbolStr only appears in cgroups-v2 files, we consider the value as MaxInt64
	CgroupMaxSymbolStr string = "max"
	// CgroupMaxValueStr math.MaxInt64; writing `memory.high` with this do the same as set as "max"
	CgroupMaxValueStr string = "9223372036854775807"
)

const EmptyValueError string = "EmptyValueError"
const ErrCgroupDir = "cgroup path or file not exist"

// CgroupFileWriteIfDifferent writes the cgroup file if current value is different from the given value.
func cgroupFileWriteIfDifferent(cgroupTaskDir string, r sysutil.Resource, value string) (bool, error) {
	if supported, msg := r.IsSupported(cgroupTaskDir); !supported {
		return false, sysutil.ResourceUnsupportedErr(fmt.Sprintf("write cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}
	if valid, msg := r.IsValid(value); !valid {
		return false, fmt.Errorf("write cgroup %s failed, value[%v] not valid, msg: %s", r.ResourceType(), value, msg)
	}
	if exist, msg := IsCgroupPathExist(cgroupTaskDir, r); !exist {
		return false, ResourceCgroupDirErr(fmt.Sprintf("write cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}

	currentValue, currentErr := cgroupFileRead(cgroupTaskDir, r)
	if currentErr != nil {
		return false, currentErr
	}
	if r.ResourceType() == sysutil.CPUSetCPUSName && cpuset.IsEqualStrCpus(currentValue, value) {
		return false, nil
	}
	if value == currentValue || value == CgroupMaxValueStr && currentValue == CgroupMaxSymbolStr {
		// compatible with cgroup valued "max"
		klog.V(6).Infof("read before write %s and got str value, considered as MaxInt64", r.Path(cgroupTaskDir))
		return false, nil
	}
	if err := cgroupFileWrite(cgroupTaskDir, r, value); err != nil {
		return false, err
	}
	return true, nil
}

// CgroupFileWrite writes the cgroup file with the given value.
func cgroupFileWrite(cgroupTaskDir string, r sysutil.Resource, value string) error {
	if supported, msg := r.IsSupported(cgroupTaskDir); !supported {
		return sysutil.ResourceUnsupportedErr(fmt.Sprintf("write cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}
	if valid, msg := r.IsValid(value); !valid {
		return fmt.Errorf("write cgroup %s failed, value[%v] not valid, msg: %s", r.ResourceType(), value, msg)
	}
	if exist, msg := IsCgroupPathExist(cgroupTaskDir, r); !exist {
		return ResourceCgroupDirErr(fmt.Sprintf("write cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}

	filePath := r.Path(cgroupTaskDir)
	klog.V(5).Infof("write %s [%s]", filePath, value)

	return os.WriteFile(filePath, []byte(value), 0644)
}

// CgroupFileReadInt reads the cgroup file and returns an int64 value.
func cgroupFileReadInt(cgroupTaskDir string, r sysutil.Resource) (*int64, error) {
	if supported, msg := r.IsSupported(cgroupTaskDir); !supported {
		return nil, sysutil.ResourceUnsupportedErr(fmt.Sprintf("read cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}

	dataStr, err := cgroupFileRead(cgroupTaskDir, r)
	if err != nil {
		return nil, err
	}
	if dataStr == "" {
		return nil, fmt.Errorf(EmptyValueError)
	}
	if dataStr == CgroupMaxSymbolStr {
		// compatible with cgroup valued "max"
		data := int64(math.MaxInt64)
		klog.V(6).Infof("read %s and got str value, considered as MaxInt64", r.Path(cgroupTaskDir))
		return &data, nil
	}
	data, err := strconv.ParseInt(strings.TrimSpace(dataStr), 10, 64)
	if err != nil {
		return nil, err
	}
	return &data, nil
}

// CgroupFileRead reads the cgroup file.
func cgroupFileRead(cgroupTaskDir string, r sysutil.Resource) (string, error) {
	if supported, msg := r.IsSupported(cgroupTaskDir); !supported {
		return "", sysutil.ResourceUnsupportedErr(fmt.Sprintf("read cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}
	if exist, msg := IsCgroupPathExist(cgroupTaskDir, r); !exist {
		return "", ResourceCgroupDirErr(fmt.Sprintf("read cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}

	filePath := r.Path(cgroupTaskDir)
	klog.V(5).Infof("read %s", filePath)

	data, err := os.ReadFile(filePath)
	return strings.Trim(string(data), "\n"), err
}

func readCgroupAndParseInt64(parentDir string, r sysutil.Resource) (int64, error) {
	s, err := cgroupFileRead(parentDir, r)
	if err != nil {
		return -1, err
	}

	// "max" means unlimited
	if strings.Trim(s, "\n ") == CgroupMaxSymbolStr {
		return -1, nil
	}
	// content: `%lld`
	v, err := strconv.ParseInt(s, 10, 64)
	if err != nil {
		return -1, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

func readCgroupAndParseUint64(parentDir string, r sysutil.Resource) (uint64, error) {
	s, err := cgroupFileRead(parentDir, r)
	if err != nil {
		return 0, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}

	// "max" means unlimited
	if strings.Trim(s, "\n ") == CgroupMaxSymbolStr {
		return math.MaxInt64, nil
	}
	// content: `%llu`
	v, err := strconv.ParseUint(s, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

// ReadCgroupAndParseInt32Slice reads the given cgroup content and parses it into an int32 slice.
// e.g. content: "1\n23\n0\n4\n56789" -> []int32{ 1, 23, 0, 4, 56789 }
func readCgroupAndParseInt32Slice(parentDir string, r sysutil.Resource) ([]int32, error) {
	s, err := cgroupFileRead(parentDir, r)
	if err != nil {
		return nil, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}

	// content: "%d\n%d\n%d\n..."
	var values []int32
	lines := strings.Split(s, "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if len(line) <= 0 {
			continue
		}
		v, err := strconv.ParseInt(line, 10, 32)
		if err != nil {
			return nil, fmt.Errorf("cannot parse cgroup value of line %s, err: %v", line, err)
		}
		values = append(values, int32(v))
	}
	return values, nil
}

func IsCgroupPathExist(parentDir string, r sysutil.Resource) (bool, string) {
	filePath := r.Path(parentDir)
	cgroupPath := filepath.Dir(filePath)
	pathexists, _ := sysutil.PathExists(cgroupPath)
	if !pathexists {
		klog.V(5).Infof("cgroup directory not exist, path: %v", cgroupPath)
		return false, "cgroup path err"
	}
	fileexists, _ := sysutil.PathExists(filePath)
	if !fileexists && pathexists {
		return false, "cgroup file err"
	}
	return true, ""
}

func ResourceCgroupDirErr(msg string) error {
	return fmt.Errorf("%s, reason: %s", ErrCgroupDir, msg)
}

func IsCgroupDirErr(err error) bool {
	return strings.HasPrefix(err.Error(), ErrCgroupDir)
}
