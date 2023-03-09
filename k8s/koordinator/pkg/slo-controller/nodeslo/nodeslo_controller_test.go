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

package nodeslo

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/record"
	"k8s.io/utils/pointer"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func TestNodeSLOReconciler_initNodeSLO(t *testing.T) {
	scheme := runtime.NewScheme()
	clientgoscheme.AddToScheme(scheme)
	slov1alpha1.AddToScheme(scheme)
	testingResourceThresholdStrategy := util.DefaultResourceThresholdStrategy()
	testingResourceThresholdStrategy.CPUSuppressThresholdPercent = pointer.Int64Ptr(60)
	testingResourceQOSStrategyOld := &slov1alpha1.ResourceQOSStrategy{
		BEClass: &slov1alpha1.ResourceQOS{
			CPUQOS: &slov1alpha1.CPUQOSCfg{
				CPUQOS: slov1alpha1.CPUQOS{
					GroupIdentity: pointer.Int64Ptr(0),
				},
			},
		},
	}
	testingResourceQOSStrategy := &slov1alpha1.ResourceQOSStrategy{
		BEClass: &slov1alpha1.ResourceQOS{
			CPUQOS: &slov1alpha1.CPUQOSCfg{
				CPUQOS: slov1alpha1.CPUQOS{
					GroupIdentity: pointer.Int64Ptr(0),
				},
			},
		},
	}
	type args struct {
		node    *corev1.Node
		nodeSLO *slov1alpha1.NodeSLO
	}
	type fields struct {
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name    string
		args    args
		fields  fields
		want    *slov1alpha1.NodeSLOSpec
		wantErr bool
	}{
		{
			name: "throw an error if no slo configmap",
			args: args{
				node:    &corev1.Node{},
				nodeSLO: &slov1alpha1.NodeSLO{},
			},
			fields: fields{},
			want: &slov1alpha1.NodeSLOSpec{
				ResourceUsedThresholdWithBE: util.DefaultResourceThresholdStrategy(),
				ResourceQOSStrategy:         &slov1alpha1.ResourceQOSStrategy{},
				CPUBurstStrategy:            util.DefaultCPUBurstStrategy(),
			},
			wantErr: false,
		},
		{
			name: "unmarshal failed, use the default",
			args: args{
				node:    &corev1.Node{},
				nodeSLO: &slov1alpha1.NodeSLO{},
			},
			fields: fields{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      config.SLOCtrlConfigMap,
					Namespace: config.ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ResourceThresholdConfigKey: "{\"clusterStrategy\":{\"invalidField\",\"cpuSuppressThresholdPercent\":60}}",
					extension.ResourceQOSConfigKey:       "{\"clusterStrategy\":{\"invalidField\"}}",
				},
			}},
			want: &slov1alpha1.NodeSLOSpec{
				ResourceUsedThresholdWithBE: util.DefaultResourceThresholdStrategy(),
				ResourceQOSStrategy:         &slov1alpha1.ResourceQOSStrategy{},
				CPUBurstStrategy:            util.DefaultCPUBurstStrategy(),
			},
			wantErr: false,
		},
		{
			name: "get spec successfully",
			args: args{
				node:    &corev1.Node{},
				nodeSLO: &slov1alpha1.NodeSLO{},
			},
			fields: fields{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      config.SLOCtrlConfigMap,
					Namespace: config.ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ResourceThresholdConfigKey: "{\"clusterStrategy\":{\"enable\":false,\"cpuSuppressThresholdPercent\":60}}",
				},
			}},
			want: &slov1alpha1.NodeSLOSpec{
				ResourceUsedThresholdWithBE: testingResourceThresholdStrategy,
				ResourceQOSStrategy:         &slov1alpha1.ResourceQOSStrategy{},
				CPUBurstStrategy:            util.DefaultCPUBurstStrategy(),
			},
			wantErr: false,
		},
		{
			name: "get spec successfully 1",
			args: args{
				node:    &corev1.Node{},
				nodeSLO: &slov1alpha1.NodeSLO{},
			},
			fields: fields{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      config.SLOCtrlConfigMap,
					Namespace: config.ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ResourceThresholdConfigKey: "{\"clusterStrategy\":{\"enable\":false,\"cpuSuppressThresholdPercent\":60}}",
					extension.ResourceQOSConfigKey: `
{
  "clusterStrategy": {
    "beClass": {
      "cpuQOS": {
        "groupIdentity": 0
      }
    }
  }
}
`,
				},
			}},
			want: &slov1alpha1.NodeSLOSpec{
				ResourceUsedThresholdWithBE: testingResourceThresholdStrategy,
				ResourceQOSStrategy:         testingResourceQOSStrategy,
				CPUBurstStrategy:            util.DefaultCPUBurstStrategy(),
			},
			wantErr: false,
		},
		{
			name: "get spec successfully from old qos config",
			args: args{
				node:    &corev1.Node{},
				nodeSLO: &slov1alpha1.NodeSLO{},
			},
			fields: fields{configMap: &corev1.ConfigMap{
				TypeMeta: metav1.TypeMeta{
					Kind:       "ConfigMap",
					APIVersion: "v1",
				},
				ObjectMeta: metav1.ObjectMeta{
					Name:      config.SLOCtrlConfigMap,
					Namespace: config.ConfigNameSpace,
				},
				Data: map[string]string{
					extension.ResourceThresholdConfigKey: "{\"clusterStrategy\":{\"enable\":false,\"cpuSuppressThresholdPercent\":60}}",
					extension.ResourceQOSConfigKey: `
{
  "clusterStrategy": {
    "beClass": {
      "cpuQOS": {
        "groupIdentity": 0
      }
    }
  }
}
`,
				},
			}},
			want: &slov1alpha1.NodeSLOSpec{
				ResourceUsedThresholdWithBE: testingResourceThresholdStrategy,
				ResourceQOSStrategy:         testingResourceQOSStrategyOld,
				CPUBurstStrategy:            util.DefaultCPUBurstStrategy(),
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctr := &NodeSLOReconciler{Client: fake.NewClientBuilder().WithScheme(scheme).Build()}
			configMapCacheHandler := NewSLOCfgHandlerForConfigMapEvent(ctr.Client, DefaultSLOCfg(), &record.FakeRecorder{})
			ctr.sloCfgCache = configMapCacheHandler
			if tt.fields.configMap != nil {
				ctr.Client.Create(context.Background(), tt.fields.configMap)
				configMapCacheHandler.SyncCacheIfChanged(tt.fields.configMap)
			}

			err := ctr.initNodeSLO(tt.args.node, tt.args.nodeSLO)
			got := &tt.args.nodeSLO.Spec
			if (err != nil) != tt.wantErr {
				t.Errorf("NodeSLOReconciler.initNodeSLO() gotErr = %v, wantErr %v", err, tt.wantErr)
			}
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestNodeSLOReconciler_Reconcile(t *testing.T) {
	// initial variants
	scheme := runtime.NewScheme()
	clientgoscheme.AddToScheme(scheme)
	slov1alpha1.AddToScheme(scheme)
	r := &NodeSLOReconciler{
		Client: fake.NewClientBuilder().WithScheme(scheme).Build(),
		Scheme: scheme,
	}

	configMapCacheHandler := NewSLOCfgHandlerForConfigMapEvent(r.Client, DefaultSLOCfg(), &record.FakeRecorder{})
	r.sloCfgCache = configMapCacheHandler

	testingNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node",
		},
	}
	testingConfigMap := &corev1.ConfigMap{
		TypeMeta: metav1.TypeMeta{
			Kind:       "ConfigMap",
			APIVersion: "v1",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      config.SLOCtrlConfigMap,
			Namespace: config.ConfigNameSpace,
		},
		Data: map[string]string{
			extension.ResourceThresholdConfigKey: "{\"clusterStrategy\":{\"enable\":true,\"cpuSuppressThresholdPercent\":60}}",
			extension.ResourceQOSConfigKey: `
{
  "clusterStrategy": {
    "beClass": {
      "cpuQOS": {
        "groupIdentity": 0
      }
    }
  }
}
`,
			extension.CPUBurstConfigKey: "{\"clusterStrategy\":{\"cfsQuotaBurstPeriodSeconds\":60}}",
		},
	}
	testingResourceThresholdStrategy := util.DefaultResourceThresholdStrategy()
	testingResourceThresholdStrategy.Enable = pointer.BoolPtr(true)
	testingResourceThresholdStrategy.CPUSuppressThresholdPercent = pointer.Int64Ptr(60)
	testingResourceQOSStrategy := &slov1alpha1.ResourceQOSStrategy{
		BEClass: &slov1alpha1.ResourceQOS{
			CPUQOS: &slov1alpha1.CPUQOSCfg{
				CPUQOS: slov1alpha1.CPUQOS{
					GroupIdentity: pointer.Int64Ptr(0),
				},
			},
		},
	}

	testingCPUBurstStrategy := util.DefaultCPUBurstStrategy()
	testingCPUBurstStrategy.CFSQuotaBurstPeriodSeconds = pointer.Int64Ptr(60)

	nodeSLOSpec := &slov1alpha1.NodeSLOSpec{
		ResourceUsedThresholdWithBE: testingResourceThresholdStrategy,
		ResourceQOSStrategy:         testingResourceQOSStrategy,
		CPUBurstStrategy:            testingCPUBurstStrategy,
	}
	nodeReq := ctrl.Request{NamespacedName: types.NamespacedName{Name: testingNode.Name}}
	// the NodeSLO does not exists before getting created
	nodeSLO := &slov1alpha1.NodeSLO{}
	err := r.Client.Get(context.TODO(), nodeReq.NamespacedName, nodeSLO)
	if !errors.IsNotFound(err) {
		t.Errorf("the testing NodeSLO should not exist before getting created, err: %s", err)
	}

	// test cfg not exist, use default config
	result, err := r.Reconcile(context.TODO(), nodeReq)
	assert.NoError(t, err)
	assert.Equal(t, reconcile.Result{Requeue: false}, result, "check_result")
	assert.Equal(t, true, r.sloCfgCache.IsCfgAvailable())

	// throw an error if the configmap does not exist
	err = r.Client.Create(context.TODO(), testingNode)
	assert.NoError(t, err)
	_, err = r.Reconcile(context.TODO(), nodeReq)
	assert.NoError(t, err)
	// create and init a NodeSLO cr if the Node and the configmap exists
	err = r.Client.Create(context.TODO(), testingConfigMap)
	assert.NoError(t, err)
	configMapCacheHandler.SyncCacheIfChanged(testingConfigMap)
	_, err = r.Reconcile(context.TODO(), nodeReq)
	assert.NoError(t, err)
	nodeSLO = &slov1alpha1.NodeSLO{}
	err = r.Client.Get(context.TODO(), nodeReq.NamespacedName, nodeSLO)
	assert.NoError(t, err)
	assert.Equal(t, *nodeSLOSpec, nodeSLO.Spec)
	// delete the NodeSLO cr if the node no longer exists
	err = r.Delete(context.TODO(), testingNode)
	assert.NoError(t, err)
	_, err = r.Reconcile(context.TODO(), nodeReq)
	assert.NoError(t, err)
	nodeSLO = &slov1alpha1.NodeSLO{}
	err = r.Client.Get(context.TODO(), nodeReq.NamespacedName, nodeSLO)
	if !errors.IsNotFound(err) {
		t.Errorf("the testing NodeSLO should not exist after the Node is deleted, err: %s", err)
	}
}
