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

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

var _ handler.EventHandler = &EnqueueRequestForDevice{}

type EnqueueRequestForDevice struct {
	client.Client
	syncContext *SyncContext
}

func (n *EnqueueRequestForDevice) Create(e event.CreateEvent, q workqueue.RateLimitingInterface) {
	device := e.Object.(*schedulingv1alpha1.Device)
	q.Add(reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name: device.Name,
		},
	})
}

func (n *EnqueueRequestForDevice) Update(e event.UpdateEvent, q workqueue.RateLimitingInterface) {
	newDevice := e.ObjectNew.(*schedulingv1alpha1.Device)
	oldDevice := e.ObjectOld.(*schedulingv1alpha1.Device)
	if reflect.DeepEqual(newDevice.Spec, oldDevice.Spec) {
		return
	}
	q.Add(reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name: newDevice.Name,
		},
	})
}

func (n *EnqueueRequestForDevice) Delete(e event.DeleteEvent, q workqueue.RateLimitingInterface) {
	device, ok := e.Object.(*schedulingv1alpha1.Device)
	if !ok {
		return
	}
	if err := n.cleanSyncContext(device); err != nil {
		klog.Errorf("%v for Device %v/%v", err, device.Namespace, device.Name)
	}
	q.Add(reconcile.Request{
		NamespacedName: types.NamespacedName{
			Name: device.Name,
		},
	})
}

func (n *EnqueueRequestForDevice) Generic(e event.GenericEvent, q workqueue.RateLimitingInterface) {
}

func (n *EnqueueRequestForDevice) cleanSyncContext(device *schedulingv1alpha1.Device) error {
	if n.syncContext == nil {
		return fmt.Errorf("failed to cleanup empty sync context")
	}

	// device's name = node's name
	n.syncContext.Delete(util.GenerateNodeKey(&device.ObjectMeta))

	return nil
}
