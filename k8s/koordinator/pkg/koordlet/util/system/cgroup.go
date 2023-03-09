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
	"fmt"
	"math"
	"os"
	"strconv"
	"strings"

	"k8s.io/klog/v2"
)

const (
	DefaultCPUCFSPeriod    int64 = 100000
	CPUShareKubeBEValue    int64 = 2
	CPUShareUnitValue      int64 = 1024
	CFSQuotaUnlimitedValue int64 = -1
	// MemoryLimitUnlimitedValue denotes the unlimited value of cgroups-v1 memory.limit_in_bytes.
	// It derives from linux PAGE_COUNTER_MAX and may be different according to the PAGE_SIZE (here we suppose `4k`).
	// https://github.com/torvalds/linux/blob/ea4424be16887a37735d6550cfd0611528dbe5d9/mm/memcontrol.c#L5337
	MemoryLimitUnlimitedValue int64 = 0x7FFFFFFFFFFFF000 // 9223372036854771712 < math.MaxInt64

	// CgroupMaxSymbolStr only appears in cgroups-v2 files, we consider the value as MaxInt64
	CgroupMaxSymbolStr string = "max"
	// CgroupMaxValueStr math.MaxInt64; writing `memory.high` with this do the same as set as "max"
	CgroupMaxValueStr string = "9223372036854775807"
)

const EmptyValueError string = "EmptyValueError"

type CPUStatRaw struct {
	NrPeriods            int64
	NrThrottled          int64
	ThrottledNanoSeconds int64
}

type MemoryStatRaw struct {
	Cache        int64
	RSS          int64
	InactiveFile int64
	ActiveFile   int64
	InactiveAnon int64
	ActiveAnon   int64
	Unevictable  int64
	// add more fields
}

func (m *MemoryStatRaw) Usage() int64 {
	// memory.stat usage: total_inactive_anon + total_active_anon + total_unevictable
	return m.InactiveAnon + m.ActiveAnon + m.Unevictable
}

