// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package controller

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"strings"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	"k8s.io/client-go/kubernetes"
)

var errNotReady = errors.New("target pod is not ready")

func (s *diagnotorService) createAgent(ctx context.Context, client kubernetes.Interface, clusterName, namespace, podName string, labels map[string]string) (*corev1.Pod, *corev1.Pod, error) {
	pod, err := client.CoreV1().Pods(namespace).Get(ctx, podName, metav1.GetOptions{})
	if err != nil {
		return nil, nil, err
	}
	if pod == nil {
		return nil, nil, errors.New("pod not found")
	}
	if len(pod.Spec.NodeName) <= 0 || pod.Status.Phase != corev1.PodRunning {
		return nil, nil, errNotReady
	}

	container := findMainContainer(pod)
	if container == nil {
		return nil, nil, fmt.Errorf("not contains container")
	}
	containerID, err := getContainerIDByName(pod, container.Name)
	if err != nil {
		return nil, nil, err
	}

retry:
	agentPod := s.newAgentPod(pod, container, containerID, clusterName, labels)
	agent, err := client.CoreV1().Pods(namespace).Create(ctx, agentPod, metav1.CreateOptions{})
	if err != nil {
		if strings.Contains(err.Error(), "already exists") {
			agent, err := client.CoreV1().Pods(namespace).Get(ctx, agentPod.ObjectMeta.Name, metav1.GetOptions{})
			if err != nil {
				if strings.Contains(err.Error(), "not found") {
					goto retry
				}
				return nil, nil, err
			} else if agent.Status.Phase == corev1.PodSucceeded || agent.Status.Phase == corev1.PodFailed {
				err := client.CoreV1().Pods(namespace).Delete(ctx, agentPod.ObjectMeta.Name, metav1.DeleteOptions{})
				if err != nil && !strings.Contains(err.Error(), "not found") {
					return nil, nil, err
				}
				goto retry
			}
		}
		return nil, nil, err
	}
	return agent, pod, nil
}

const (
	maxPodNameMaxLength = 253
	agentPodNameSuffix  = "-diagnotor"
	agentContainerName  = "diagnotor-agent"
	targetPodIPKey      = "target-pod-ip"
)

func getAgentPodName(name string) string {
	if len(name) > maxPodNameMaxLength-len(agentPodNameSuffix) {
		return name[:maxPodNameMaxLength-len(agentPodNameSuffix)] + agentPodNameSuffix
	}
	return name + agentPodNameSuffix
}

