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

package metricsadvisor

import (
	"fmt"
	"os"
	"path"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mockmetriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	mockstatesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/perf"
)

func TestNewPerformanceCollector(t *testing.T) {
	type args struct {
		cfg            *Config
		statesInformer statesinformer.StatesInformer
		metricCache    metriccache.MetricCache
		timeWindow     int
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "new-performance-collector",
			args: args{
				cfg:            &Config{},
				statesInformer: nil,
				metricCache:    nil,
				timeWindow:     10,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := NewPerformanceCollector(tt.args.statesInformer, tt.args.metricCache, tt.args.timeWindow); got == nil {
				t.Errorf("NewPerformanceCollector() = %v", got)
			}
		})
	}
}

func Test_collectContainerCPI(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockStatesInformer := mockstatesinformer.NewMockStatesInformer(ctrl)
	mockMetricCache := mockmetriccache.NewMockMetricCache(ctrl)
	cpuInfo := mockNodeCPUInfo()
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{}).AnyTimes()
	mockMetricCache.EXPECT().GetNodeCPUInfo(&metriccache.QueryParam{}).Return(cpuInfo, nil).AnyTimes()

	c := NewPerformanceCollector(mockStatesInformer, mockMetricCache, 1)
	assert.NotPanics(t, func() {
		c.collectContainerCPI()
	})
}

func Test_collectContainerCPI_cpuInfoErr(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockStatesInformer := mockstatesinformer.NewMockStatesInformer(ctrl)
	mockMetricCache := mockmetriccache.NewMockMetricCache(ctrl)
	cpuInfo := mockNodeCPUInfo()
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{}).AnyTimes()
	mockMetricCache.EXPECT().GetNodeCPUInfo(&metriccache.QueryParam{}).Return(cpuInfo, fmt.Errorf("cpu_error")).AnyTimes()

	c := NewPerformanceCollector(mockStatesInformer, mockMetricCache, 1)
	assert.NotPanics(t, func() {
		c.collectContainerCPI()
	})
}

func Test_collectContainerCPI_mockPod(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	mockStatesInformer := mockstatesinformer.NewMockStatesInformer(ctrl)
	mockMetricCache := mockmetriccache.NewMockMetricCache(ctrl)
	cpuInfo := mockNodeCPUInfo()
	pod := mockPodMeta()
	mockLSPod()
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{pod}).AnyTimes()
	mockMetricCache.EXPECT().GetNodeCPUInfo(&metriccache.QueryParam{}).Return(cpuInfo, nil).AnyTimes()

	c := NewPerformanceCollector(mockStatesInformer, mockMetricCache, 1)
	assert.NotPanics(t, func() {
		c.collectContainerCPI()
	})
}

func mockPodMeta() *statesinformer.PodMeta {
	return &statesinformer.PodMeta{
		Pod: &corev1.Pod{
			ObjectMeta: metav1.ObjectMeta{
				UID: "test-pod-uid",
			},
			Status: corev1.PodStatus{
				ContainerStatuses: []corev1.ContainerStatus{
					{
						ContainerID: "test-01",
						Name:        "test-container",
					},
				},
			},
		},
	}
}

func mockNodeCPUInfo() *metriccache.NodeCPUInfo {
	return &metriccache.NodeCPUInfo{
		TotalInfo: util.CPUTotalInfo{
			NumberCPUs: 0,
		},
	}
}

func Test_getAndStartCollectorOnSingleContainer(t *testing.T) {
	tempDir := t.TempDir()
	containerStatus := &corev1.ContainerStatus{
		ContainerID: "containerd://test",
	}
	c := NewPerformanceCollector(nil, nil, 0)
	assert.NotPanics(t, func() {
		_, err := c.getAndStartCollectorOnSingleContainer(tempDir, containerStatus, 0)
		if err != nil {
			return
		}
	})
}

