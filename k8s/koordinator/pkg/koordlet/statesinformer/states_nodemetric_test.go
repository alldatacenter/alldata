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

package statesinformer

import (
	"context"
	"reflect"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	faketopologyclientset "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/clientset/versioned/fake"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime/schema"
	fakeclientset "k8s.io/client-go/kubernetes/fake"
	"k8s.io/utils/pointer"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	fakekoordclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	clientbeta1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/slo/v1alpha1"
	fakeclientslov1alpha1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/typed/slo/v1alpha1/fake"
	listerbeta1 "github.com/koordinator-sh/koordinator/pkg/client/listers/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mock_metriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
)

var _ listerbeta1.NodeMetricLister = &fakeNodeMetricLister{}

type fakeNodeMetricLister struct {
	nodeMetrics *slov1alpha1.NodeMetric
	getErr      error
}

func (f *fakeNodeMetricLister) List(selector labels.Selector) (ret []*slov1alpha1.NodeMetric, err error) {
	return []*slov1alpha1.NodeMetric{f.nodeMetrics}, nil
}

func (f *fakeNodeMetricLister) Get(name string) (*slov1alpha1.NodeMetric, error) {
	return f.nodeMetrics, f.getErr
}

func Test_reporter_isNodeMetricInited(t *testing.T) {
	type fields struct {
		nodeMetric *slov1alpha1.NodeMetric
	}
	tests := []struct {
		name   string
		fields fields
		want   bool
	}{
		{
			name: "is-node-metric-inited",
			fields: fields{
				nodeMetric: &slov1alpha1.NodeMetric{},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := &nodeMetricInformer{
				nodeMetric: tt.fields.nodeMetric,
			}
			if got := r.isNodeMetricInited(); got != tt.want {
				t.Errorf("isNodeMetricInited() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_nodeMetricInformer_getNodeMetricReportInterval(t *testing.T) {
	type fields struct {
		nodeMetric *slov1alpha1.NodeMetric
	}
	tests := []struct {
		name   string
		fields fields
		want   time.Duration
	}{
		{
			name: "get report interval from node metric",
			fields: fields{
				nodeMetric: &slov1alpha1.NodeMetric{
					Spec: slov1alpha1.NodeMetricSpec{
						CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
							ReportIntervalSeconds: pointer.Int64(666),
						},
					},
				},
			},
			want: 666 * time.Second,
		},
		{
			name: "get default interval from nil",
			fields: fields{
				nodeMetric: nil,
			},
			want: defaultReportIntervalSeconds * time.Second,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := &nodeMetricInformer{
				nodeMetric: tt.fields.nodeMetric,
			}
			if got := r.getNodeMetricReportInterval(); got != tt.want {
				t.Errorf("getNodeMetricReportInterval() = %v, want %v", got, tt.want)
			}
		})
	}
}

type fakeNodeMetricClient struct {
	fakeclientslov1alpha1.FakeNodeMetrics
	nodeMetrics map[string]*slov1alpha1.NodeMetric
}

func (c *fakeNodeMetricClient) Get(ctx context.Context, name string, opts metav1.GetOptions) (*slov1alpha1.NodeMetric, error) {
	nodeMetric, ok := c.nodeMetrics[name]
	if !ok {
		return &slov1alpha1.NodeMetric{}, errors.NewNotFound(schema.GroupResource{Group: "slo.koordinator.sh", Resource: "nodemetrics"}, name)
	}
	return nodeMetric, nil
}

func (c *fakeNodeMetricClient) UpdateStatus(ctx context.Context, nodeMetric *slov1alpha1.NodeMetric, opts metav1.UpdateOptions) (*slov1alpha1.NodeMetric, error) {
	currentNodeMetric, ok := c.nodeMetrics[nodeMetric.Name]
	if !ok {
		return &slov1alpha1.NodeMetric{}, errors.NewNotFound(schema.GroupResource{Group: "slo.koordinator.sh", Resource: "nodemetrics"}, nodeMetric.Name)
	}
	currentNodeMetric.Status = nodeMetric.Status
	c.nodeMetrics[nodeMetric.Name] = currentNodeMetric
	return currentNodeMetric, nil
}

// check sync with single node metric in metric cache
func Test_reporter_sync_with_single_node_metric(t *testing.T) {
	type fields struct {
		nodeName         string
		nodeMetric       *slov1alpha1.NodeMetric
		metricCache      func(ctrl *gomock.Controller) metriccache.MetricCache
		podsInformer     *podsInformer
		nodeMetricLister listerbeta1.NodeMetricLister
		nodeMetricClient clientbeta1.NodeMetricInterface
	}
	tests := []struct {
		name             string
		fields           fields
		wantNilStatus    bool
		wantNodeResource slov1alpha1.ResourceMap
		wantPodsMetric   []*slov1alpha1.PodMetricInfo
		wantErr          bool
	}{
		{
			name: "nodeMetric not initialized",
			fields: fields{
				nodeName:   "test",
				nodeMetric: nil,
				metricCache: func(ctrl *gomock.Controller) metriccache.MetricCache {
					return nil
				},
				podsInformer:     NewPodsInformer(),
				nodeMetricLister: nil,
				nodeMetricClient: &fakeNodeMetricClient{},
			},
			wantNilStatus:    true,
			wantNodeResource: slov1alpha1.ResourceMap{},
			wantPodsMetric:   nil,
			wantErr:          true,
		},
		{
			name: "successfully report nodeMetric",
			fields: fields{
				nodeName: "test",
				nodeMetric: &slov1alpha1.NodeMetric{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test",
					},
					Spec: slov1alpha1.NodeMetricSpec{
						CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
							AggregateDurationSeconds: defaultNodeMetricSpec.CollectPolicy.AggregateDurationSeconds,
							NodeAggregatePolicy: &slov1alpha1.AggregatePolicy{
								Durations: []metav1.Duration{
									{
										Duration: 5 * time.Minute,
									},
								},
							},
						},
					},
				},
				metricCache: func(ctrl *gomock.Controller) metriccache.MetricCache {
					c := mock_metriccache.NewMockMetricCache(ctrl)
					c.EXPECT().GetNodeResourceMetric(gomock.Any()).Return(metriccache.NodeResourceQueryResult{
						Metric: &metriccache.NodeResourceMetric{
							CPUUsed: metriccache.CPUMetric{
								CPUUsed: resource.MustParse("1000"),
							},
							MemoryUsed: metriccache.MemoryMetric{
								MemoryWithoutCache: resource.MustParse("1Gi"),
							},
							GPUs: []metriccache.GPUMetric{
								{
									DeviceUUID:  "1",
									Minor:       0,
									SMUtil:      80,
									MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
									MemoryTotal: *resource.NewQuantity(100, resource.BinarySI),
								},
								{
									DeviceUUID:  "2",
									Minor:       1,
									SMUtil:      40,
									MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
									MemoryTotal: *resource.NewQuantity(200, resource.BinarySI),
								},
							},
						},
					}).AnyTimes()
					c.EXPECT().GetPodResourceMetric(gomock.Any(), gomock.Any()).Return(metriccache.PodResourceQueryResult{
						Metric: &metriccache.PodResourceMetric{
							PodUID: "test-pod",
							CPUUsed: metriccache.CPUMetric{
								CPUUsed: resource.MustParse("1000"),
							},
							MemoryUsed: metriccache.MemoryMetric{
								MemoryWithoutCache: resource.MustParse("1Gi"),
							},
							GPUs: []metriccache.GPUMetric{
								{
									DeviceUUID:  "1",
									Minor:       0,
									SMUtil:      80,
									MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
									MemoryTotal: *resource.NewQuantity(100, resource.BinarySI),
								},
								{
									DeviceUUID:  "2",
									Minor:       1,
									SMUtil:      40,
									MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
									MemoryTotal: *resource.NewQuantity(200, resource.BinarySI),
								},
							},
						},
					}).Times(1)
					return c
				},
				podsInformer: &podsInformer{
					podMap: map[string]*PodMeta{
						"default/test-pod": {
							Pod: &v1.Pod{
								ObjectMeta: metav1.ObjectMeta{
									Name:      "test-pod",
									Namespace: "default",
									UID:       "test-pod",
								},
							},
						},
					},
				},
				nodeMetricLister: &fakeNodeMetricLister{
					nodeMetrics: &slov1alpha1.NodeMetric{
						ObjectMeta: metav1.ObjectMeta{
							Name: "test",
						},
					},
				},
				nodeMetricClient: &fakeNodeMetricClient{
					nodeMetrics: map[string]*slov1alpha1.NodeMetric{
						"test": {
							ObjectMeta: metav1.ObjectMeta{
								Name: "test",
							},
						},
					},
				},
			},
			wantNilStatus: false,
			wantNodeResource: convertNodeMetricToResourceMap(&metriccache.NodeResourceMetric{
				CPUUsed: metriccache.CPUMetric{
					CPUUsed: resource.MustParse("1000"),
				},
				MemoryUsed: metriccache.MemoryMetric{
					MemoryWithoutCache: resource.MustParse("1Gi"),
				},
				GPUs: []metriccache.GPUMetric{
					{
						DeviceUUID:  "1",
						Minor:       0,
						SMUtil:      80,
						MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
						MemoryTotal: *resource.NewQuantity(100, resource.BinarySI),
					},
					{
						DeviceUUID:  "2",
						Minor:       1,
						SMUtil:      40,
						MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
						MemoryTotal: *resource.NewQuantity(200, resource.BinarySI),
					},
				},
			}),
			wantPodsMetric: []*slov1alpha1.PodMetricInfo{
				{
					Name:      "test-pod",
					Namespace: "default",
					PodUsage: *convertPodMetricToResourceMap(&metriccache.PodResourceMetric{
						PodUID: "test-pod",
						CPUUsed: metriccache.CPUMetric{
							CPUUsed: resource.MustParse("1000"),
						},
						MemoryUsed: metriccache.MemoryMetric{
							MemoryWithoutCache: resource.MustParse("1Gi"),
						},
						GPUs: []metriccache.GPUMetric{
							{
								DeviceUUID:  "1",
								Minor:       0,
								SMUtil:      80,
								MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(100, resource.BinarySI),
							},
							{
								DeviceUUID:  "2",
								Minor:       1,
								SMUtil:      40,
								MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
								MemoryTotal: *resource.NewQuantity(200, resource.BinarySI),
							},
						},
					}),
				},
			},
			wantErr: false,
		},
		{
			name: "skip for nodeMetric not found",
			fields: fields{
				nodeName: "test",
				nodeMetric: &slov1alpha1.NodeMetric{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test",
					},
					Spec: defaultNodeMetricSpec,
				},
				metricCache: func(ctrl *gomock.Controller) metriccache.MetricCache {
					c := mock_metriccache.NewMockMetricCache(ctrl)
					c.EXPECT().GetNodeResourceMetric(gomock.Any()).Return(metriccache.NodeResourceQueryResult{
						Metric: &metriccache.NodeResourceMetric{
							CPUUsed: metriccache.CPUMetric{
								CPUUsed: resource.MustParse("1000"),
							},
							MemoryUsed: metriccache.MemoryMetric{
								MemoryWithoutCache: resource.MustParse("1Gi"),
							},
						},
					}).AnyTimes()
					return c
				},
				podsInformer: NewPodsInformer(),
				nodeMetricLister: &fakeNodeMetricLister{
					nodeMetrics: &slov1alpha1.NodeMetric{
						ObjectMeta: metav1.ObjectMeta{
							Name: "test",
						},
					},
					getErr: errors.NewNotFound(schema.GroupResource{Group: "slo.koordinator.sh", Resource: "nodemetrics"}, "test"),
				},
				nodeMetricClient: &fakeNodeMetricClient{
					nodeMetrics: map[string]*slov1alpha1.NodeMetric{
						"test": {
							ObjectMeta: metav1.ObjectMeta{
								Name: "test",
							},
						},
					},
				},
			},
			wantNilStatus:    true,
			wantPodsMetric:   nil,
			wantNodeResource: slov1alpha1.ResourceMap{},
			wantErr:          false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()
			r := &nodeMetricInformer{
				nodeName:         tt.fields.nodeName,
				nodeMetric:       tt.fields.nodeMetric,
				metricCache:      tt.fields.metricCache(ctrl),
				podsInformer:     tt.fields.podsInformer,
				nodeMetricLister: tt.fields.nodeMetricLister,
				statusUpdater:    newStatusUpdater(tt.fields.nodeMetricClient),
			}

			r.sync()

			nodeMetric, err := r.statusUpdater.nodeMetricClient.Get(context.TODO(), tt.fields.nodeName, metav1.GetOptions{})
			assert.Equal(t, tt.wantErr, err != nil)
			if !tt.wantErr {
				assert.NotNil(t, nodeMetric)
				if tt.wantNilStatus {
					assert.Nil(t, nodeMetric.Status.NodeMetric)
					assert.Nil(t, nodeMetric.Status.PodsMetric)
				} else {
					assert.Equal(t, tt.wantNodeResource, nodeMetric.Status.NodeMetric.NodeUsage)
					assert.Equal(t, tt.wantPodsMetric, nodeMetric.Status.PodsMetric)
				}
			}
		})
	}
}

