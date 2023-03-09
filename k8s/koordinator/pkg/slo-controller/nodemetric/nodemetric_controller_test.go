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

package nodemetric

import (
	"context"
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/record"
	"k8s.io/client-go/util/workqueue"
	"k8s.io/utils/pointer"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
)

func TestNodeMetricReconciler_getNodeMetricSpec(t *testing.T) {

	oldSpec := &slov1alpha1.NodeMetricSpec{
		CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
			AggregateDurationSeconds: pointer.Int64Ptr(10),
			ReportIntervalSeconds:    pointer.Int64Ptr(20),
		},
	}

	type args struct {
		node    *corev1.Node
		oldSpec *slov1alpha1.NodeMetricSpec
	}
	type fields struct {
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *slov1alpha1.NodeMetricSpec
		wantErr bool
	}{
		{
			name:    "no node",
			fields:  fields{},
			want:    nil,
			wantErr: true,
		},
		{
			name:   "use old spec if the configmap does not exist",
			fields: fields{},
			args: args{
				node:    &corev1.Node{},
				oldSpec: oldSpec.DeepCopy(),
			},
			want:    oldSpec,
			wantErr: false,
		},
		{
			name:   "use default spec if the configmap does not exist and oldSpec is nil",
			fields: fields{},
			args: args{
				node: &corev1.Node{},
			},
			want:    getDefaultSpec(),
			wantErr: false,
		},
		{
			name: "use old spec when unmarshal failed",
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
					extension.ColocationConfigKey: "invalid contents",
				},
			}},
			args: args{
				node:    &corev1.Node{},
				oldSpec: oldSpec.DeepCopy(),
			},
			want:    oldSpec,
			wantErr: false,
		},
		{
			name: "use default spec when the config is empty",
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
					extension.ColocationConfigKey: "{\"enable\":true}",
				},
			}},
			args: args{
				node:    &corev1.Node{},
				oldSpec: oldSpec.DeepCopy(),
			},
			want:    getDefaultSpec(),
			wantErr: false,
		},
		{
			name: "get spec successfully",
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
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":10,\"metricReportIntervalSeconds\":30}",
				},
			}},
			args: args{node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						"xxx": "yyy",
					},
				},
			}},
			want: &slov1alpha1.NodeMetricSpec{
				CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
					AggregateDurationSeconds: pointer.Int64Ptr(10),
					ReportIntervalSeconds:    pointer.Int64Ptr(30),
				},
			},
			wantErr: false,
		},
		{
			name: "get spec failed for invalid cluster config,then use old config",
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
					extension.ColocationConfigKey: "{\"metricAggregateDurationSeconds\":-10}",
				},
			}},
			args:    args{node: &corev1.Node{}},
			want:    getDefaultSpec(),
			wantErr: false,
		},
		{
			name: "get spec successfully with node configs",
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
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":30," +
						"\"cpuReclaimThresholdPercent\":70,\"memoryReclaimThresholdPercent\":80,\"updateTimeThresholdSeconds\":300," +
						"\"degradeTimeMinutes\":5,\"resourceDiffThreshold\":0.1,\"nodeConfigs\":[{\"nodeSelector\":" +
						"{\"matchLabels\":{\"xxx\":\"yyy\"}},\"metricAggregateDurationSeconds\":20}]}",
				},
			}},
			args: args{node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						"xxx": "yyy",
					},
				},
			}},
			want: &slov1alpha1.NodeMetricSpec{
				CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
					AggregateDurationSeconds: pointer.Int64Ptr(20),
					ReportIntervalSeconds:    getDefaultSpec().CollectPolicy.ReportIntervalSeconds,
				},
			},
			wantErr: false,
		},
		{
			name: "get spec successfully with illegal node configs,then use cluster config",
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
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":30," +
						"\"cpuReclaimThresholdPercent\":70,\"memoryReclaimThresholdPercent\":80,\"updateTimeThresholdSeconds\":300," +
						"\"degradeTimeMinutes\":5,\"resourceDiffThreshold\":0.1,\"nodeConfigs\":[{\"nodeSelector\":" +
						"{\"matchLabels\":{\"xxx\":\"yyy\"}},\"metricAggregateDurationSeconds\":-1}]}",
				},
			}},
			args: args{node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						"xxx": "yyy",
					},
				},
			}},
			want: &slov1alpha1.NodeMetricSpec{
				CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
					AggregateDurationSeconds: pointer.Int64Ptr(30),
					ReportIntervalSeconds:    getDefaultSpec().CollectPolicy.ReportIntervalSeconds,
				},
			},
			wantErr: false,
		},
		{
			name: "get spec successfully with first-matched node configs",
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
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":30,\"metricReportIntervalSeconds\":50," +
						"\"nodeConfigs\":[{\"nodeSelector\":{\"matchLabels\":{\"xxx\":\"yyy\"}}," +
						"\"name\":\"xxx-yyy\"," +
						"\"metricAggregateDurationSeconds\":10},{\"nodeSelector\":" +
						"{\"matchLabels\":{\"zzz\":\"zzz\"}},\"name\":\"zzz\",\"metricAggregateDurationSeconds\":90}]}",
				},
			}},
			args: args{node: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Labels: map[string]string{
						"xxx": "yyy",
					},
				},
			}},
			want: &slov1alpha1.NodeMetricSpec{
				CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
					AggregateDurationSeconds: pointer.Int64Ptr(10),
					ReportIntervalSeconds:    pointer.Int64Ptr(50),
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctr, handler := createTestReconciler()

			ctr.Client.Create(context.Background(), tt.fields.configMap)
			handler.SyncCacheIfChanged(tt.fields.configMap)

			got, err := ctr.getNodeMetricSpec(tt.args.node, tt.args.oldSpec)
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.wantErr, err != nil)
		})
	}
}

