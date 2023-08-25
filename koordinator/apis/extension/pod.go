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

package extension

import (
	"encoding/json"

	corev1 "k8s.io/api/core/v1"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

const (
	AnnotationPodCPUBurst = DomainPrefix + "cpuBurst"

	AnnotationPodMemoryQoS = DomainPrefix + "memoryQOS"
)

func GetPodCPUBurstConfig(pod *corev1.Pod) (*slov1alpha1.CPUBurstConfig, error) {
	if pod == nil || pod.Annotations == nil {
		return nil, nil
	}
	annotation, exist := pod.Annotations[AnnotationPodCPUBurst]
	if !exist {
		return nil, nil
	}
	cpuBurst := slov1alpha1.CPUBurstConfig{}

	err := json.Unmarshal([]byte(annotation), &cpuBurst)
	if err != nil {
		return nil, err
	}
	return &cpuBurst, nil
}

func GetPodMemoryQoSConfig(pod *corev1.Pod) (*slov1alpha1.PodMemoryQOSConfig, error) {
	if pod == nil || pod.Annotations == nil {
		return nil, nil
	}
	value, exist := pod.Annotations[AnnotationPodMemoryQoS]
	if !exist {
		return nil, nil
	}
	cfg := slov1alpha1.PodMemoryQOSConfig{}
	err := json.Unmarshal([]byte(value), &cfg)
	if err != nil {
		return nil, err
	}
	return &cfg, nil
}
