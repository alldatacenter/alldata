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
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/tools/cache"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

func (c *Controller) onPodAdd(obj interface{}) {
	pod, _ := obj.(*corev1.Pod)
	if pod == nil {
		return
	}
	c.updatePod(pod)
	c.enqueueIfPodBoundReservation(pod)
}

func (c *Controller) onPodUpdate(oldObj, newObj interface{}) {
	newPod, _ := newObj.(*corev1.Pod)
	if newPod == nil {
		return
	}
	c.updatePod(newPod)
	c.enqueueIfPodBoundReservation(newPod)
}

func (c *Controller) onPodDelete(obj interface{}) {
	var pod *corev1.Pod
	switch t := obj.(type) {
	case *corev1.Pod:
		pod = t
	case cache.DeletedFinalStateUnknown:
		pod, _ = t.Obj.(*corev1.Pod)
	}
	if pod == nil {
		return
	}
	c.deletePod(pod)
	c.enqueueIfPodBoundReservation(pod)
}

func (c *Controller) enqueueIfPodBoundReservation(pod *corev1.Pod) {
	if pod.Spec.NodeName == "" {
		return
	}

	reservationAllocated, err := apiext.GetReservationAllocated(pod)
	if err != nil || reservationAllocated == nil || reservationAllocated.Name == "" {
		return
	}

	c.queue.Add(reservationAllocated.Name)
}

func (c *Controller) updatePod(pod *corev1.Pod) {
	if pod.Spec.NodeName == "" {
		return
	}

	c.lock.Lock()
	defer c.lock.Unlock()
	pods := c.pods[pod.Spec.NodeName]
	if pods == nil {
		pods = map[types.UID]*corev1.Pod{}
		c.pods[pod.Spec.NodeName] = pods
	}
	pods[pod.UID] = pod
}

func (c *Controller) deletePod(pod *corev1.Pod) {
	if pod.Spec.NodeName == "" {
		return
	}

	c.lock.Lock()
	defer c.lock.Unlock()
	pods := c.pods[pod.Spec.NodeName]
	delete(pods, pod.UID)
	if len(pods) == 0 {
		delete(c.pods, pod.Spec.NodeName)
	}
}

func (c *Controller) getPods(nodeName string) map[types.UID]*corev1.Pod {
	c.lock.Lock()
	defer c.lock.Unlock()

	pods := c.pods[nodeName]
	if len(pods) == 0 {
		return nil
	}
	m := make(map[types.UID]*corev1.Pod)
	for k, v := range pods {
		m[k] = v
	}
	return m
}
