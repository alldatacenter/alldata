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
	"errors"
	"fmt"

	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

var ErrResourceNotRegistered = errors.New("resource not registered")

type CgroupReader interface {
	ReadCPUQuota(parentDir string) (int64, error)
	ReadCPUPeriod(parentDir string) (int64, error)
	ReadCPUShares(parentDir string) (int64, error)
	ReadCPUSet(parentDir string) (*cpuset.CPUSet, error)
	ReadCPUAcctUsage(parentDir string) (uint64, error)
	ReadCPUStat(parentDir string) (*sysutil.CPUStatRaw, error)
	ReadMemoryLimit(parentDir string) (int64, error)
	ReadMemoryStat(parentDir string) (*sysutil.MemoryStatRaw, error)
	ReadCPUTasks(parentDir string) ([]int32, error)
}

var _ CgroupReader = &CgroupV1Reader{}

type CgroupV1Reader struct{}

func (r *CgroupV1Reader) ReadCPUQuota(parentDir string) (int64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.CPUCFSQuotaName)
	if !ok {
		return -1, ErrResourceNotRegistered
	}
	return sysutil.ReadCgroupAndParseInt64(parentDir, resource)
}

func (r *CgroupV1Reader) ReadCPUPeriod(parentDir string) (int64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.CPUCFSPeriodName)
	if !ok {
		return -1, ErrResourceNotRegistered
	}
	return sysutil.ReadCgroupAndParseInt64(parentDir, resource)
}

func (r *CgroupV1Reader) ReadCPUShares(parentDir string) (int64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.CPUSharesName)
	if !ok {
		return -1, ErrResourceNotRegistered
	}
	return sysutil.ReadCgroupAndParseInt64(parentDir, resource)
}

func (r *CgroupV1Reader) ReadCPUSet(parentDir string) (*cpuset.CPUSet, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.CPUSetCPUSName)
	if !ok {
		return nil, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return nil, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}

	v, err := cpuset.Parse(s)
	if err != nil {
		return nil, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return &v, nil
}

func (r *CgroupV1Reader) ReadCPUAcctUsage(parentDir string) (uint64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.CPUAcctUsageName)
	if !ok {
		return 0, ErrResourceNotRegistered
	}
	return sysutil.ReadCgroupAndParseUint64(parentDir, resource)
}

func (r *CgroupV1Reader) ReadCPUStat(parentDir string) (*sysutil.CPUStatRaw, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.CPUStatName)
	if !ok {
		return nil, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return nil, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}
	// content: "nr_periods 0\nnr_throttled 0\nthrottled_time 0\n..."
	v, err := sysutil.ParseCPUStatRaw(s)
	if err != nil {
		return nil, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

func (r *CgroupV1Reader) ReadMemoryLimit(parentDir string) (int64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.MemoryLimitName)
	if !ok {
		return -1, ErrResourceNotRegistered
	}
	v, err := sysutil.ReadCgroupAndParseInt64(parentDir, resource)
	if err != nil {
		return -1, err
	}
	// `memory.limit_in_bytes=9223372036854771712` means memory is unlimited, consider as value -1
	if v >= sysutil.MemoryLimitUnlimitedValue {
		return -1, nil
	}
	return v, nil
}

func (r *CgroupV1Reader) ReadMemoryStat(parentDir string) (*sysutil.MemoryStatRaw, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.MemoryStatName)
	if !ok {
		return nil, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return nil, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}
	// content: `...total_inactive_anon $total_inactive_anon\ntotal_active_anon $total_active_anon\n
	//           total_inactive_file $total_inactive_file\ntotal_active_file $total_active_file\n
	//           total_unevictable $total_unevictable\n`
	v, err := sysutil.ParseMemoryStatRaw(s)
	if err != nil {
		return nil, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

func (r *CgroupV1Reader) ReadCPUTasks(parentDir string) ([]int32, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV1, sysutil.CPUTasksName)
	if !ok {
		return nil, ErrResourceNotRegistered
	}
	// content: `7742\n10971\n11049\n11051...`
	return sysutil.ReadCgroupAndParseInt32Slice(parentDir, resource)
}

