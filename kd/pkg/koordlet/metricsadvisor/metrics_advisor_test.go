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
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mock_metriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	mock_statesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func TestNewCollector(t *testing.T) {
	type args struct {
		cfg            *framework.Config
		statesInformer statesinformer.StatesInformer
		metricCache    metriccache.MetricCache
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "new-collector",
			args: args{
				cfg:            &framework.Config{},
				statesInformer: nil,
				metricCache:    nil,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := NewMetricAdvisor(tt.args.cfg, tt.args.statesInformer, tt.args.metricCache); got == nil {
				t.Errorf("NewMetricAdvisor() = %v", got)
			}
		})
	}
}

func Test_collector(t *testing.T) {
	testPodMetaDir := "/kubepods-podxxx.slice"
	testPodParentDir := "/kubepods.slice/kubepods-podxxx.slice"
	testContainerParentDir := "/kubepods.slice/kubepods-podxxx.slice/cri-containerd-123abc.scope"
	type fields struct {
		SetSysUtil func(helper *system.FileTestUtil)
	}
	tests := []struct {
		name   string
		fields fields
	}{
		{
			name: "cgroups-v1 failed",
		},
		{
			name: "cgroups-v1 succeeded",
			fields: fields{
				SetSysUtil: func(helper *system.FileTestUtil) {
					helper.WriteProcSubFileContents("stat", `
cpu  2 0 2 2 2 0 2 0 0 0
cpu0 1 0 1 1 1 0 1 0 0 0
cpu1 1 0 1 1 1 0 1 0 0 0
`) // omit unused lines
					helper.WriteProcSubFileContents("meminfo", `
MemTotal:        2000000 kB
MemFree:          800000 kB
MemAvailable:    1000000 kB
Buffers:               0 kB
Cached:          1000000 kB
SwapCached:            0 kB
Active(anon):     100000 kB
Inactive(anon):   200000 kB
Active(file):     100000 kB
Inactive(file):   800000 kB
Unevictable:           0 kB
`) // omit unused lines
					helper.WriteCgroupFileContents(testPodParentDir, system.CPUAcctUsage, `
100000000
`)
					helper.WriteCgroupFileContents(testContainerParentDir, system.CPUAcctUsage, `
100000000
`)
					helper.WriteCgroupFileContents(testPodParentDir, system.MemoryStat, `
total_cache 1000000
total_rss 1000000
total_inactive_anon 1000000
total_active_anon 0
total_inactive_file 1000000
total_active_file 0
total_unevictable 0
`)
					helper.WriteCgroupFileContents(testContainerParentDir, system.MemoryStat, `
total_cache 1000000
total_rss 1000000
total_inactive_anon 1000000
total_active_anon 0
total_inactive_file 1000000
total_active_file 0
total_unevictable 0
`)
				},
			},
		},
		{
			name: "cgroups-v2 succeeded",
			fields: fields{
				SetSysUtil: func(helper *system.FileTestUtil) {
					helper.SetCgroupsV2(true)
					helper.WriteProcSubFileContents("stat", `
cpu  2 0 2 2 2 0 2 0 0 0
cpu0 1 0 1 1 1 0 1 0 0 0
cpu1 1 0 1 1 1 0 1 0 0 0
`) // omit unused lines
					helper.WriteProcSubFileContents("meminfo", `
MemTotal:        2000000 kB
MemFree:          800000 kB
MemAvailable:    1000000 kB
Buffers:               0 kB
Cached:          1000000 kB
SwapCached:            0 kB
Active(anon):     100000 kB
Inactive(anon):   200000 kB
Active(file):     100000 kB
Inactive(file):   800000 kB
Unevictable:           0 kB
`) // omit unused lines

					helper.WriteCgroupFileContents(testPodParentDir, system.CPUAcctUsageV2, `
usage_usec 100000
user_usec 90000
system_usec 10000
nr_periods 0
nr_throttled 0
throttled_usec 0
`)
					helper.WriteCgroupFileContents(testContainerParentDir, system.CPUAcctUsageV2, `
usage_usec 100000
user_usec 90000
system_usec 10000
nr_periods 0
nr_throttled 0
throttled_usec 0
`)
					helper.WriteCgroupFileContents(testPodParentDir, system.MemoryStatV2, `
file 1000000
anon 1000000
inactive_anon 1000000
active_anon 0
inactive_file 1000000
active_file 0
unevictable 0
`)
					helper.WriteCgroupFileContents(testContainerParentDir, system.MemoryStat, `
file 1000000
anon 1000000
inactive_anon 1000000
active_anon 0
inactive_file 1000000
active_file 0
unevictable 0
`)
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			helper := system.NewFileTestUtil(t)
			defer helper.Cleanup()
			if tt.fields.SetSysUtil != nil {
				tt.fields.SetSysUtil(helper)
			}

			ctrl := gomock.NewController(t)
			defer ctrl.Finish()

			statesInformer := mock_statesinformer.NewMockStatesInformer(ctrl)
			metricCache := mock_metriccache.NewMockMetricCache(ctrl)

			c := NewMetricAdvisor(&framework.Config{
				CollectResUsedInterval:     1 * time.Second,
				CollectNodeCPUInfoInterval: 1 * time.Second,
			}, statesInformer, metricCache)

			statesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{{
				CgroupDir: testPodMetaDir,
				Pod: &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Name:      "test-pod",
						Namespace: "test",
					},
					Status: corev1.PodStatus{
						ContainerStatuses: []corev1.ContainerStatus{
							{
								Name:        "test-container",
								ContainerID: "containerd://123abc",
								State: corev1.ContainerState{
									Running: &corev1.ContainerStateRunning{},
								},
							},
						},
					},
				}}}).AnyTimes()
			metricCache.EXPECT().InsertNodeResourceMetric(gomock.Any(), gomock.Any()).AnyTimes()
			metricCache.EXPECT().InsertPodResourceMetric(gomock.Any(), gomock.Any()).AnyTimes()
			metricCache.EXPECT().InsertContainerResourceMetric(gomock.Any(), gomock.Any()).AnyTimes()
			metricCache.EXPECT().InsertNodeCPUInfo(gomock.Any()).AnyTimes()
			statesInformer.EXPECT().HasSynced().Return(true).AnyTimes()

			stopCh := make(chan struct{}, 1)
			close(stopCh)
			assert.NotPanics(t, func() {
				c.Run(stopCh)
			})
		})
	}
}