func Test_profilePerfOnSingleContainer(t *testing.T) {
	config := &metriccache.Config{
		MetricGCIntervalSeconds: 60,
		MetricExpireSeconds:     60,
	}
	m, _ := metriccache.NewMetricCache(config)

	containerStatus := &corev1.ContainerStatus{
		ContainerID: "containerd://test",
	}
	tempDir := t.TempDir()
	f, _ := os.OpenFile(tempDir, os.O_RDONLY, os.ModeDir)
	perfCollector, _ := perf.NewPerfCollector(f, []int{})

	c := NewPerformanceCollector(nil, m, 0)
	testingPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "test_pod",
			Namespace: "test_pod_namespace",
			UID:       "test01",
		},
	}
	assert.NotPanics(t, func() {
		c.profilePerfOnSingleContainer(containerStatus, perfCollector, testingPod)
	})
}

func mockInterferencePodMeta(cgroupDir string) *statesinformer.PodMeta {
	return &statesinformer.PodMeta{
		Pod: &corev1.Pod{
			Status: corev1.PodStatus{
				ContainerStatuses: []corev1.ContainerStatus{
					{
						ContainerID: "containerd://test01",
					},
				},
			},
		},
		CgroupDir: cgroupDir,
	}
}

const (
	FullCorrectPSIContents = "some avg10=0.00 avg60=0.00 avg300=0.00 total=0\nfull avg10=0.00 avg60=0.00 avg300=0.00 total=0"
)

func Test_collectContainerPSI(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	cgroupDir := t.TempDir()
	mockStatesInformer := mockstatesinformer.NewMockStatesInformer(ctrl)
	mockMetricCache := mockmetriccache.NewMockMetricCache(ctrl)
	testPodMeta := mockInterferencePodMeta(t.TempDir())
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{testPodMeta}).AnyTimes()

	paths := util.GetPodCgroupCPUAcctPSIPath(cgroupDir)
	errCreateCPU := createTestPSIFile(paths.CPU, FullCorrectPSIContents)
	if errCreateCPU != nil {
		t.Fatalf("got error when create psi files: %v", errCreateCPU)
	}
	errCreateMem := createTestPSIFile(paths.Mem, FullCorrectPSIContents)
	if errCreateMem != nil {
		t.Fatalf("got error when create psi files: %v", errCreateMem)
	}
	errCreateIO := createTestPSIFile(paths.IO, FullCorrectPSIContents)
	if errCreateIO != nil {
		t.Fatalf("got error when create psi files: %v", errCreateIO)
	}

	c := NewPerformanceCollector(mockStatesInformer, mockMetricCache, 1)
	assert.NotPanics(t, func() {
		c.collectContainerPSI()
	})
}

func Test_collectPodPSI(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	cgroupDir := t.TempDir()
	mockStatesInformer := mockstatesinformer.NewMockStatesInformer(ctrl)
	mockMetricCache := mockmetriccache.NewMockMetricCache(ctrl)
	testPodMeta := mockInterferencePodMeta(t.TempDir())
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{testPodMeta}).AnyTimes()

	paths := util.GetPodCgroupCPUAcctPSIPath(cgroupDir)
	errCreateCPU := createTestPSIFile(paths.CPU, FullCorrectPSIContents)
	if errCreateCPU != nil {
		t.Fatalf("got error when create psi files: %v", errCreateCPU)
	}
	errCreateMem := createTestPSIFile(paths.Mem, FullCorrectPSIContents)
	if errCreateMem != nil {
		t.Fatalf("got error when create psi files: %v", errCreateMem)
	}
	errCreateIO := createTestPSIFile(paths.IO, FullCorrectPSIContents)
	if errCreateIO != nil {
		t.Fatalf("got error when create psi files: %v", errCreateIO)
	}

	c := NewPerformanceCollector(mockStatesInformer, mockMetricCache, 1)
	assert.NotPanics(t, func() {
		c.collectPodPSI()
	})
}

func createTestPSIFile(filePath, contents string) error {
	dir, _ := path.Split(filePath)
	if err := os.MkdirAll(dir, 0777); err != nil {
		return err
	}
	if _, err := os.Create(filePath); err != nil {
		return err
	}
	err := os.WriteFile(filePath, []byte(contents), 0644)
	if err != nil {
		return err
	}
	return nil
}