func Test_nodeMetricInformer_queryNodeMetric(t *testing.T) {
	type fields struct {
		nodeResult metriccache.NodeResourceQueryResult
	}
	type args struct {
		aggregateType metriccache.AggregationType
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		{
			name: "query node p90 usage metric",
			fields: fields{
				nodeResult: metriccache.NodeResourceQueryResult{
					QueryResult: metriccache.QueryResult{
						AggregateInfo: &metriccache.AggregateInfo{
							MetricsCount: 1,
						},
						Error: nil,
					},
					Metric: &metriccache.NodeResourceMetric{
						CPUUsed: metriccache.CPUMetric{
							CPUUsed: *resource.NewQuantity(10, resource.DecimalSI),
						},
						MemoryUsed: metriccache.MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(1024, resource.BinarySI),
						},
					},
				},
			},
			args: args{
				aggregateType: metriccache.AggregationTypeP90,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()
			c := mock_metriccache.NewMockMetricCache(ctrl)
			end := time.Now()
			start := end.Add(-defaultAggregateDurationSeconds * time.Second)
			queryParam := &metriccache.QueryParam{
				Aggregate: tt.args.aggregateType,
				Start:     &start,
				End:       &end,
			}
			c.EXPECT().GetNodeResourceMetric(queryParam).Return(tt.fields.nodeResult)
			r := &nodeMetricInformer{
				metricCache: c,
			}
			want := convertNodeMetricToResourceMap(tt.fields.nodeResult.Metric)
			if got := r.queryNodeMetric(start, end, tt.args.aggregateType, false); !reflect.DeepEqual(got, want) {
				t.Errorf("queryNodeMetric() = %v, want %v", got, want)
			}
		})
	}
}