func (s *diagnotorService) newAgentPod(target *corev1.Pod, targetContainer *corev1.Container, targetContainerID, clusterName string, ls map[string]string) *corev1.Pod {
	labels := map[string]string{
		"erda-diagnotor": "true",
		"target-pod":     target.Name,
		targetPodIPKey:   target.Status.PodIP,
	}
	for k, v := range ls {
		labels[k] = v
	}

	resources := targetContainer.Resources
	cpuLimit, _ := strconv.ParseFloat(fmt.Sprint(resources.Limits.Cpu().AsDec()), 64)
	memLimit, _ := strconv.ParseInt(fmt.Sprint(resources.Limits.Memory().AsDec()), 10, 64)

	privileged := true
	agentHttpServerPort := 14975
	pod := &corev1.Pod{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "v1",
			Kind:       "Pod",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:        getAgentPodName(target.Name),
			Namespace:   target.Namespace,
			Labels:      labels,
			Annotations: map[string]string{},
		},
		Spec: corev1.PodSpec{
			NodeName:      target.Spec.NodeName,
			HostPID:       true,
			HostNetwork:   true,
			HostIPC:       true,
			RestartPolicy: corev1.RestartPolicyOnFailure,
			Containers: []corev1.Container{
				{
					Name:  agentContainerName,
					Image: s.p.Cfg.AgentImage,
					Env: []corev1.EnvVar{
						{
							Name:  "CLUSTER_NAME",
							Value: clusterName,
						},
						{
							Name:  "TARGET_POD_NAME",
							Value: target.Name,
						},
						{
							Name:  "TARGET_POD_UID",
							Value: string(target.ObjectMeta.UID),
						},
						{
							Name:  "TARGET_CONTAINER_NAME",
							Value: targetContainer.Name,
						},
						{
							Name:  "TARGET_CONTAINER_ID",
							Value: targetContainerID,
						},
						{
							Name:  "TARGET_CONTAINER_CPU_LIMIT",
							Value: strconv.FormatInt(int64(cpuLimit*1000), 10),
						},
						{
							Name:  "TARGET_CONTAINER_MEM_LIMIT",
							Value: strconv.FormatInt(memLimit, 10),
						},
						{
							Name: "NAMESPACE",
							ValueFrom: &corev1.EnvVarSource{
								FieldRef: &corev1.ObjectFieldSelector{
									FieldPath: "metadata.namespace",
								},
							},
						},
						{
							Name: "POD_NAME",
							ValueFrom: &corev1.EnvVarSource{
								FieldRef: &corev1.ObjectFieldSelector{
									FieldPath: "metadata.name",
								},
							},
						},
						{
							Name:  "HTTP_SERVER_PORT",
							Value: strconv.Itoa(agentHttpServerPort),
						},
						{
							Name:  "LOG_LEVEL",
							Value: "debug",
						},
					},
					SecurityContext: &corev1.SecurityContext{
						Privileged: &privileged,
						Capabilities: &corev1.Capabilities{
							Add: []corev1.Capability{
								corev1.Capability("SYS_PTRACE"),
								corev1.Capability("SYS_ADMIN"),
							},
						},
					},
					TTY: true,
					// ReadinessProbe: getHealthProbe(agentHttpServerPort),
					// LivenessProbe:  getHealthProbe(agentHttpServerPort),
					// Ports: []corev1.ContainerPort{
					// 	{
					// 		Name:          "http",
					// 		ContainerPort: int32(agentHttpServerPort),
					// 	},
					// },
					VolumeMounts: []corev1.VolumeMount{
						{
							Name:      "hostfs",
							MountPath: "/hostfs",
						},
					},
				},
			},
			Volumes: []corev1.Volume{
				{
					Name: "hostfs",
					VolumeSource: corev1.VolumeSource{
						HostPath: &corev1.HostPathVolumeSource{
							Path: "/",
						},
					},
				},
			},
			ImagePullSecrets: target.Spec.ImagePullSecrets,
			Tolerations:      target.Spec.Tolerations,
		},
	}
	return pod
}

func getHealthProbe(port int) *corev1.Probe {
	return &corev1.Probe{
		FailureThreshold: 28,
		PeriodSeconds:    15,
		SuccessThreshold: 1,
		TimeoutSeconds:   10,
		Handler: corev1.Handler{
			HTTPGet: &corev1.HTTPGetAction{
				Path:   "/health",
				Port:   intstr.FromInt(port),
				Scheme: corev1.URISchemeHTTP,
			},
		},
	}
}

func findMainContainer(pod *corev1.Pod) *corev1.Container {
	for _, c := range pod.Spec.Containers {
		var hasOrg, hasProject, hasApp bool
		for _, env := range c.Env {
			switch env.Name {
			case "DICE_ORG_NAME":
				hasOrg = true
			case "DICE_PROJECT_NAME":
				hasProject = true
			case "DICE_APPLICATION_NAME":
				hasApp = true
			}
			if hasOrg && hasProject && hasApp {
				return &c
			}
		}
	}
	if len(pod.Spec.Containers) > 0 {
		return &pod.Spec.Containers[0]
	}
	return nil
}

func getContainerIDByName(pod *corev1.Pod, containerName string) (string, error) {
	for _, containerStatus := range pod.Status.ContainerStatuses {
		if containerStatus.Name != containerName {
			continue
		}
		// if a pod is running but not ready(because of readiness probe), we can connect
		if containerStatus.State.Running == nil {
			return "", fmt.Errorf("container [%s] not running", containerName)
		}
		idx := strings.Index(containerStatus.ContainerID, "//")
		if idx <= 0 {
			return "", fmt.Errorf("unknown containerID format: %q", containerStatus.ContainerID)
		}
		return containerStatus.ContainerID[idx+2:], nil
	}
	return "", fmt.Errorf("cannot find specified container %s", containerName)
}
