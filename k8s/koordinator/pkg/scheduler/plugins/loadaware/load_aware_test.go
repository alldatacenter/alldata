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

package loadaware

import (
	"context"
	"encoding/json"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	frameworkruntime "k8s.io/kubernetes/pkg/scheduler/framework/runtime"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	koordfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/v1beta2"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
)

var _ framework.SharedLister = &testSharedLister{}

type testSharedLister struct {
	nodes       []*corev1.Node
	nodeInfos   []*framework.NodeInfo
	nodeInfoMap map[string]*framework.NodeInfo
}

func newTestSharedLister(pods []*corev1.Pod, nodes []*corev1.Node) *testSharedLister {
	nodeInfoMap := make(map[string]*framework.NodeInfo)
	nodeInfos := make([]*framework.NodeInfo, 0)
	for _, pod := range pods {
		nodeName := pod.Spec.NodeName
		if _, ok := nodeInfoMap[nodeName]; !ok {
			nodeInfoMap[nodeName] = framework.NewNodeInfo()
		}
		nodeInfoMap[nodeName].AddPod(pod)
	}
	for _, node := range nodes {
		if _, ok := nodeInfoMap[node.Name]; !ok {
			nodeInfoMap[node.Name] = framework.NewNodeInfo()
		}
		nodeInfoMap[node.Name].SetNode(node)
	}

	for _, v := range nodeInfoMap {
		nodeInfos = append(nodeInfos, v)
	}

	return &testSharedLister{
		nodes:       nodes,
		nodeInfos:   nodeInfos,
		nodeInfoMap: nodeInfoMap,
	}
}

func (f *testSharedLister) NodeInfos() framework.NodeInfoLister {
	return f
}

func (f *testSharedLister) List() ([]*framework.NodeInfo, error) {
	return f.nodeInfos, nil
}

func (f *testSharedLister) HavePodsWithAffinityList() ([]*framework.NodeInfo, error) {
	return nil, nil
}

func (f *testSharedLister) HavePodsWithRequiredAntiAffinityList() ([]*framework.NodeInfo, error) {
	return nil, nil
}

func (f *testSharedLister) Get(nodeName string) (*framework.NodeInfo, error) {
	return f.nodeInfoMap[nodeName], nil
}

func TestNew(t *testing.T) {
	var v1beta2args v1beta2.LoadAwareSchedulingArgs
	v1beta2.SetDefaults_LoadAwareSchedulingArgs(&v1beta2args)
	var loadAwareSchedulingArgs config.LoadAwareSchedulingArgs
	err := v1beta2.Convert_v1beta2_LoadAwareSchedulingArgs_To_config_LoadAwareSchedulingArgs(&v1beta2args, &loadAwareSchedulingArgs, nil)
	assert.NoError(t, err)

	koordClientSet := koordfake.NewSimpleClientset()
	koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
	extendHandle := frameworkext.NewExtendedHandle(
		frameworkext.WithKoordinatorClientSet(koordClientSet),
		frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
	)
	proxyNew := frameworkext.PluginFactoryProxy(extendHandle, New)

	cs := kubefake.NewSimpleClientset()
	informerFactory := informers.NewSharedInformerFactory(cs, 0)
	snapshot := newTestSharedLister(nil, nil)
	registeredPlugins := []schedulertesting.RegisterPluginFunc{
		schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
		schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
	}
	fh, err := schedulertesting.NewFramework(registeredPlugins, "koord-scheduler",
		frameworkruntime.WithClientSet(cs),
		frameworkruntime.WithInformerFactory(informerFactory),
		frameworkruntime.WithSnapshotSharedLister(snapshot),
	)
	assert.Nil(t, err)

	p, err := proxyNew(&loadAwareSchedulingArgs, fh)
	assert.NotNil(t, p)
	assert.Nil(t, err)
}

