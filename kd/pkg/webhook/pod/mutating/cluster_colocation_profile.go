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
	"encoding/json"
	"fmt"

	admissionv1 "k8s.io/api/admission/v1"
	corev1 "k8s.io/api/core/v1"
	schedulingv1 "k8s.io/api/scheduling/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/strategicpatch"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"

	configv1alpha1 "github.com/koordinator-sh/koordinator/apis/config/v1alpha1"
	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/util"
	utilclient "github.com/koordinator-sh/koordinator/pkg/util/client"
)

// +kubebuilder:rbac:groups=core,resources=namespaces,verbs=get;list;watch
// +kubebuilder:rbac:groups=config.koordinator.sh,resources=clustercolocationprofiles,verbs=get;list;watch

func (h *PodMutatingHandler) clusterColocationProfileMutatingPod(ctx context.Context, req admission.Request, pod *corev1.Pod) error {
	if req.Operation != admissionv1.Create {
		return nil
	}

	profileList := &configv1alpha1.ClusterColocationProfileList{}
	err := h.Client.List(ctx, profileList, utilclient.DisableDeepCopy)
	if err != nil {
		return err
	}

	if len(profileList.Items) == 0 {
		return nil
	}

	var matchedProfiles []*configv1alpha1.ClusterColocationProfile
	for i := range profileList.Items {
		profile := &profileList.Items[i]
		if profile.Spec.NamespaceSelector != nil {
			matched, err := h.matchNamespaceSelector(ctx, pod.Namespace, profile.Spec.NamespaceSelector)
			if !matched && err == nil {
				continue
			}
		}
		if profile.Spec.Selector != nil {
			matched, err := h.matchObjectSelector(pod, nil, profile.Spec.Selector)
			if !matched && err == nil {
				continue
			}
		}
		matchedProfiles = append(matchedProfiles, profile)
	}
	if len(matchedProfiles) == 0 {
		return nil
	}

	for _, profile := range matchedProfiles {
		err := h.doMutateByColocationProfile(ctx, pod, profile)
		if err != nil {
			return err
		}
		klog.V(4).Infof("mutate Pod %s/%s by clusterColocationProfile %s", pod.Namespace, pod.Name, profile.Name)
	}

	if err = h.mutatePodResourceSpec(pod); err != nil {
		return err
	}

	return nil
}

func (h *PodMutatingHandler) matchNamespaceSelector(ctx context.Context, namespaceName string, namespaceSelector *metav1.LabelSelector) (bool, error) {
	selector, err := util.GetFastLabelSelector(namespaceSelector)
	if err != nil {
		return false, err
	}
	if selector.Empty() {
		return true, nil
	}

	namespace := &corev1.Namespace{}
	err = h.Client.Get(ctx, types.NamespacedName{Name: namespaceName}, namespace)
	if err != nil {
		return false, err
	}
	return selector.Matches(labels.Set(namespace.Labels)), nil
}

func (h *PodMutatingHandler) matchObjectSelector(pod, oldPod *corev1.Pod, objectSelector *metav1.LabelSelector) (bool, error) {
	selector, err := util.GetFastLabelSelector(objectSelector)
	if err != nil {
		return false, err
	}
	if selector.Empty() {
		return true, nil
	}
	matched := selector.Matches(labels.Set(pod.Labels))
	if !matched && oldPod != nil {
		matched = selector.Matches(labels.Set(oldPod.Labels))
	}
	return matched, nil
}

