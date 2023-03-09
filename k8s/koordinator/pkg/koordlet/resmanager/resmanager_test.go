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

package resmanager

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	apiruntime "k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/kubernetes"
	clientsetfake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/component-base/featuregate"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	clientsetalpha1 "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	"github.com/koordinator-sh/koordinator/pkg/features"
	mock_metriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor"
	mock_statesinformer "github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer/mockstatesinformer"
	expireCache "github.com/koordinator-sh/koordinator/pkg/util/cache"
)

var podsResource = schema.GroupVersionResource{Group: "", Version: "v1", Resource: "pods"}

func TestNewResManager(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		ctrl := gomock.NewController(t)
		defer ctrl.Finish()

		scheme := apiruntime.NewScheme()
		kubeClient := &kubernetes.Clientset{}
		crdClient := &clientsetalpha1.Clientset{}
		nodeName := "test-node"
		statesInformer := mock_statesinformer.NewMockStatesInformer(ctrl)
		metricCache := mock_metriccache.NewMockMetricCache(ctrl)

		r := NewResManager(NewDefaultConfig(), scheme, kubeClient, crdClient, nodeName, statesInformer, metricCache, int64(metricsadvisor.NewDefaultConfig().CollectResUsedIntervalSeconds))
		assert.NotNil(t, r)
	})
}

func Test_isFeatureDisabled(t *testing.T) {
	type args struct {
		nodeSLO *slov1alpha1.NodeSLO
		feature featuregate.Feature
	}
	tests := []struct {
		name    string
		args    args
		want    bool
		wantErr bool
	}{
		{
			name:    "throw an error for nil nodeSLO",
			want:    true,
			wantErr: true,
		},
		{
			name: "throw an error for config field is nil",
			args: args{
				nodeSLO: &slov1alpha1.NodeSLO{},
				feature: features.BECPUSuppress,
			},
			want:    true,
			wantErr: true,
		},
		{
			name: "throw an error for unknown feature",
			args: args{
				nodeSLO: &slov1alpha1.NodeSLO{
					Spec: slov1alpha1.NodeSLOSpec{
						ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
							Enable: pointer.BoolPtr(false),
						},
					},
				},
				feature: featuregate.Feature("unknown_feature"),
			},
			want:    true,
			wantErr: true,
		},
		{
			name: "use default config for nil switch",
			args: args{
				nodeSLO: &slov1alpha1.NodeSLO{
					Spec: slov1alpha1.NodeSLOSpec{
						ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{},
					},
				},
				feature: features.BECPUSuppress,
			},
			want:    true,
			wantErr: true,
		},
		{
			name: "parse config successfully",
			args: args{
				nodeSLO: &slov1alpha1.NodeSLO{
					Spec: slov1alpha1.NodeSLOSpec{
						ResourceUsedThresholdWithBE: &slov1alpha1.ResourceThresholdStrategy{
							Enable: pointer.BoolPtr(false),
						},
					},
				},
				feature: features.BECPUSuppress,
			},
			want:    true,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, gotErr := isFeatureDisabled(tt.args.nodeSLO, tt.args.feature)
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.wantErr, gotErr != nil)
		})
	}
}

func Test_EvictPodsIfNotEvicted(t *testing.T) {
	// test data
	pod := createTestPod(apiext.QoSBE, "test_be_pod")
	node := getNode("80", "120G")
	// env
	ctl := gomock.NewController(t)
	defer ctl.Finish()

	fakeRecorder := &FakeRecorder{}
	client := clientsetfake.NewSimpleClientset()
	r := &resmanager{eventRecorder: fakeRecorder, kubeClient: client, podsEvicted: expireCache.NewCacheDefault()}
	stop := make(chan struct{})
	err := r.podsEvicted.Run(stop)
	assert.NoError(t, err)
	defer func() { stop <- struct{}{} }()

	// create pod
	_, err = client.CoreV1().Pods(pod.Namespace).Create(context.TODO(), pod, metav1.CreateOptions{})
	assert.NoError(t, err)

	// evict success
	r.evictPodsIfNotEvicted([]*corev1.Pod{pod}, node, "evict pod first", "")
	getEvictObject, err := client.Tracker().Get(podsResource, pod.Namespace, pod.Name)
	assert.NoError(t, err)
	assert.NotNil(t, getEvictObject, "evictPod Fail", err)
	assert.Equal(t, evictPodSuccess, fakeRecorder.eventReason, "expect evict success event! but got %s", fakeRecorder.eventReason)

	_, found := r.podsEvicted.Get(string(pod.UID))
	assert.True(t, found, "check PodEvicted cached")

	// evict duplication
	fakeRecorder.eventReason = ""
	r.evictPodsIfNotEvicted([]*corev1.Pod{pod}, node, "evict pod duplication", "")
	assert.Equal(t, "", fakeRecorder.eventReason, "check evict duplication, no event send!")
}

func Test_evictPod(t *testing.T) {
	// test data
	pod := createTestPod(apiext.QoSBE, "test_be_pod")
	node := getNode("80", "120G")
	// env
	ctl := gomock.NewController(t)
	defer ctl.Finish()

	mockStatesInformer := mock_statesinformer.NewMockStatesInformer(ctl)
	mockStatesInformer.EXPECT().GetAllPods().Return(getPodMetas([]*corev1.Pod{pod})).AnyTimes()
	mockStatesInformer.EXPECT().GetNode().Return(node).AnyTimes()

	fakeRecorder := &FakeRecorder{}
	client := clientsetfake.NewSimpleClientset()
	r := &resmanager{statesInformer: mockStatesInformer, eventRecorder: fakeRecorder, kubeClient: client}

	// create pod
	_, err := client.CoreV1().Pods(pod.Namespace).Create(context.TODO(), pod, metav1.CreateOptions{})
	assert.NoError(t, err)
	// check pod
	existPod, err := client.CoreV1().Pods(pod.Namespace).Get(context.TODO(), pod.Name, metav1.GetOptions{})
	assert.NotNil(t, existPod, "pod exist in k8s!", err)

	// evict success
	r.evictPod(pod, node, "evict pod first", "")
	getEvictObject, err := client.Tracker().Get(podsResource, pod.Namespace, pod.Name)
	assert.NoError(t, err)
	assert.NotNil(t, getEvictObject, "evictPod Fail", err)

	assert.Equal(t, evictPodSuccess, fakeRecorder.eventReason, "expect evict success event! but got %s", fakeRecorder.eventReason)
}

func createTestPod(qosClass apiext.QoSClass, name string) *corev1.Pod {
	return &corev1.Pod{
		TypeMeta: metav1.TypeMeta{Kind: "Pod"},
		ObjectMeta: metav1.ObjectMeta{
			Name: name,
			UID:  types.UID(name),
			Labels: map[string]string{
				apiext.LabelPodQoS: string(qosClass),
			},
		},
	}
}

func getNode(cpu, memory string) *corev1.Node {
	return &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "test-node",
			Namespace: "default",
		},
		Status: corev1.NodeStatus{
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse(cpu),
				corev1.ResourceMemory: resource.MustParse(memory),
			},
			Capacity: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse(cpu),
				corev1.ResourceMemory: resource.MustParse(memory),
			},
		},
	}
}
