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

package mutating

import (
	"context"
	"fmt"
	"reflect"

	admissionv1 "k8s.io/api/admission/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

func (h *PodMutatingHandler) extendedResourceSpecMutatingPod(ctx context.Context, req admission.Request, pod *corev1.Pod) error {
	if req.Operation != admissionv1.Create && req.Operation != admissionv1.Update {
		return nil
	}

	return h.mutateByExtendedResources(pod)
}

func (h *PodMutatingHandler) mutateByExtendedResources(pod *corev1.Pod) error {
	// dump batch-resource of pod.spec.containers[*].resources.requests/limits into ExtendedResourceSpec{}
	extendedResourceSpec := &extension.ExtendedResourceSpec{}
	containersSpec := map[string]extension.ExtendedResourceContainerSpec{}

	// TODO: count init containers and pod overhead
	for i := range pod.Spec.Containers {
		container := &pod.Spec.Containers[i]
		r := getContainerExtendedResourcesRequirement(container, []corev1.ResourceName{
			extension.BatchCPU,
			extension.BatchMemory,
		})
		if r == nil {
			continue
		}
		containersSpec[container.Name] = *r
	}

	// no requirement of specified extended resources
	if len(containersSpec) > 0 {
		extendedResourceSpec.Containers = containersSpec
	}

	// compare annotation values
	spec, err := extension.GetExtendedResourceSpec(pod.Annotations)
	if err != nil {
		return fmt.Errorf("failed to get current extended resource spec, err: %v", err)
	}
	if reflect.DeepEqual(extendedResourceSpec, spec) {
		// if resource requirements not changed, just return
		klog.V(6).Infof("extended resource spec of pod %s/%s unchanged, skip patch the annotation")
		return nil
	}

	// mutate pod annotation
	err = extension.SetExtendedResourceSpec(pod, extendedResourceSpec)
	if err != nil {
		return fmt.Errorf("failed to set extended resource spec, err: %v", err)
	}

	klog.V(4).Infof("mutate Pod %s/%s by ExtendedResources", pod.Namespace, pod.Name)
	return nil
}

func getContainerExtendedResourcesRequirement(container *corev1.Container, resourceNames []corev1.ResourceName) *extension.ExtendedResourceContainerSpec {
	if container == nil {
		return nil
	}
	r := &extension.ExtendedResourceContainerSpec{
		Requests: corev1.ResourceList{},
		Limits:   corev1.ResourceList{},
	}
	for _, name := range resourceNames {
		q, ok := container.Resources.Requests[name]
		if ok {
			r.Requests[name] = q
		}
		q, ok = container.Resources.Limits[name]
		if ok {
			r.Limits[name] = q
		}
	}

	if len(r.Requests) <= 0 && len(r.Limits) <= 0 { // container has no requirement of specified extended resources
		return nil
	}

	return r
}
