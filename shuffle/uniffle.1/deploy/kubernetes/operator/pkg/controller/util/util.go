/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import (
	"fmt"
	"strings"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/kubernetes/scheme"
	v1core "k8s.io/client-go/kubernetes/typed/core/v1"
	"k8s.io/client-go/tools/record"
	"k8s.io/utils/pointer"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	controllerconstants "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

// AddOwnerReference add OwnerReference to an object.
func AddOwnerReference(meta *metav1.ObjectMeta, rss *v1alpha1.RemoteShuffleService) {
	meta.OwnerReferences = append(meta.OwnerReferences, generateOwnerReference(rss)...)
}

// generateOwnerReference builds an OwnerReference for rss objects.
func generateOwnerReference(rss *v1alpha1.RemoteShuffleService) []metav1.OwnerReference {
	return []metav1.OwnerReference{
		{
			APIVersion: v1alpha1.SchemeGroupVersion.String(),
			// Use a const instead of rss.Kind as it maybe empty, this is odd.
			Kind:               constants.RSSKind,
			BlockOwnerDeletion: pointer.Bool(true),
			Name:               rss.Name,
			UID:                rss.UID,
			Controller:         pointer.Bool(true),
		},
	}
}

// GenerateMainContainer generate main container of coordinators or shuffle servers.
func GenerateMainContainer(name, configDir string, podSpec *v1alpha1.RSSPodSpec,
	ports []corev1.ContainerPort, env []corev1.EnvVar,
	initVolumeMounts []corev1.VolumeMount) *corev1.Container {
	mainContainer := &corev1.Container{
		Name:            name,
		Image:           podSpec.Image,
		Command:         podSpec.Command,
		Args:            podSpec.Args,
		ImagePullPolicy: corev1.PullAlways,
		Resources:       podSpec.Resources,
		Ports:           ports,
		Env:             env,
		VolumeMounts: []corev1.VolumeMount{
			{
				Name:      controllerconstants.ConfigurationVolumeName,
				MountPath: configDir,
			},
		},
	}
	mainContainer.VolumeMounts = append(mainContainer.VolumeMounts, initVolumeMounts...)
	addVolumeMountsOfMainContainer(mainContainer, podSpec.HostPathMounts, podSpec.VolumeMounts)
	return mainContainer
}

func addVolumeMountsOfMainContainer(mainContainer *corev1.Container,
	hostPathMounts map[string]string, volumeMounts []corev1.VolumeMount) {
	var clearPathCMDs []string
	mainContainer.VolumeMounts = append(mainContainer.VolumeMounts,
		GenerateHostPathVolumeMounts(hostPathMounts)...)
	for _, mountPath := range hostPathMounts {
		clearPathCMDs = append(clearPathCMDs, fmt.Sprintf("rm -rf %v/*", strings.TrimSuffix(mountPath, "/")))
	}
	if len(clearPathCMDs) > 0 {
		mainContainer.Lifecycle = &corev1.Lifecycle{
			PreStop: &corev1.Handler{
				Exec: &corev1.ExecAction{
					Command: []string{"/bin/sh", "-c", strings.Join(clearPathCMDs, ";")},
				},
			},
		}
	}
	mainContainer.VolumeMounts = append(mainContainer.VolumeMounts, volumeMounts...)
}

// GenerateHostPathVolumeMounts generates volume mounts for hostPaths configured in rss objects.
func GenerateHostPathVolumeMounts(hostPathMounts map[string]string) []corev1.VolumeMount {
	var volumeMounts []corev1.VolumeMount
	for hostPath, mountPath := range hostPathMounts {
		volumeMounts = append(volumeMounts, corev1.VolumeMount{
			Name:      GenerateHostPathVolumeName(hostPath),
			MountPath: mountPath,
		})
	}
	return volumeMounts
}

// GenerateHostPathVolumes generates all hostPath volumes. If logHostPath is not empty, we need to handle it in a
// special way by adding the subPath to the hostPath which is the same as logHostPath.
func GenerateHostPathVolumes(hostPathMounts map[string]string, logHostPath, subPath string) []corev1.Volume {
	volumes := make([]corev1.Volume, 0)
	for hostPath := range hostPathMounts {
		if len(hostPath) == 0 {
			continue
		}
		if hostPath == logHostPath {
			volumes = append(volumes, *GenerateHostPathVolume(hostPath, subPath))
		} else {
			volumes = append(volumes, *GenerateHostPathVolume(hostPath, ""))
		}
	}
	return volumes
}

// GenerateHostPathVolume convert host path to hostPath volume.
func GenerateHostPathVolume(hostPath, subPath string) *corev1.Volume {
	path := hostPath
	if len(subPath) > 0 {
		path = fmt.Sprintf("%v/%v", strings.TrimSuffix(path, "/"), subPath)
	}
	hostPathType := corev1.HostPathDirectoryOrCreate
	return &corev1.Volume{
		Name: GenerateHostPathVolumeName(hostPath),
		VolumeSource: corev1.VolumeSource{
			HostPath: &corev1.HostPathVolumeSource{
				Path: path,
				Type: &hostPathType,
			},
		},
	}
}

// GenerateHostPathVolumeName converts host path to volume name.
func GenerateHostPathVolumeName(hostPath string) string {
	hostPath = strings.TrimPrefix(hostPath, "/")
	hostPath = strings.TrimSuffix(hostPath, "/")
	hostPath = strings.ReplaceAll(hostPath, "/", "-")
	return hostPath
}

// GenerateInitContainers generates init containers for coordinators and shuffle servers.
func GenerateInitContainers(rssPodSpec *v1alpha1.RSSPodSpec) []corev1.Container {
	var initContainers []corev1.Container
	if rssPodSpec.SecurityContext == nil || rssPodSpec.SecurityContext.FSGroup == nil {
		return initContainers
	}
	if len(rssPodSpec.HostPathMounts) > 0 {
		image := rssPodSpec.InitContainerImage
		if len(image) == 0 {
			image = constants.DefaultInitContainerImage
		}
		commands := generateMakeDataDirCommand(rssPodSpec)
		initContainers = append(initContainers, corev1.Container{
			Name:            "init-data-dir",
			Image:           image,
			ImagePullPolicy: corev1.PullIfNotPresent,
			Command:         []string{"sh", "-c", strings.Join(commands, ";")},
			SecurityContext: &corev1.SecurityContext{
				RunAsUser:  pointer.Int64(0),
				Privileged: pointer.Bool(true),
			},
			VolumeMounts: GenerateHostPathVolumeMounts(rssPodSpec.HostPathMounts),
			Resources:    rssPodSpec.Resources,
		})
		if len(rssPodSpec.LogHostPath) > 0 {
			initContainers = append(initContainers, corev1.Container{
				Name:            "init-log-dir",
				Image:           image,
				ImagePullPolicy: corev1.PullIfNotPresent,
				Command: []string{
					"sh", "-c",
					fmt.Sprintf("mkdir -p %v && chmod -R 777 %v", rssPodSpec.LogHostPath,
						rssPodSpec.LogHostPath),
				},
				SecurityContext: &corev1.SecurityContext{
					RunAsUser:  pointer.Int64(0),
					Privileged: pointer.Bool(true),
				},
				VolumeMounts: []corev1.VolumeMount{
					*generateLogVolumeMount(rssPodSpec),
				},
				Resources: rssPodSpec.Resources,
			})
		}
	}
	return initContainers
}

func generateLogVolumeMount(rssPodSpec *v1alpha1.RSSPodSpec) *corev1.VolumeMount {
	logHostPath := rssPodSpec.LogHostPath
	return &corev1.VolumeMount{
		Name:      GenerateHostPathVolumeName(logHostPath),
		MountPath: rssPodSpec.HostPathMounts[logHostPath],
	}
}

func generateMakeDataDirCommand(rssPodSpec *v1alpha1.RSSPodSpec) []string {
	var commands []string

	var runAsUser int64
	if rssPodSpec.SecurityContext != nil && rssPodSpec.SecurityContext.RunAsUser != nil {
		runAsUser = *rssPodSpec.SecurityContext.RunAsUser
	}

	var fsGroup int64
	if rssPodSpec.SecurityContext != nil && rssPodSpec.SecurityContext.FSGroup != nil {
		fsGroup = *rssPodSpec.SecurityContext.FSGroup
	}

	for _, mountPath := range rssPodSpec.HostPathMounts {
		commands = append(commands, fmt.Sprintf("chown -R %v:%v %v", runAsUser, fsGroup, mountPath))
	}
	return commands
}

// CreateRecorder creates an event recorder.
func CreateRecorder(kubeClient kubernetes.Interface, component string) record.EventRecorder {
	eventBroadcaster := record.NewBroadcaster()
	eventBroadcaster.StartRecordingToSink(&v1core.EventSinkImpl{
		Interface: kubeClient.CoreV1().Events(utils.GetCurrentNamespace()),
	})
	return eventBroadcaster.NewRecorder(scheme.Scheme, corev1.EventSource{Component: component})
}

// GetConfigMapOwner returns name of rss object as owner of configMap.
func GetConfigMapOwner(cm *corev1.ConfigMap) string {
	return cm.Labels[controllerconstants.OwnerLabel]
}