func TestNodeMetricReconciler_initNodeMetric(t *testing.T) {
	type args struct {
		node       *corev1.Node
		nodeMetric *slov1alpha1.NodeMetric
	}
	type fields struct {
		configMap *corev1.ConfigMap
	}
	tests := []struct {
		name    string
		args    args
		fields  fields
		want    *slov1alpha1.NodeMetricSpec
		wantErr bool
	}{
		{
			name: "throw an error if cannot get the configmap",
			args: args{
				node:       &corev1.Node{},
				nodeMetric: &slov1alpha1.NodeMetric{},
			},
			fields:  fields{},
			want:    getDefaultSpec(),
			wantErr: false,
		},
		{
			name: "get spec successfully",
			args: args{
				node:       &corev1.Node{},
				nodeMetric: &slov1alpha1.NodeMetric{},
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
					extension.ColocationConfigKey: "{\"enable\":true,\"metricAggregateDurationSeconds\":10,\"metricReportIntervalSeconds\":20}",
				},
			}},
			want: &slov1alpha1.NodeMetricSpec{
				CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
					AggregateDurationSeconds: pointer.Int64Ptr(10),
					ReportIntervalSeconds:    pointer.Int64Ptr(20),
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctr, handler := createTestReconciler()
			ctr.Create(context.Background(), tt.fields.configMap)
			handler.SyncCacheIfChanged(tt.fields.configMap)

			err := ctr.initNodeMetric(tt.args.node, tt.args.nodeMetric)
			got := &tt.args.nodeMetric.Spec
			assert.Equal(t, tt.want, got)
			assert.Equal(t, tt.wantErr, err != nil)
		})
	}
}

func Test_Reconcile_CacheNotAvailable(t *testing.T) {
	reconciler, _ := createTestReconciler()

	ctx := context.Background()
	nodeName := "test-node"

	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}
	got, gotErr := reconciler.Reconcile(ctx, nodeReq)
	assert.NoError(t, gotErr)
	assert.Equal(t, reconcile.Result{Requeue: false}, got)
}