func (h *PodMutatingHandler) doMutateByColocationProfile(ctx context.Context, pod *corev1.Pod, profile *configv1alpha1.ClusterColocationProfile) error {
	if len(profile.Spec.Labels) > 0 {
		if pod.Labels == nil {
			pod.Labels = make(map[string]string)
		}
		for k, v := range profile.Spec.Labels {
			pod.Labels[k] = v
		}
	}

	if len(profile.Spec.Annotations) > 0 {
		if pod.Annotations == nil {
			pod.Annotations = make(map[string]string)
		}
		for k, v := range profile.Spec.Annotations {
			pod.Annotations[k] = v
		}
	}

	if profile.Spec.SchedulerName != "" {
		pod.Spec.SchedulerName = profile.Spec.SchedulerName
	}

	if profile.Spec.QoSClass != "" {
		if pod.Labels == nil {
			pod.Labels = make(map[string]string)
		}
		pod.Labels[extension.LabelPodQoS] = profile.Spec.QoSClass
	}

	if profile.Spec.PriorityClassName != "" {
		priorityClass := &schedulingv1.PriorityClass{}
		err := h.Client.Get(ctx, types.NamespacedName{Name: profile.Spec.PriorityClassName}, priorityClass)
		if err != nil {
			return err
		}
		pod.Spec.PriorityClassName = profile.Spec.PriorityClassName
		pod.Spec.Priority = pointer.Int32(priorityClass.Value)
	}

	if profile.Spec.KoordinatorPriority != nil {
		if pod.Labels == nil {
			pod.Labels = make(map[string]string)
		}
		pod.Labels[extension.LabelPodPriority] = fmt.Sprintf("%d", *profile.Spec.KoordinatorPriority)
	}

	if profile.Spec.Patch.Raw != nil {
		cloneBytes, _ := json.Marshal(pod)
		modified, err := strategicpatch.StrategicMergePatch(cloneBytes, profile.Spec.Patch.Raw, &corev1.Pod{})
		if err != nil {
			return err
		}
		newPod := &corev1.Pod{}
		if err = json.Unmarshal(modified, newPod); err != nil {
			return err
		}
		*pod = *newPod
	}

	return nil
}

func (h *PodMutatingHandler) mutatePodResourceSpec(pod *corev1.Pod) error {
	priorityClass := extension.GetPriorityClass(pod)
	if priorityClass == extension.PriorityNone || priorityClass == extension.PriorityProd {
		return nil
	}

	for _, containers := range [][]corev1.Container{pod.Spec.InitContainers, pod.Spec.Containers} {
		for i := range containers {
			container := &containers[i]
			replaceAndEraseResource(priorityClass, container.Resources.Requests, corev1.ResourceCPU)
			replaceAndEraseResource(priorityClass, container.Resources.Requests, corev1.ResourceMemory)

			replaceAndEraseResource(priorityClass, container.Resources.Limits, corev1.ResourceCPU)
			replaceAndEraseResource(priorityClass, container.Resources.Limits, corev1.ResourceMemory)

			restrictResourceRequestAndLimit(priorityClass, &container.Resources, corev1.ResourceCPU)
			restrictResourceRequestAndLimit(priorityClass, &container.Resources, corev1.ResourceMemory)
		}
	}

	if pod.Spec.Overhead != nil {
		replaceAndEraseResource(priorityClass, pod.Spec.Overhead, corev1.ResourceCPU)
		replaceAndEraseResource(priorityClass, pod.Spec.Overhead, corev1.ResourceMemory)
	}
	return nil
}

func replaceAndEraseResource(priorityClass extension.PriorityClass, resourceList corev1.ResourceList, resourceName corev1.ResourceName) {
	extendResourceName := extension.ResourceNameMap[priorityClass][resourceName]
	if extendResourceName == "" {
		return
	}
	quantity, ok := resourceList[resourceName]
	if ok {
		if resourceName == corev1.ResourceCPU {
			quantity = *resource.NewQuantity(quantity.MilliValue(), resource.DecimalSI)
		}
		resourceList[extendResourceName] = quantity
		delete(resourceList, resourceName)
	}
}

// TODO move the hook to pod mutating for all Pods
func restrictResourceRequestAndLimit(priorityClass extension.PriorityClass, requirements *corev1.ResourceRequirements, resourceName corev1.ResourceName) {
	extendResourceName := extension.ResourceNameMap[priorityClass][resourceName]
	if extendResourceName == "" {
		return
	}
	_, requestOK := requirements.Requests[extendResourceName]
	limitQuantity, limitOK := requirements.Limits[extendResourceName]
	if !requestOK && limitOK {
		if requirements.Requests == nil {
			requirements.Requests = corev1.ResourceList{}
		}
		requirements.Requests[extendResourceName] = limitQuantity
	}
}
