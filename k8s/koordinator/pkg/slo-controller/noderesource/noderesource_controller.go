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

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/fields"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/clock"
	"k8s.io/client-go/tools/record"
	"k8s.io/klog/v2"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/source"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
)

const (
	disableInConfig          string = "DisableInConfig"
	degradeByKoordController string = "DegradeByKoordController"
)

type NodeResourceReconciler struct {
	client.Client
	Recorder       record.EventRecorder
	Scheme         *runtime.Scheme
	Clock          clock.Clock
	BESyncContext  SyncContext
	GPUSyncContext SyncContext
	cfgCache       config.ColocationCfgCache
}

// +kubebuilder:rbac:groups=core,resources=configmaps,verbs=get;list;watch
// +kubebuilder:rbac:groups=core,resources=nodes,verbs=get;list;watch;patch
// +kubebuilder:rbac:groups=core,resources=nodes/status,verbs=get;update;patch
// +kubebuilder:rbac:groups=core,resources=pods,verbs=get;list;watch
// +kubebuilder:rbac:groups=scheduling.koordinator.sh,resources=devices,verbs=get;list;watch
// +kubebuilder:rbac:groups=slo.koordinator.sh,resources=nodemetrics,verbs=get;list;watch
// +kubebuilder:rbac:groups=slo.koordinator.sh,resources=nodeslos,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=slo.koordinator.sh,resources=nodeslos/status,verbs=get;update;patch

func (r *NodeResourceReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	if !r.cfgCache.IsCfgAvailable() {
		klog.Warningf("colocation config is not available")
		return ctrl.Result{Requeue: false}, nil
	}

	node := &corev1.Node{}
	if err := r.Client.Get(context.TODO(), req.NamespacedName, node); err != nil {
		if errors.IsNotFound(err) {
			// skip non-existing node and return no error to forget the request
			klog.V(3).Infof("skip for node %v not found", req.Name)
			return ctrl.Result{Requeue: false}, nil
		}
		klog.Errorf("failed to get node %v, error: %v", req.Name, err)
		return ctrl.Result{Requeue: true}, err
	}

	if r.isColocationCfgDisabled(node) {
		klog.Infof("colocation for node %v is disabled, reset BE resource", req.Name)
		if err := r.resetNodeBEResource(node, disableInConfig, "node colocation is disabled in Config"); err != nil {
			return ctrl.Result{Requeue: true}, err
		}
		return ctrl.Result{Requeue: false}, nil
	}

	nodeMetric := &slov1alpha1.NodeMetric{}
	if err := r.Client.Get(context.TODO(), req.NamespacedName, nodeMetric); err != nil {
		if errors.IsNotFound(err) {
			// skip non-existing node metric and return no error to forget the request
			klog.V(3).Infof("skip for nodemetric %v not found", req.Name)
			return ctrl.Result{Requeue: false}, nil
		}
		klog.Errorf("failed to get nodemetric %v, error: %v", req.Name, err)
		return ctrl.Result{Requeue: true}, err
	}

	if r.isDegradeNeeded(nodeMetric, node) {
		klog.Warningf("node %v need degradation, reset BE resource", req.Name)
		if err := r.resetNodeBEResource(node, degradeByKoordController, "degrade node resource because of abnormal NodeMetric"); err != nil {
			return ctrl.Result{Requeue: true}, err
		}
		return ctrl.Result{Requeue: false}, nil
	}

	podList := &corev1.PodList{}
	if err := r.Client.List(context.TODO(), podList, &client.ListOptions{
		FieldSelector: fields.OneTermEqualSelector("spec.nodeName", node.Name),
	}); err != nil {
		return ctrl.Result{Requeue: true}, err
	}

	beResource := r.calculateBEResource(node, podList, nodeMetric)

	if err := r.updateNodeBEResource(node, beResource); err != nil {
		klog.Errorf("failed to update node %v BE resource, error: %v", node.Name, err)
		return ctrl.Result{Requeue: true}, err
	}
	device := &schedulingv1alpha1.Device{}
	err := r.Client.Get(context.TODO(), types.NamespacedName{Name: node.Name, Namespace: node.Namespace}, device)
	if err != nil {
		if !errors.IsNotFound(err) {
			klog.Errorf("failed to get device %s, err: %v", node.Name, err)
			return ctrl.Result{Requeue: true}, err
		}
		return ctrl.Result{Requeue: false}, nil
	}
	if err := r.updateGPUNodeResource(node, device); err != nil {
		klog.Errorf("failed to update node %v gpu resource and label, error: %v", node.Name, err)
		return ctrl.Result{Requeue: true}, err
	}
	if err := r.updateGPUDriverAndModel(node, device); err != nil {
		klog.Errorf("failed to update node %v gpu model and driver, error: %v", node.Name, err)
		return ctrl.Result{Requeue: true}, err
	}

	return ctrl.Result{Requeue: false}, nil
}

func Add(mgr ctrl.Manager) error {
	reconciler := &NodeResourceReconciler{
		Recorder:       mgr.GetEventRecorderFor("noderesource-controller"),
		Client:         mgr.GetClient(),
		Scheme:         mgr.GetScheme(),
		BESyncContext:  NewSyncContext(),
		GPUSyncContext: NewSyncContext(),
		Clock:          clock.RealClock{},
	}
	return reconciler.SetupWithManager(mgr)
}

func (r *NodeResourceReconciler) SetupWithManager(mgr ctrl.Manager) error {
	handler := config.NewColocationHandlerForConfigMapEvent(r.Client, *config.NewDefaultColocationCfg(), r.Recorder)
	r.cfgCache = handler
	return ctrl.NewControllerManagedBy(mgr).
		For(&corev1.Node{}).
		Watches(&source.Kind{Type: &slov1alpha1.NodeMetric{}}, &EnqueueRequestForNodeMetric{syncContext: &r.BESyncContext}).
		Watches(&source.Kind{Type: &schedulingv1alpha1.Device{}}, &EnqueueRequestForDevice{syncContext: &r.GPUSyncContext}).
		Watches(&source.Kind{Type: &corev1.ConfigMap{}}, handler).
		Complete(r)
}