func Test_Reconcile_SkipCreateNodeMetricWhenNodeNotExist(t *testing.T) {
	reconciler, handler := createTestReconciler()
	nodeName := "test-node"

	ctx := context.Background()

	//prepare config
	_, configMap := createValidColocationConfigMap(t)
	prepareConfig(t, reconciler, handler, configMap)

	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}
	reconciler.Reconcile(ctx, nodeReq)

	createdNodeMetric := &slov1alpha1.NodeMetric{}
	err := reconciler.Get(ctx, key, createdNodeMetric)
	if !errors.IsNotFound(err) {
		t.Fatal("nodeMetric should not created", err)
	}
}

func Test_Reconcile_RemoveNodeMetricWhenNodeNotExist(t *testing.T) {
	reconciler, handler := createTestReconciler()
	//prepare config
	_, configMap := createValidColocationConfigMap(t)
	prepareConfig(t, reconciler, handler, configMap)

	nodeName := "test-node"
	ctx := context.Background()
	//prepare nodemetric
	nodeMetric := &slov1alpha1.NodeMetric{
		ObjectMeta: metav1.ObjectMeta{
			Name: nodeName,
		},
	}
	err := reconciler.Create(ctx, nodeMetric)
	if err != nil {
		t.Fatalf("failed create nodemetric: %v", err)
	}
	//test reconcile
	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}
	reconciler.Reconcile(ctx, nodeReq)

	createdNodeMetric := &slov1alpha1.NodeMetric{}
	err = reconciler.Get(ctx, key, createdNodeMetric)
	if !errors.IsNotFound(err) {
		t.Fatal("nodeMetric should be removed", err)
	}
}

// 1. Test createNodeMetric
// 2. Test updateNodeMetric by invalid config contents
func Test_CreateNodeMetricAndUpdateUnmarshalError(t *testing.T) {
	reconciler, handler := createTestReconciler()
	nodeName := "test-node"

	//prepare node
	node := newNodeForTest(nodeName)
	ctx := context.Background()
	err := reconciler.Create(ctx, node)
	if err != nil {
		t.Fatalf("failed create node: %v", err)
	}

	//prepare config valid
	cfg, configMap := createValidColocationConfigMap(t)
	prepareConfig(t, reconciler, handler, configMap)

	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}
	reconciler.Reconcile(ctx, nodeReq)

	//Test createNode Metric
	createdNodeMetric := &slov1alpha1.NodeMetric{}
	err = reconciler.Get(ctx, key, createdNodeMetric)
	if err != nil {
		t.Fatal("nodeMetric should created", err)
	}
	wantSpec := &slov1alpha1.NodeMetricSpec{
		CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
			AggregateDurationSeconds: cfg.MetricAggregateDurationSeconds,
			ReportIntervalSeconds:    cfg.MetricReportIntervalSeconds,
		},
	}
	assert.Equal(t, wantSpec, &createdNodeMetric.Spec, "create node metric success by valid config")

	//TEST updateNodeMetric by Invalid Config
	//update config invalid
	invalidConfigMap := createConfigMapUnmashalError()
	err = reconciler.Update(context.TODO(), invalidConfigMap)
	if err != nil {
		t.Fatalf("failed update configmap: %v", err)
	}
	queue := workqueue.NewRateLimitingQueue(workqueue.DefaultControllerRateLimiter())
	handler.Update(event.UpdateEvent{ObjectOld: configMap, ObjectNew: invalidConfigMap}, queue)

	reconciler.Reconcile(ctx, nodeReq)

	updateNodeMetric := &slov1alpha1.NodeMetric{}
	err = reconciler.Get(ctx, key, updateNodeMetric)
	if err != nil {
		t.Fatal("nodeMetric should created", err)
	}
	assert.Equal(t, wantSpec, &updateNodeMetric.Spec, "unmarshal configmap error then use old spec")
}

