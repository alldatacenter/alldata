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

package agentinjector

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/admission/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

var (
	podResource       = metav1.GroupVersionResource{Version: "v1", Resource: "pods"}
	ignoredNamespaces = []string{
		metav1.NamespaceSystem,
		metav1.NamespacePublic,
	}
)

const (
	admissionWebhookInjectKey = "monitor-agent.erda.cloud/inject"

	initContainerName   = "monitor-agent-erda-cloud"
	initVolumeName      = "monitor-agent-erda-cloud"
	initVolumeMountPath = "/opt/spot"
	javaAgentOps        = "-javaagent:/opt/spot/java-agent/spot-agent.jar"
)

func (p *provider) matchPod(pod *corev1.Pod) bool {
	metadata := pod.ObjectMeta

	// skip special kubernete system namespaces
	for _, namespace := range ignoredNamespaces {
		if metadata.Namespace == namespace {
			logrus.Infof("skip pod mutation %s/%s, it's in special namespace", metadata.Namespace, metadata.Name)
			return false
		}
	}

	for _, value := range []string{
		strings.ToLower(metadata.Annotations[admissionWebhookInjectKey]),
		strings.ToLower(metadata.Labels[admissionWebhookInjectKey]),
	} {
		switch value {
		case "y", "yes", "true", "on":
			return true
		case "n", "no", "false", "off":
			return false
		}
	}

	if p.isErdaApplication(pod) {
		return true
	}
	return false
}

func (p *provider) isErdaApplication(pod *corev1.Pod) bool {
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
				return true
			}
		}
	}
	return false
}

func (p *provider) findInitContainer(pod *corev1.Pod) (int, *corev1.Container) {
	for i, c := range pod.Spec.InitContainers {
		if c.Name == initContainerName {
			return i, &c
		}
	}
	return -1, nil
}

func (p *provider) findInitVolume(pod *corev1.Pod) (int, *corev1.Volume) {
	for i, v := range pod.Spec.Volumes {
		if v.Name == initVolumeName {
			return i, &v
		}
	}
	return -1, nil
}

func addContainer(target, added []corev1.Container, basePath string) (patch []patchOperation) {
	first := len(target) == 0
	var value interface{}
	for _, add := range added {
		value = add
		path := basePath
		if first {
			first = false
			value = []corev1.Container{add}
		} else {
			path = path + "/-"
		}
		patch = append(patch, patchOperation{
			Op:    "add",
			Path:  path,
			Value: value,
		})
	}
	return patch
}

func addVolume(target, added []corev1.Volume, basePath string) (patch []patchOperation) {
	first := len(target) == 0
	var value interface{}
	for _, add := range added {
		value = add
		path := basePath
		if first {
			first = false
			value = []corev1.Volume{add}
		} else {
			path = path + "/-"
		}
		patch = append(patch, patchOperation{
			Op:    "add",
			Path:  path,
			Value: value,
		})
	}
	return patch
}

func addVolumeMounts(target, added []corev1.VolumeMount, basePath string) (patch []patchOperation) {
	first := len(target) == 0
	var value interface{}
	for _, add := range added {
		value = add
		path := basePath
		if first {
			first = false
			value = []corev1.VolumeMount{add}
		} else {
			path = path + "/-"
		}
		patch = append(patch, patchOperation{
			Op:    "add",
			Path:  path,
			Value: value,
		})
	}
	return patch
}

func addEnvs(target, added []corev1.EnvVar, basePath string) (patch []patchOperation) {
	first := len(target) == 0
	var value interface{}
	for _, add := range added {
		value = add
		path := basePath
		if first {
			first = false
			value = []corev1.EnvVar{add}
		} else {
			path = path + "/-"
		}
		patch = append(patch, patchOperation{
			Op:    "add",
			Path:  path,
			Value: value,
		})
	}
	return patch
}

func (p *provider) newInitContainer() *corev1.Container {
	c := &corev1.Container{
		Name:            initContainerName,
		Image:           p.Cfg.InitContainerImage,
		ImagePullPolicy: corev1.PullIfNotPresent,
		VolumeMounts:    []corev1.VolumeMount{*p.newInitVolumeMount()},
		Command: []string{
			"/bin/container-init.sh",
		},
	}
	return c
}

func (p *provider) checkUpdateForInitContainer(c *corev1.Container) bool {
	idx := strings.LastIndex(c.Image, ":")
	if idx <= 0 {
		return true
	}
	tag := c.Image[idx+1:]
	return p.initContainerImageTag != tag // check image tag only
}

func (p *provider) newInitVolume() *corev1.Volume {
	return &corev1.Volume{
		Name: initVolumeName,
		VolumeSource: corev1.VolumeSource{
			EmptyDir: &corev1.EmptyDirVolumeSource{},
		},
	}
}

func (p *provider) newInitVolumeMount() *corev1.VolumeMount {
	return &corev1.VolumeMount{
		Name:      initVolumeName,
		MountPath: initVolumeMountPath,
	}
}

func (p *provider) checkUpdateForInitVolumeMount(vm *corev1.VolumeMount) bool {
	return vm.MountPath != initVolumeMountPath
}

