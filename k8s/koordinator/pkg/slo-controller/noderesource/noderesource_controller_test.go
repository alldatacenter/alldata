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

package noderesource

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/clock"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/record"
	"k8s.io/utils/pointer"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

func Test_NodeResourceController_ConfigNotAvaliable(t *testing.T) {
	r := &NodeResourceReconciler{
		cfgCache: &FakeCfgCache{
			available: false,
		},
		Recorder:      &record.FakeRecorder{},
		BESyncContext: SyncContext{},
		Clock:         clock.RealClock{},
	}

	nodeName := "test-node"
	ctx := context.Background()
	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}
	result, err := r.Reconcile(ctx, nodeReq)
	if err != nil {
		t.Fatal(err)
	}
	if result.Requeue != false {
		t.Errorf("failed to reconcile")
	}
}

func Test_NodeResourceController_NodeNotFound(t *testing.T) {
	client := fake.NewClientBuilder().Build()
	r := &NodeResourceReconciler{
		Client: client,
		cfgCache: &FakeCfgCache{
			available: true,
		},
		Recorder:      &record.FakeRecorder{},
		BESyncContext: SyncContext{},
		Clock:         clock.RealClock{},
	}

	nodeName := "test-node"
	ctx := context.Background()
	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}

	result, err := r.Reconcile(ctx, nodeReq)
	assert.NoError(t, err)
	assert.Equal(t, false, result.Requeue)
}

func Test_NodeResourceController_NodeMetricNotExist(t *testing.T) {
	scheme := runtime.NewScheme()
	clientgoscheme.AddToScheme(scheme)
	slov1alpha1.AddToScheme(scheme)

	client := fake.NewClientBuilder().WithScheme(scheme).Build()
	r := &NodeResourceReconciler{
		Client: client,
		cfgCache: &FakeCfgCache{
			available: true,
			cfg: extension.ColocationCfg{
				ColocationStrategy: extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
			},
		},
		Recorder:      &record.FakeRecorder{},
		BESyncContext: SyncContext{},
		Clock:         clock.RealClock{},
	}

	nodeName := "test-node"
	ctx := context.Background()
	r.Client.Create(ctx, &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: nodeName,
		},
	})

	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}

	result, err := r.Reconcile(ctx, nodeReq)
	assert.NoError(t, err)
	assert.Equal(t, false, result.Requeue)
}

func Test_NodeResourceController_ColocationEnabled(t *testing.T) {
	scheme := runtime.NewScheme()
	clientgoscheme.AddToScheme(scheme)
	slov1alpha1.AddToScheme(scheme)
	schedulingv1alpha1.AddToScheme(scheme)
	client := fake.NewClientBuilder().WithScheme(scheme).Build()
	r := &NodeResourceReconciler{
		Client: client,
		cfgCache: &FakeCfgCache{
			available: true,
			cfg: extension.ColocationCfg{
				ColocationStrategy: extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
			},
		},
		Recorder:      &record.FakeRecorder{},
		BESyncContext: NewSyncContext(),
		Clock:         clock.RealClock{},
	}

	nodeName := "test-node"
	ctx := context.Background()
	r.Client.Create(ctx, &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: nodeName,
		},
		Status: corev1.NodeStatus{
			Capacity: corev1.ResourceList{
				corev1.ResourceCPU: *resource.NewQuantity(100, resource.DecimalSI),
			},
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU: *resource.NewQuantity(100, resource.DecimalSI),
			},
		},
	})
	r.Client.Create(ctx, &slov1alpha1.NodeMetric{
		ObjectMeta: metav1.ObjectMeta{
			Name: nodeName,
		},
		Status: slov1alpha1.NodeMetricStatus{
			UpdateTime: makeTime(),
			NodeMetric: &slov1alpha1.NodeMetricInfo{},
		},
	})

	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}

	result, err := r.Reconcile(ctx, nodeReq)
	if err != nil {
		t.Fatal(err)
	}
	if result.Requeue != false {
		t.Errorf("failed to reconcile")
	}

	node := &corev1.Node{}
	err = r.Client.Get(ctx, key, node)
	if err != nil {
		t.Fatal(err)
	}
	batchCPUQ := node.Status.Allocatable[extension.BatchCPU]
	batchcpu, _ := batchCPUQ.AsInt64()
	assert.Equal(t, int64(65000), batchcpu)

	// reset node resources
	r.Clock = clock.NewFakeClock(r.Clock.Now().Add(time.Hour))
	result, err = r.Reconcile(ctx, nodeReq)
	if err != nil {
		t.Fatal(err)
	}
	if result.Requeue != false {
		t.Errorf("failed to reconcile")
	}
	node = &corev1.Node{}
	err = r.Client.Get(ctx, key, node)
	if err != nil {
		t.Fatal(err)
	}
	batchCPUQ = node.Status.Allocatable[extension.BatchCPU]
	batchcpu, _ = batchCPUQ.AsInt64()
	assert.Equal(t, int64(0), batchcpu)
}
