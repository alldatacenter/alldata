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
	"path/filepath"
	"strconv"
	"strings"

	"k8s.io/klog/v2"
)

const (
	CgroupControllersName    = "cgroup.controllers"
	CgroupSubtreeControlName = "cgroup.subtree_control"
)

type CPUStatV2Raw struct {
	UsageUsec  int64
	UserUsec   int64
	SystemUSec int64

	NrPeriods     int64
	NrThrottled   int64
	ThrottledUSec int64
}

func initCgroupsVersion() {
	UseCgroupsV2 = IsUsingCgroupsV2()
}

func IsUsingCgroupsV2() bool {
	// currently we only check the absences of `cgroup.controllers` and `cgroup.subtree_control`
	// TBD: check if the filesystem type of the cgroup root.
	// link: https://kubernetes.io/docs/concepts/architecture/cgroups/#check-cgroup-version
	cgroupControllersPath := filepath.Join(Conf.CgroupRootDir, CgroupControllersName)
	exists, err := PathExists(cgroupControllersPath)
	klog.V(2).Infof("[%v] PathExists exists %v, err: %v", cgroupControllersPath, exists, err)
	if err != nil || !exists {
		return false
	}

	cgroupSubtreeControlPath := filepath.Join(Conf.CgroupRootDir, CgroupSubtreeControlName)
	exists, err = PathExists(cgroupSubtreeControlPath)
	klog.V(2).Infof("[%v] PathExists exists %v, err: %v", cgroupSubtreeControlPath, exists, err)
	if err != nil && !exists {
		return false
	}

	klog.V(5).Infof("check cgroups v2 successfully")
	return true
}

func ParseCPUCFSQuotaV2(content string) (int64, error) {
	// content: "max 100000", "100000 100000"; the first field indicates cfs quota
	ss := strings.Fields(content)
	if len(ss) != 2 {
		return -1, fmt.Errorf("parse cpu.max failed, raw content: %s, err: invalid pattern", content)
	}
	// "max" means unlimited
	if ss[0] == CgroupMaxSymbolStr {
		return -1, nil
	}
	v, err := strconv.ParseInt(ss[0], 10, 64)
	if err != nil {
		return -1, fmt.Errorf("parse cpu.max failed, content: %s, err: %v", ss[0], err)
	}
	return v, nil
}

func ParseCPUCFSPeriodV2(content string) (int64, error) {
	// content: "max 100000", "100000 100000"; the second field indicates cfs period
	ss := strings.Fields(content)
	if len(ss) != 2 {
		return -1, fmt.Errorf("parse cpu.max failed, raw content: %s, err: invalid pattern", content)
	}
	v, err := strconv.ParseInt(ss[1], 10, 64)
	if err != nil {
		return -1, fmt.Errorf("parse cpu.max failed, content: %s, err: %v", ss[1], err)
	}
	return v, nil
}

func ParseCPUAcctStatRawV2(content string) (*CPUStatV2Raw, error) {
	cpuStatRaw := &CPUStatV2Raw{}

	m := ParseKVMap(content)
	for _, t := range []struct {
		key   string
		value *int64
	}{
		{
			key:   "usage_usec",
			value: &cpuStatRaw.UsageUsec,
		},
		{
			key:   "user_usec",
			value: &cpuStatRaw.UserUsec,
		},
		{
			key:   "system_usec",
			value: &cpuStatRaw.SystemUSec,
		},
		{
			key:   "nr_periods",
			value: &cpuStatRaw.NrPeriods,
		},
		{
			key:   "nr_throttled",
			value: &cpuStatRaw.NrThrottled,
		},
		{
			key:   "throttled_usec",
			value: &cpuStatRaw.ThrottledUSec,
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

func ParseCPUAcctUsageV2(content string) (uint64, error) {
	v := uint64(0)

	m := ParseKVMap(content)
	for _, t := range []struct {
		key   string
		value *uint64
	}{
		{
			key:   "usage_usec",
			value: &v,
		},
	} {
		valueStr, ok := m[t.key]
		if !ok {
			return 0, fmt.Errorf("parse cpu.stat failed, raw content %s, err: missing field %s", content, t.key)
		}
		v, err := strconv.ParseUint(valueStr, 10, 64)
		if err != nil {
			return 0, fmt.Errorf("parse cpu.stat failed, raw content %s, field %s, err: %v", content, t.key, err)
		}
		*t.value = v
	}

	// return usage in nanosecond (compatible to v1)
	// assert no overflow
	return v * 1000, nil
}

func ParseCPUStatRawV2(content string) (*CPUStatRaw, error) {
	cpuStatRawV2 := &CPUStatV2Raw{}

	m := ParseKVMap(content)
	for _, t := range []struct {
		key   string
		value *int64
	}{
		{
			key:   "nr_periods",
			value: &cpuStatRawV2.NrPeriods,
		},
		{
			key:   "nr_throttled",
			value: &cpuStatRawV2.NrThrottled,
		},
		{
			key:   "throttled_usec",
			value: &cpuStatRawV2.ThrottledUSec,
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

	return &CPUStatRaw{
		NrPeriods:            cpuStatRawV2.NrPeriods,
		NrThrottled:          cpuStatRawV2.NrThrottled,
		ThrottledNanoSeconds: cpuStatRawV2.ThrottledUSec * 1000, // assert no overflow
	}, nil
}

func ParseMemoryStatRawV2(content string) (*MemoryStatRaw, error) {
	memoryStatRaw := &MemoryStatRaw{}

	m := ParseKVMap(content)
	for _, t := range []struct {
		key   string
		value *int64
	}{
		{
			key:   "file",
			value: &memoryStatRaw.Cache,
		},
		{
			key:   "anon",
			value: &memoryStatRaw.RSS,
		},
		{
			key:   "inactive_file",
			value: &memoryStatRaw.InactiveFile,
		},
		{
			key:   "active_file",
			value: &memoryStatRaw.ActiveFile,
		},
		{
			key:   "inactive_anon",
			value: &memoryStatRaw.InactiveAnon,
		},
		{
			key:   "active_anon",
			value: &memoryStatRaw.ActiveAnon,
		},
		{
			key:   "unevictable",
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

// ConvertCPUWeightToShares converts the value of `cpu.weight` (cgroups-v2) into the value of `cpu.shares` (cgroups-v1)
func ConvertCPUWeightToShares(v int64) (int64, error) {
	isValid, msg := CPUWeightValidator.Validate(strconv.FormatInt(v, 10))
	if !isValid {
		return -1, fmt.Errorf("invalid cpu.weight value, err: %s", msg)
	}
	s := v * 1024 / 100 // no overflow since v is in [1, 10000]
	if s < CPUSharesMinValue {
		s = CPUSharesMinValue
	}
	return s, nil
}

func ConvertCPUSharesToWeight(s string) (int64, error) {
	isValid, msg := CPUSharesValidator.Validate(s)
	if !isValid {
		return -1, fmt.Errorf("invalid cpu.weight value, err: %s", msg)
	}
	v, _ := strconv.ParseInt(s, 10, 64) // the valid value must be an integer
	w := v * 100 / 1024                 // assert no overflow since in k8s v is no more than 1024*num_cpus << math.MaxInt64
	if w < CPUWeightMinValue {
		w = CPUWeightMinValue
	} else if w > CPUWeightMaxValue {
		w = CPUWeightMaxValue
	}
	return w, nil
}
