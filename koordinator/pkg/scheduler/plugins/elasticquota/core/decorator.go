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

package core

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	schedulingv1alpha1 "sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
)

var decorators []Decorator

func RegisterDecorator(decorator Decorator) {
	decorators = append(decorators, decorator)
}

// Decorator can decorate the Node, Pod and ElasticQuota as needed
// e.g: If you want to append some additional resources into Node, Pod and ElasticQuota
//  such as GPU Resources or modify Spec by GPU model, you can implement the Decorator.
type Decorator interface {
	Init(handle framework.Handle) error
	DecorateNode(node *corev1.Node) *corev1.Node
	DecoratePod(pod *corev1.Pod) *corev1.Pod
	DecorateElasticQuota(quota *schedulingv1alpha1.ElasticQuota) *schedulingv1alpha1.ElasticQuota
}

func RunDecorateInit(handle framework.Handle) error {
	if len(decorators) == 0 {
		return nil
	}
	for _, decorator := range decorators {
		if err := decorator.Init(handle); err != nil {
			return err
		}
	}
	return nil
}

func RunDecorateNode(node *corev1.Node) *corev1.Node {
	if len(decorators) == 0 {
		return node
	}
	node = node.DeepCopy()
	for _, decorator := range decorators {
		node = decorator.DecorateNode(node)
	}
	return node
}

func RunDecoratePod(pod *corev1.Pod) *corev1.Pod {
	if len(decorators) == 0 {
		return pod
	}
	pod = pod.DeepCopy()
	for _, decorator := range decorators {
		pod = decorator.DecoratePod(pod)
	}
	return pod
}

func RunDecorateElasticQuota(quota *schedulingv1alpha1.ElasticQuota) *schedulingv1alpha1.ElasticQuota {
	if len(decorators) == 0 {
		return quota
	}
	quota = quota.DeepCopy()
	for _, decorator := range decorators {
		quota = decorator.DecorateElasticQuota(quota)
	}
	return quota
}
