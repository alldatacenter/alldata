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
	"testing"

	"github.com/stretchr/testify/assert"

	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

func TestNewCgroupReader(t *testing.T) {
	type fields struct {
		UseCgroupsV2 bool
	}
	tests := []struct {
		name   string
		fields fields
		want   CgroupReader
	}{
		{
			name: "cgroups-v1 reader",
			fields: fields{
				UseCgroupsV2: false,
			},
			want: &CgroupV1Reader{},
		},
		{
			name: "cgroups-v1 reader",
			fields: fields{
				UseCgroupsV2: true,
			},
			want: &CgroupV2Reader{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)

			r := NewCgroupReader()
			assert.Equal(t, tt.want, r)
		})
	}
}

func TestCgroupReader_ReadCPUQuota(t *testing.T) {
	type fields struct {
		UseCgroupsV2     bool
		CPUCFSQuotaValue string
		CPUMaxValue      string
	}
	type args struct {
		parentDir string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    int64
		wantErr bool
	}{
		{
			name:    "v1 path not exist",
			fields:  fields{},
			want:    -1,
			wantErr: true,
		},
		{
			name: "parse v1 value successfully",
			fields: fields{
				CPUCFSQuotaValue: "200000",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    200000,
			wantErr: false,
		},
		{
			name: "parse v1 value successfully 1",
			fields: fields{
				CPUCFSQuotaValue: "-1",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: false,
		},
		{
			name: "parse v1 value failed", // only for testing
			fields: fields{
				CPUCFSQuotaValue: "unknown",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
		{
			name: "v2 path not exist",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2: true,
				CPUMaxValue:  "200000 100000",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    200000,
			wantErr: false,
		},
		{
			name: "parse v2 value successfully 1",
			fields: fields{
				UseCgroupsV2: true,
				CPUMaxValue:  "max 100000",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: false,
		},
		{
			name: "parse v2 value failed", // only for testing
			fields: fields{
				UseCgroupsV2: true,
				CPUMaxValue:  "unknown",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.CPUCFSQuotaValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUCFSQuota, tt.fields.CPUCFSQuotaValue)
			}
			if tt.fields.CPUMaxValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUCFSQuotaV2, tt.fields.CPUMaxValue)
			}

			got, gotErr := NewCgroupReader().ReadCPUQuota(tt.args.parentDir)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestCgroupReader_ReadCPUPeriod(t *testing.T) {
	type fields struct {
		UseCgroupsV2      bool
		CPUCFSPeriodValue string
		CPUMaxValue       string
	}
	type args struct {
		parentDir string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    int64
		wantErr bool
	}{
		{
			name:    "v1 path not exist",
			fields:  fields{},
			want:    -1,
			wantErr: true,
		},
		{
			name: "parse v1 value successfully",
			fields: fields{
				CPUCFSPeriodValue: "100000",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    100000,
			wantErr: false,
		},
		{
			name: "v2 path not exist",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2: true,
				CPUMaxValue:  "200000 100000",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    100000,
			wantErr: false,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2: true,
				CPUMaxValue:  "max 200000",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    200000,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.CPUCFSPeriodValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUCFSPeriod, tt.fields.CPUCFSPeriodValue)
			}
			if tt.fields.CPUMaxValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUCFSPeriodV2, tt.fields.CPUMaxValue)
			}

			got, gotErr := NewCgroupReader().ReadCPUPeriod(tt.args.parentDir)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestCgroupReader_ReadCPUAcctUsage(t *testing.T) {
	type fields struct {
		UseCgroupsV2      bool
		CPUAcctUsageValue string
		CPUStatV2Value    string
	}
	type args struct {
		parentDir string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    uint64
		wantErr bool
	}{
		{
			name:   "v1 path not exist",
			fields: fields{},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "parse v1 value successfully",
			fields: fields{
				CPUAcctUsageValue: "9000000000000000",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    9000000000000000,
			wantErr: false,
		},
		{
			name: "v2 path not exist",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2: true,
				CPUStatV2Value: `usage_usec 90000
user_usec 20000
system_usec 30000
nr_periods 0
nr_throttled 0
throttled_usec 0`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    90000000,
			wantErr: false,
		},
		{
			name: "parse v2 value failed",
			fields: fields{
				UseCgroupsV2: true,
				CPUStatV2Value: `user_usec 20000
system_usec 30000
nr_periods 0
nr_throttled 0
throttled_usec 0`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    0,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.CPUAcctUsageValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUAcctUsage, tt.fields.CPUAcctUsageValue)
			}
			if tt.fields.CPUStatV2Value != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUStatV2, tt.fields.CPUStatV2Value)
			}

			got, gotErr := NewCgroupReader().ReadCPUAcctUsage(tt.args.parentDir)
			assert.Equal(t, tt.wantErr, gotErr != nil, gotErr)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestCgroupReader_ReadCPUStat(t *testing.T) {
	type fields struct {
		UseCgroupsV2   bool
		CPUStatValue   string
		CPUStatV2Value string
	}
	type args struct {
		parentDir string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *sysutil.CPUStatRaw
		wantErr bool
	}{
		{
			name:   "v1 path not exist",
			fields: fields{},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse v1 value successfully",
			fields: fields{
				CPUStatValue: `nr_periods 1
nr_throttled 2
throttled_time 3`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want: &sysutil.CPUStatRaw{
				NrPeriods:            1,
				NrThrottled:          2,
				ThrottledNanoSeconds: 3,
			},
			wantErr: false,
		},
		{
			name: "parse v1 value successfully 1",
			fields: fields{
				CPUStatValue: `nr_periods 1
nr_throttled 2
throttled_time 3
nr_burst 4
burst_time 5`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want: &sysutil.CPUStatRaw{
				NrPeriods:            1,
				NrThrottled:          2,
				ThrottledNanoSeconds: 3,
			},
			wantErr: false,
		},
		{
			name: "parse v1 value failed",
			fields: fields{
				CPUStatValue: `throttled_time 3`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "v2 path not exist",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2: true,
				CPUStatV2Value: `usage_usec 90000
user_usec 20000
system_usec 30000
nr_periods 1
nr_throttled 2
throttled_usec 3`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want: &sysutil.CPUStatRaw{
				NrPeriods:            1,
				NrThrottled:          2,
				ThrottledNanoSeconds: 3000,
			},
			wantErr: false,
		},
		{
			name: "parse v2 value failed",
			fields: fields{
				UseCgroupsV2: true,
				CPUStatV2Value: `usage_usec 90000
user_usec 20000
system_usec 30000`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.CPUStatValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUStat, tt.fields.CPUStatValue)
			}
			if tt.fields.CPUStatV2Value != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUStatV2, tt.fields.CPUStatV2Value)
			}

			got, gotErr := NewCgroupReader().ReadCPUStat(tt.args.parentDir)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestCgroupReader_ReadCPUShares(t *testing.T) {
	type fields struct {
		UseCgroupsV2   bool
		CPUSharesValue string
		CPUWeightValue string
	}
	type args struct {
		parentDir string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    int64
		wantErr bool
	}{
		{
			name:   "v1 path not exist",
			fields: fields{},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
		{
			name: "parse v1 value successfully",
			fields: fields{
				CPUSharesValue: "2",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    2,
			wantErr: false,
		},
		{
			name: "parse v1 value successfully 1",
			fields: fields{
				CPUSharesValue: "2048",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    2048,
			wantErr: false,
		},
		{
			name: "v2 path not exist",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2:   true,
				CPUWeightValue: "1",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    10,
			wantErr: false,
		},
		{
			name: "parse v2 value successfully 1",
			fields: fields{
				UseCgroupsV2:   true,
				CPUWeightValue: "100",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    1024,
			wantErr: false,
		},
		{
			name: "parse v2 value failed",
			fields: fields{
				UseCgroupsV2:   true,
				CPUWeightValue: "", // only for testing
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.CPUSharesValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUShares, tt.fields.CPUSharesValue)
			}
			if tt.fields.CPUWeightValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUSharesV2, tt.fields.CPUWeightValue)
			}

			got, gotErr := NewCgroupReader().ReadCPUShares(tt.args.parentDir)
			assert.Equal(t, tt.wantErr, gotErr != nil, gotErr)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestCgroupReader_ReadCPUSet(t *testing.T) {
	testCPUSetStr := "0-7"
	testCPUSet := cpuset.MustParse(testCPUSetStr)
	testCPUSetStr1 := "1,52-53"
	testCPUSet1 := cpuset.MustParse(testCPUSetStr1)
	type fields struct {
		UseCgroupsV2           bool
		CPUSetValue            string
		CPUSetEffectiveV2Value string
	}
	type args struct {
		parentDir string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *cpuset.CPUSet
		wantErr bool
	}{
		{
			name:   "v1 path not exist",
			fields: fields{},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse v1 value successfully",
			fields: fields{
				CPUSetValue: testCPUSetStr,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    &testCPUSet,
			wantErr: false,
		},
		{
			name: "parse v1 value successfully 1",
			fields: fields{
				CPUSetValue: testCPUSetStr1,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    &testCPUSet1,
			wantErr: false,
		},
		{
			name: "v2 path not exist",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2:           true,
				CPUSetEffectiveV2Value: testCPUSetStr,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    &testCPUSet,
			wantErr: false,
		},
		{
			name: "parse v2 value successfully 1",
			fields: fields{
				UseCgroupsV2:           true,
				CPUSetEffectiveV2Value: testCPUSetStr1,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    &testCPUSet1,
			wantErr: false,
		},
		{
			name: "parse v2 value failed",
			fields: fields{
				UseCgroupsV2:           true,
				CPUSetEffectiveV2Value: "unknown", // only for testing
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.CPUSetValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUSet, tt.fields.CPUSetValue)
			}
			if tt.fields.CPUSetEffectiveV2Value != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.CPUSetEffectiveV2, tt.fields.CPUSetEffectiveV2Value)
			}

			got, gotErr := NewCgroupReader().ReadCPUSet(tt.args.parentDir)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestCgroupReader_ReadMemoryLimit(t *testing.T) {
	type fields struct {
		UseCgroupsV2     bool
		MemoryLimitValue string
		MemoryMaxValue   string
	}
	type args struct {
		parentDir string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    int64
		wantErr bool
	}{
		{
			name:   "v1 path not exist",
			fields: fields{},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
		{
			name: "parse v1 value successfully",
			fields: fields{
				MemoryLimitValue: "2147483648",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    2147483648,
			wantErr: false,
		},
		{
			name: "parse v1 value successfully 1",
			fields: fields{
				MemoryLimitValue: "1048576",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    1048576,
			wantErr: false,
		},
		{
			name: "parse v1 value successfully 2",
			fields: fields{
				MemoryLimitValue: "9223372036854771712",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: false,
		},
		{
			name: "v2 path not exist",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2:   true,
				MemoryMaxValue: "2147483648",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    2147483648,
			wantErr: false,
		},
		{
			name: "parse v2 value successfully 1",
			fields: fields{
				UseCgroupsV2:   true,
				MemoryMaxValue: "max",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: false,
		},
		{
			name: "parse v2 value failed",
			fields: fields{
				UseCgroupsV2:   true,
				MemoryMaxValue: "unknown",
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    -1,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.MemoryLimitValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.MemoryLimit, tt.fields.MemoryLimitValue)
			}
			if tt.fields.MemoryMaxValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.MemoryLimitV2, tt.fields.MemoryMaxValue)
			}

			got, gotErr := NewCgroupReader().ReadMemoryLimit(tt.args.parentDir)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestCgroupReader_ReadMemoryStat(t *testing.T) {
	type fields struct {
		UseCgroupsV2       bool
		MemoryStateValue   string
		MemoryStateV2Value string
	}
	type args struct {
		parentDir string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *sysutil.MemoryStatRaw
		wantErr bool
	}{
		{
			name:   "v1 path not exist",
			fields: fields{},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse v1 value successfully",
			fields: fields{
				MemoryStateValue: `cache 0
rss 0
rss_huge 0
shmem 0
mapped_file 0
dirty 0
writeback 0
swap 0
workingset_refault_anon 0
workingset_refault_file 0
workingset_activate_anon 0
workingset_activate_file 0
workingset_restore_anon 0
workingset_restore_file 0
workingset_nodereclaim 0
pgpgin 0
pgpgout 0
pgfault 0
pgmajfault 0
inactive_anon 0
active_anon 0
inactive_file 0
active_file 0
unevictable 0
hierarchical_memory_limit 0
hierarchical_memsw_limit 9223372036854771712
total_cache 2147483648
total_rss 2147483648
total_rss_huge 0
total_shmem 0
total_mapped_file 0
total_dirty 0
total_writeback 0
total_swap 0
total_workingset_refault_anon 0
total_workingset_refault_file 0
total_workingset_activate_anon 0
total_workingset_activate_file 0
total_workingset_restore_anon 0
total_workingset_restore_file 0
total_workingset_nodereclaim 0
total_pgpgin 0
total_pgpgout 0
total_pgfault 0
total_pgmajfault 0
total_inactive_anon 2147483648
total_active_anon 0
total_inactive_file 0
total_active_file 2147483648
total_unevictable 0`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want: &sysutil.MemoryStatRaw{
				Cache:        2147483648,
				RSS:          2147483648,
				InactiveAnon: 2147483648,
				ActiveAnon:   0,
				InactiveFile: 0,
				ActiveFile:   2147483648,
				Unevictable:  0,
			},
			wantErr: false,
		},
		{
			name: "parse v1 value failed",
			fields: fields{
				MemoryStateValue: `cache 0
rss 0
rss_huge 0
shmem 0
mapped_file 0
dirty 0
writeback 0
swap 0`,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "v2 path not exist",
			fields: fields{
				UseCgroupsV2: true,
			},
			args: args{
				parentDir: "/kubepods.slice",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse v2 value successfully",
			fields: fields{
				UseCgroupsV2: true,
				MemoryStateV2Value: `anon 2147483648
file 2147483648
kernel_stack 0
pagetables 1048576
percpu 0
sock 0
shmem 0
file_mapped 1048576
file_dirty 0
file_writeback 0
swapcached 0
anon_thp 0
file_thp 0
shmem_thp 0
inactive_anon 2147483648
active_anon 0
inactive_file 0
active_file 2147483648
unevictable 0
slab_reclaimable 1048576
slab_unreclaimable 0
slab 1048576
workingset_refault_anon 0
workingset_refault_file 0
workingset_activate_anon 0
workingset_activate_file 0
workingset_restore_anon 0
workingset_restore_file 0
workingset_nodereclaim 0
pgfault 0
pgmajfault 0
pgrefill 0
pgscan 0
pgsteal 0
pgactivate 0
pgdeactivate 0
pglazyfree 0
pglazyfreed 0
thp_fault_alloc 0
thp_collapse_alloc 0`,
			},
			want: &sysutil.MemoryStatRaw{
				Cache:        2147483648,
				RSS:          2147483648,
				InactiveAnon: 2147483648,
				ActiveAnon:   0,
				InactiveFile: 0,
				ActiveFile:   2147483648,
				Unevictable:  0,
			},
			wantErr: false,
		},
		{
			name: "parse v2 value failed",
			fields: fields{
				UseCgroupsV2: true,
				MemoryStateV2Value: `anon 2147483648
file 2147483648
kernel_stack 0
pagetables 1048576`,
			},
			want:    nil,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := sysutil.NewFileTestUtil(t)
			defer helper.Cleanup()
			helper.SetCgroupsV2(tt.fields.UseCgroupsV2)
			if tt.fields.MemoryStateValue != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.MemoryStat, tt.fields.MemoryStateValue)
			}
			if tt.fields.MemoryStateV2Value != "" {
				helper.WriteCgroupFileContents(tt.args.parentDir, sysutil.MemoryStatV2, tt.fields.MemoryStateV2Value)
			}

			got, gotErr := NewCgroupReader().ReadMemoryStat(tt.args.parentDir)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}