func TestFilterExpiredNodeMetric(t *testing.T) {
	tests := []struct {
		name       string
		nodeMetric *slov1alpha1.NodeMetric
		wantStatus *framework.Status
	}{
		{
			name: "filter healthy nodeMetrics",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
				},
			},
			wantStatus: nil,
		},
		{
			name: "filter unhealthy nodeMetric with nil updateTime",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Unschedulable, ErrReasonNodeMetricExpired),
		},
		{
			name: "filter unhealthy nodeMetric with expired updateTime",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now().Add(-180 * time.Second),
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Unschedulable, ErrReasonNodeMetricExpired),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var v1beta2args v1beta2.LoadAwareSchedulingArgs
			v1beta2.SetDefaults_LoadAwareSchedulingArgs(&v1beta2args)
			var loadAwareSchedulingArgs config.LoadAwareSchedulingArgs
			err := v1beta2.Convert_v1beta2_LoadAwareSchedulingArgs_To_config_LoadAwareSchedulingArgs(&v1beta2args, &loadAwareSchedulingArgs, nil)
			assert.NoError(t, err)

			koordClientSet := koordfake.NewSimpleClientset()
			koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
			extendHandle := frameworkext.NewExtendedHandle(
				frameworkext.WithKoordinatorClientSet(koordClientSet),
				frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
			)
			proxyNew := frameworkext.PluginFactoryProxy(extendHandle, New)

			cs := kubefake.NewSimpleClientset()
			informerFactory := informers.NewSharedInformerFactory(cs, 0)

			nodes := []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: tt.nodeMetric.Name,
					},
				},
			}

			snapshot := newTestSharedLister(nil, nodes)
			registeredPlugins := []schedulertesting.RegisterPluginFunc{
				schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
				schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
			}
			fh, err := schedulertesting.NewFramework(registeredPlugins, "koord-scheduler",
				frameworkruntime.WithClientSet(cs),
				frameworkruntime.WithInformerFactory(informerFactory),
				frameworkruntime.WithSnapshotSharedLister(snapshot),
			)
			assert.Nil(t, err)

			p, err := proxyNew(&loadAwareSchedulingArgs, fh)
			assert.NotNil(t, p)
			assert.Nil(t, err)

			_, err = koordClientSet.SloV1alpha1().NodeMetrics().Create(context.TODO(), tt.nodeMetric, metav1.CreateOptions{})
			assert.NoError(t, err)

			koordSharedInformerFactory.Start(context.TODO().Done())
			koordSharedInformerFactory.WaitForCacheSync(context.TODO().Done())

			cycleState := framework.NewCycleState()

			nodeInfo, err := snapshot.Get(tt.nodeMetric.Name)
			assert.NoError(t, err)
			assert.NotNil(t, nodeInfo)

			status := p.(*Plugin).Filter(context.TODO(), cycleState, &corev1.Pod{}, nodeInfo)
			assert.True(t, tt.wantStatus.Equal(status), "want status: %s, but got %s", tt.wantStatus.Message(), status.Message())
		})
	}
}