func (p *provider) patchInitContainer(match bool, pod *corev1.Pod) (patch []patchOperation) {
	idx, c := p.findInitContainer(pod)
	if match {
		ic := p.newInitContainer()
		if c == nil {
			// add init container
			patch = append(patch, addContainer(pod.Spec.InitContainers, []corev1.Container{*ic}, "/spec/initContainers")...)
		} else {
			if p.checkUpdateForInitContainer(c) {
				// update init container
				patch = append(patch, patchOperation{
					Op:    "replace",
					Path:  "/spec/initContainers/" + strconv.Itoa(idx),
					Value: ic,
				})
			}
		}
	} else {
		if c != nil {
			// remove init container
			patch = append(patch, patchOperation{
				Op:   "remove",
				Path: "/spec/initContainers/" + strconv.Itoa(idx),
			})
		}
	}
	return patch
}

func (p *provider) patchInitVolume(match bool, pod *corev1.Pod) (patch []patchOperation) {
	idx, v := p.findInitVolume(pod)
	if match {
		volume := p.newInitVolume()
		volumeMount := p.newInitVolumeMount()
		if v == nil {
			// add init volume
			patch = append(patch, addVolume(pod.Spec.Volumes, []corev1.Volume{*volume}, "/spec/volumes")...)
			for ci, c := range pod.Spec.Containers {
				patch = append(patch, addVolumeMounts(c.VolumeMounts, []corev1.VolumeMount{*volumeMount}, "/spec/containers/"+strconv.Itoa(ci)+"/volumeMounts")...)
			}
		} else {
			for ci, c := range pod.Spec.Containers {
				var find bool
				for vi, vm := range c.VolumeMounts {
					if vm.Name == v.Name {
						if p.checkUpdateForInitVolumeMount(&vm) {
							patch = append(patch, patchOperation{
								Op:    "replace",
								Path:  "/spec/containers/" + strconv.Itoa(ci) + "/volumeMounts/" + strconv.Itoa(vi),
								Value: volumeMount,
							})
						}
						find = true
					}
				}
				if !find {
					patch = append(patch, addVolumeMounts(c.VolumeMounts, []corev1.VolumeMount{*volumeMount}, "/spec/containers/"+strconv.Itoa(ci)+"/volumeMounts")...)
				}
			}
		}
	} else {
		if v != nil {
			patch = append(patch, patchOperation{
				Op:   "remove",
				Path: "/spec/volumes/" + strconv.Itoa(idx),
			})
			for ci, c := range pod.Spec.Containers {
				var find bool
				newVolumeMounts := make([]corev1.VolumeMount, 0, len(c.VolumeMounts))
				for _, vm := range c.VolumeMounts {
					if vm.Name == v.Name {
						find = true
						continue
					}
					newVolumeMounts = append(newVolumeMounts, vm)
				}
				if find {
					patch = append(patch, patchOperation{
						Op:    "replace",
						Path:  "/spec/containers/" + strconv.Itoa(ci) + "/volumeMounts",
						Value: newVolumeMounts,
					})
				}
			}
		}
	}
	return patch
}

func (p *provider) patchEnv(match bool, pod *corev1.Pod) (patch []patchOperation) {
	if match {
	containerLoop:
		for ci, c := range pod.Spec.Containers {
			for ei, env := range c.Env {
				if env.Name == "JAVA_OPTS" {
					if !strings.Contains(env.Value, "-javaagent") {
						patch = append(patch, patchOperation{
							Op:    "replace",
							Path:  "/spec/containers/" + strconv.Itoa(ci) + "/env/" + strconv.Itoa(ei) + "/value",
							Value: env.Value + " " + javaAgentOps,
						})
					}
					continue containerLoop
				}
			}
			patch = append(patch, addEnvs(c.Env, []corev1.EnvVar{
				{
					Name:  "JAVA_OPTS",
					Value: javaAgentOps,
				},
			}, "/spec/containers/"+strconv.Itoa(ci)+"/env")...)
		}
	}
	return patch
}

func (p *provider) HandlePods(rw http.ResponseWriter, r *http.Request) {
	p.processAdmissionReview(rw, r, func(review *v1.AdmissionReview) (*v1.AdmissionReview, error) {
		req := review.Request
		// check resource
		if req.Resource != podResource {
			return p.newFailedAdmissionReview(
				fmt.Sprintf("got resource %s, expect resource %s", req.Resource.String(), podResource.String()),
			), nil
		}

		// parse the Pod object.
		raw := req.Object.Raw
		pod := &corev1.Pod{}
		if _, _, err := deserializer.Decode(raw, nil, pod); err != nil {
			return p.newFailedAdmissionReview(
				fmt.Sprintf("could not deserialize pod object: %s", err),
			), nil
		}

		resp := &v1.AdmissionReview{
			Response: &v1.AdmissionResponse{
				Allowed: true,
			},
		}
		if pod == nil {
			return resp, nil
		}

		// patch
		match := p.matchPod(pod)
		patch := p.patchInitContainer(match, pod)
		patch = append(patch, p.patchInitVolume(match, pod)...)
		patch = append(patch, p.patchEnv(match, pod)...)

		if len(patch) > 0 {
			patchBytes, err := json.Marshal(patch)
			if err != nil {
				return nil, err
			}
			resp.Response.Patch = patchBytes
			logrus.Infof("patch Pod{%s/%s} ok", pod.ObjectMeta.Namespace, pod.ObjectMeta.Name)
		}
		return resp, nil
	})
}
