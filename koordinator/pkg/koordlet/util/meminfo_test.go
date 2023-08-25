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
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_readMemInfo(t *testing.T) {
	tempDir := t.TempDir()
	tempInvalidMemInfoPath := filepath.Join(tempDir, "no_meminfo")
	tempMemInfoPath := filepath.Join(tempDir, "meminfo")
	memInfoContentStr := `MemTotal:       263432804 kB
MemFree:        254391744 kB
MemAvailable:   256703236 kB
Buffers:          958096 kB
Cached:          3763224 kB
SwapCached:            0 kB
Active:          2786012 kB
Inactive:        2223752 kB
Active(anon):     289488 kB
Inactive(anon):     1300 kB
Active(file):    2496524 kB
Inactive(file):  2222452 kB
Unevictable:           0 kB
Mlocked:               0 kB
SwapTotal:             0 kB
SwapFree:              0 kB
Dirty:               624 kB
Writeback:             0 kB
AnonPages:        281748 kB
Mapped:           495936 kB
Shmem:              2340 kB
Slab:            1097040 kB
SReclaimable:     445164 kB
SUnreclaim:       651876 kB
KernelStack:       20944 kB
PageTables:         7896 kB
NFS_Unstable:          0 kB
Bounce:                0 kB
WritebackTmp:          0 kB
CommitLimit:    131716400 kB
Committed_AS:    3825364 kB
VmallocTotal:   34359738367 kB
VmallocUsed:           0 kB
VmallocChunk:          0 kB
HardwareCorrupted:     0 kB
AnonHugePages:     38912 kB
ShmemHugePages:        0 kB
ShmemPmdMapped:        0 kB
CmaTotal:              0 kB
CmaFree:               0 kB
HugePages_Total:       0
HugePages_Free:        0
HugePages_Rsvd:        0
HugePages_Surp:        0
Hugepagesize:       2048 kB
DirectMap4k:      414760 kB
DirectMap2M:     8876032 kB
DirectMap1G:    261095424 kB`
	err := os.WriteFile(tempMemInfoPath, []byte(memInfoContentStr), 0666)
	assert.NoError(t, err)
	tempMemInfoPath1 := filepath.Join(tempDir, "meminfo1")
	memInfoContentStr1 := `MemTotal:       263432804 kB
MemFree:        254391744 kB
MemAvailable:   256703236 kB
Buffers:          958096 kB
Cached:                0 kB
SwapCached:            0 kB
Active:          2786012 kB
Inactive:        2223752 kB
Active(anon):     289488 kB
Inactive(anon):     1300 kB
Active(file):    2496524 kB
Inactive(file):  2222452 kB
Unevictable:           0 kB
Mlocked:               0 kB
SwapTotal:             0 kB
SwapFree:              0 kB
Dirty:               624 kB
Writeback:             0 kB
AnonPages:        281748 kB
Mapped:           495936 kB
Shmem:              2340 kB
Slab:            1097040 kB
SReclaimable:     445164 kB
SUnreclaim:       651876 kB
KernelStack:       20944 kB
PageTables:         7896 kB
NFS_Unstable:          0 kB
Bounce:                0 kB
WritebackTmp:          0 kB
CommitLimit:    131716400 kB
Committed_AS:    3825364 kB
VmallocTotal:   34359738367 kB
VmallocUsed:           0 kB
VmallocChunk:          0 kB
HardwareCorrupted:     0 kB
AnonHugePages:     38912 kB
ShmemHugePages:        0 kB
ShmemPmdMapped:        0 kB
CmaTotal:              0 kB
CmaFree:               0 kB
HugePages_Total:       0
HugePages_Free:        0
HugePages_Rsvd:        0
HugePages_Surp:        0
Hugepagesize:       2048 kB
DirectMap4k:      414760 kB
DirectMap2M:     8876032 kB
DirectMap1G:    261095424 kB`
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
			got, gotErr := readMemInfo(tt.args.path)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_GetMemInfoUsageKB(t *testing.T) {
	testMemInfo := `MemTotal:       263432804 kB
MemFree:        254391744 kB
MemAvailable:   256703236 kB
Buffers:          958096 kB
Cached:          3763224 kB
SwapCached:            0 kB
Active:          2786012 kB
Inactive:        2223752 kB
Active(anon):     289488 kB
Inactive(anon):     1300 kB
Active(file):    2496524 kB
Inactive(file):  2222452 kB
Unevictable:           0 kB
Mlocked:               0 kB
SwapTotal:             0 kB
SwapFree:              0 kB
Dirty:               624 kB
Writeback:             0 kB
AnonPages:        281748 kB
Mapped:           495936 kB
Shmem:              2340 kB
Slab:            1097040 kB
SReclaimable:     445164 kB
SUnreclaim:       651876 kB
KernelStack:       20944 kB
PageTables:         7896 kB
NFS_Unstable:          0 kB
Bounce:                0 kB
WritebackTmp:          0 kB
CommitLimit:    131716400 kB
Committed_AS:    3825364 kB
VmallocTotal:   34359738367 kB
VmallocUsed:           0 kB
VmallocChunk:          0 kB
HardwareCorrupted:     0 kB
AnonHugePages:     38912 kB
ShmemHugePages:        0 kB
ShmemPmdMapped:        0 kB
CmaTotal:              0 kB
CmaFree:               0 kB
HugePages_Total:       0
HugePages_Free:        0
HugePages_Rsvd:        0
HugePages_Surp:        0
Hugepagesize:       2048 kB
DirectMap4k:      414760 kB
DirectMap2M:     8876032 kB
DirectMap1G:    261095424 kB`

	helper := system.NewFileTestUtil(t)
	defer helper.Cleanup()
	helper.WriteProcSubFileContents(system.ProcMemInfoName, testMemInfo)

	memInfoUsage, err := GetMemInfoUsageKB()
	assert.NoError(t, err)
	assert.NotNil(t, memInfoUsage)
}