func TestFilterUsage(t *testing.T) {
	tests := []struct {
		name                      string
		usageThresholds           map[corev1.ResourceName]int64
		prodUsageThresholds       map[corev1.ResourceName]int64
		aggregated                *v1beta2.LoadAwareSchedulingAggregatedArgs
		customUsageThresholds     map[corev1.ResourceName]int64
		customProdUsageThresholds map[corev1.ResourceName]int64
		customAggregatedUsage     *extension.CustomAggregatedUsage
		nodeName                  string
		nodeMetric                *slov1alpha1.NodeMetric
		pods                      []*corev1.Pod
		testPod                   *corev1.Pod
		wantStatus                *framework.Status
	}{
		{
			name:     "filter normal usage",
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("60"),
								corev1.ResourceMemory: resource.MustParse("256Gi"),
							},
						},
					},
				},
			},
			wantStatus: nil,
		},
		{
			name:       "filter node missing NodeMetrics",
			nodeName:   "test-node-1",
			wantStatus: nil,
		},
		{
			name:     "filter exceed cpu usage",
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("70"),
								corev1.ResourceMemory: resource.MustParse("256Gi"),
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Unschedulable, fmt.Sprintf(ErrReasonUsageExceedThreshold, corev1.ResourceCPU)),
		},
		{
			name:     "filter exceed p95 cpu usage",
			nodeName: "test-node-1",
			aggregated: &v1beta2.LoadAwareSchedulingAggregatedArgs{
				UsageThresholds: map[corev1.ResourceName]int64{
					corev1.ResourceCPU: 60,
				},
				UsageAggregationType:    slov1alpha1.P95,
				UsageAggregatedDuration: &metav1.Duration{Duration: 5 * time.Minute},
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("30"),
								corev1.ResourceMemory: resource.MustParse("100Gi"),
							},
						},
						AggregatedNodeUsages: []slov1alpha1.AggregatedUsage{
							{
								Duration: metav1.Duration{Duration: 5 * time.Minute},
								Usage: map[slov1alpha1.AggregationType]slov1alpha1.ResourceMap{
									slov1alpha1.P95: {
										ResourceList: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("70"),
											corev1.ResourceMemory: resource.MustParse("256Gi"),
										},
									},
								},
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Unschedulable, fmt.Sprintf(ErrReasonAggregatedUsageExceedThreshold, corev1.ResourceCPU)),
		},
		{
			name:     "filter exceed memory usage",
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("30"),
								corev1.ResourceMemory: resource.MustParse("500Gi"),
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Unschedulable, fmt.Sprintf(ErrReasonUsageExceedThreshold, corev1.ResourceMemory)),
		},
		{
			name: "filter exceed memory usage by custom usage thresholds",
			customUsageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceMemory: 60,
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("30"),
								corev1.ResourceMemory: resource.MustParse("316Gi"),
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Unschedulable, fmt.Sprintf(ErrReasonUsageExceedThreshold, corev1.ResourceMemory)),
		},
		{
			name:     "filter exceed p95 cpu usage by custom usage",
			nodeName: "test-node-1",
			customAggregatedUsage: &extension.CustomAggregatedUsage{
				UsageThresholds: map[corev1.ResourceName]int64{
					corev1.ResourceCPU: 60,
				},
				UsageAggregationType:    slov1alpha1.P95,
				UsageAggregatedDuration: &metav1.Duration{Duration: 5 * time.Minute},
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("30"),
								corev1.ResourceMemory: resource.MustParse("100Gi"),
							},
						},
						AggregatedNodeUsages: []slov1alpha1.AggregatedUsage{
							{
								Duration: metav1.Duration{Duration: 5 * time.Minute},
								Usage: map[slov1alpha1.AggregationType]slov1alpha1.ResourceMap{
									slov1alpha1.P95: {
										ResourceList: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("70"),
											corev1.ResourceMemory: resource.MustParse("256Gi"),
										},
									},
								},
							},
						},
					},
				},
			},
			wantStatus: framework.NewStatus(framework.Unschedulable, fmt.Sprintf(ErrReasonAggregatedUsageExceedThreshold, corev1.ResourceCPU)),
		},
		{
			name: "disable filter exceed memory usage",
			usageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceMemory: 0,
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("30"),
								corev1.ResourceMemory: resource.MustParse("500Gi"),
							},
						},
					},
				},
			},
			wantStatus: nil,
		},
		{
			name:     "prod usage filter is not enabled by default",
			nodeName: "test-node-1",
			usageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    100,
				corev1.ResourceMemory: 100,
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("63"),    // cpu usage: 65.6%
								corev1.ResourceMemory: resource.MustParse("500Gi"), // memory usage: 97.6%
							},
						},
					},
					PodsMetric: []*slov1alpha1.PodMetricInfo{
						{
							Namespace: "default",
							Name:      "prod-pod-1",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("30"),
									corev1.ResourceMemory: resource.MustParse("200Gi"),
								},
							},
						},
						{
							Namespace: "default",
							Name:      "prod-pod-2",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("33"),
									corev1.ResourceMemory: resource.MustParse("300Gi"),
								},
							},
						},
					},
				},
			},
			pods: []*corev1.Pod{
				schedulertesting.MakePod().Namespace("default").Name("prod-pod-1").Priority(extension.PriorityProdValueMax).Obj(),
				schedulertesting.MakePod().Namespace("default").Name("prod-pod-2").Priority(extension.PriorityProdValueMax).Obj(),
			},
			wantStatus: nil,
		},
		{
			name:     "filter prod cpu usage",
			nodeName: "test-node-1",
			usageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    100,
				corev1.ResourceMemory: 100,
			},
			prodUsageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    50,
				corev1.ResourceMemory: 100,
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("63"),    // cpu usage: 65.6%
								corev1.ResourceMemory: resource.MustParse("500Gi"), // memory usage: 97.6%
							},
						},
					},
					PodsMetric: []*slov1alpha1.PodMetricInfo{
						{
							Namespace: "default",
							Name:      "prod-pod-1",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("30"),
									corev1.ResourceMemory: resource.MustParse("200Gi"),
								},
							},
						},
						{
							Namespace: "default",
							Name:      "prod-pod-2",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("33"),
									corev1.ResourceMemory: resource.MustParse("300Gi"),
								},
							},
						},
					},
				},
			},
			pods: []*corev1.Pod{
				schedulertesting.MakePod().Namespace("default").Name("prod-pod-1").Priority(extension.PriorityProdValueMax).Obj(),
				schedulertesting.MakePod().Namespace("default").Name("prod-pod-2").Priority(extension.PriorityProdValueMax).Obj(),
			},
			testPod:    schedulertesting.MakePod().Namespace("default").Name("prod-pod-3").Priority(extension.PriorityProdValueMax).Obj(),
			wantStatus: framework.NewStatus(framework.Unschedulable, fmt.Sprintf(ErrReasonUsageExceedThreshold, corev1.ResourceCPU)),
		},
		{
			name:     "filter prod memory usage",
			nodeName: "test-node-1",
			usageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    100,
				corev1.ResourceMemory: 100,
			},
			prodUsageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    100,
				corev1.ResourceMemory: 50,
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("63"),    // cpu usage: 65.6%
								corev1.ResourceMemory: resource.MustParse("500Gi"), // memory usage: 97.6%
							},
						},
					},
					PodsMetric: []*slov1alpha1.PodMetricInfo{
						{
							Namespace: "default",
							Name:      "prod-pod-1",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("30"),
									corev1.ResourceMemory: resource.MustParse("200Gi"),
								},
							},
						},
						{
							Namespace: "default",
							Name:      "prod-pod-2",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("33"),
									corev1.ResourceMemory: resource.MustParse("300Gi"),
								},
							},
						},
					},
				},
			},
			pods: []*corev1.Pod{
				schedulertesting.MakePod().Namespace("default").Name("prod-pod-1").Priority(extension.PriorityProdValueMax).Obj(),
				schedulertesting.MakePod().Namespace("default").Name("prod-pod-2").Priority(extension.PriorityProdValueMax).Obj(),
			},
			testPod:    schedulertesting.MakePod().Namespace("default").Name("prod-pod-3").Priority(extension.PriorityProdValueMax).Obj(),
			wantStatus: framework.NewStatus(framework.Unschedulable, fmt.Sprintf(ErrReasonUsageExceedThreshold, corev1.ResourceMemory)),
		},
		{
			name:     "filter prod memory usage with custom usage configuration",
			nodeName: "test-node-1",
			usageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    100,
				corev1.ResourceMemory: 100,
			},
			prodUsageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    100,
				corev1.ResourceMemory: 100,
			},
			customProdUsageThresholds: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    100,
				corev1.ResourceMemory: 50,
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("63"),    // cpu usage: 65.6%
								corev1.ResourceMemory: resource.MustParse("500Gi"), // memory usage: 97.6%
							},
						},
					},
					PodsMetric: []*slov1alpha1.PodMetricInfo{
						{
							Namespace: "default",
							Name:      "prod-pod-1",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("30"),
									corev1.ResourceMemory: resource.MustParse("200Gi"),
								},
							},
						},
						{
							Namespace: "default",
							Name:      "prod-pod-2",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("33"),
									corev1.ResourceMemory: resource.MustParse("300Gi"),
								},
							},
						},
					},
				},
			},
			pods: []*corev1.Pod{
				schedulertesting.MakePod().Namespace("default").Name("prod-pod-1").Priority(extension.PriorityProdValueMax).Obj(),
				schedulertesting.MakePod().Namespace("default").Name("prod-pod-2").Priority(extension.PriorityProdValueMax).Obj(),
			},
			testPod:    schedulertesting.MakePod().Namespace("default").Name("prod-pod-3").Priority(extension.PriorityProdValueMax).Obj(),
			wantStatus: framework.NewStatus(framework.Unschedulable, fmt.Sprintf(ErrReasonUsageExceedThreshold, corev1.ResourceMemory)),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var v1beta2args v1beta2.LoadAwareSchedulingArgs
			v1beta2args.FilterExpiredNodeMetrics = pointer.Bool(false)
			if len(tt.usageThresholds) > 0 {
				v1beta2args.UsageThresholds = tt.usageThresholds
			}
			if len(tt.prodUsageThresholds) > 0 {
				v1beta2args.ProdUsageThresholds = tt.prodUsageThresholds
			}
			if tt.aggregated != nil {
				v1beta2args.Aggregated = tt.aggregated
			}
			v1beta2.SetDefaults_LoadAwareSchedulingArgs(&v1beta2args)
			var loadAwareSchedulingArgs config.LoadAwareSchedulingArgs
			err := v1beta2.Convert_v1beta2_LoadAwareSchedulingArgs_To_config_LoadAwareSchedulingArgs(&v1beta2args, &loadAwareSchedulingArgs, nil)
			assert.NoError(t, err)

			koordClientSet := koordfake.NewSimpleClientset()
			koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
			extendHandle := frameworkext.NewExtendedHandle(
				frameworkext.WithKoordinatorClientSet(koordClientSet),
				frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
			)
			proxyNew := frameworkext.PluginFactoryProxy(extendHandle, New)

			cs := kubefake.NewSimpleClientset()
			informerFactory := informers.NewSharedInformerFactory(cs, 0)

			for _, v := range tt.pods {
				_, err = cs.CoreV1().Pods(v.Namespace).Create(context.TODO(), v, metav1.CreateOptions{})
				assert.Nil(t, err)
			}

			nodes := []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: tt.nodeName,
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("96"),
							corev1.ResourceMemory: resource.MustParse("512Gi"),
						},
					},
				},
			}

			if len(tt.customUsageThresholds) > 0 || len(tt.customProdUsageThresholds) > 0 || tt.customAggregatedUsage != nil {
				data, err := json.Marshal(&extension.CustomUsageThresholds{
					UsageThresholds:     tt.customUsageThresholds,
					ProdUsageThresholds: tt.customProdUsageThresholds,
					AggregatedUsage:     tt.customAggregatedUsage,
				})
				if err != nil {
					t.Errorf("failed to marshal, err: %v", err)
				}
				node := nodes[0]
				if len(node.Annotations) == 0 {
					node.Annotations = map[string]string{}
				}
				node.Annotations[extension.AnnotationCustomUsageThresholds] = string(data)
			}

			registeredPlugins := []schedulertesting.RegisterPluginFunc{
				schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
				schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
			}

			snapshot := newTestSharedLister(nil, nodes)
			fh, err := schedulertesting.NewFramework(registeredPlugins, "koord-scheduler",
				frameworkruntime.WithClientSet(cs),
				frameworkruntime.WithInformerFactory(informerFactory),
				frameworkruntime.WithSnapshotSharedLister(snapshot),
			)
			assert.Nil(t, err)

			p, err := proxyNew(&loadAwareSchedulingArgs, fh)
			assert.NotNil(t, p)
			assert.Nil(t, err)

			if tt.nodeMetric != nil {
				_, err = koordClientSet.SloV1alpha1().NodeMetrics().Create(context.TODO(), tt.nodeMetric, metav1.CreateOptions{})
				assert.NoError(t, err)
			}

			informerFactory.Start(context.TODO().Done())
			informerFactory.WaitForCacheSync(context.TODO().Done())

			koordSharedInformerFactory.Start(context.TODO().Done())
			koordSharedInformerFactory.WaitForCacheSync(context.TODO().Done())

			cycleState := framework.NewCycleState()

			nodeInfo, err := snapshot.Get(tt.nodeName)
			assert.NoError(t, err)
			assert.NotNil(t, nodeInfo)

			testPod := tt.testPod
			if testPod == nil {
				testPod = &corev1.Pod{}
			}

			status := p.(*Plugin).Filter(context.TODO(), cycleState, testPod, nodeInfo)
			assert.True(t, tt.wantStatus.Equal(status), "want status: %s, but got %s", tt.wantStatus.Message(), status.Message())
		})
	}
}