func Test_nodeMetricInformer_collectNodeMetric(t *testing.T) {
	end := time.Now()
	start := end.Add(-defaultAggregateDurationSeconds * time.Second)
	type fields struct {
		nodeResultAVG metriccache.NodeResourceQueryResult
		nodeResultP50 metriccache.NodeResourceQueryResult
		nodeResultP90 metriccache.NodeResourceQueryResult
		nodeResultP95 metriccache.NodeResourceQueryResult
		nodeResultP99 metriccache.NodeResourceQueryResult
	}
	tests := []struct {
		name   string
		fields fields
	}{
		{
			name: "merge node metric",
			fields: fields{
				nodeResultAVG: metriccache.NodeResourceQueryResult{
					QueryResult: metriccache.QueryResult{
						AggregateInfo: &metriccache.AggregateInfo{
							MetricStart:  &start,
							MetricEnd:    &end,
							MetricsCount: 1,
						},
						Error: nil,
					},
					Metric: &metriccache.NodeResourceMetric{
						CPUUsed: metriccache.CPUMetric{
							CPUUsed: *resource.NewQuantity(1, resource.DecimalSI),
						},
						MemoryUsed: metriccache.MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(1, resource.BinarySI),
						},
					},
				},
				nodeResultP50: metriccache.NodeResourceQueryResult{
					QueryResult: metriccache.QueryResult{
						AggregateInfo: &metriccache.AggregateInfo{
							MetricStart:  &start,
							MetricEnd:    &end,
							MetricsCount: 1,
						},
						Error: nil,
					},
					Metric: &metriccache.NodeResourceMetric{
						CPUUsed: metriccache.CPUMetric{
							CPUUsed: *resource.NewQuantity(2, resource.DecimalSI),
						},
						MemoryUsed: metriccache.MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(2, resource.BinarySI),
						},
					},
				},
				nodeResultP90: metriccache.NodeResourceQueryResult{
					QueryResult: metriccache.QueryResult{
						AggregateInfo: &metriccache.AggregateInfo{
							MetricStart:  &start,
							MetricEnd:    &end,
							MetricsCount: 1,
						},
						Error: nil,
					},
					Metric: &metriccache.NodeResourceMetric{
						CPUUsed: metriccache.CPUMetric{
							CPUUsed: *resource.NewQuantity(3, resource.DecimalSI),
						},
						MemoryUsed: metriccache.MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(3, resource.BinarySI),
						},
					},
				},
				nodeResultP95: metriccache.NodeResourceQueryResult{
					QueryResult: metriccache.QueryResult{
						AggregateInfo: &metriccache.AggregateInfo{
							MetricStart:  &start,
							MetricEnd:    &end,
							MetricsCount: 1,
						},
						Error: nil,
					},
					Metric: &metriccache.NodeResourceMetric{
						CPUUsed: metriccache.CPUMetric{
							CPUUsed: *resource.NewQuantity(4, resource.DecimalSI),
						},
						MemoryUsed: metriccache.MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(4, resource.BinarySI),
						},
					},
				},
				nodeResultP99: metriccache.NodeResourceQueryResult{
					QueryResult: metriccache.QueryResult{
						AggregateInfo: &metriccache.AggregateInfo{
							MetricStart:  &start,
							MetricEnd:    &end,
							MetricsCount: 1,
						},
						Error: nil,
					},
					Metric: &metriccache.NodeResourceMetric{
						CPUUsed: metriccache.CPUMetric{
							CPUUsed: *resource.NewQuantity(5, resource.DecimalSI),
						},
						MemoryUsed: metriccache.MemoryMetric{
							MemoryWithoutCache: *resource.NewQuantity(5, resource.BinarySI),
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			defer ctrl.Finish()
			c := mock_metriccache.NewMockMetricCache(ctrl)
			c.EXPECT().GetNodeResourceMetric(&metriccache.QueryParam{
				Aggregate: metriccache.AggregationTypeP50,
				Start:     &start,
				End:       &end,
			}).Return(tt.fields.nodeResultP50)
			c.EXPECT().GetNodeResourceMetric(&metriccache.QueryParam{
				Aggregate: metriccache.AggregationTypeP90,
				Start:     &start,
				End:       &end,
			}).Return(tt.fields.nodeResultP90)
			c.EXPECT().GetNodeResourceMetric(&metriccache.QueryParam{
				Aggregate: metriccache.AggregationTypeP95,
				Start:     &start,
				End:       &end,
			}).Return(tt.fields.nodeResultP95)
			c.EXPECT().GetNodeResourceMetric(&metriccache.QueryParam{
				Aggregate: metriccache.AggregationTypeP99,
				Start:     &start,
				End:       &end,
			}).Return(tt.fields.nodeResultP99)
			r := &nodeMetricInformer{
				metricCache: c,
				nodeMetric: &slov1alpha1.NodeMetric{
					Spec: slov1alpha1.NodeMetricSpec{
						CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
							AggregateDurationSeconds: defaultNodeMetricSpec.CollectPolicy.AggregateDurationSeconds,
							ReportIntervalSeconds:    defaultNodeMetricSpec.CollectPolicy.ReportIntervalSeconds,
							NodeAggregatePolicy: &slov1alpha1.AggregatePolicy{
								Durations: []metav1.Duration{
									{Duration: 5 * time.Minute},
								},
							},
						},
					},
				},
			}
			want := &slov1alpha1.NodeMetricInfo{
				NodeUsage: convertNodeMetricToResourceMap(tt.fields.nodeResultAVG.Metric),
				AggregatedNodeUsages: []slov1alpha1.AggregatedUsage{
					{
						Usage: map[slov1alpha1.AggregationType]slov1alpha1.ResourceMap{
							slov1alpha1.P50: convertNodeMetricToResourceMap(tt.fields.nodeResultP50.Metric),
							slov1alpha1.P90: convertNodeMetricToResourceMap(tt.fields.nodeResultP90.Metric),
							slov1alpha1.P95: convertNodeMetricToResourceMap(tt.fields.nodeResultP95.Metric),
							slov1alpha1.P99: convertNodeMetricToResourceMap(tt.fields.nodeResultP99.Metric),
						},
						Duration: metav1.Duration{
							Duration: end.Sub(start),
						},
					},
				},
			}
			if got := r.collectNodeAggregateMetric(end, r.nodeMetric.Spec.CollectPolicy.NodeAggregatePolicy); !reflect.DeepEqual(got, want.AggregatedNodeUsages) {
				t.Errorf("collectNodeAggregateMetric() = %v, want %v", got, want)
			}
		})
	}
}

