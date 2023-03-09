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
	"fmt"
	"reflect"

	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/util/workqueue"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

var _ handler.EventHandler = &EnqueueRequestForNodeMetric{}

type EnqueueRequestForNodeMetric struct {
	client.Client
	syncContext *SyncContext
}

func (n *EnqueueRequestForNodeMetric) Create(e event.CreateEvent, q workqueue.RateLimitingInterface) {
}

func (n *EnqueueRequestForNodeMetric) Update(e event.UpdateEvent, q workqueue.RateLimitingInterface) {
	newNodeMetric := e.ObjectNew.(*slov1alpha1.NodeMetric)
	oldNodeMetric := e.ObjectOld.(*slov1alpha1.NodeMetric)
	if reflect.DeepEqual(oldNodeMetric.Status, newNodeMetric.Status) {
		return
	}
	q.Add(reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name: newNodeMetric.Name,
		},
	})
}

func (n *EnqueueRequestForNodeMetric) Delete(e event.DeleteEvent, q workqueue.RateLimitingInterface) {
	nodeMetirc, ok := e.Object.(*slov1alpha1.NodeMetric)
	if !ok {
		return
	}
	if err := n.cleanSyncContext(nodeMetirc); err != nil {
		klog.Errorf("%v for NodeMetric %v/%v", err, nodeMetirc.Namespace, nodeMetirc.Name)
	}
	q.Add(reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name: nodeMetirc.Name,
		},
	})
}

func (n *EnqueueRequestForNodeMetric) Generic(e event.GenericEvent, q workqueue.RateLimitingInterface) {
}

func (n *EnqueueRequestForNodeMetric) cleanSyncContext(nodeMetric *slov1alpha1.NodeMetric) error {
	if n.syncContext == nil {
		return fmt.Errorf("failed to cleanup empty sync context")
	}

	// nodeMetric's name = node's name
	n.syncContext.Delete(util.GenerateNodeKey(&nodeMetric.ObjectMeta))

	return nil
}
