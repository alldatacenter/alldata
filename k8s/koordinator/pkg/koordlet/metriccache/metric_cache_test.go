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

package metriccache

import (
	"reflect"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/api/resource"

	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
)

func Test_metricCache_NodeResourceMetric_CRUD(t *testing.T) {
	now := time.Now()
	firstTime := now.Add(-time.Second * 120)
	midTime := now.Add(-time.Second * 10)
	lastTime := now.Add(-time.Second * 5)
	type args struct {
		config  *Config
		samples map[time.Time]NodeResourceMetric
	}

	tests := []struct {
		name            string
		args            args
		want            NodeResourceQueryResult
		wantAfterDelete NodeResourceQueryResult
	}{
		{
			name: "node-resource-metric-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				samples: map[time.Time]NodeResourceMetric{
					firstTime: {
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(30, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      80,
								MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      40,
								MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
					midTime: {
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(10, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      60,
								MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      50,
								MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
					lastTime: {
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      70,
								MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      60,
								MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
				},
			},
			want: NodeResourceQueryResult{
				Metric: &NodeResourceMetric{
					CPUUsed: CPUMetric{
						CPUUsed: *resource.NewMilliQuantity(1500, resource.DecimalSI),
					},
					MemoryUsed: MemoryMetric{
						MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
					},
					GPUs: []GPUMetric{
						{
							DeviceUUID:  "1",
							Minor:       0,
							SMUtil:      70,
							MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
						},
						{
							DeviceUUID:  "2",
							Minor:       1,
							SMUtil:      50,
							MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
						},
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{
					MetricStart:  &firstTime,
					MetricEnd:    &lastTime,
					MetricsCount: 3,
				}},
			},
			wantAfterDelete: NodeResourceQueryResult{
				Metric: &NodeResourceMetric{
					CPUUsed: CPUMetric{
						CPUUsed: *resource.NewMilliQuantity(1250, resource.DecimalSI),
					},
					MemoryUsed: MemoryMetric{
						MemoryWithoutCache: *resource.NewQuantity(15, resource.BinarySI),
					},
					GPUs: []GPUMetric{
						{
							DeviceUUID:  "1",
							Minor:       0,
							SMUtil:      65,
							MemoryUsed:  *resource.NewQuantity(45, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
						},
						{
							DeviceUUID:  "2",
							Minor:       1,
							SMUtil:      55,
							MemoryUsed:  *resource.NewQuantity(35, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
						},
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{
					MetricStart:  &midTime,
					MetricEnd:    &lastTime,
					MetricsCount: 2,
				}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertNodeResourceMetric(ts, &sample)
				if err != nil {
					t.Errorf("insert node metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: AggregationTypeAVG,
				Start:     &oldStartTime,
				End:       &now,
			}
			got := m.GetNodeResourceMetric(params)
			if got.Error != nil {
				t.Errorf("get node metric failed %v", got.Error)
			}

			assert.Equal(t, tt.want.Metric, got.Metric, "GetNodeResourceMetric() Metric")
			assert.True(t, tt.want.AggregateInfo.MetricStart.Equal(*got.QueryResult.AggregateInfo.MetricStart), "GetNodeResourceMetric() StartTime")
			assert.True(t, tt.want.AggregateInfo.MetricEnd.Equal(*got.QueryResult.AggregateInfo.MetricEnd), "GetNodeResourceMetric() EndTime")
			assert.Equal(t, tt.want.AggregateInfo.MetricsCount, got.QueryResult.AggregateInfo.MetricsCount, "GetNodeResourceMetric() Count")
			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetNodeResourceMetric(params)
			if gotAfterDel.Error != nil {
				t.Errorf("get node metric failed %v", gotAfterDel.Error)
			}
			assert.Equal(t, tt.wantAfterDelete.Metric, gotAfterDel.Metric, "GetNodeResourceMetric() Metric after delete")
			assert.True(t, tt.wantAfterDelete.AggregateInfo.MetricStart.Equal(*gotAfterDel.QueryResult.AggregateInfo.MetricStart), "GetNodeResourceMetric() StartTime")
			assert.True(t, tt.wantAfterDelete.AggregateInfo.MetricEnd.Equal(*gotAfterDel.QueryResult.AggregateInfo.MetricEnd), "GetNodeResourceMetric() EndTime")
			assert.Equal(t, tt.wantAfterDelete.AggregateInfo.MetricsCount, gotAfterDel.QueryResult.AggregateInfo.MetricsCount, "GetNodeResourceMetric() Count")
		})
	}
}

func Test_metricCache_PodResourceMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		config  *Config
		podUID  string
		samples map[time.Time]PodResourceMetric
	}

	tests := []struct {
		name            string
		args            args
		want            PodResourceQueryResult
		wantAfterDelete PodResourceQueryResult
	}{
		{
			name: "pod-resource-metric-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				podUID: "pod-uid-1",
				samples: map[time.Time]PodResourceMetric{
					now.Add(-time.Second * 120): {
						PodUID: "pod-uid-1",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(30, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      80,
								MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      40,
								MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
					now.Add(-time.Second * 10): {
						PodUID: "pod-uid-1",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(10, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      60,
								MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      50,
								MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
					now.Add(-time.Second * 5): {
						PodUID: "pod-uid-1",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      70,
								MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      60,
								MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
					now.Add(-time.Second * 4): {
						PodUID: "pod-uid-2",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
						},
					},
				},
			},
			want: PodResourceQueryResult{
				Metric: &PodResourceMetric{
					PodUID: "pod-uid-1",
					CPUUsed: CPUMetric{
						CPUUsed: *resource.NewMilliQuantity(1500, resource.DecimalSI),
					},
					MemoryUsed: MemoryMetric{
						MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
					},
					GPUs: []GPUMetric{
						{
							DeviceUUID:  "1",
							Minor:       0,
							SMUtil:      70,
							MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
						},
						{
							DeviceUUID:  "2",
							Minor:       1,
							SMUtil:      50,
							MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
						},
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},

			wantAfterDelete: PodResourceQueryResult{
				Metric: &PodResourceMetric{
					PodUID: "pod-uid-1",
					CPUUsed: CPUMetric{
						CPUUsed: *resource.NewMilliQuantity(1250, resource.DecimalSI),
					},
					MemoryUsed: MemoryMetric{
						MemoryWithoutCache: *resource.NewQuantity(15, resource.BinarySI),
					},
					GPUs: []GPUMetric{
						{
							DeviceUUID:  "1",
							Minor:       0,
							SMUtil:      65,
							MemoryUsed:  *resource.NewQuantity(45, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
						},
						{
							DeviceUUID:  "2",
							Minor:       1,
							SMUtil:      55,
							MemoryUsed:  *resource.NewQuantity(35, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
						},
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertPodResourceMetric(ts, &sample)
				if err != nil {
					t.Errorf("insert pod metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: AggregationTypeAVG,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetPodResourceMetric(&tt.args.podUID, params)
			if got.Error != nil {
				t.Errorf("get pod metric failed %v", got.Error)
			}

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetPodResourceMetric() got = %v, want %v", got, tt.want)
			}
			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetPodResourceMetric(&tt.args.podUID, params)
			if gotAfterDel.Error != nil {
				t.Errorf("get pod metric failed %v", gotAfterDel.Error)
			}
			if !reflect.DeepEqual(gotAfterDel, tt.wantAfterDelete) {
				t.Errorf("GetNodeResourceMetric() after delete, got = %v, want %v", gotAfterDel, tt.wantAfterDelete)
			}
		})
	}
}

func Test_metricCache_ContainerResourceMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		containerID  string
		aggregateArg AggregationType
		samples      map[time.Time]ContainerResourceMetric
	}

	tests := []struct {
		name            string
		args            args
		want            ContainerResourceQueryResult
		wantAfterDelete ContainerResourceQueryResult
	}{
		{
			name: "container-resource-metric-avg-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				containerID:  "container-id-1",
				aggregateArg: AggregationTypeAVG,
				samples: map[time.Time]ContainerResourceMetric{
					now.Add(-time.Second * 120): {
						ContainerID: "container-id-1",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(30, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      80,
								MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      40,
								MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
					now.Add(-time.Second * 10): {
						ContainerID: "container-id-1",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(10, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      60,
								MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      50,
								MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
					now.Add(-time.Second * 5): {
						ContainerID: "container-id-1",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
						},
						GPUs: []GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      70,
								MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      60,
								MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
							},
						},
					},
					now.Add(-time.Second * 4): {
						ContainerID: "container-id-2",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
						},
					},
				},
			},
			want: ContainerResourceQueryResult{
				Metric: &ContainerResourceMetric{
					ContainerID: "container-id-1",
					CPUUsed: CPUMetric{
						CPUUsed: *resource.NewMilliQuantity(1500, resource.DecimalSI),
					},
					MemoryUsed: MemoryMetric{
						MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
					},
					GPUs: []GPUMetric{
						{
							DeviceUUID:  "1",
							Minor:       0,
							SMUtil:      70,
							MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
						},
						{
							DeviceUUID:  "2",
							Minor:       1,
							SMUtil:      50,
							MemoryUsed:  *resource.NewQuantity(40, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
						},
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: ContainerResourceQueryResult{
				Metric: &ContainerResourceMetric{
					ContainerID: "container-id-1",
					CPUUsed: CPUMetric{
						CPUUsed: *resource.NewMilliQuantity(1250, resource.DecimalSI),
					},
					MemoryUsed: MemoryMetric{
						MemoryWithoutCache: *resource.NewQuantity(15, resource.BinarySI),
					},
					GPUs: []GPUMetric{
						{
							DeviceUUID:  "1",
							Minor:       0,
							SMUtil:      65,
							MemoryUsed:  *resource.NewQuantity(45, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
						},
						{
							DeviceUUID:  "2",
							Minor:       1,
							SMUtil:      55,
							MemoryUsed:  *resource.NewQuantity(35, resource.BinarySI),
							MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
						},
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
		{
			name: "container-resource-metric-latest-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				containerID:  "container-id-3",
				aggregateArg: AggregationTypeLast,
				samples: map[time.Time]ContainerResourceMetric{
					now.Add(-time.Second * 120): {
						ContainerID: "container-id-3",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(30, resource.BinarySI),
						},
					},
					now.Add(-time.Second * 10): {
						ContainerID: "container-id-3",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(10, resource.BinarySI),
						},
					},
					now.Add(-time.Second * 5): {
						ContainerID: "container-id-3",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
						},
					},
					now.Add(-time.Second * 4): {
						ContainerID: "container-id-4",
						CPUUsed: CPUMetric{
							CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
						},
						MemoryUsed: MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
						},
					},
				},
			},
			want: ContainerResourceQueryResult{
				Metric: &ContainerResourceMetric{
					ContainerID: "container-id-3",
					CPUUsed: CPUMetric{
						CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
					},
					MemoryUsed: MemoryMetric{
						MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: ContainerResourceQueryResult{
				Metric: &ContainerResourceMetric{
					ContainerID: "container-id-3",
					CPUUsed: CPUMetric{
						CPUUsed: *resource.NewMilliQuantity(500, resource.DecimalSI),
					},
					MemoryUsed: MemoryMetric{
						MemoryWithoutCache: *resource.NewQuantity(20, resource.BinarySI),
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertContainerResourceMetric(ts, &sample)
				if err != nil {
					t.Errorf("insert container metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetContainerResourceMetric(&tt.args.containerID, params)
			if got.Error != nil {
				t.Errorf("get container metric failed %v", got.Error)
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetContainerResourceMetric() got = %v, want %v", got, tt.want)
			}
			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetContainerResourceMetric(&tt.args.containerID, params)
			if gotAfterDel.Error != nil {
				t.Errorf("get container metric failed %v", gotAfterDel.Error)
			}
			if !reflect.DeepEqual(gotAfterDel, tt.wantAfterDelete) {
				t.Errorf("GetContainerResourceMetric() after delete, got = %v, want %v",
					gotAfterDel, tt.wantAfterDelete)
			}
		})
	}
}

func Test_metricCache_BECPUResourceMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		config  *Config
		samples map[time.Time]BECPUResourceMetric
	}
	tests := []struct {
		name            string
		args            args
		want            BECPUResourceQueryResult
		wantAfterDelete BECPUResourceQueryResult
	}{
		{
			name: "node-resource-metric-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				samples: map[time.Time]BECPUResourceMetric{
					now.Add(-time.Second * 120): {
						CPUUsed:      *resource.NewQuantity(2, resource.DecimalSI),
						CPURequest:   *resource.NewQuantity(4, resource.DecimalSI),
						CPURealLimit: *resource.NewQuantity(2, resource.DecimalSI),
					},
					now.Add(-time.Second * 10): {
						CPUUsed:      *resource.NewQuantity(2, resource.DecimalSI),
						CPURequest:   *resource.NewQuantity(4, resource.DecimalSI),
						CPURealLimit: *resource.NewQuantity(2, resource.DecimalSI),
					},
					now.Add(-time.Second * 5): {
						CPUUsed:      *resource.NewMilliQuantity(500, resource.DecimalSI),
						CPURequest:   *resource.NewMilliQuantity(1000, resource.DecimalSI),
						CPURealLimit: *resource.NewMilliQuantity(500, resource.DecimalSI),
					},
				},
			},
			want: BECPUResourceQueryResult{
				Metric: &BECPUResourceMetric{
					CPUUsed:      *resource.NewMilliQuantity(1500, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(3000, resource.DecimalSI),
					CPURealLimit: *resource.NewMilliQuantity(1500, resource.DecimalSI),
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: BECPUResourceQueryResult{
				Metric: &BECPUResourceMetric{
					CPUUsed:      *resource.NewMilliQuantity(1250, resource.DecimalSI),
					CPURequest:   *resource.NewMilliQuantity(2500, resource.DecimalSI),
					CPURealLimit: *resource.NewMilliQuantity(1250, resource.DecimalSI),
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertBECPUResourceMetric(ts, &sample)
				if err != nil {
					t.Errorf("InsertBECPUResourceMetric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: AggregationTypeAVG,
				Start:     &oldStartTime,
				End:       &now,
			}
			got := m.GetBECPUResourceMetric(params)
			if got.Error != nil {
				t.Errorf("GetBECPUResourceMetric failed %v", got.Error)
			}

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetBECPUResourceMetric() got = %+v, want %+v", got, tt.want)
			}

			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetBECPUResourceMetric(params)
			if gotAfterDel.Error != nil {
				t.Errorf("GetBECPUResourceMetric failed %v", gotAfterDel.Error)
			}

			if !reflect.DeepEqual(gotAfterDel, tt.wantAfterDelete) {
				t.Errorf("GetBECPUResourceMetric() after delete, got = %+v, want %+v", gotAfterDel, tt.wantAfterDelete)
			}
		})
	}
}

func Test_metricCache_NodeCPUInfo_CRUD(t *testing.T) {
	type args struct {
		config  *Config
		samples []NodeCPUInfo
	}
	tests := []struct {
		name  string
		args  args
		want  *NodeCPUInfo
		want1 *NodeCPUInfo
	}{
		{
			name: "node-cpu-info-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				samples: []NodeCPUInfo{
					{
						ProcessorInfos: []koordletutil.ProcessorInfo{
							{
								CPUID:    0,
								CoreID:   0,
								SocketID: 0,
								NodeID:   0,
							},
							{
								CPUID:    1,
								CoreID:   1,
								SocketID: 0,
								NodeID:   0,
							},
						},
					},
					{
						ProcessorInfos: []koordletutil.ProcessorInfo{
							// HT on
							{
								CPUID:    0,
								CoreID:   0,
								SocketID: 0,
								NodeID:   0,
							},
							{
								CPUID:    1,
								CoreID:   1,
								SocketID: 0,
								NodeID:   0,
							},
							{
								CPUID:    2,
								CoreID:   0,
								SocketID: 0,
								NodeID:   0,
							},
							{
								CPUID:    3,
								CoreID:   1,
								SocketID: 0,
								NodeID:   0,
							},
						},
					},
				},
			},
			want: &NodeCPUInfo{
				ProcessorInfos: []koordletutil.ProcessorInfo{
					{
						CPUID:    0,
						CoreID:   0,
						SocketID: 0,
						NodeID:   0,
					},
					{
						CPUID:    1,
						CoreID:   1,
						SocketID: 0,
						NodeID:   0,
					},
					{
						CPUID:    2,
						CoreID:   0,
						SocketID: 0,
						NodeID:   0,
					},
					{
						CPUID:    3,
						CoreID:   1,
						SocketID: 0,
						NodeID:   0,
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for _, sample := range tt.args.samples {
				err := m.InsertNodeCPUInfo(&sample)
				if err != nil {
					t.Errorf("insert node cpu info failed %v", err)
				}
			}

			params := &QueryParam{}
			got, err := m.GetNodeCPUInfo(params)
			if err != nil {
				t.Errorf("get node cpu info failed %v", err)
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetNodeCPUInfo() got = %v, want %v", got, tt.want)
			}

			// delete expire items, should not change nodeCPUInfo record
			m.recycleDB()

			gotAfterDel, err := m.GetNodeCPUInfo(params)
			if err != nil {
				t.Errorf("get node cpu info failed %v", err)
			}
			if !reflect.DeepEqual(gotAfterDel, tt.want) {
				t.Errorf("GetNodeCPUInfo() got = %v, want %v", gotAfterDel, tt.want)
			}
		})
	}
}

func Test_metricCache_ContainerThrottledMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		containerID  string
		aggregateArg AggregationType
		samples      map[time.Time]ContainerThrottledMetric
	}

	tests := []struct {
		name            string
		args            args
		want            ContainerThrottledQueryResult
		wantAfterDelete ContainerThrottledQueryResult
	}{
		{
			name: "container-throttled-metric-latest-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				containerID:  "container-id-1",
				aggregateArg: AggregationTypeLast,
				samples: map[time.Time]ContainerThrottledMetric{
					now.Add(-time.Second * 120): {
						ContainerID: "container-id-1",
						CPUThrottledMetric: &CPUThrottledMetric{
							ThrottledRatio: 0.7,
						},
					},
					now.Add(-time.Second * 10): {
						ContainerID: "container-id-1",
						CPUThrottledMetric: &CPUThrottledMetric{
							ThrottledRatio: 0.6,
						},
					},
					now.Add(-time.Second * 5): {
						ContainerID: "container-id-1",
						CPUThrottledMetric: &CPUThrottledMetric{
							ThrottledRatio: 0.5,
						},
					},
					now.Add(-time.Second * 4): {
						ContainerID: "container-id-2",
						CPUThrottledMetric: &CPUThrottledMetric{
							ThrottledRatio: 0.4,
						},
					},
				},
			},
			want: ContainerThrottledQueryResult{
				Metric: &ContainerThrottledMetric{
					ContainerID: "container-id-1",
					CPUThrottledMetric: &CPUThrottledMetric{
						ThrottledRatio: 0.5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: ContainerThrottledQueryResult{
				Metric: &ContainerThrottledMetric{
					ContainerID: "container-id-1",
					CPUThrottledMetric: &CPUThrottledMetric{
						ThrottledRatio: 0.5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertContainerThrottledMetrics(ts, &sample)
				if err != nil {
					t.Errorf("insert container metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetContainerThrottledMetric(&tt.args.containerID, params)
			if got.Error != nil {
				t.Errorf("get container metric failed %v", got.Error)
			}

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetContainerThrottledMetric() got = %v, want %v", got, tt.want)
			}
			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetContainerThrottledMetric(&tt.args.containerID, params)
			if gotAfterDel.Error != nil {
				t.Errorf("get container metric failed %v", gotAfterDel.Error)
			}
			if !reflect.DeepEqual(gotAfterDel, tt.wantAfterDelete) {
				t.Errorf("GetContainerThrottledMetric() after delete, got = %v, want %v",
					gotAfterDel, tt.wantAfterDelete)
			}
		})
	}
}

func Test_metricCache_PodThrottledMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		podUID       string
		aggregateArg AggregationType
		samples      map[time.Time]PodThrottledMetric
	}

	tests := []struct {
		name            string
		args            args
		want            PodThrottledQueryResult
		wantAfterDelete PodThrottledQueryResult
	}{
		{
			name: "pod-throttled-metric-latest-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				podUID:       "pod-uid-1",
				aggregateArg: AggregationTypeLast,
				samples: map[time.Time]PodThrottledMetric{
					now.Add(-time.Second * 120): {
						PodUID: "pod-uid-1",
						CPUThrottledMetric: &CPUThrottledMetric{
							ThrottledRatio: 0.7,
						},
					},
					now.Add(-time.Second * 10): {
						PodUID: "pod-uid-1",
						CPUThrottledMetric: &CPUThrottledMetric{
							ThrottledRatio: 0.6,
						},
					},
					now.Add(-time.Second * 5): {
						PodUID: "pod-uid-1",
						CPUThrottledMetric: &CPUThrottledMetric{
							ThrottledRatio: 0.5,
						},
					},
					now.Add(-time.Second * 4): {
						PodUID: "pod-uid-2",
						CPUThrottledMetric: &CPUThrottledMetric{
							ThrottledRatio: 0.4,
						},
					},
				},
			},
			want: PodThrottledQueryResult{
				Metric: &PodThrottledMetric{
					PodUID: "pod-uid-1",
					CPUThrottledMetric: &CPUThrottledMetric{
						ThrottledRatio: 0.5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: PodThrottledQueryResult{
				Metric: &PodThrottledMetric{
					PodUID: "pod-uid-1",
					CPUThrottledMetric: &CPUThrottledMetric{
						ThrottledRatio: 0.5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertPodThrottledMetrics(ts, &sample)
				if err != nil {
					t.Errorf("insert pod metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetPodThrottledMetric(&tt.args.podUID, params)
			if got.Error != nil {
				t.Errorf("get pod metric failed %v", got.Error)
			}

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetPodThrottledMetric() got = %v, want %v", got, tt.want)
			}
			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetPodThrottledMetric(&tt.args.podUID, params)
			if gotAfterDel.Error != nil {
				t.Errorf("get pod metric failed %v", gotAfterDel.Error)
			}
			if !reflect.DeepEqual(gotAfterDel, tt.wantAfterDelete) {
				t.Errorf("GetPodThrottledMetric() after delete, got = %v, want %v",
					gotAfterDel, tt.wantAfterDelete)
			}
		})
	}
}

func Test_metricCache_aggregateGPUUsages(t *testing.T) {
	type fields struct {
		config *Config
	}
	type args struct {
		gpuResourceMetrics [][]gpuResourceMetric
		aggregateFunc      AggregationFunc
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    []GPUMetric
		wantErr bool
	}{
		{
			name: "sample device",
			fields: fields{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
			},
			args: args{
				aggregateFunc: getAggregateFunc(AggregationTypeAVG),
				gpuResourceMetrics: [][]gpuResourceMetric{
					{
						{DeviceUUID: "1-1", Minor: 0, SMUtil: 20, MemoryUsed: 1000, MemoryTotal: 10000},
						{DeviceUUID: "2-1", Minor: 1, SMUtil: 40, MemoryUsed: 2000, MemoryTotal: 20000},
					},
					{
						{DeviceUUID: "1-1", Minor: 0, SMUtil: 40, MemoryUsed: 4000, MemoryTotal: 10000},
						{DeviceUUID: "2-1", Minor: 1, SMUtil: 30, MemoryUsed: 1000, MemoryTotal: 20000},
					},
				},
			},
			want: []GPUMetric{
				{
					DeviceUUID:  "1-1",
					Minor:       0,
					SMUtil:      30,
					MemoryUsed:  *resource.NewQuantity(2500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
				},
				{
					DeviceUUID:  "2-1",
					Minor:       1,
					SMUtil:      35,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(20000, resource.BinarySI),
				},
			},
		},

		{
			name: "difference device",
			fields: fields{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
			},
			args: args{
				aggregateFunc: getAggregateFunc(AggregationTypeAVG),
				gpuResourceMetrics: [][]gpuResourceMetric{
					{
						{DeviceUUID: "1-1", Minor: 0, SMUtil: 20, MemoryUsed: 1000, MemoryTotal: 8000},
						{DeviceUUID: "2-1", Minor: 1, SMUtil: 40, MemoryUsed: 4000, MemoryTotal: 10000},
					},
					{
						{DeviceUUID: "3-1", Minor: 2, SMUtil: 40, MemoryUsed: 4000, MemoryTotal: 12000},
						{DeviceUUID: "4-1", Minor: 3, SMUtil: 30, MemoryUsed: 1000, MemoryTotal: 14000},
					},
				},
			},
			want: []GPUMetric{
				{
					DeviceUUID:  "3-1",
					Minor:       2,
					SMUtil:      30,
					MemoryUsed:  *resource.NewQuantity(2500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(12000, resource.BinarySI),
				},
				{
					DeviceUUID:  "4-1",
					Minor:       3,
					SMUtil:      35,
					MemoryUsed:  *resource.NewQuantity(2500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
			},
		},
		{
			name: "single device",
			fields: fields{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
			},
			args: args{
				aggregateFunc: getAggregateFunc(AggregationTypeAVG),
				gpuResourceMetrics: [][]gpuResourceMetric{
					{
						{DeviceUUID: "2-1", Minor: 1, SMUtil: 40, MemoryUsed: 2000, MemoryTotal: 10000},
					},
					{
						{DeviceUUID: "2-1", Minor: 1, SMUtil: 30, MemoryUsed: 1000, MemoryTotal: 12000},
					},
				},
			},
			want: []GPUMetric{
				{
					DeviceUUID:  "2-1",
					Minor:       1,
					SMUtil:      35,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(12000, resource.BinarySI),
				},
			},
		},
		{
			name: "single device and multiple device",
			fields: fields{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
			},
			args: args{
				aggregateFunc: getAggregateFunc(AggregationTypeAVG),
				gpuResourceMetrics: [][]gpuResourceMetric{
					{
						{DeviceUUID: "1-1", Minor: 0, SMUtil: 20, MemoryUsed: 1000, MemoryTotal: 10000},
						{DeviceUUID: "3-1", Minor: 3, SMUtil: 40, MemoryUsed: 2000, MemoryTotal: 20000},
					},
					{
						{DeviceUUID: "1-1", Minor: 0, SMUtil: 40, MemoryUsed: 1000, MemoryTotal: 10000},
						{DeviceUUID: "3-1", Minor: 3, SMUtil: 30, MemoryUsed: 1000, MemoryTotal: 20000},
					},
				},
			},
			want: []GPUMetric{
				{
					DeviceUUID:  "1-1",
					Minor:       0,
					SMUtil:      30,
					MemoryUsed:  *resource.NewQuantity(1000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
				},
				{
					DeviceUUID:  "3-1",
					Minor:       3,
					SMUtil:      35,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(20000, resource.BinarySI),
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			m := &metricCache{
				config: tt.fields.config,
				db:     s,
			}
			got, err := m.aggregateGPUUsages(tt.args.gpuResourceMetrics, tt.args.aggregateFunc)
			if (err != nil) != tt.wantErr {
				t.Errorf("metricCache.aggregateGPUUsages() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("metricCache.aggregateGPUUsages() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_metricCache_ContainerInterferenceMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		metricName   InterferenceMetricName
		containerID  string
		podUID       string
		aggregateArg AggregationType
		samples      map[time.Time]ContainerInterferenceMetric
	}

	tests := []struct {
		name            string
		args            args
		want            ContainerInterferenceQueryResult
		wantAfterDelete ContainerInterferenceQueryResult
	}{
		// test container CPI CRUD
		{
			name: "container-cpi-latest-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   MetricNameContainerCPI,
				containerID:  "container-uid-1",
				podUID:       "pod-uid-1",
				aggregateArg: AggregationTypeLast,
				samples: map[time.Time]ContainerInterferenceMetric{
					now.Add(-time.Second * 120): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-1",
						PodUID:      "pod-uid-1",
						MetricValue: &CPIMetric{
							Cycles:       7,
							Instructions: 7,
						},
					},
					now.Add(-time.Second * 10): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-1",
						PodUID:      "pod-uid-1",
						MetricValue: &CPIMetric{
							Cycles:       6,
							Instructions: 6,
						},
					},
					now.Add(-time.Second * 5): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-1",
						PodUID:      "pod-uid-1",
						MetricValue: &CPIMetric{
							Cycles:       5,
							Instructions: 5,
						},
					},
					now.Add(-time.Second * 4): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-2",
						PodUID:      "pod-uid-2",
						MetricValue: &CPIMetric{
							Cycles:       4,
							Instructions: 4,
						},
					},
				},
			},
			want: ContainerInterferenceQueryResult{
				Metric: &ContainerInterferenceMetric{
					MetricName:  MetricNameContainerCPI,
					ContainerID: "container-uid-1",
					PodUID:      "pod-uid-1",
					MetricValue: &CPIMetric{
						Cycles:       5,
						Instructions: 5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: ContainerInterferenceQueryResult{
				Metric: &ContainerInterferenceMetric{
					MetricName:  MetricNameContainerCPI,
					ContainerID: "container-uid-1",
					PodUID:      "pod-uid-1",
					MetricValue: &CPIMetric{
						Cycles:       5,
						Instructions: 5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
		{
			name: "container-cpi-avg-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   MetricNameContainerCPI,
				containerID:  "container-uid-3",
				podUID:       "pod-uid-3",
				aggregateArg: AggregationTypeAVG,
				samples: map[time.Time]ContainerInterferenceMetric{
					now.Add(-time.Second * 120): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-3",
						PodUID:      "pod-uid-3",
						MetricValue: &CPIMetric{
							Cycles:       7,
							Instructions: 7,
						},
					},
					now.Add(-time.Second * 10): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-3",
						PodUID:      "pod-uid-3",
						MetricValue: &CPIMetric{
							Cycles:       6,
							Instructions: 6,
						},
					},
					now.Add(-time.Second * 5): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-3",
						PodUID:      "pod-uid-3",
						MetricValue: &CPIMetric{
							Cycles:       5,
							Instructions: 5,
						},
					},
					now.Add(-time.Second * 4): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-4",
						PodUID:      "pod-uid-4",
						MetricValue: &CPIMetric{
							Cycles:       4,
							Instructions: 4,
						},
					},
				},
			},
			want: ContainerInterferenceQueryResult{
				Metric: &ContainerInterferenceMetric{
					MetricName:  MetricNameContainerCPI,
					ContainerID: "container-uid-3",
					PodUID:      "pod-uid-3",
					MetricValue: &CPIMetric{
						Cycles:       6,
						Instructions: 6,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: ContainerInterferenceQueryResult{
				Metric: &ContainerInterferenceMetric{
					MetricName:  MetricNameContainerCPI,
					ContainerID: "container-uid-3",
					PodUID:      "pod-uid-3",
					MetricValue: &CPIMetric{
						Cycles:       5,
						Instructions: 5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
		// test container PSI CRUD
		{
			name: "container-psi-latest-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   MetricNameContainerPSI,
				containerID:  "container-uid-1",
				podUID:       "pod-uid-1",
				aggregateArg: AggregationTypeLast,
				samples: map[time.Time]ContainerInterferenceMetric{
					now.Add(-time.Second * 120): {
						MetricName:  MetricNameContainerPSI,
						ContainerID: "container-uid-1",
						PodUID:      "pod-uid-1",
						MetricValue: &PSIMetric{
							SomeCPUAvg10:     0.7,
							SomeMemAvg10:     0.7,
							SomeIOAvg10:      0.7,
							FullCPUAvg10:     0.7,
							FullMemAvg10:     0.7,
							FullIOAvg10:      0.7,
							CPUFullSupported: true,
						},
					},
					now.Add(-time.Second * 10): {
						MetricName:  MetricNameContainerPSI,
						ContainerID: "container-uid-1",
						PodUID:      "pod-uid-1",
						MetricValue: &PSIMetric{
							SomeCPUAvg10:     0.6,
							SomeMemAvg10:     0.6,
							SomeIOAvg10:      0.6,
							FullCPUAvg10:     0.6,
							FullMemAvg10:     0.6,
							FullIOAvg10:      0.6,
							CPUFullSupported: true,
						},
					},
					now.Add(-time.Second * 5): {
						MetricName:  MetricNameContainerPSI,
						ContainerID: "container-uid-1",
						PodUID:      "pod-uid-1",
						MetricValue: &PSIMetric{
							SomeCPUAvg10:     0.5,
							SomeMemAvg10:     0.5,
							SomeIOAvg10:      0.5,
							FullCPUAvg10:     0.5,
							FullMemAvg10:     0.5,
							FullIOAvg10:      0.5,
							CPUFullSupported: true,
						},
					},
					now.Add(-time.Second * 4): {
						MetricName:  MetricNameContainerPSI,
						ContainerID: "container-uid-2",
						PodUID:      "pod-uid-2",
						MetricValue: &PSIMetric{
							SomeCPUAvg10:     0.4,
							SomeMemAvg10:     0.4,
							SomeIOAvg10:      0.4,
							FullCPUAvg10:     0.4,
							FullMemAvg10:     0.4,
							FullIOAvg10:      0.4,
							CPUFullSupported: true,
						},
					},
				},
			},
			want: ContainerInterferenceQueryResult{
				Metric: &ContainerInterferenceMetric{
					MetricName:  MetricNameContainerPSI,
					ContainerID: "container-uid-1",
					PodUID:      "pod-uid-1",
					MetricValue: &PSIMetric{
						SomeCPUAvg10:     0.5,
						SomeMemAvg10:     0.5,
						SomeIOAvg10:      0.5,
						FullCPUAvg10:     0.5,
						FullMemAvg10:     0.5,
						FullIOAvg10:      0.5,
						CPUFullSupported: true,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: ContainerInterferenceQueryResult{
				Metric: &ContainerInterferenceMetric{
					MetricName:  MetricNameContainerPSI,
					ContainerID: "container-uid-1",
					PodUID:      "pod-uid-1",
					MetricValue: &PSIMetric{
						SomeCPUAvg10:     0.5,
						SomeMemAvg10:     0.5,
						SomeIOAvg10:      0.5,
						FullCPUAvg10:     0.5,
						FullMemAvg10:     0.5,
						FullIOAvg10:      0.5,
						CPUFullSupported: true,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
		{
			name: "container-psi-avg-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   MetricNameContainerPSI,
				containerID:  "container-uid-3",
				podUID:       "pod-uid-3",
				aggregateArg: AggregationTypeAVG,
				samples: map[time.Time]ContainerInterferenceMetric{
					now.Add(-time.Second * 120): {
						MetricName:  MetricNameContainerPSI,
						ContainerID: "container-uid-3",
						PodUID:      "pod-uid-3",
						MetricValue: &PSIMetric{
							SomeCPUAvg10:     0.7,
							SomeMemAvg10:     0.7,
							SomeIOAvg10:      0.7,
							FullCPUAvg10:     0.7,
							FullMemAvg10:     0.7,
							FullIOAvg10:      0.7,
							CPUFullSupported: true,
						},
					},
					now.Add(-time.Second * 10): {
						MetricName:  MetricNameContainerPSI,
						ContainerID: "container-uid-3",
						PodUID:      "pod-uid-3",
						MetricValue: &PSIMetric{
							SomeCPUAvg10:     0.6,
							SomeMemAvg10:     0.6,
							SomeIOAvg10:      0.6,
							FullCPUAvg10:     0.6,
							FullMemAvg10:     0.6,
							FullIOAvg10:      0.6,
							CPUFullSupported: true,
						},
					},
					now.Add(-time.Second * 5): {
						MetricName:  MetricNameContainerPSI,
						ContainerID: "container-uid-3",
						PodUID:      "pod-uid-3",
						MetricValue: &PSIMetric{
							SomeCPUAvg10:     0.5,
							SomeMemAvg10:     0.5,
							SomeIOAvg10:      0.5,
							FullCPUAvg10:     0.5,
							FullMemAvg10:     0.5,
							FullIOAvg10:      0.5,
							CPUFullSupported: true,
						},
					},
					now.Add(-time.Second * 4): {
						MetricName:  MetricNameContainerPSI,
						ContainerID: "container-uid-4",
						PodUID:      "pod-uid-4",
						MetricValue: &PSIMetric{
							SomeCPUAvg10:     0.4,
							SomeMemAvg10:     0.4,
							SomeIOAvg10:      0.4,
							FullCPUAvg10:     0.4,
							FullMemAvg10:     0.4,
							FullIOAvg10:      0.4,
							CPUFullSupported: true,
						},
					},
				},
			},
			want: ContainerInterferenceQueryResult{
				Metric: &ContainerInterferenceMetric{
					MetricName:  MetricNameContainerPSI,
					ContainerID: "container-uid-3",
					PodUID:      "pod-uid-3",
					MetricValue: &PSIMetric{
						SomeCPUAvg10:     0.6,
						SomeMemAvg10:     0.6,
						SomeIOAvg10:      0.6,
						FullCPUAvg10:     0.6,
						FullMemAvg10:     0.6,
						FullIOAvg10:      0.6,
						CPUFullSupported: true,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: ContainerInterferenceQueryResult{
				Metric: &ContainerInterferenceMetric{
					MetricName:  MetricNameContainerPSI,
					ContainerID: "container-uid-3",
					PodUID:      "pod-uid-3",
					MetricValue: &PSIMetric{
						SomeCPUAvg10:     0.55,
						SomeMemAvg10:     0.55,
						SomeIOAvg10:      0.55,
						FullCPUAvg10:     0.55,
						FullMemAvg10:     0.55,
						FullIOAvg10:      0.55,
						CPUFullSupported: true,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertContainerInterferenceMetrics(ts, &sample)
				if err != nil {
					t.Errorf("insert interference metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetContainerInterferenceMetric(tt.args.metricName, &tt.args.podUID, &tt.args.containerID, params)
			if got.Error != nil {
				t.Errorf("get interference metric failed %v", got.Error)
			}

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetContainerInterferenceMetric() got = %v, want %v", got, tt.want)
			}
			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetContainerInterferenceMetric(tt.args.metricName, &tt.args.podUID, &tt.args.containerID, params)
			if gotAfterDel.Error != nil {
				t.Errorf("get interference metric failed %v", gotAfterDel.Error)
			}
			if !reflect.DeepEqual(gotAfterDel, tt.wantAfterDelete) {
				t.Errorf("GetContainerInterferenceMetric() after delete, got = %v, want %v",
					gotAfterDel, tt.wantAfterDelete)
			}
		})
	}
}

func Test_metricCache_PodCPIMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		metricName   InterferenceMetricName
		containerID  string
		podUID       string
		aggregateArg AggregationType
		samples      map[time.Time]ContainerInterferenceMetric
	}

	tests := []struct {
		name            string
		args            args
		want            PodInterferenceQueryResult
		wantAfterDelete PodInterferenceQueryResult
	}{
		// test pod CPI CRUD
		{
			name: "pod-cpi-latest-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   MetricNamePodCPI,
				containerID:  "container-uid-5",
				podUID:       "pod-uid-5",
				aggregateArg: AggregationTypeLast,
				samples: map[time.Time]ContainerInterferenceMetric{
					now.Add(-time.Second * 120): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-5",
						PodUID:      "pod-uid-5",
						MetricValue: &CPIMetric{
							Cycles:       7,
							Instructions: 7,
						},
					},
					now.Add(-time.Second * 10): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-5",
						PodUID:      "pod-uid-5",
						MetricValue: &CPIMetric{
							Cycles:       6,
							Instructions: 6,
						},
					},
					now.Add(-time.Second * 5): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-6",
						PodUID:      "pod-uid-5",
						MetricValue: &CPIMetric{
							Cycles:       5,
							Instructions: 5,
						},
					},
					now.Add(-time.Second * 4): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-6",
						PodUID:      "pod-uid-6",
						MetricValue: &CPIMetric{
							Cycles:       4,
							Instructions: 4,
						},
					},
				},
			},
			want: PodInterferenceQueryResult{
				Metric: &PodInterferenceMetric{
					MetricName: MetricNamePodCPI,
					PodUID:     "pod-uid-5",
					MetricValue: &CPIMetric{
						Cycles:       18,
						Instructions: 18,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 1}},
			},
			wantAfterDelete: PodInterferenceQueryResult{
				Metric: &PodInterferenceMetric{
					MetricName: MetricNamePodCPI,
					PodUID:     "pod-uid-5",
					MetricValue: &CPIMetric{
						Cycles:       11,
						Instructions: 11,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 1}},
			},
		},
		{
			name: "pod-cpi-avg-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   MetricNamePodCPI,
				containerID:  "container-uid-7",
				podUID:       "pod-uid-7",
				aggregateArg: AggregationTypeAVG,
				samples: map[time.Time]ContainerInterferenceMetric{
					now.Add(-time.Second * 120): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-7",
						PodUID:      "pod-uid-7",
						MetricValue: &CPIMetric{
							Cycles:       7,
							Instructions: 7,
						},
					},
					now.Add(-time.Second * 10): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-7",
						PodUID:      "pod-uid-7",
						MetricValue: &CPIMetric{
							Cycles:       6,
							Instructions: 6,
						},
					},
					now.Add(-time.Second * 5): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-8",
						PodUID:      "pod-uid-7",
						MetricValue: &CPIMetric{
							Cycles:       5,
							Instructions: 5,
						},
					},
					now.Add(-time.Second * 4): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-8",
						PodUID:      "pod-uid-8",
						MetricValue: &CPIMetric{
							Cycles:       4,
							Instructions: 4,
						},
					},
				},
			},
			want: PodInterferenceQueryResult{
				Metric: &PodInterferenceMetric{
					MetricName: MetricNamePodCPI,
					PodUID:     "pod-uid-7",
					MetricValue: &CPIMetric{
						Cycles:       18,
						Instructions: 18,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 1}},
			},
			wantAfterDelete: PodInterferenceQueryResult{
				Metric: &PodInterferenceMetric{
					MetricName: MetricNamePodCPI,
					PodUID:     "pod-uid-7",
					MetricValue: &CPIMetric{
						Cycles:       11,
						Instructions: 11,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 1}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertContainerInterferenceMetrics(ts, &sample)
				if err != nil {
					t.Errorf("insert interference metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetPodInterferenceMetric(tt.args.metricName, &tt.args.podUID, params)
			if got.Error != nil {
				t.Errorf("get interference metric failed %v", got.Error)
			}

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetPodInterferenceMetric() got = %v, want %v", got, tt.want)
			}
			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetPodInterferenceMetric(tt.args.metricName, &tt.args.podUID, params)
			if gotAfterDel.Error != nil {
				t.Errorf("get interference metric failed %v", gotAfterDel.Error)
			}
			if !reflect.DeepEqual(gotAfterDel, tt.wantAfterDelete) {
				t.Errorf("GetPodInterferenceMetric() after delete, got = %v, want %v",
					gotAfterDel, tt.wantAfterDelete)
			}
		})
	}
}

func Test_convertAndGetPodInterferenceMetric_zeroResult(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		metricName   InterferenceMetricName
		containerID  string
		podUID       string
		aggregateArg AggregationType
		samples      map[time.Time]ContainerInterferenceMetric
	}

	tests := []struct {
		name string
		args args
	}{
		// test get pod CPI 0 result
		{
			name: "get-pod-cpi-0-result",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   MetricNamePodCPI,
				containerID:  "container-uid-9",
				podUID:       "pod-uid-9",
				aggregateArg: AggregationTypeLast,
				samples: map[time.Time]ContainerInterferenceMetric{
					now.Add(-time.Second * 4): {
						MetricName:  MetricNameContainerCPI,
						ContainerID: "container-uid-10",
						PodUID:      "pod-uid-10",
						MetricValue: &CPIMetric{
							Cycles:       4,
							Instructions: 4,
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertContainerInterferenceMetrics(ts, &sample)
				if err != nil {
					t.Errorf("insert interference metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetPodInterferenceMetric(tt.args.metricName, &tt.args.podUID, params)
			assert.NotNil(t, got.Error)
		})
	}
}

func Test_GetContainerInterferenceMetric_errWrongMetricName(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		metricName   InterferenceMetricName
		containerID  string
		podUID       string
		aggregateArg AggregationType
	}

	tests := []struct {
		name string
		args args
	}{
		// test wrong metric name err
		{
			name: "container-wrong-metric-name",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   "WrongMetricName",
				containerID:  "container-uid-1",
				podUID:       "pod-uid-1",
				aggregateArg: AggregationTypeLast,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetContainerInterferenceMetric(tt.args.metricName, &tt.args.podUID, &tt.args.containerID, params)
			if got.Error == nil {
				t.Errorf("get interference metric did not report err")
			}
		})
	}
}

func Test_GetContainerInterferenceMetric_errNilParam(t *testing.T) {
	type args struct {
		config      *Config
		metricName  InterferenceMetricName
		containerID string
		podUID      string
	}

	tests := []struct {
		name string
		args args
	}{
		// test nil param err
		{
			name: "container-nil-param",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:  MetricNameContainerCPI,
				containerID: "container-uid-1",
				podUID:      "pod-uid-1",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}

			got := m.GetContainerInterferenceMetric(tt.args.metricName, &tt.args.podUID, &tt.args.containerID, nil)
			if got.Error == nil {
				t.Errorf("get interference metric did not report err")
			}
		})
	}
}

func Test_GetPodInterferenceMetric_errWrongMetricName(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		metricName   InterferenceMetricName
		containerID  string
		podUID       string
		aggregateArg AggregationType
	}

	tests := []struct {
		name string
		args args
	}{
		// test wrong metric name err
		{
			name: "pod-wrong-metric-name",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   "WrongMetricName",
				podUID:       "pod-uid-1",
				aggregateArg: AggregationTypeLast,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetPodInterferenceMetric(tt.args.metricName, &tt.args.podUID, params)
			if got.Error == nil {
				t.Errorf("get interference metric did not report err")
			}
		})
	}
}

func Test_GetPodInterferenceMetric_errNilParam(t *testing.T) {
	type args struct {
		config      *Config
		metricName  InterferenceMetricName
		containerID string
		podUID      string
	}

	tests := []struct {
		name string
		args args
	}{
		// test nil param err
		{
			name: "pod-nil-param",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName: MetricNameContainerCPI,
				podUID:     "pod-uid-1",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}

			got := m.GetPodInterferenceMetric(tt.args.metricName, &tt.args.podUID, nil)
			if got.Error == nil {
				t.Errorf("get interference metric did not report err")
			}
		})
	}
}

func Test_metricCache_PodPSIMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		config       *Config
		metricName   InterferenceMetricName
		containerID  string
		podUID       string
		aggregateArg AggregationType
		samples      map[time.Time]PodInterferenceMetric
	}

	tests := []struct {
		name            string
		args            args
		want            PodInterferenceQueryResult
		wantAfterDelete PodInterferenceQueryResult
	}{
		// test pod PSI CRUD
		{
			name: "pod-psi-latest-crud",
			args: args{
				config: &Config{
					MetricGCIntervalSeconds: 60,
					MetricExpireSeconds:     60,
				},
				metricName:   MetricNamePodPSI,
				podUID:       "pod-uid-1",
				aggregateArg: AggregationTypeLast,
				samples: map[time.Time]PodInterferenceMetric{
					now.Add(-time.Second * 120): {
						MetricName: MetricNamePodPSI,
						PodUID:     "pod-uid-1",
						MetricValue: &PSIMetric{
							SomeCPUAvg10: 7,
							SomeMemAvg10: 7,
							SomeIOAvg10:  7,
							FullCPUAvg10: 7,
							FullMemAvg10: 7,
							FullIOAvg10:  7,
						},
					},
					now.Add(-time.Second * 10): {
						MetricName: MetricNamePodPSI,
						PodUID:     "pod-uid-1",
						MetricValue: &PSIMetric{
							SomeCPUAvg10: 6,
							SomeMemAvg10: 6,
							SomeIOAvg10:  6,
							FullCPUAvg10: 6,
							FullMemAvg10: 6,
							FullIOAvg10:  6,
						},
					},
					now.Add(-time.Second * 5): {
						MetricName: MetricNamePodPSI,
						PodUID:     "pod-uid-1",
						MetricValue: &PSIMetric{
							SomeCPUAvg10: 5,
							SomeMemAvg10: 5,
							SomeIOAvg10:  5,
							FullCPUAvg10: 5,
							FullMemAvg10: 5,
							FullIOAvg10:  5,
						},
					},
					now.Add(-time.Second * 4): {
						MetricName: MetricNamePodPSI,
						PodUID:     "pod-uid-2",
						MetricValue: &PSIMetric{
							SomeCPUAvg10: 4,
							SomeMemAvg10: 4,
							SomeIOAvg10:  4,
							FullCPUAvg10: 4,
							FullMemAvg10: 4,
							FullIOAvg10:  4,
						},
					},
				},
			},
			want: PodInterferenceQueryResult{
				Metric: &PodInterferenceMetric{
					MetricName: MetricNamePodPSI,
					PodUID:     "pod-uid-1",
					MetricValue: &PSIMetric{
						SomeCPUAvg10: 5,
						SomeMemAvg10: 5,
						SomeIOAvg10:  5,
						FullCPUAvg10: 5,
						FullMemAvg10: 5,
						FullIOAvg10:  5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 3}},
			},
			wantAfterDelete: PodInterferenceQueryResult{
				Metric: &PodInterferenceMetric{
					MetricName: MetricNamePodPSI,
					PodUID:     "pod-uid-1",
					MetricValue: &PSIMetric{
						SomeCPUAvg10: 5,
						SomeMemAvg10: 5,
						SomeIOAvg10:  5,
						FullCPUAvg10: 5,
						FullMemAvg10: 5,
						FullIOAvg10:  5,
					},
				},
				QueryResult: QueryResult{AggregateInfo: &AggregateInfo{MetricsCount: 2}},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			m := &metricCache{
				config: tt.args.config,
				db:     s,
			}
			for ts, sample := range tt.args.samples {
				err := m.InsertPodInterferenceMetrics(ts, &sample)
				if err != nil {
					t.Errorf("insert interference metric failed %v", err)
				}
			}

			oldStartTime := time.Unix(0, 0)
			params := &QueryParam{
				Aggregate: tt.args.aggregateArg,
				Start:     &oldStartTime,
				End:       &now,
			}

			got := m.GetPodInterferenceMetric(tt.args.metricName, &tt.args.podUID, params)
			if got.Error != nil {
				t.Errorf("get interference metric failed %v", got.Error)
			}

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetPodInterferenceMetric() got = %v, want %v", got, tt.want)
				t.Errorf("GetPodInterferenceMetric() got = %v, want %v", got.Metric.MetricName, tt.want.Metric.MetricName)
				t.Errorf("GetPodInterferenceMetric() got = %v, want %v", got.QueryResult.AggregateInfo.MetricsCount, tt.want.QueryResult.AggregateInfo.MetricsCount)
				t.Errorf("GetPodInterferenceMetric() got = %v, want %v", got.Metric.PodUID, tt.want.Metric.PodUID)
				t.Errorf("GetPodInterferenceMetric() got = %v, want %v", got.Metric.MetricValue.(*CPIMetric).Cycles, tt.want.Metric.MetricValue.(*CPIMetric).Cycles)
			}
			// delete expire items
			m.recycleDB()

			gotAfterDel := m.GetPodInterferenceMetric(tt.args.metricName, &tt.args.podUID, params)
			if gotAfterDel.Error != nil {
				t.Errorf("get interference metric failed %v", gotAfterDel.Error)
			}
			if !reflect.DeepEqual(gotAfterDel, tt.wantAfterDelete) {
				t.Errorf("GetPodInterferenceMetric() after delete, got = %v, want %v",
					gotAfterDel, tt.wantAfterDelete)
			}
		})
	}
}
