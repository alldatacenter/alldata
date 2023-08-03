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
	"errors"
	"fmt"
	"strconv"
	"strings"
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

const ErrCgroupDir = "cgroup path or file not exist"

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

type NumaMemoryPages struct {
	NumaId   int
	PagesNum uint64
}

func (m *MemoryStatRaw) Usage() int64 {
	// memory.stat usage: total_inactive_anon + total_active_anon + total_unevictable
	return m.InactiveAnon + m.ActiveAnon + m.Unevictable
}

// GetCgroupFilePath gets the full path of the given cgroup dir and resource.
// @cgroupTaskDir kubepods.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpu.shares
func GetCgroupFilePath(cgroupTaskDir string, r Resource) string {
	return r.Path(cgroupTaskDir)
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

func ParseMemoryNumaStat(content string) ([]NumaMemoryPages, error) {
	stat := []NumaMemoryPages{}
	parseErr := errors.New("parse cgroup memory numa stat err")
	lines := strings.Split(content, "\n")
	if len(lines) <= 0 {
		return nil, parseErr
	}
	line := strings.TrimSpace(lines[0])
	if len(line) <= 0 || !strings.HasPrefix(line, "total") {
		return nil, parseErr
	}
	mems := strings.Split(line, " ")
	if len(mems) < 2 {
		return nil, parseErr
	}
	for i := 1; i < len(mems); i++ {
		str := strings.Split(mems[i], "=")
		numaStr := strings.TrimLeft(str[0], "N")
		numaId, err := strconv.Atoi(numaStr)
		if err != nil {
			return nil, err
		}
		pagesCnt, err := strconv.ParseUint(str[1], 10, 64)
		if err != nil {
			return nil, err
		}
		stat = append(stat, NumaMemoryPages{NumaId: numaId, PagesNum: pagesCnt})

	}
	return stat, nil
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