func TestScore(t *testing.T) {
	tests := []struct {
		name                    string
		pod                     *corev1.Pod
		assignedPod             []*podAssignInfo
		nodeName                string
		nodeMetric              *slov1alpha1.NodeMetric
		scoreAccordingProdUsage bool
		aggregatedArgs          *v1beta2.LoadAwareSchedulingAggregatedArgs
		wantScore               int64
		wantStatus              *framework.Status
	}{
		{
			name:     "score node with expired nodeMetric",
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now().Add(-180 * time.Second),
					},
				},
			},
			wantScore:  0,
			wantStatus: nil,
		},
		{
			name: "score empty node",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
				},
			},
			wantScore:  90,
			wantStatus: nil,
		},
		{
			name: "score node missing NodeMetrics",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			nodeName:   "test-node-1",
			wantScore:  0,
			wantStatus: nil,
		},
		{
			name: "score load node",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("32"),
								corev1.ResourceMemory: resource.MustParse("10Gi"),
							},
						},
					},
				},
			},
			wantScore:  72,
			wantStatus: nil,
		},
		{
			name: "score load node with p95",
			aggregatedArgs: &v1beta2.LoadAwareSchedulingAggregatedArgs{
				ScoreAggregationType:    slov1alpha1.P95,
				ScoreAggregatedDuration: &metav1.Duration{Duration: 5 * time.Minute},
			},
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("0"),
								corev1.ResourceMemory: resource.MustParse("0Gi"),
							},
						},
						AggregatedNodeUsages: []slov1alpha1.AggregatedUsage{
							{
								Duration: metav1.Duration{Duration: 5 * time.Minute},
								Usage: map[slov1alpha1.AggregationType]slov1alpha1.ResourceMap{
									slov1alpha1.P95: {
										ResourceList: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("32"),
											corev1.ResourceMemory: resource.MustParse("10Gi"),
										},
									},
									slov1alpha1.P99: {
										ResourceList: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("50"),
											corev1.ResourceMemory: resource.MustParse("70Gi"),
										},
									},
								},
							},
						},
					},
				},
			},
			wantScore:  72,
			wantStatus: nil,
		},
		{
			name: "score load node with p95 but have not reported usage",
			aggregatedArgs: &v1beta2.LoadAwareSchedulingAggregatedArgs{
				ScoreAggregationType:    slov1alpha1.P95,
				ScoreAggregatedDuration: &metav1.Duration{Duration: 5 * time.Minute},
			},
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("0"),
								corev1.ResourceMemory: resource.MustParse("0Gi"),
							},
						},
					},
				},
			},
			wantScore:  90,
			wantStatus: nil,
		},
		{
			name: "score load node with p95 but have not reported usage and have assigned pods",
			aggregatedArgs: &v1beta2.LoadAwareSchedulingAggregatedArgs{
				ScoreAggregationType:    slov1alpha1.P95,
				ScoreAggregatedDuration: &metav1.Duration{Duration: 5 * time.Minute},
			},
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			assignedPod: []*podAssignInfo{
				{
					timestamp: time.Now().Add(-10 * time.Minute),
					pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "assigned-pod-1",
						},
						Spec: corev1.PodSpec{
							NodeName: "test-node-1",
							Containers: []corev1.Container{
								{
									Name: "test-container",
									Resources: corev1.ResourceRequirements{
										Limits: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
										Requests: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
									},
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					PodsMetric: []*slov1alpha1.PodMetricInfo{
						{
							Namespace: "default",
							Name:      "assigned-pod-1",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("1"),
									corev1.ResourceMemory: resource.MustParse("1Gi"),
								},
							},
						},
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("0"),
								corev1.ResourceMemory: resource.MustParse("0Gi"),
							},
						},
					},
				},
			},
			wantScore:  81,
			wantStatus: nil,
		},
		{
			name: "score load node with just assigned pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			assignedPod: []*podAssignInfo{
				{
					timestamp: time.Now(),
					pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "assigned-pod-1",
						},
						Spec: corev1.PodSpec{
							NodeName: "test-node-1",
							Containers: []corev1.Container{
								{
									Name: "test-container",
									Resources: corev1.ResourceRequirements{
										Limits: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
										Requests: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
									},
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("32"),
								corev1.ResourceMemory: resource.MustParse("10Gi"),
							},
						},
					},
				},
			},
			wantScore:  63,
			wantStatus: nil,
		},
		{
			name: "score load node with just assigned pod where after updateTime",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			assignedPod: []*podAssignInfo{
				{
					timestamp: time.Now(),
					pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "assigned-pod-1",
						},
						Spec: corev1.PodSpec{
							NodeName: "test-node-1",
							Containers: []corev1.Container{
								{
									Name: "test-container",
									Resources: corev1.ResourceRequirements{
										Limits: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
										Requests: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
									},
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now().Add(-10 * time.Second),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("32"),
								corev1.ResourceMemory: resource.MustParse("10Gi"),
							},
						},
					},
				},
			},
			wantScore:  63,
			wantStatus: nil,
		},
		{
			name: "score load node with just assigned pod where before updateTime",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			assignedPod: []*podAssignInfo{
				{
					timestamp: time.Now().Add(-10 * time.Second),
					pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "assigned-pod-1",
						},
						Spec: corev1.PodSpec{
							NodeName: "test-node-1",
							Containers: []corev1.Container{
								{
									Name: "test-container",
									Resources: corev1.ResourceRequirements{
										Limits: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
										Requests: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
									},
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    resource.MustParse("32"),
								corev1.ResourceMemory: resource.MustParse("10Gi"),
							},
						},
					},
				},
			},
			wantScore:  63,
			wantStatus: nil,
		},
		{
			name: "score batch Pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32(extension.PriorityBatchValueMin),
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									extension.BatchCPU:    resource.MustParse("16000"),
									extension.BatchMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									extension.BatchCPU:    resource.MustParse("16000"),
									extension.BatchMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
				},
			},
			wantScore:  90,
			wantStatus: nil,
		},
		{
			name:                    "score prod Pod",
			scoreAccordingProdUsage: true,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "prod-pod-1",
				},
				Spec: corev1.PodSpec{
					Priority: pointer.Int32(extension.PriorityProdValueMax),
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16000"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16000"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
							},
						},
					},
				},
			},
			assignedPod: []*podAssignInfo{
				{
					timestamp: time.Now(),
					pod: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "default",
							Name:      "assign-prod-pod-1",
						},
						Spec: corev1.PodSpec{
							NodeName: "test-node-1",
							Priority: pointer.Int32(extension.PriorityProdValueMax),
							Containers: []corev1.Container{
								{
									Name: "test-container",
									Resources: corev1.ResourceRequirements{
										Limits: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
										Requests: corev1.ResourceList{
											corev1.ResourceCPU:    resource.MustParse("16"),
											corev1.ResourceMemory: resource.MustParse("32Gi"),
										},
									},
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
					PodsMetric: []*slov1alpha1.PodMetricInfo{
						{
							Namespace: "default",
							Name:      "assign-prod-pod-1",
							PodUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("30"),
									corev1.ResourceMemory: resource.MustParse("100Gi"),
								},
							},
						},
					},
				},
			},
			wantScore:  38,
			wantStatus: nil,
		},
		{
			name: "score request less than limit",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("16"),
									corev1.ResourceMemory: resource.MustParse("32Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("8"),
									corev1.ResourceMemory: resource.MustParse("16Gi"),
								},
							},
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
				},
			},
			wantScore:  88,
			wantStatus: nil,
		},
		{
			name: "score empty pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container",
						},
					},
				},
			},
			nodeName: "test-node-1",
			nodeMetric: &slov1alpha1.NodeMetric{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: slov1alpha1.NodeMetricSpec{
					CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
						ReportIntervalSeconds: pointer.Int64(60),
					},
				},
				Status: slov1alpha1.NodeMetricStatus{
					UpdateTime: &metav1.Time{
						Time: time.Now(),
					},
				},
			},
			wantScore:  99,
			wantStatus: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var v1beta2args v1beta2.LoadAwareSchedulingArgs
			v1beta2args.ScoreAccordingProdUsage = &tt.scoreAccordingProdUsage
			if tt.aggregatedArgs != nil {
				v1beta2args.Aggregated = tt.aggregatedArgs
			}
			v1beta2.SetDefaults_LoadAwareSchedulingArgs(&v1beta2args)
			var loadAwareSchedulingArgs config.LoadAwareSchedulingArgs
			err := v1beta2.Convert_v1beta2_LoadAwareSchedulingArgs_To_config_LoadAwareSchedulingArgs(&v1beta2args, &loadAwareSchedulingArgs, nil)
			assert.NoError(t, err)

			koordClientSet := koordfake.NewSimpleClientset()
			koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
			extendHandle := frameworkext.NewExtendedHandle(
				frameworkext.WithKoordinatorClientSet(koordClientSet),
				frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
			)
			proxyNew := frameworkext.PluginFactoryProxy(extendHandle, New)

			cs := kubefake.NewSimpleClientset()
			informerFactory := informers.NewSharedInformerFactory(cs, 0)

			nodes := []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: tt.nodeName,
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("96"),
							corev1.ResourceMemory: resource.MustParse("512Gi"),
						},
					},
				},
			}

			snapshot := newTestSharedLister(nil, nodes)
			registeredPlugins := []schedulertesting.RegisterPluginFunc{
				schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
				schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
			}
			fh, err := schedulertesting.NewFramework(
				registeredPlugins,
				"koord-scheduler",
				frameworkruntime.WithClientSet(cs),
				frameworkruntime.WithInformerFactory(informerFactory),
				frameworkruntime.WithSnapshotSharedLister(snapshot),
			)
			assert.Nil(t, err)

			if tt.nodeMetric != nil {
				_, err = koordClientSet.SloV1alpha1().NodeMetrics().Create(context.TODO(), tt.nodeMetric, metav1.CreateOptions{})
				assert.NoError(t, err)
			}

			if tt.pod != nil {
				_, err = cs.CoreV1().Pods(tt.pod.Namespace).Create(context.TODO(), tt.pod, metav1.CreateOptions{})
				assert.NoError(t, err)
			}
			for _, v := range tt.assignedPod {
				v.pod.UID = uuid.NewUUID()
				v.pod.ResourceVersion = "111"
				_, err = cs.CoreV1().Pods(v.pod.Namespace).Create(context.TODO(), v.pod, metav1.CreateOptions{})
				assert.NoError(t, err)
			}

			p, err := proxyNew(&loadAwareSchedulingArgs, fh)
			assert.NotNil(t, p)
			assert.Nil(t, err)

			informerFactory.Start(context.TODO().Done())
			informerFactory.WaitForCacheSync(context.TODO().Done())

			koordSharedInformerFactory.Start(context.TODO().Done())
			koordSharedInformerFactory.WaitForCacheSync(context.TODO().Done())

			assignCache := p.(*Plugin).podAssignCache
			for _, v := range tt.assignedPod {
				m := assignCache.podInfoItems[tt.nodeName]
				if m == nil {
					m = map[types.UID]*podAssignInfo{}
					assignCache.podInfoItems[tt.nodeName] = m
				}
				m[v.pod.UID] = v
			}

			cycleState := framework.NewCycleState()

			nodeInfo, err := snapshot.Get(tt.nodeName)
			assert.NoError(t, err)
			assert.NotNil(t, nodeInfo)

			score, status := p.(*Plugin).Score(context.TODO(), cycleState, tt.pod, tt.nodeName)
			assert.Equal(t, tt.wantScore, score)
			assert.True(t, tt.wantStatus.Equal(status), "want status: %s, but got %s", tt.wantStatus.Message(), status.Message())
		})
	}
}
