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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/util/workqueue"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

func makeTime() *metav1.Time {
	n := metav1.Now()
	return &n
}

func Test_EnqueueRequestForNodeMetricMetric(t *testing.T) {
	assert := assert.New(t)

	testCases := []struct {
		name      string
		fn        func(handler *EnqueueRequestForNodeMetric, q workqueue.RateLimitingInterface)
		hasEvent  bool
		eventName string
	}{
		{
			name: "create nodemetric event",
			fn: func(handler *EnqueueRequestForNodeMetric, q workqueue.RateLimitingInterface) {
				handler.Create(event.CreateEvent{}, q)
			},
			hasEvent: false,
		},
		{
			name: "delete nodemetric event",
			fn: func(handler *EnqueueRequestForNodeMetric, q workqueue.RateLimitingInterface) {
				handler.Delete(event.DeleteEvent{
					Object: &slov1alpha1.NodeMetric{
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
			name: "delete event not nodemetric",
			fn: func(handler *EnqueueRequestForNodeMetric, q workqueue.RateLimitingInterface) {
				handler.Delete(event.DeleteEvent{
					Object: &corev1.Node{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
				}, q)
			},
			hasEvent: false,
		},
		{
			name: "generic event ignore",
			fn: func(handler *EnqueueRequestForNodeMetric, q workqueue.RateLimitingInterface) {
				handler.Generic(event.GenericEvent{}, q)
			},
			hasEvent: false,
		},
		{
			name: "update nodemetric event",
			fn: func(handler *EnqueueRequestForNodeMetric, q workqueue.RateLimitingInterface) {
				handler.Update(event.UpdateEvent{
					ObjectOld: &slov1alpha1.NodeMetric{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
					ObjectNew: &slov1alpha1.NodeMetric{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
						Status: slov1alpha1.NodeMetricStatus{
							UpdateTime: makeTime(),
						},
					},
				}, q)
			},
			hasEvent:  true,
			eventName: "node1",
		},
		{
			name: "update nodemetric event ignore",
			fn: func(handler *EnqueueRequestForNodeMetric, q workqueue.RateLimitingInterface) {
				handler.Update(event.UpdateEvent{
					ObjectOld: &slov1alpha1.NodeMetric{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
					ObjectNew: &slov1alpha1.NodeMetric{
						ObjectMeta: metav1.ObjectMeta{
							Name: "node1",
						},
					},
				}, q)
			},
			hasEvent: false,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			queue := workqueue.NewRateLimitingQueue(workqueue.DefaultControllerRateLimiter())
			handler := &EnqueueRequestForNodeMetric{}
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