func Test_UpdateNodeMetricFromConfigmap(t *testing.T) {
	reconciler, handler := createTestReconciler()
	nodeName := "test-node"

	nodeMetric := &slov1alpha1.NodeMetric{
		ObjectMeta: metav1.ObjectMeta{
			Name: nodeName,
		},
	}
	ctx := context.Background()
	err := reconciler.Create(ctx, nodeMetric)
	if err != nil {
		t.Fatalf("failed create nodemetric: %v", err)
	}
	err = reconciler.Create(ctx, newNodeForTest(nodeName))
	if err != nil {
		t.Fatalf("failed create node: %v", err)
	}

	//prepare config cache
	cfg, configMap := createValidColocationConfigMap(t)
	prepareConfig(t, reconciler, handler, configMap)

	//test reconcile
	key := types.NamespacedName{Name: nodeName}
	nodeReq := ctrl.Request{NamespacedName: key}
	reconciler.Reconcile(ctx, nodeReq)

	nodeMetric = &slov1alpha1.NodeMetric{}
	err = reconciler.Get(ctx, key, nodeMetric)
	if err != nil {
		t.Fatal("nodeMetric should created", err)
	}
	assert.Equal(t, &slov1alpha1.NodeMetricSpec{
		CollectPolicy: &slov1alpha1.NodeMetricCollectPolicy{
			AggregateDurationSeconds: cfg.MetricAggregateDurationSeconds,
			ReportIntervalSeconds:    cfg.MetricReportIntervalSeconds,
		},
	}, &nodeMetric.Spec, "update node metric success by valid config")
}

func newNodeForTest(name string) *corev1.Node {
	return &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: name,
		},
	}
}

func createTestReconciler() (*NodeMetricReconciler, *config.ColocationHandlerForConfigMapEvent) {
	scheme := runtime.NewScheme()
	slov1alpha1.AddToScheme(scheme)
	clientgoscheme.AddToScheme(scheme)
	client := fake.NewClientBuilder().WithScheme(scheme).Build()
	reconciler := &NodeMetricReconciler{
		Client: client,
		Scheme: scheme,
	}
	handler := config.NewColocationHandlerForConfigMapEvent(reconciler.Client, *config.NewDefaultColocationCfg(), &record.FakeRecorder{})
	reconciler.cfgCache = handler
	return reconciler, handler
}

func createValidColocationConfigMap(t *testing.T) (*extension.ColocationCfg, *corev1.ConfigMap) {
	policyConfig := &extension.ColocationCfg{
		ColocationStrategy: extension.ColocationStrategy{
			Enable:                         pointer.Bool(true),
			MetricAggregateDurationSeconds: pointer.Int64(60),
			MetricReportIntervalSeconds:    pointer.Int64(180),
		},
	}
	data, err := json.Marshal(policyConfig)
	if err != nil {
		t.Fatal("failed to marshal ColocationCfg", err)
	}
	configMap := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: config.ConfigNameSpace,
			Name:      config.SLOCtrlConfigMap,
		},
		Data: map[string]string{
			extension.ColocationConfigKey: string(data),
		},
	}
	return policyConfig, configMap
}

func createConfigMapUnmashalError() *corev1.ConfigMap {
	configMap := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: config.ConfigNameSpace,
			Name:      config.SLOCtrlConfigMap,
		},
		Data: map[string]string{
			extension.ColocationConfigKey: "invalid contents",
		},
	}
	return configMap
}

func prepareConfig(t *testing.T, reconciler *NodeMetricReconciler, handler *config.ColocationHandlerForConfigMapEvent, configMap *corev1.ConfigMap) {
	err := reconciler.Create(context.TODO(), configMap)
	if err != nil {
		t.Fatalf("failed create configmap: %v", err)
	}

	queue := workqueue.NewRateLimitingQueue(workqueue.DefaultControllerRateLimiter())
	handler.Create(event.CreateEvent{Object: configMap}, queue)
}