// CgroupFileWriteIfDifferent writes the cgroup file if current value is different from the given value.
// TODO: moved into resourceexecutor package and marked as private.
func CgroupFileWriteIfDifferent(cgroupTaskDir string, r Resource, value string) error {
	if supported, msg := r.IsSupported(cgroupTaskDir); !supported {
		return ResourceUnsupportedErr(fmt.Sprintf("write cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}
	if valid, msg := r.IsValid(value); !valid {
		return fmt.Errorf("write cgroup %s failed, value[%v] not valid, msg: %s", r.ResourceType(), value, msg)
	}

	currentValue, currentErr := CgroupFileRead(cgroupTaskDir, r)
	if currentErr != nil {
		return currentErr
	}
	if value == currentValue || value == CgroupMaxValueStr && currentValue == CgroupMaxSymbolStr {
		// compatible with cgroup valued "max"
		klog.V(6).Infof("read before write %s and got str value, considered as MaxInt64", r.Path(cgroupTaskDir))
		return nil
	}
	return CgroupFileWrite(cgroupTaskDir, r, value)
}

// CgroupFileWrite writes the cgroup file with the given value.
// TODO: moved into resourceexecutor package and marked as private.
func CgroupFileWrite(cgroupTaskDir string, r Resource, value string) error {
	if supported, msg := r.IsSupported(cgroupTaskDir); !supported {
		return ResourceUnsupportedErr(fmt.Sprintf("write cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}
	if valid, msg := r.IsValid(value); !valid {
		return fmt.Errorf("write cgroup %s failed, value[%v] not valid, msg: %s", r.ResourceType(), value, msg)
	}

	filePath := r.Path(cgroupTaskDir)
	klog.V(5).Infof("write %s [%s]", filePath, value)

	return os.WriteFile(filePath, []byte(value), 0644)
}

// CgroupFileReadInt reads the cgroup file and returns an int64 value.
// TODO: moved into resourceexecutor package and marked as private.
func CgroupFileReadInt(cgroupTaskDir string, r Resource) (*int64, error) {
	if supported, msg := r.IsSupported(cgroupTaskDir); !supported {
		return nil, ResourceUnsupportedErr(fmt.Sprintf("read cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}

	dataStr, err := CgroupFileRead(cgroupTaskDir, r)
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
// TODO: moved into resourceexecutor package and marked as private.
func CgroupFileRead(cgroupTaskDir string, r Resource) (string, error) {
	if supported, msg := r.IsSupported(cgroupTaskDir); !supported {
		return "", ResourceUnsupportedErr(fmt.Sprintf("read cgroup %s failed, msg: %s", r.ResourceType(), msg))
	}

	filePath := r.Path(cgroupTaskDir)
	klog.V(5).Infof("read %s", filePath)

	data, err := os.ReadFile(filePath)
	return strings.Trim(string(data), "\n"), err
}

// @cgroupTaskDir kubepods.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpu.shares
func GetCgroupFilePath(cgroupTaskDir string, r Resource) string {
	return r.Path(cgroupTaskDir)
}

func GetCgroupCurTasks(cgroupPath string) ([]int, error) {
	var tasks []int
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return nil, err
	}
	lines := strings.Split(string(rawContent), "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		if len(line) <= 0 {
			continue
		}
		task, err := strconv.Atoi(line)
		if err != nil {
			return nil, err
		}
		tasks = append(tasks, task)
	}
	return tasks, nil
}

func ReadCgroupAndParseInt64(parentDir string, r Resource) (int64, error) {
	s, err := CgroupFileRead(parentDir, r)
	if err != nil {
		return -1, fmt.Errorf("cannot read cgroup file, err: %v", err)
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

func ReadCgroupAndParseUint64(parentDir string, r Resource) (uint64, error) {
	s, err := CgroupFileRead(parentDir, r)
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
func ReadCgroupAndParseInt32Slice(parentDir string, r Resource) ([]int32, error) {
	s, err := CgroupFileRead(parentDir, r)
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

func ParseCPUStatRaw(content string) (*CPUStatRaw, error) {
	cpuStatRaw := &CPUStatRaw{}

	m := ParseKVMap(content)
	for _, t := range []struct {
		key   string
		value *int64
	}{
		{
			key:   "nr_periods",
			value: &cpuStatRaw.NrPeriods,
		},
		{
			key:   "nr_throttled",
			value: &cpuStatRaw.NrThrottled,
		},
		{
			key:   "throttled_time",
			value: &cpuStatRaw.ThrottledNanoSeconds,
		},
	} {
		valueStr, ok := m[t.key]
		if !ok {
			return nil, fmt.Errorf("parse cpu.stat failed, raw content %s, err: missing field %s", content, t.key)
		}
		v, err := strconv.ParseInt(valueStr, 10, 64)
		if err != nil {
			return nil, fmt.Errorf("parse cpu.stat failed, raw content %s, field %s, err: %v", content, t.key, err)
		}
		*t.value = v
	}

	return cpuStatRaw, nil
}

func ParseMemoryStatRaw(content string) (*MemoryStatRaw, error) {
	memoryStatRaw := &MemoryStatRaw{}

	m := ParseKVMap(content)
	for _, t := range []struct {
		key   string
		value *int64
	}{
		{
			key:   "total_cache",
			value: &memoryStatRaw.Cache,
		},
		{
			key:   "total_rss",
			value: &memoryStatRaw.RSS,
		},
		{
			key:   "total_inactive_file",
			value: &memoryStatRaw.InactiveFile,
		},
		{
			key:   "total_active_file",
			value: &memoryStatRaw.ActiveFile,
		},
		{
			key:   "total_inactive_anon",
			value: &memoryStatRaw.InactiveAnon,
		},
		{
			key:   "total_active_anon",
			value: &memoryStatRaw.ActiveAnon,
		},
		{
			key:   "total_unevictable",
			value: &memoryStatRaw.Unevictable,
		},
	} {
		valueStr, ok := m[t.key]
		if !ok {
			return nil, fmt.Errorf("parse memory.stat failed, raw content %s, err: missing field %s", content, t.key)
		}
		v, err := strconv.ParseInt(valueStr, 10, 64)
		if err != nil {
			return nil, fmt.Errorf("parse memory.stat failed, raw content %s, field %s, err: %v", content, t.key, err)
		}
		*t.value = v
	}

	return memoryStatRaw, nil
}

// ReadCPUStatRaw reads the cpu.stat under the given cgroup path.
// DEPRECATED: use NewCgroupReader().ReadCPUStat() instead.
func ReadCPUStatRaw(cgroupPath string) (*CPUStatRaw, error) {
	content, err := os.ReadFile(cgroupPath)
	if err != nil {
		return nil, err
	}

	cpuThrottledRaw := &CPUStatRaw{}
	lines := strings.Split(string(content), "\n")
	counter := 0
	for _, line := range lines {
		lineItems := strings.Fields(line)
		if len(lineItems) < 2 {
			continue
		}
		key := lineItems[0]
		val := lineItems[1]
		switch key {
		case "nr_periods":
			if cpuThrottledRaw.NrPeriods, err = strconv.ParseInt(val, 10, 64); err != nil {
				return nil, fmt.Errorf("parsec nr_periods field failed, path %s, raw content %s, err: %v",
					cgroupPath, content, err)
			}
			counter++
		case "nr_throttled":
			if cpuThrottledRaw.NrThrottled, err = strconv.ParseInt(val, 10, 64); err != nil {
				return nil, fmt.Errorf("parse nr_throttled field failed, path %s, raw content %s, err: %v",
					cgroupPath, content, err)
			}
			counter++
		case "throttled_time":
			if cpuThrottledRaw.ThrottledNanoSeconds, err = strconv.ParseInt(val, 10, 64); err != nil {
				return nil, fmt.Errorf("parse throttled_time field failed, path %s, raw content %s, err: %v",
					cgroupPath, content, err)
			}
			counter++
		}
	}

	if counter != 3 {
		return nil, fmt.Errorf("parse cpu throttled iterms failed, path %s, raw content %s, err: %v",
			cgroupPath, content, err)
	}

	return cpuThrottledRaw, nil
}

func CalcCPUThrottledRatio(curPoint, prePoint *CPUStatRaw) float64 {
	deltaPeriod := curPoint.NrPeriods - prePoint.NrPeriods
	deltaThrottled := curPoint.NrThrottled - prePoint.NrThrottled
	throttledRatio := float64(0)
	if deltaPeriod > 0 {
		throttledRatio = float64(deltaThrottled) / float64(deltaPeriod)
	}
	return throttledRatio
}