func Test_nodeMetricInformer_updateMetricSpec(t *testing.T) {
	type fields struct {
		nodeMetric *slov1alpha1.NodeMetric
	}
	type args struct {
		newNodeMetric *slov1alpha1.NodeMetric
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   *slov1alpha1.NodeMetricSpec
	}{
		{
			name: "old and new are nil, do nothing for old",
			fields: fields{
				nodeMetric: &slov1alpha1.NodeMetric{
					Spec: slov1alpha1.NodeMetricSpec{
						CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{},
					},
				},
			},
			args: args{
				newNodeMetric: nil,
			},
			want: &slov1alpha1.NodeMetricSpec{
				CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{},
			},
		},
		{
			name: "new is empty, set old as default",
			fields: fields{
				nodeMetric: nil,
			},
			args: args{
				newNodeMetric: &slov1alpha1.NodeMetric{
					Spec: slov1alpha1.NodeMetricSpec{
						CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{},
					},
				},
			},
			want: &defaultNodeMetricSpec,
		},
		{
			name: "new is defined, merge default and set to old",
			fields: fields{
				nodeMetric: nil,
			},
			args: args{
				newNodeMetric: &slov1alpha1.NodeMetric{
					Spec: slov1alpha1.NodeMetricSpec{
						CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
							ReportIntervalSeconds: pointer.Int64(180),
						},
					},
				},
			},
			want: &slov1alpha1.NodeMetricSpec{
				CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
					AggregateDurationSeconds: defaultNodeMetricSpec.CollectPolicy.AggregateDurationSeconds,
					ReportIntervalSeconds:    pointer.Int64(180),
					NodeAggregatePolicy:      defaultNodeMetricSpec.CollectPolicy.NodeAggregatePolicy,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := &nodeMetricInformer{
				nodeMetric: tt.fields.nodeMetric,
			}
			r.updateMetricSpec(tt.args.newNodeMetric)
			assert.Equal(t, &r.nodeMetric.Spec, tt.want, "node metric spec should equal")
		})
	}
}

