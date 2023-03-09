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

package metricsquery

import (
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/common/testutil"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	mock_statesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
)

func Test_collectNodeMetricsAvg(t *testing.T) {

	type nodeMetric struct {
		timestamp   time.Time
		nodeResUsed *metriccache.NodeResourceMetric
	}
	type args struct {
		name             string
		nodeMetrics      []*nodeMetric
		windowSize       int64
		expectNodeMetric *metriccache.NodeResourceMetric
	}

	tests := []args{
		{
			name:             "test no metrics in db",
			windowSize:       4,
			expectNodeMetric: nil,
		},
		{
			name: "test windowSize < dataInterval",
			nodeMetrics: []*nodeMetric{
				{
					timestamp: time.Now().Add(-3 * time.Second),
					nodeResUsed: &metriccache.NodeResourceMetric{
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("14")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("60G")},
					},
				},
				{
					timestamp: time.Now().Add(-1 * time.Second),
					nodeResUsed: &metriccache.NodeResourceMetric{
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("16")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("70G")},
					},
				},
			},
			windowSize:       1,
			expectNodeMetric: nil,
		},
		{
			name: "test windowSize > dataInterval",
			nodeMetrics: []*nodeMetric{
				{
					timestamp: time.Now().Add(-7 * time.Second),
					nodeResUsed: &metriccache.NodeResourceMetric{
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("10")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("40G")},
					},
				},
				{
					timestamp: time.Now().Add(-5 * time.Second),
					nodeResUsed: &metriccache.NodeResourceMetric{
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("12")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("50G")},
					},
				},
				{
					timestamp: time.Now().Add(-3 * time.Second),
					nodeResUsed: &metriccache.NodeResourceMetric{
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("14")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("60G")},
					},
				},
				{
					timestamp: time.Now().Add(-1 * time.Second),
					nodeResUsed: &metriccache.NodeResourceMetric{
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("16")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("70G")},
					},
				},
			},
			windowSize: 4,
			expectNodeMetric: &metriccache.NodeResourceMetric{
				CPUUsed:    metriccache.CPUMetric{CPUUsed: *resource.NewMilliQuantity(15000, resource.DecimalSI)},
				MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: *resource.NewQuantity(65000000000, resource.BinarySI)},
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			metricCache, _ := metriccache.NewCacheNotShareMetricCache(&metriccache.Config{MetricGCIntervalSeconds: 1, MetricExpireSeconds: 1})
			for _, nodeMetric := range tt.nodeMetrics {
				_ = metricCache.InsertNodeResourceMetric(nodeMetric.timestamp, nodeMetric.nodeResUsed)
			}

			metricsQuery := NewMetricsQuery(metricCache, nil)
			queryResult := metricsQuery.CollectNodeMetricsAvg(tt.windowSize)
			gotNodeMetric := queryResult.Metric
			assert.Equal(t, tt.expectNodeMetric, gotNodeMetric)
		})
	}
}

func Test_collectNodeAndPodMetricLast(t *testing.T) {

	type metricInfos struct {
		timestamp   time.Time
		nodeResUsed *metriccache.NodeResourceMetric
		podResUsed  *metriccache.PodResourceMetric
	}
	type args struct {
		name             string
		metricInfos      []*metricInfos
		pod              *statesinformer.PodMeta
		expectNodeMetric *metriccache.NodeResourceMetric
		expectPodMetric  *metriccache.PodResourceMetric
	}

	tests := []args{
		{
			name:             "test no metrics in db",
			pod:              &statesinformer.PodMeta{Pod: testutil.MockTestPod(extension.QoSLSR, "test_pod")},
			expectNodeMetric: nil,
			expectPodMetric:  nil,
		},
		{
			name: "test normal",
			pod:  &statesinformer.PodMeta{Pod: testutil.MockTestPod(extension.QoSLSR, "test_pod")},
			metricInfos: []*metricInfos{
				{
					timestamp: time.Now().Add(-3 * time.Second),
					nodeResUsed: &metriccache.NodeResourceMetric{
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("14")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("60G")},
					},
					podResUsed: &metriccache.PodResourceMetric{
						PodUID:     "test_pod",
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("14")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("60G")},
					},
				},
				{
					timestamp: time.Now().Add(-1 * time.Second),
					nodeResUsed: &metriccache.NodeResourceMetric{
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("16")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("70G")},
					},
					podResUsed: &metriccache.PodResourceMetric{
						PodUID:     "test_pod",
						CPUUsed:    metriccache.CPUMetric{CPUUsed: resource.MustParse("16")},
						MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: resource.MustParse("70G")},
					},
				},
			},
			expectNodeMetric: &metriccache.NodeResourceMetric{
				CPUUsed:    metriccache.CPUMetric{CPUUsed: *resource.NewMilliQuantity(16000, resource.DecimalSI)},
				MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: *resource.NewQuantity(70000000000, resource.BinarySI)},
			},
			expectPodMetric: &metriccache.PodResourceMetric{
				PodUID:     "test_pod",
				CPUUsed:    metriccache.CPUMetric{CPUUsed: *resource.NewMilliQuantity(16000, resource.DecimalSI)},
				MemoryUsed: metriccache.MemoryMetric{MemoryWithoutCache: *resource.NewQuantity(70000000000, resource.BinarySI)},
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			metricCache, _ := metriccache.NewCacheNotShareMetricCache(&metriccache.Config{MetricGCIntervalSeconds: 60, MetricExpireSeconds: 60})
			for _, metricInfo := range tt.metricInfos {
				_ = metricCache.InsertNodeResourceMetric(metricInfo.timestamp, metricInfo.nodeResUsed)
				_ = metricCache.InsertPodResourceMetric(metricInfo.timestamp, metricInfo.podResUsed)
			}

			ctl := gomock.NewController(t)
			defer ctl.Finish()
			mockstatesinformer := mock_statesinformer.NewMockStatesInformer(ctl)
			mockstatesinformer.EXPECT().GetAllPods().Return([]*statesinformer.PodMeta{tt.pod}).AnyTimes()

			metricsQuery := NewMetricsQuery(metricCache, mockstatesinformer)
			gotNodeMetric, gotPodMetrics := metricsQuery.CollectNodeAndPodMetricLast(60)
			assert.Equal(t, tt.expectNodeMetric, gotNodeMetric)
			if tt.expectPodMetric == nil {
				assert.True(t, len(gotPodMetrics) == 0)
			} else {
				assert.Equal(t, tt.expectPodMetric, gotPodMetrics[0])
			}
		})
	}
}
