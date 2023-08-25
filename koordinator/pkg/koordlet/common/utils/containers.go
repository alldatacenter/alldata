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

package utils

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/runtime"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

// KillContainers kills containers inside the pod
func KillContainers(pod *corev1.Pod, message string) {
	for _, container := range pod.Spec.Containers {
		containerID, containerStatus, err := util.FindContainerIdAndStatusByName(&pod.Status, container.Name)
		if err != nil {
			klog.Errorf("failed to find container id and status, error: %v", err)
			return
		}

		if containerStatus == nil || containerStatus.State.Running == nil {
			return
		}

		if containerID != "" {
			runtimeType, _, _ := util.ParseContainerId(containerStatus.ContainerID)
			runtimeHandler, err := runtime.GetRuntimeHandler(runtimeType)
			if err != nil || runtimeHandler == nil {
				klog.Errorf("%s, kill container(%s) error! GetRuntimeHandler fail! error: %v", message, containerStatus.ContainerID, err)
				continue
			}
			if err := runtimeHandler.StopContainer(containerID, 0); err != nil {
				klog.Errorf("%s, stop container error! error: %v", message, err)
			}
		} else {
			klog.Warningf("%s, get container ID failed, pod %s/%s containerName %s status: %v", message, pod.Namespace, pod.Name, container.Name, pod.Status.ContainerStatuses)
		}
	}
}
