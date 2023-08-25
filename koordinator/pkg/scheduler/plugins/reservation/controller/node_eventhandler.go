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

package controller

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/indexer"
)

func (c *Controller) onNodeDelete(obj interface{}) {
	var node *corev1.Node
	switch t := obj.(type) {
	case *corev1.Node:
		node = t
	case cache.DeletedFinalStateUnknown:
		node, _ = t.Obj.(*corev1.Node)
	}
	if node == nil {
		return
	}

	reservationInformer := c.koordSharedInformerFactory.Scheduling().V1alpha1().Reservations().Informer()
	objs, err := reservationInformer.GetIndexer().ByIndex(indexer.ReservationStatusNodeNameIndex, node.Name)
	if err != nil {
		klog.Errorf("Failed to index reservation by nodeName %v, err: %v", node.Name, err)
		return
	}
	for _, v := range objs {
		reservation := v.(*schedulingv1alpha1.Reservation)
		c.queue.Add(reservation.Name)
	}
}
