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

package podresource

import (
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	gocache "github.com/patrickmn/go-cache"
	"github.com/stretchr/testify/assert"
	"go.uber.org/atomic"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mock_metriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	mock_statesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_collector_collectPodResUsed(t *testing.T) {
	testNow := time.Now()
	testContainerID := "containerd://123abc"
	testPodMetaDir := "kubepods.slice/kubepods-podxxxxxxxx.slice"
	testPodParentDir := "/kubepods.slice/kubepods-podxxxxxxxx.slice"
	testContainerParentDir := "/kubepods.slice/kubepods-podxxxxxxxx.slice/cri-containerd-123abc.scope"
	testPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "test-pod",
			Namespace: "test",
			UID:       "xxxxxxxx",
		},
		Status: corev1.PodStatus{
			ContainerStatuses: []corev1.ContainerStatus{
				{
					Name:        "test-container",
					ContainerID: testContainerID,
					State: corev1.ContainerState{
						Running: &corev1.ContainerStateRunning{},
					},
				},
			},
		},
	}
	type fields struct {
		initPodLastStat       func(lastState *gocache.Cache)
		initContainerLastStat func(lastState *gocache.Cache)
		SetSysUtil            func(helper *system.FileTestUtil)
	}
	type wantFields struct {
		podResourceMetric       bool
		containerResourceMetric bool
	}
	tests := []struct {
		name   string
		fields fields
		want   wantFields
	}{
		{
			name: "cgroups v1",
			fields: fields{
				initPodLastStat: func(lastState *gocache.Cache) {
					lastState.Set(string(testPod.UID), framework.CPUStat{
						CPUUsage:  0,
						Timestamp: testNow.Add(-time.Second),
					}, gocache.DefaultExpiration)
				},
				initContainerLastStat: func(lastState *gocache.Cache) {
					lastState.Set(testContainerID, framework.CPUStat{
						CPUUsage:  0,
						Timestamp: testNow.Add(-time.Second),
					}, gocache.DefaultExpiration)
				},
				SetSysUtil: func(helper *system.FileTestUtil) {
					helper.WriteCgroupFileContents(testPodParentDir, system.CPUAcctUsage, `
1000000000
`)
					helper.WriteCgroupFileContents(testContainerParentDir, system.CPUAcctUsage, `
1000000000
`)
					helper.WriteCgroupFileContents(testPodParentDir, system.MemoryStat, `
total_cache 104857600
total_rss 104857600
total_inactive_anon 104857600
total_active_anon 0
total_inactive_file 104857600
total_active_file 0
total_unevictable 0
`)
					helper.WriteCgroupFileContents(testContainerParentDir, system.MemoryStat, `
total_cache 104857600
total_rss 104857600
total_inactive_anon 104857600
total_active_anon 0
total_inactive_file 104857600
total_active_file 0
total_unevictable 0
`)
				},
			},
			want: wantFields{
				podResourceMetric:       true,
				containerResourceMetric: true,
			},
		},
		{
			name: "cgroups v2",
			fields: fields{
				initPodLastStat: func(lastState *gocache.Cache) {
					lastState.Set(string(testPod.UID), framework.CPUStat{
						CPUUsage:  0,
						Timestamp: testNow.Add(-time.Second),
					}, gocache.DefaultExpiration)
				},
				initContainerLastStat: func(lastState *gocache.Cache) {
					lastState.Set(testContainerID, framework.CPUStat{
						CPUUsage:  0,
						Timestamp: testNow.Add(-time.Second),
					}, gocache.DefaultExpiration)
				},
				SetSysUtil: func(helper *system.FileTestUtil) {
					helper.SetCgroupsV2(true)
					helper.WriteCgroupFileContents(testPodParentDir, system.CPUAcctUsageV2, `
usage_usec 1000000
user_usec 900000
system_usec 100000
nr_periods 0
nr_throttled 0
throttled_usec 0
`)
					helper.WriteCgroupFileContents(testContainerParentDir, system.CPUAcctUsageV2, `
usage_usec 1000000
user_usec 900000
system_usec 100000
nr_periods 0
nr_throttled 0
throttled_usec 0
`)
					helper.WriteCgroupFileContents(testPodParentDir, system.MemoryStatV2, `
file 104857600
anon 104857600
inactive_anon 104857600
active_anon 0
inactive_file 104857600
active_file 0
unevictable 0
`)
					helper.WriteCgroupFileContents(testContainerParentDir, system.MemoryStatV2, `
file 104857600
anon 104857600
inactive_anon 104857600
active_anon 0
inactive_file 104857600
active_file 0
unevictable 0
`)
				},
			},
			want: wantFields{
				podResourceMetric:       true,
				containerResourceMetric: true,
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
			statesInformer.EXPECT().HasSynced().Return(true).AnyTimes()
			statesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{{
				CgroupDir: testPodMetaDir,
				Pod:       testPod,
			}}).Times(1)

			if tt.want.podResourceMetric {
				metricCache.EXPECT().InsertPodResourceMetric(gomock.Any(), gomock.Not(nil)).Times(1)
			}
			if tt.want.containerResourceMetric {
				metricCache.EXPECT().InsertContainerResourceMetric(gomock.Any(), gomock.Not(nil)).Times(1)
			}

			collector := New(&framework.Options{
				Config: &framework.Config{
					CollectResUsedInterval: 1 * time.Second,
				},
				StatesInformer: statesInformer,
				MetricCache:    metricCache,
				CgroupReader:   resourceexecutor.NewCgroupReader(),
			})
			c := collector.(*podResourceCollector)
			tt.fields.initPodLastStat(c.lastPodCPUStat)
			tt.fields.initContainerLastStat(c.lastContainerCPUStat)

			assert.NotPanics(t, func() {
				c.collectPodResUsed()
			})
		})
	}
}

func Test_podResourceCollector_Run(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	metricCache, _ := metriccache.NewMetricCache(metriccache.NewDefaultConfig())
	mockStatesInformer := mock_statesinformer.NewMockStatesInformer(ctrl)
	mockStatesInformer.EXPECT().HasSynced().Return(true).AnyTimes()
	mockStatesInformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{}).AnyTimes()
	c := New(&framework.Options{
		Config:         framework.NewDefaultConfig(),
		StatesInformer: mockStatesInformer,
		MetricCache:    metricCache,
		CgroupReader:   resourceexecutor.NewCgroupReader(),
	})
	collector := c.(*podResourceCollector)
	collector.started = atomic.NewBool(true)
	collector.Setup(&framework.Context{})
	assert.True(t, collector.Enabled())
	assert.True(t, collector.Started())
	assert.NotPanics(t, func() {
		stopCh := make(chan struct{}, 1)
		collector.Run(stopCh)
		stopCh <- struct{}{}
	})
}
