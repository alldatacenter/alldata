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

package validating

import (
	"fmt"
	"sort"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/util/validation/field"
)

type resourceValidateFunc func(container *corev1.Container) field.ErrorList

type requestLimitValidator struct {
	containers []*corev1.Container
	predicates []resourceValidateFunc
	allErrs    field.ErrorList
}

// NewRequestLimitValidator return a requestLimitValidator object, used to validate the resource
// request of pod with different QoSClass and PriorityClass (including request/limits). Check README
// for more details.
func NewRequestLimitValidator(pod *corev1.Pod) *requestLimitValidator {
	containers := make([]*corev1.Container, 0, len(pod.Spec.Containers))
	for i := range pod.Spec.Containers {
		containers = append(containers, &pod.Spec.Containers[i])
	}
	return &requestLimitValidator{containers: containers}
}

func (v *requestLimitValidator) ExpectRequestLimitShouldEqual(resourceName corev1.ResourceName) *requestLimitValidator {
	v.predicates = append(v.predicates, func(container *corev1.Container) field.ErrorList {
		requestQuantity, ok := container.Resources.Requests[resourceName]
		if !ok {
			return nil
		}
		limitQuantity, ok := container.Resources.Limits[resourceName]
		if !ok {
			return nil
		}
		if requestQuantity.Cmp(limitQuantity) != 0 {
			allErrs := field.ErrorList{}
			errDetail := fmt.Sprintf("resource %s of container %s: quantity of request and limit should be equal",
				resourceName, container.Name)
			fieldPath := field.NewPath("pod.spec.containers", container.Name, "resources")
			allErrs = append(allErrs, field.Forbidden(fieldPath, errDetail))
			return allErrs
		}
		return nil
	})
	return v
}

func (v *requestLimitValidator) ExpectRequestLimitMustEqual(resourceName corev1.ResourceName) *requestLimitValidator {
	v.predicates = append(v.predicates, func(container *corev1.Container) field.ErrorList {
		allErrs := field.ErrorList{}
		fieldPath := field.NewPath("pod.spec.containers", container.Name, "resources")
		requestQuantity, ok := container.Resources.Requests[resourceName]
		if !ok {
			allErrs = append(allErrs, field.NotFound(fieldPath.Child("requests", string(resourceName)), nil))
		}
		limitQuantity, ok := container.Resources.Limits[resourceName]
		if !ok {
			allErrs = append(allErrs, field.NotFound(fieldPath.Child("limits", string(resourceName)), nil))
		}
		if requestQuantity.Cmp(limitQuantity) != 0 {
			errDetail := fmt.Sprintf("resource %s of container %s: quantity of request and limit must be equal",
				resourceName, container.Name)
			allErrs = append(allErrs, field.Forbidden(fieldPath, errDetail))
		}
		return allErrs
	})
	return v
}

func (v *requestLimitValidator) ExpectRequestNoMoreThanLimit(resourceName corev1.ResourceName) *requestLimitValidator {
	v.predicates = append(v.predicates, func(container *corev1.Container) field.ErrorList {
		allErrs := field.ErrorList{}
		fieldPath := field.NewPath("pod.spec.containers", container.Name, "resources")
		requestQuantity, _ := container.Resources.Requests[resourceName]
		limitQuantity, ok := container.Resources.Limits[resourceName]
		if cmp := requestQuantity.Cmp(limitQuantity); ok && cmp > 0 {
			op := "<="
			errDetail := fmt.Sprintf("container %s: resource %s quantity should satisify request %s limit",
				container.Name, resourceName, op)
			allErrs = append(allErrs, field.Forbidden(fieldPath, errDetail))
		}
		return allErrs
	})
	return v
}

type resourceNameAndQuantity struct {
	name     corev1.ResourceName
	quantity resource.Quantity
}

func expectPositive(fieldPath *field.Path, resourceList corev1.ResourceList) field.ErrorList {
	var rqs []resourceNameAndQuantity
	for name, quantity := range resourceList {
		rqs = append(rqs, resourceNameAndQuantity{
			name:     name,
			quantity: quantity,
		})
	}
	sort.Slice(rqs, func(i, j int) bool {
		return rqs[i].name < rqs[j].name
	})

	var allErrs field.ErrorList
	for _, v := range rqs {
		name, quantity := v.name, v.quantity
		if quantity.Value() <= 0 {
			allErrs = append(allErrs, field.Invalid(fieldPath.Child(string(name)), quantity.String(), "quantity must be positive"))
		}
	}
	return allErrs
}

func (v *requestLimitValidator) ExpectPositive() *requestLimitValidator {
	v.predicates = append(v.predicates, func(container *corev1.Container) field.ErrorList {
		allErrs := field.ErrorList{}
		fieldPath := field.NewPath("pod.spec.containers", container.Name, "resources")

		allErrs = append(allErrs, expectPositive(fieldPath.Child("requests"), container.Resources.Requests)...)
		allErrs = append(allErrs, expectPositive(fieldPath.Child("limits"), container.Resources.Limits)...)
		return allErrs
	})
	return v
}

type ContainerFilterFunc func(container *corev1.Container) bool

var containerFilterFns []ContainerFilterFunc

func RegisterContainerFilterFunc(fn ContainerFilterFunc) {
	containerFilterFns = append(containerFilterFns, fn)
}

func (v *requestLimitValidator) Validate() field.ErrorList {
	for _, container := range v.containers {
		filtered := false
		for _, filterFn := range containerFilterFns {
			if !filterFn(container) {
				filtered = true
				break
			}
		}
		if filtered {
			continue
		}

		for _, predicate := range v.predicates {
			if err := predicate(container); err != nil {
				v.allErrs = append(v.allErrs, err...)
			}
		}
	}
	return v.allErrs
}
