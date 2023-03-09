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

package util

import (
	"os"
	"path/filepath"
	"runtime"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_readMemInfo(t *testing.T) {
	tempDir := t.TempDir()
	tempInvalidMemInfoPath := filepath.Join(tempDir, "no_meminfo")
	tempMemInfoPath := filepath.Join(tempDir, "meminfo")
	memInfoContentStr := "MemTotal:       263432804 kB\nMemFree:        254391744 kB\nMemAvailable:   256703236 kB\n" +
		"Buffers:          958096 kB\nCached:          3763224 kB\nSwapCached:            0 kB\n" +
		"Active:          2786012 kB\nInactive:        2223752 kB\nActive(anon):     289488 kB\n" +
		"Inactive(anon):     1300 kB\nActive(file):    2496524 kB\nInactive(file):  2222452 kB\n" +
		"Unevictable:           0 kB\nMlocked:               0 kB\nSwapTotal:             0 kB\n" +
		"SwapFree:              0 kB\nDirty:               624 kB\nWriteback:             0 kB\n" +
		"AnonPages:        281748 kB\nMapped:           495936 kB\nShmem:              2340 kB\n" +
		"Slab:            1097040 kB\nSReclaimable:     445164 kB\nSUnreclaim:       651876 kB\n" +
		"KernelStack:       20944 kB\nPageTables:         7896 kB\nNFS_Unstable:          0 kB\n" +
		"Bounce:                0 kB\nWritebackTmp:          0 kB\nCommitLimit:    131716400 kB\n" +
		"Committed_AS:    3825364 kB\nVmallocTotal:   34359738367 kB\nVmallocUsed:           0 kB\n" +
		"VmallocChunk:          0 kB\nHardwareCorrupted:     0 kB\nAnonHugePages:     38912 kB\n" +
		"ShmemHugePages:        0 kB\nShmemPmdMapped:        0 kB\nCmaTotal:              0 kB\n" +
		"CmaFree:               0 kB\nHugePages_Total:       0\nHugePages_Free:        0\n" +
		"HugePages_Rsvd:        0\nHugePages_Surp:        0\nHugepagesize:       2048 kB\n" +
		"DirectMap4k:      414760 kB\nDirectMap2M:     8876032 kB\nDirectMap1G:    261095424 kB\n"
	err := os.WriteFile(tempMemInfoPath, []byte(memInfoContentStr), 0666)
	assert.NoError(t, err)
	tempMemInfoPath1 := filepath.Join(tempDir, "meminfo1")
	memInfoContentStr1 := "MemTotal:       263432804 kB\nMemFree:        254391744 kB\nMemAvailable:   256703236 kB\n" +
		"Buffers:          958096 kB\nCached:          invalidField kB\nSwapCached:            0 kB\n" +
		"Active:          2786012 kB\nInactive:        2223752 kB\nActive(anon):     289488 kB\n" +
		"Inactive(anon):     1300 kB\nActive(file):    2496524 kB\nInactive(file):  2222452 kB\n" +
		"Unevictable:           0 kB\nMlocked:               0 kB\nSwapTotal:             0 kB\n" +
		"SwapFree:              0 kB\nDirty:               624 kB\nWriteback:             0 kB\n" +
		"AnonPages:        281748 kB\nMapped:           495936 kB\nShmem:              2340 kB\n" +
		"Slab:            1097040 kB\nSReclaimable:     445164 kB\nSUnreclaim:       651876 kB\n" +
		"KernelStack:       20944 kB\nPageTables:         7896 kB\nNFS_Unstable:          0 kB\n" +
		"Bounce:                0 kB\nWritebackTmp:          0 kB\nCommitLimit:    131716400 kB\n" +
		"Committed_AS:    3825364 kB\nVmallocTotal:   34359738367 kB\nVmallocUsed:           0 kB\n" +
		"VmallocChunk:          0 kB\nHardwareCorrupted:     0 kB\nAnonHugePages:     38912 kB\n" +
		"ShmemHugePages:        0 kB\nShmemPmdMapped:        0 kB\nCmaTotal:              0 kB\n" +
		"CmaFree:               0 kB\nHugePages_Total:       0\nHugePages_Free:        0\n" +
		"HugePages_Rsvd:        0\nHugePages_Surp:        0\nHugepagesize:       2048 kB\n" +
		"DirectMap4k:      414760 kB\nDirectMap2M:     8876032 kB\nDirectMap1G:    261095424 kB\n"
	err = os.WriteFile(tempMemInfoPath1, []byte(memInfoContentStr1), 0666)
	assert.NoError(t, err)
	type args struct {
		path string
	}
	tests := []struct {
		name    string
		args    args
		want    *MemInfo
		wantErr bool
	}{
		{
			name:    "read illegal mem stat",
			args:    args{path: tempInvalidMemInfoPath},
			want:    nil,
			wantErr: true,
		},
		{
			name: "read test mem stat path",
			args: args{path: tempMemInfoPath},
			want: &MemInfo{
				MemTotal: 263432804, MemFree: 254391744, MemAvailable: 256703236,
				Buffers: 958096, Cached: 3763224, SwapCached: 0,
				Active: 2786012, Inactive: 2223752, ActiveAnon: 289488,
				InactiveAnon: 1300, ActiveFile: 2496524, InactiveFile: 2222452,
				Unevictable: 0, Mlocked: 0, SwapTotal: 0,
				SwapFree: 0, Dirty: 624, Writeback: 0,
				AnonPages: 281748, Mapped: 495936, Shmem: 2340,
				Slab: 1097040, SReclaimable: 445164, SUnreclaim: 651876,
				KernelStack: 20944, PageTables: 7896, NFS_Unstable: 0,
				Bounce: 0, WritebackTmp: 0, CommitLimit: 131716400,
				Committed_AS: 3825364, VmallocTotal: 34359738367, VmallocUsed: 0,
				VmallocChunk: 0, HardwareCorrupted: 0, AnonHugePages: 38912,
				HugePages_Total: 0, HugePages_Free: 0, HugePages_Rsvd: 0,
				HugePages_Surp: 0, Hugepagesize: 2048, DirectMap4k: 414760,
				DirectMap2M: 8876032, DirectMap1G: 261095424,
			},
			wantErr: false,
		},
		{
			name: "read test mem stat path",
			args: args{path: tempMemInfoPath},
			want: &MemInfo{
				MemTotal: 263432804, MemFree: 254391744, MemAvailable: 256703236,
				Buffers: 958096, Cached: 3763224, SwapCached: 0,
				Active: 2786012, Inactive: 2223752, ActiveAnon: 289488,
				InactiveAnon: 1300, ActiveFile: 2496524, InactiveFile: 2222452,
				Unevictable: 0, Mlocked: 0, SwapTotal: 0,
				SwapFree: 0, Dirty: 624, Writeback: 0,
				AnonPages: 281748, Mapped: 495936, Shmem: 2340,
				Slab: 1097040, SReclaimable: 445164, SUnreclaim: 651876,
				KernelStack: 20944, PageTables: 7896, NFS_Unstable: 0,
				Bounce: 0, WritebackTmp: 0, CommitLimit: 131716400,
				Committed_AS: 3825364, VmallocTotal: 34359738367, VmallocUsed: 0,
				VmallocChunk: 0, HardwareCorrupted: 0, AnonHugePages: 38912,
				HugePages_Total: 0, HugePages_Free: 0, HugePages_Rsvd: 0,
				HugePages_Surp: 0, Hugepagesize: 2048, DirectMap4k: 414760,
				DirectMap2M: 8876032, DirectMap1G: 261095424,
			},
			wantErr: false,
		},
		{
			name: "read test mem stat path",
			args: args{path: tempMemInfoPath1},
			want: &MemInfo{
				MemTotal: 263432804, MemFree: 254391744, MemAvailable: 256703236,
				Buffers: 958096, Cached: 0, SwapCached: 0,
				Active: 2786012, Inactive: 2223752, ActiveAnon: 289488,
				InactiveAnon: 1300, ActiveFile: 2496524, InactiveFile: 2222452,
				Unevictable: 0, Mlocked: 0, SwapTotal: 0,
				SwapFree: 0, Dirty: 624, Writeback: 0,
				AnonPages: 281748, Mapped: 495936, Shmem: 2340,
				Slab: 1097040, SReclaimable: 445164, SUnreclaim: 651876,
				KernelStack: 20944, PageTables: 7896, NFS_Unstable: 0,
				Bounce: 0, WritebackTmp: 0, CommitLimit: 131716400,
				Committed_AS: 3825364, VmallocTotal: 34359738367, VmallocUsed: 0,
				VmallocChunk: 0, HardwareCorrupted: 0, AnonHugePages: 38912,
				HugePages_Total: 0, HugePages_Free: 0, HugePages_Rsvd: 0,
				HugePages_Surp: 0, Hugepagesize: 2048, DirectMap4k: 414760,
				DirectMap2M: 8876032, DirectMap1G: 261095424,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := readMemInfo(tt.args.path)
			if (err != nil) != tt.wantErr {
				t.Errorf("readMemInfo wantErr %v but got err %s", tt.wantErr, err)
			}
			if !tt.wantErr {
				assert.Equal(t, tt.want, got)
			}
		})
	}
}

func Test_GetMemInfoUsageKB(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Log("Ignore non-Linux environment")
		return
	}
	memInfoUsage, err := GetMemInfoUsageKB()
	if err != nil {
		t.Error("failed to get MemInfo usage: ", err)
	}
	t.Log("meminfo: ", memInfoUsage)
}

