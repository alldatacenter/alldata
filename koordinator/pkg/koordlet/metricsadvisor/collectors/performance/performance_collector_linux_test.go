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

package performance

import (
	"fmt"
	"os"
	"path"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mockmetriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	mockstatesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/perf"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func TestNewPerformanceCollector(t *testing.T) {
	type args struct {
		cfg            *framework.Config
		statesInformer statesinformer.StatesInformer
		metricCache    metriccache.MetricCache
		cgroupReader   resourceexecutor.CgroupReader
		timeWindow     int
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "new-performance-collector",
			args: args{
				cfg:            &framework.Config{},
				statesInformer: nil,
				metricCache:    nil,
				cgroupReader:   nil,
				timeWindow:     10,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			opt := &framework.Options{
				Config:         framework.NewDefaultConfig(),
				StatesInformer: tt.args.statesInformer,
				MetricCache:    tt.args.metricCache,
				CgroupReader:   resourceexecutor.NewCgroupReader(),
			}
			if got := New(opt); got == nil {
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
	mockStatesInformer.EXPECT().HasSynced().Return(true).AnyTimes()
	mockMetricCache.EXPECT().GetNodeCPUInfo(&metriccache.QueryParam{}).Return(cpuInfo, nil).AnyTimes()

	collector := New(&framework.Options{
		Config:         framework.NewDefaultConfig(),
		StatesInformer: mockStatesInformer,
		MetricCache:    mockMetricCache,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	})
	c := collector.(*performanceCollector)
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
	mockStatesInformer.EXPECT().HasSynced().Return(true).AnyTimes()
	mockMetricCache.EXPECT().GetNodeCPUInfo(&metriccache.QueryParam{}).Return(cpuInfo, fmt.Errorf("cpu_error")).AnyTimes()

	collector := New(&framework.Options{
		Config:         framework.NewDefaultConfig(),
		StatesInformer: mockStatesInformer,
		MetricCache:    mockMetricCache,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	})
	c := collector.(*performanceCollector)
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
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{}).AnyTimes()
	mockMetricCache.EXPECT().GetNodeCPUInfo(&metriccache.QueryParam{}).Return(cpuInfo, nil).AnyTimes()

	collector := New(&framework.Options{
		Config:         framework.NewDefaultConfig(),
		StatesInformer: mockStatesInformer,
		MetricCache:    mockMetricCache,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	})
	c := collector.(*performanceCollector)
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
	collector := New(&framework.Options{
		Config:         framework.NewDefaultConfig(),
		StatesInformer: nil,
		MetricCache:    nil,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	})
	c := collector.(*performanceCollector)
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

	collector := New(&framework.Options{
		Config:         framework.NewDefaultConfig(),
		StatesInformer: nil,
		MetricCache:    m,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	})
	c := collector.(*performanceCollector)
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
	dir := t.TempDir()
	system.Conf.CgroupRootDir = dir

	cgroupDir := t.TempDir()
	mockStatesInformer := mockstatesinformer.NewMockStatesInformer(ctrl)
	mockMetricCache := mockmetriccache.NewMockMetricCache(ctrl)
	testPodMeta := mockInterferencePodMeta(t.TempDir())
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{testPodMeta}).AnyTimes()

	paths := getPodCgroupCPUAcctPSIPath(cgroupDir)
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

	collector := New(&framework.Options{
		Config:         framework.NewDefaultConfig(),
		StatesInformer: mockStatesInformer,
		MetricCache:    mockMetricCache,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	})
	c := collector.(*performanceCollector)
	assert.NotPanics(t, func() {
		c.collectContainerPSI()
	})
}

func Test_collectPodPSI(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	dir := t.TempDir()
	system.Conf.CgroupRootDir = dir

	cgroupDir := t.TempDir()
	mockStatesInformer := mockstatesinformer.NewMockStatesInformer(ctrl)
	mockMetricCache := mockmetriccache.NewMockMetricCache(ctrl)
	testPodMeta := mockInterferencePodMeta(t.TempDir())
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{testPodMeta}).AnyTimes()

	paths := getPodCgroupCPUAcctPSIPath(cgroupDir)
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

	collector := New(&framework.Options{
		Config:         framework.NewDefaultConfig(),
		StatesInformer: mockStatesInformer,
		MetricCache:    mockMetricCache,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	})
	c := collector.(*performanceCollector)
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

func mockLSPod() *corev1.Pod {
	return &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "test-ns",
			Name:      "test-name-ls",
			UID:       "test-pod-uid-ls",
			Labels: map[string]string{
				apiext.LabelPodQoS: string(apiext.QoSLS),
			},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "test-container-1",
					Resources: corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							corev1.ResourceCPU: *resource.NewQuantity(1, resource.DecimalSI),
						},
						Requests: corev1.ResourceList{
							corev1.ResourceCPU: *resource.NewQuantity(1, resource.DecimalSI),
						},
					},
				},
				{
					Name: "test-container-2",
					Resources: corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							corev1.ResourceCPU: *resource.NewQuantity(2, resource.DecimalSI),
						},
						Requests: corev1.ResourceList{
							corev1.ResourceCPU: *resource.NewQuantity(2, resource.DecimalSI),
						},
					},
				},
			},
		},
	}
}

// @podParentDir kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return {
//    CPU: /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpu.pressure
//    Mem: /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/memory.pressure
//    IO:  /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/io.pressure
//  }
func getPodCgroupCPUAcctPSIPath(podParentDir string) resourceexecutor.PSIPath {
	return resourceexecutor.PSIPath{
		CPU: system.GetCgroupFilePath(podParentDir, system.CPUAcctCPUPressure),
		Mem: system.GetCgroupFilePath(podParentDir, system.CPUAcctMemoryPressure),
		IO:  system.GetCgroupFilePath(podParentDir, system.CPUAcctIOPressure),
	}
}
