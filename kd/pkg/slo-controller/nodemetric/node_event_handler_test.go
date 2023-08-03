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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/util/workqueue"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

func Test_isNodeAllocatableUpdated(t *testing.T) {
	assert := assert.New(t)
	newNode := &corev1.Node{}
	oldNode := &corev1.Node{}
	assert.Equal(false, isNodeAllocatableUpdated(nil, oldNode))
	assert.Equal(false, isNodeAllocatableUpdated(newNode, nil))
	assert.Equal(false, isNodeAllocatableUpdated(nil, nil))
	assert.Equal(false, isNodeAllocatableUpdated(newNode, oldNode))
	newNode.Status.Allocatable = corev1.ResourceList{}
	newNode.Status.Allocatable["test"] = resource.Quantity{}
	assert.Equal(true, isNodeAllocatableUpdated(newNode, oldNode))
}

func Test_EnqueueRequestForNode(t *testing.T) {
	assert := assert.New(t)

	testCases := []struct {
		name      string
		fn        func(handler *EnqueueRequestForNode, q workqueue.RateLimitingInterface)
		hasEvent  bool
		eventName string
	}{
		{
			name: "create node event",
			fn: func(handler *EnqueueRequestForNode, q workqueue.RateLimitingInterface) {
				handler.Create(event.CreateEvent{
					Object: &corev1.Node{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
				}, q)
			},
			hasEvent:  true,
			eventName: "node1",
		},
		{
			name: "create event not node",
			fn: func(handler *EnqueueRequestForNode, q workqueue.RateLimitingInterface) {
				handler.Create(event.CreateEvent{
					Object: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Name: "pod1",
						},
					},
				}, q)
			},
			hasEvent: false,
		},
		{
			name: "delete node event",
			fn: func(handler *EnqueueRequestForNode, q workqueue.RateLimitingInterface) {
				handler.Delete(event.DeleteEvent{
					Object: &corev1.Node{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
				}, q)
			},
			hasEvent:  true,
			eventName: "node1",
		},
		{
			name: "delete event not node",
			fn: func(handler *EnqueueRequestForNode, q workqueue.RateLimitingInterface) {
				handler.Delete(event.DeleteEvent{
					Object: &corev1.Pod{
						ObjectMeta: metav1.ObjectMeta{
							Name: "pod1",
						},
					},
				}, q)
			},
			hasEvent: false,
		},
		{
			name: "update node event",
			fn: func(handler *EnqueueRequestForNode, q workqueue.RateLimitingInterface) {
				handler.Update(event.UpdateEvent{
					ObjectOld: &corev1.Node{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
					ObjectNew: &corev1.Node{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
						Status: corev1.NodeStatus{
							Allocatable: corev1.ResourceList{
								corev1.ResourceCPU: *resource.NewQuantity(500, resource.DecimalSI),
							},
						},
					},
				}, q)
			},
			hasEvent:  true,
			eventName: "node1",
		},
		{
			name: "update node event ignore",
			fn: func(handler *EnqueueRequestForNode, q workqueue.RateLimitingInterface) {
				handler.Update(event.UpdateEvent{
					ObjectOld: &corev1.Node{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
					ObjectNew: &corev1.Node{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
				}, q)
			},
			hasEvent: false,
		},
		{
			name: "generic node event ignore",
			fn: func(handler *EnqueueRequestForNode, q workqueue.RateLimitingInterface) {
				handler.Generic(event.GenericEvent{}, q)
			},
			hasEvent: false,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			queue := workqueue.NewRateLimitingQueue(workqueue.DefaultControllerRateLimiter())
			handler := &EnqueueRequestForNode{}
			tc.fn(handler, queue)
			if tc.hasEvent {
				if queue.Len() < 0 {
					t.Error("expected event but queue is empty")
				}
				e, _ := queue.Get()
				assert.Equal(tc.eventName, e.(reconcile.Request).Name)
			}
			if !tc.hasEvent && queue.Len() > 0 {
				e, _ := queue.Get()
				t.Errorf("unexpeced event: %v", e)
			}
		})
	}

}
