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
	"reflect"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/util/workqueue"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

var _ handler.EventHandler = &EnqueueRequestForNode{}

type EnqueueRequestForNode struct {
	client.Client
}

func (n *EnqueueRequestForNode) Create(e event.CreateEvent, q workqueue.RateLimitingInterface) {
	if node, ok := e.Object.(*corev1.Node); !ok {
		return
	} else {
		q.Add(reconcile.Request{
			NamespacedName: types.NamespacedName{
				Name: node.Name,
			},
		})
	}
}

func (n *EnqueueRequestForNode) Update(e event.UpdateEvent, q workqueue.RateLimitingInterface) {
	newNode, oldNode := e.ObjectNew.(*corev1.Node), e.ObjectOld.(*corev1.Node)
	// TODO, only use for noderesource
	if !isNodeAllocatableUpdated(newNode, oldNode) {
		return
	}
	q.Add(reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name: newNode.Name,
		},
	})
}

func (n *EnqueueRequestForNode) Delete(e event.DeleteEvent, q workqueue.RateLimitingInterface) {
	if node, ok := e.Object.(*corev1.Node); !ok {
		return
	} else {
		q.Add(reconcile.Request{
			NamespacedName: types.NamespacedName{
				Name: node.Name,
			},
		})
	}
}

func (n *EnqueueRequestForNode) Generic(e event.GenericEvent, q workqueue.RateLimitingInterface) {}

// isNodeAllocatableUpdated returns whether the new node's allocatable is different from the old one's
func isNodeAllocatableUpdated(newNode *corev1.Node, oldNode *corev1.Node) bool {
	if newNode == nil || oldNode == nil {
		return false
	}
	return !reflect.DeepEqual(oldNode.Status.Allocatable, newNode.Status.Allocatable)
}
