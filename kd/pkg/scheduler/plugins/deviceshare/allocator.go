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

package deviceshare

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/informers"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
)

var defaultAllocatorName = "default"

var allocatorFactories = map[string]AllocatorFactoryFn{
	defaultAllocatorName: NewDefaultAllocator,
}

type AllocatorOptions struct {
	SharedInformerFactory      informers.SharedInformerFactory
	KoordSharedInformerFactory koordinatorinformers.SharedInformerFactory
}

type AllocatorFactoryFn func(options AllocatorOptions) Allocator

type Allocator interface {
	Name() string
	Allocate(nodeName string, pod *corev1.Pod, podRequest corev1.ResourceList, nodeDevice *nodeDevice, preemptibleFreeDevices map[schedulingv1alpha1.DeviceType]deviceResources) (apiext.DeviceAllocations, error)
	Reserve(pod *corev1.Pod, nodeDevice *nodeDevice, allocations apiext.DeviceAllocations)
	Unreserve(pod *corev1.Pod, nodeDevice *nodeDevice, allocations apiext.DeviceAllocations)
}

func NewAllocator(
	name string,
	options AllocatorOptions,
) Allocator {
	factoryFn := allocatorFactories[name]
	if factoryFn == nil {
		factoryFn = NewDefaultAllocator
	}
	return factoryFn(options)
}

func NewDefaultAllocator(
	options AllocatorOptions,
) Allocator {
	return &defaultAllocator{}
}

type defaultAllocator struct {
}

func (a *defaultAllocator) Name() string {
	return defaultAllocatorName
}

func (a *defaultAllocator) Allocate(nodeName string, pod *corev1.Pod, podRequest corev1.ResourceList, nodeDevice *nodeDevice, preemptibleFreeDevices map[schedulingv1alpha1.DeviceType]deviceResources) (apiext.DeviceAllocations, error) {
	return nodeDevice.tryAllocateDevice(podRequest, preemptibleFreeDevices)
}

func (a *defaultAllocator) Reserve(pod *corev1.Pod, nodeDevice *nodeDevice, allocations apiext.DeviceAllocations) {
	nodeDevice.updateCacheUsed(allocations, pod, true)
}

func (a *defaultAllocator) Unreserve(pod *corev1.Pod, nodeDevice *nodeDevice, allocations apiext.DeviceAllocations) {
	nodeDevice.updateCacheUsed(allocations, pod, false)
}