func Test_nodeMetricInformer_NewAndSetup(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	type args struct {
		ctx   *pluginOption
		state *pluginState
	}
	tests := []struct {
		name string
		args args
	}{
		{
			name: "new and setup node metric",
			args: args{
				ctx: &pluginOption{
					config:      NewDefaultConfig(),
					KubeClient:  fakeclientset.NewSimpleClientset(),
					KoordClient: fakekoordclientset.NewSimpleClientset(),
					TopoClient:  faketopologyclientset.NewSimpleClientset(),
					NodeName:    "test-node",
				},
				state: &pluginState{
					metricCache: mock_metriccache.NewMockMetricCache(ctrl),
					informerPlugins: map[pluginName]informerPlugin{
						podsInformerName: NewPodsInformer(),
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := NewNodeMetricInformer()
			r.Setup(tt.args.ctx, tt.args.state)
		})
	}
}

func Test_metricsInColdStart(t *testing.T) {
	queryEnd := time.Now()
	queryDuration := time.Minute * 10
	queryStart := queryEnd.Add(-queryDuration)
	shortDuration := time.Duration(int64(float64(queryDuration) * float64(validateTimeRangeRatio) / 2))
	shortStart := queryEnd.Add(-shortDuration)
	type args struct {
		queryStart  time.Time
		queryEnd    time.Time
		queryResult *metriccache.QueryResult
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "metric in cold start",
			args: args{
				queryResult: &metriccache.QueryResult{
					AggregateInfo: &metriccache.AggregateInfo{
						MetricStart:  &shortStart,
						MetricEnd:    &queryEnd,
						MetricsCount: 10,
					},
				},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := metricsInColdStart(queryStart, queryEnd, tt.args.queryResult); got != tt.want {
				t.Errorf("metricsInColdStart() = %v, want %v", got, tt.want)
			}
		})
	}
}
