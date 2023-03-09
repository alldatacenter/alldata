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
	"reflect"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/tools/record"
	"k8s.io/klog/v2"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/nodemetric"
)

// NodeSLOReconciler reconciles a NodeSLO object
type NodeSLOReconciler struct {
	client.Client
	sloCfgCache SLOCfgCache
	Scheme      *runtime.Scheme
	Recorder    record.EventRecorder
}

func (r *NodeSLOReconciler) initNodeSLO(node *corev1.Node, nodeSLO *slov1alpha1.NodeSLO) error {
	// NOTE: the node and nodeSLO should not be nil
	// get spec from a configmap
	spec, err := r.getNodeSLOSpec(node, nil)
	if err != nil {
		klog.Errorf("initNodeSLO failed to get NodeSLO %s/%s spec, error: %v",
			node.GetNamespace(), node.GetName(), err)
		return err
	}

	nodeSLO.Status = slov1alpha1.NodeSLOStatus{}
	nodeSLO.Spec = *spec
	nodeSLO.SetName(node.GetName())
	nodeSLO.SetNamespace(node.GetNamespace())

	return nil
}

func (r *NodeSLOReconciler) getNodeSLOSpec(node *corev1.Node, oldSpec *slov1alpha1.NodeSLOSpec) (*slov1alpha1.NodeSLOSpec, error) {
	nodeSLOSpec := &slov1alpha1.NodeSLOSpec{}
	if oldSpec != nil {
		nodeSLOSpec = oldSpec.DeepCopy()
	}

	sloCfg := r.sloCfgCache.GetCfgCopy()

	var err error
	nodeSLOSpec.ResourceUsedThresholdWithBE, err = getResourceThresholdSpec(node, &sloCfg.ThresholdCfgMerged)
	if err != nil {
		klog.Warningf("getNodeSLOSpec(): failed to get resourceTheshold spec for node %s,error: %v", node.Name, err)
	}

	// resourceQOS spec
	nodeSLOSpec.ResourceQOSStrategy, err = getResourceQOSSpec(node, &sloCfg.ResourceQOSCfgMerged)
	if err != nil {
		klog.Warningf("getNodeSLOSpec(): failed to get resourceQoS spec for node %s,error: %v", node.Name, err)
	}

	nodeSLOSpec.CPUBurstStrategy, err = getCPUBurstConfigSpec(node, &sloCfg.CPUBurstCfgMerged)
	if err != nil {
		klog.Warningf("getNodeSLOSpec(): failed to get cpuBurstConfig spec for node %s,error: %v", node.Name, err)
	}

	return nodeSLOSpec, nil
}

// +kubebuilder:rbac:groups=core,resources=nodes,verbs=get;list;watch
// +kubebuilder:rbac:groups=core,resources=configmaps,verbs=get;list;watch
// +kubebuilder:rbac:groups=slo.koordinator.sh,resources=nodeslos,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=slo.koordinator.sh,resources=nodeslos/status,verbs=get;update;patch

func (r *NodeSLOReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
	// reconcile for 2 things:
	//   1. ensuring the NodeSLO exists iff the Node exists
	//   2. update NodeSLO Spec
	_ = log.FromContext(ctx, "node-slo-reconciler", req.NamespacedName)

	// if cache unavailable, requeue the req
	if !r.sloCfgCache.IsCfgAvailable() {
		// all nodes would be enqueued once the config is available, so here we just drop the req
		klog.Warningf("slo config is not available, drop the req %v until a valid config is set",
			req.NamespacedName)
		return reconcile.Result{Requeue: false}, nil
	}

	// get the node
	nodeExist := true
	nodeName := req.Name
	node := &corev1.Node{}
	err := r.Client.Get(context.TODO(), req.NamespacedName, node)
	if err != nil {
		if !errors.IsNotFound(err) {
			klog.Errorf("syncNodeSLO failed to find node %v, error: %v", nodeName, err)
			return reconcile.Result{Requeue: true}, err
		}
		nodeExist = false
	}

	// get the nodeSLO
	nodeSLOExist := true
	nodeSLOName := req.Name
	nodeSLO := &slov1alpha1.NodeSLO{}
	err = r.Client.Get(context.TODO(), req.NamespacedName, nodeSLO)
	if err != nil {
		if !errors.IsNotFound(err) {
			klog.Errorf("syncNodeSLO failed to find nodeSLO %v, error: %v", nodeName, err)
			return reconcile.Result{Requeue: true}, err
		}
		nodeSLOExist = false
	}

	// NodeSLO lifecycle management
	if !nodeExist && !nodeSLOExist {
		// do nothing if both does not exist
		return ctrl.Result{}, nil
	} else if !nodeExist {
		// delete CR if only the nodeSLO exists
		err = r.Client.Delete(context.TODO(), nodeSLO)
		if err != nil {
			if errors.IsNotFound(err) {
				klog.Errorf("syncNodeSLO failed to delete nodeSLO %v because error: %v", nodeSLOName, err)
				return reconcile.Result{Requeue: false}, err
			}
			klog.Errorf("syncNodeSLO failed to delete nodeSLO: %v error: %v", nodeSLOName, err)
			return reconcile.Result{Requeue: true}, err
		}
		return ctrl.Result{}, nil
	} else if !nodeSLOExist {
		// create and initialize CR if only the node exists
		if err = r.initNodeSLO(node, nodeSLO); err != nil {
			klog.Errorf("syncNodeSLO failed to init nodeSLO instance: %v", err)
			return reconcile.Result{Requeue: true}, err
		}
		err = r.Client.Create(context.TODO(), nodeSLO)
		if err != nil {
			klog.Errorf("syncNodeSLO failed to create nodeSLO instance: %v", err)
			return reconcile.Result{Requeue: true}, err
		}
	} else {
		// update nodeSLO spec if both exists
		nodeSLOSpec, err := r.getNodeSLOSpec(node, &nodeSLO.Spec)
		if err != nil {
			klog.Errorf("syncNodeSLO failed to get nodeSLO spec: %v", err)
			return reconcile.Result{Requeue: true}, err
		}
		if !reflect.DeepEqual(nodeSLOSpec, &nodeSLO.Spec) {
			nodeSLO.Spec = *nodeSLOSpec
			err = r.Client.Update(context.TODO(), nodeSLO)
			if err != nil {
				klog.Errorf("syncNodeSLO failed to update nodeSLO %v, error: %v", nodeSLOName, err)
				return reconcile.Result{Requeue: true}, err
			}
		}
	}

	klog.V(6).Infof("syncNodeSLO succeed to update nodeSLO %v", nodeSLOName)
	return ctrl.Result{}, nil
}

func Add(mgr ctrl.Manager) error {
	reconciler := NodeSLOReconciler{
		Client:   mgr.GetClient(),
		Scheme:   mgr.GetScheme(),
		Recorder: mgr.GetEventRecorderFor("nodeslo-controller"),
	}
	return reconciler.SetupWithManager(mgr)
}

func (r *NodeSLOReconciler) SetupWithManager(mgr ctrl.Manager) error {
	configMapCacheHandler := NewSLOCfgHandlerForConfigMapEvent(r.Client, DefaultSLOCfg(), r.Recorder)
	r.sloCfgCache = configMapCacheHandler
	return ctrl.NewControllerManagedBy(mgr).
		For(&slov1alpha1.NodeSLO{}).
		Watches(&source.Kind{Type: &corev1.Node{}}, &nodemetric.EnqueueRequestForNode{
			Client: r.Client,
		}).
		Watches(&source.Kind{Type: &corev1.ConfigMap{}}, configMapCacheHandler).
		Complete(r)
}
