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

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/perf"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_readTotalCPUStat(t *testing.T) {
	tempDir := t.TempDir()
	tempInvalidStatPath := filepath.Join(tempDir, "no_stat")
	tempStatPath := filepath.Join(tempDir, "stat")
	statContentStr := "cpu  514003 37519 593580 1706155242 5134 45033 38832 0 0 0\n" +
		"cpu0 9755 845 15540 26635869 3021 2312 9724 0 0 0\n" +
		"cpu1 10075 664 10790 26653871 214 973 1163 0 0 0\n" +
		"intr 574218032 193 0 0 0 4209 0 0 225 131056 131080 130910 130673 130935 130681 130682 130949 131048\n" +
		"ctxt 701110258\n" +
		"btime 1620641098\n" +
		"processes 4488302\n" +
		"procs_running 53\n" +
		"procs_blocked 0\n" +
		"softirq 134422017 2 39835165 107003 28614585 2166152 0 2398085 30750729 0 30550296\n"
	err := os.WriteFile(tempStatPath, []byte(statContentStr), 0666)
	if err != nil {
		t.Error(err)
	}
	type args struct {
		statPath string
	}
	tests := []struct {
		name    string
		args    args
		want    uint64
		wantErr bool
	}{
		{
			name:    "read illegal stat",
			args:    args{statPath: tempInvalidStatPath},
			want:    0,
			wantErr: true,
		},
		{
			name:    "read test cpu stat path",
			args:    args{statPath: tempStatPath},
			want:    1228967,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := readTotalCPUStat(tt.args.statPath)
			if (err != nil) != tt.wantErr {
				t.Errorf("readTotalCPUStat wantErr %v but got err %s", tt.wantErr, err)
			}
			if !tt.wantErr && got != tt.want {
				t.Errorf("readTotalCPUStat want %v but got %v", tt.want, got)
			}
		})
	}
}

func Test_GetCPUStatUsageTicks(t *testing.T) {
	if runtime.GOOS != "linux" {
		t.Log("Ignore non-Linux environment")
		return
	}
	system.SetConf(*system.NewHostModeConfig())
	cpuStatUsage, err := GetCPUStatUsageTicks()
	if err != nil {
		t.Error("failed to get CPU stat usage: ", err)
	}
	t.Log("get cpu stat usage ticks ", cpuStatUsage)
}

func Test_readPodCPUUsage(t *testing.T) {
	tempDir := t.TempDir()
	tempInvalidPodCgroupDir := filepath.Join(tempDir, "no_cgroup")
	tempPodStatPath := filepath.Join(tempDir, system.CPUAcctUsageName)
	err := os.WriteFile(tempPodStatPath, []byte(getUsageContents()), 0666)
	assert.NoError(t, err)
	tempInvalidPodCgroupDir1 := filepath.Join(tempDir, "no_cgroup_1")
	err = os.Mkdir(tempInvalidPodCgroupDir1, 0755)
	assert.NoError(t, err)
	tempPodInvalidStatPath := filepath.Join(tempInvalidPodCgroupDir1, system.CPUAcctUsageName)
	err = os.WriteFile(tempPodInvalidStatPath, []byte(getInvalidUsageContents()), 0666)
	assert.NoError(t, err)
	type args struct {
		podCgroupDir string
	}
	tests := []struct {
		name    string
		args    args
		want    uint64
		wantErr bool
	}{
		{
			name:    "read illegal cpu usage path",
			args:    args{podCgroupDir: tempInvalidPodCgroupDir},
			want:    0,
			wantErr: true,
		},
		{
			name:    "read test cpu usage path",
			args:    args{podCgroupDir: tempPodStatPath},
			want:    1356232,
			wantErr: false,
		},
		{
			name:    "read invalid cpu usage content",
			args:    args{podCgroupDir: tempPodInvalidStatPath},
			want:    0,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := readCPUAcctUsage(tt.args.podCgroupDir)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_GetPodCPUUsageNanoseconds(t *testing.T) {
	tempDir := t.TempDir()
	system.Conf = system.NewDsModeConfig()
	_, err := GetPodCPUUsageNanoseconds(tempDir)
	assert.NotNil(t, err)
}

func Test_GetContainerCPUUsageNanoseconds(t *testing.T) {
	tempDir := t.TempDir()
	system.Conf = system.NewDsModeConfig()
	container := &corev1.ContainerStatus{ContainerID: "fakeid"}
	_, err := GetContainerCPUUsageNanoseconds(tempDir, container)
	assert.NotNil(t, err)
}

func Test_GetRootCgroupCPUUsageNanoseconds(t *testing.T) {
	helper := system.NewFileTestUtil(t)
	helper.WriteCgroupFileContents(GetKubeQosRelativePath(corev1.PodQOSBestEffort), system.CPUAcctUsage, getUsageContents())
	got, err := GetRootCgroupCPUUsageNanoseconds(corev1.PodQOSBestEffort)
	assert.NoError(t, err)
	assert.Equal(t, uint64(1356232), got)
}

func Test_GetContainerCyclesAndInstructions(t *testing.T) {
	tempDir := t.TempDir()
	f, _ := os.OpenFile(tempDir, os.O_RDONLY, os.ModeDir)
	collector, _ := perf.NewPerfCollector(f, []int{})
	_, _, err := GetContainerCyclesAndInstructions(collector)
	assert.Nil(t, err)
}

func Test_GetContainerPerfCollector(t *testing.T) {
	tempDir := t.TempDir()
	containerStatus := &corev1.ContainerStatus{
		ContainerID: "containerd://test",
	}
	assert.NotPanics(t, func() {
		_, err := GetContainerPerfCollector(tempDir, containerStatus, 1)
		if err != nil {
			return
		}
	})
	wrongContainerStatus := &corev1.ContainerStatus{
		ContainerID: "wrong-container-status-test",
	}
	_, err := GetContainerPerfCollector(tempDir, wrongContainerStatus, 1)
	assert.NotNil(t, err)
}

func Test_GetPodPSI(t *testing.T) {
	tempDir := t.TempDir()
	_, err := GetPodPSI(tempDir)
	assert.NotNil(t, err)
}

func Test_GetContainerPSI(t *testing.T) {
	tempDir := t.TempDir()
	c := &corev1.ContainerStatus{
		ContainerID: "containerd://test",
	}
	_, err := GetContainerPSI(tempDir, c)
	assert.NotNil(t, err)
}

func getUsageContents() string {
	return "1356232"
}

func getInvalidUsageContents() string {
	return "-987654321"
}