var _ CgroupReader = &CgroupV2Reader{}

type CgroupV2Reader struct{}

func (r *CgroupV2Reader) ReadCPUQuota(parentDir string) (int64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.CPUCFSQuotaName)
	if !ok {
		return -1, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return -1, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}

	// content: "max 100000", "100000 100000"
	v, err := sysutil.ParseCPUCFSQuotaV2(s)
	if err != nil {
		return -1, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

func (r *CgroupV2Reader) ReadCPUPeriod(parentDir string) (int64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.CPUCFSPeriodName)
	if !ok {
		return -1, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return -1, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}

	// content: "max 100000", "100000 100000"
	v, err := sysutil.ParseCPUCFSPeriodV2(s)
	if err != nil {
		return -1, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

func (r *CgroupV2Reader) ReadCPUShares(parentDir string) (int64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.CPUSharesName)
	if !ok {
		return -1, ErrResourceNotRegistered
	}

	v, err := sysutil.ReadCgroupAndParseInt64(parentDir, resource)
	if err != nil {
		return -1, err
	}
	// convert cpu.weight value into cpu.shares value
	return sysutil.ConvertCPUWeightToShares(v)
}

func (r *CgroupV2Reader) ReadCPUSet(parentDir string) (*cpuset.CPUSet, error) {
	// use `cpuset.cpus.effective` for read cpuset on cgroups-v2
	// https://docs.kernel.org/admin-guide/cgroup-v2.html#cpuset-interface-files
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.CPUSetCPUSEffectiveName)
	if !ok {
		return nil, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return nil, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}

	v, err := cpuset.Parse(s)
	if err != nil {
		return nil, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return &v, nil
}

func (r *CgroupV2Reader) ReadCPUAcctUsage(parentDir string) (uint64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.CPUAcctUsageName)
	if !ok {
		return 0, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return 0, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}
	// content: "usage_usec 1000000\nuser_usec 800000\nsystem_usec 200000\n..."
	v, err := sysutil.ParseCPUAcctUsageV2(s)
	if err != nil {
		return 0, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

func (r *CgroupV2Reader) ReadCPUStat(parentDir string) (*sysutil.CPUStatRaw, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.CPUStatName)
	if !ok {
		return nil, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return nil, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}
	// content: "...\nnr_periods 0\nnr_throttled 0\nthrottled_usec 0\n..."
	v, err := sysutil.ParseCPUStatRawV2(s)
	if err != nil {
		return nil, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

func (r *CgroupV2Reader) ReadMemoryLimit(parentDir string) (int64, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.MemoryLimitName)
	if !ok {
		return -1, ErrResourceNotRegistered
	}
	return sysutil.ReadCgroupAndParseInt64(parentDir, resource)
}

func (r *CgroupV2Reader) ReadMemoryStat(parentDir string) (*sysutil.MemoryStatRaw, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.MemoryStatName)
	if !ok {
		return nil, ErrResourceNotRegistered
	}
	s, err := sysutil.CgroupFileRead(parentDir, resource)
	if err != nil {
		return nil, fmt.Errorf("cannot read cgroup file, err: %v", err)
	}
	// content: `anon 0\nfile 0\nkernel_stack 0\n...inactive_anon 0\nactive_anon 0\n...`
	v, err := sysutil.ParseMemoryStatRawV2(s)
	if err != nil {
		return nil, fmt.Errorf("cannot parse cgroup value %s, err: %v", s, err)
	}
	return v, nil
}

func (r *CgroupV2Reader) ReadCPUTasks(parentDir string) ([]int32, error) {
	resource, ok := sysutil.DefaultRegistry.Get(sysutil.CgroupVersionV2, sysutil.CPUTasksName)
	if !ok {
		return nil, ErrResourceNotRegistered
	}
	// content: `7742\n10971\n11049\n11051...`
	return sysutil.ReadCgroupAndParseInt32Slice(parentDir, resource)
}

func NewCgroupReader() CgroupReader {
	if sysutil.GetCurrentCgroupVersion() == sysutil.CgroupVersionV2 {
		return &CgroupV2Reader{}
	}
	return &CgroupV1Reader{}
}