func Test_readPodMemStat(t *testing.T) {
	tempDir := t.TempDir()
	tempInvalidPodCgroupDir := filepath.Join(tempDir, "no_cgroup")
	tempPodMemStatPath := filepath.Join(tempDir, system.MemoryStatName)
	memStatContentStr := "...\ntotal_cache 4843945984\ntotal_rss 310595584\ntotal_rss_huge 60817408\n" +
		"total_mapped_file 243519488\ntotal_dirty 413696\ntotal_writeback 0\ntotal_swap 0\n" +
		"total_workingset_refault 0\ntotal_workingset_activate 0total_workingset_restore 0\n" +
		"total_pgpgin 723136924\ntotal_pgpgout 721893310\ntotal_pgfault 1613203098\n" +
		"total_pgmajfault 5663\ntotal_pgoutrun 0\ntotal_allocstall 0\n" +
		"total_kswapd_steal 0\ntotal_pg_pgsteal 0\ntotal_kswapd_pgscan 0\n" +
		"total_pg_pgscan 0\ntotal_pgrefill 0\ntotal_inactive_anon 1331200\n" +
		"total_active_anon 310775808\ntotal_inactive_file 2277351424\ntotal_active_file 2564194304\n" +
		"total_unevictable 0"
	err := os.WriteFile(tempPodMemStatPath, []byte(memStatContentStr), 0666)
	if err != nil {
		t.Error(err)
	}
	type args struct {
		podCgroupDir string
	}
	tests := []struct {
		name    string
		args    args
		want    int64
		wantErr bool
	}{
		{
			name:    "read illegal mem stat path",
			args:    args{podCgroupDir: tempInvalidPodCgroupDir},
			want:    0,
			wantErr: true,
		},
		{
			name:    "read test mem stat path",
			args:    args{podCgroupDir: tempPodMemStatPath},
			want:    312107008,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := readCgroupMemStat(tt.args.podCgroupDir)
			if (err != nil) != tt.wantErr {
				t.Errorf("readCgroupMemStat wantErr %v but got err %s", tt.wantErr, err)
			}
			if !tt.wantErr && got != tt.want {
				t.Errorf("readCgroupMemStat want %v but got %v", tt.want, got)
			}
		})
	}
}

func Test_GetPodMemStatUsageBytes(t *testing.T) {
	tempDir := t.TempDir()
	system.Conf = system.NewDsModeConfig()
	_, err := GetPodMemStatUsageBytes(tempDir)
	assert.NotNil(t, err)
}

func TestGetContainerMemStatUsageBytes(t *testing.T) {
	tempDir := t.TempDir()
	container := &corev1.ContainerStatus{
		Name:        "test-container",
		ContainerID: "test-container-id",
	}
	system.Conf = system.NewDsModeConfig()
	_, err := GetContainerMemStatUsageBytes(tempDir, container)
	assert.NotNil(t, err)
}
