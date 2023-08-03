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

package util

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/kubernetes/pkg/apis/core/v1/helper/qos"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

// NOTE: functions in this file can be overwritten for extension

// IsPodCfsQuotaNeedUnset checks if the pod-level and container-level cfs_quota should be unset to avoid unnecessary
// throttles.
// https://github.com/koordinator-sh/koordinator/issues/489
func IsPodCfsQuotaNeedUnset(annotations map[string]string) (bool, error) {
	cpusetVal, err := GetCPUSetFromPod(annotations)
	if err != nil {
		return false, err
	}
	return cpusetVal != "", nil
}

// IsPodCPUBurstable checks if cpu burst is allowed for the pod.
func IsPodCPUBurstable(pod *corev1.Pod) bool {
	qosClass := apiext.GetPodQoSClass(pod)
	return qosClass != apiext.QoSLSR && qosClass != apiext.QoSLSE && qosClass != apiext.QoSBE
}

func GetKubeQosClass(pod *corev1.Pod) corev1.PodQOSClass {
	qosClass := pod.Status.QOSClass
	if len(qosClass) <= 0 {
		qosClass = qos.GetPodQOS(pod)
	}
	return qosClass
}
