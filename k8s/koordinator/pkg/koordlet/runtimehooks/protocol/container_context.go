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

package protocol

import (
	"fmt"

	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	runtimeapi "github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

type ContainerMeta struct {
	Name string
	ID   string // docker://xxx; containerd://
}

func (c *ContainerMeta) FromProxy(containerMeta *runtimeapi.ContainerMetadata, podAnnotations map[string]string) {
	c.Name = containerMeta.GetName()
	uid := containerMeta.GetId()
	c.ID = getContainerID(podAnnotations, uid)
}

type ContainerRequest struct {
	PodMeta           PodMeta
	ContainerMeta     ContainerMeta
	PodLabels         map[string]string
	PodAnnotations    map[string]string
	CgroupParent      string
	ContainerEnvs     map[string]string
	ExtendedResources *apiext.ExtendedResourceContainerSpec
}

func (c *ContainerRequest) FromProxy(req *runtimeapi.ContainerResourceHookRequest) {
	c.PodMeta.FromProxy(req.PodMeta)
	c.ContainerMeta.FromProxy(req.ContainerMeta, req.PodAnnotations)
	c.PodLabels = req.GetPodLabels()
	c.PodAnnotations = req.GetPodAnnotations()
	c.CgroupParent, _ = koordletutil.GetContainerCgroupPathWithKubeByID(req.GetPodCgroupParent(), c.ContainerMeta.ID)
	c.ContainerEnvs = req.GetContainerEnvs()
	// retrieve ExtendedResources from pod annotations
	spec, err := apiext.GetExtendedResourceSpec(req.GetPodAnnotations())
	if err != nil {
		klog.V(4).Infof("failed to get ExtendedResourceSpec from proxy via annotation, container %s/%s, err: %s",
			c.PodMeta.Namespace, c.PodMeta.Name, c.ContainerMeta.Name, err)
	}
	if spec != nil && spec.Containers != nil {
		if containerSpec, ok := spec.Containers[c.ContainerMeta.Name]; ok {
			c.ExtendedResources = &containerSpec
		}
	}
}

func (c *ContainerRequest) FromReconciler(podMeta *statesinformer.PodMeta, containerName string) {
	c.PodMeta.FromReconciler(podMeta.Pod.ObjectMeta)
	c.ContainerMeta.Name = containerName
	for _, containerStat := range podMeta.Pod.Status.ContainerStatuses {
		if containerStat.Name == containerName {
			c.ContainerMeta.ID = containerStat.ContainerID
			break
		}
	}
	var specFromContainer *apiext.ExtendedResourceContainerSpec
	for _, containerSpec := range podMeta.Pod.Spec.Containers {
		if containerSpec.Name == containerName {
			if c.ContainerEnvs == nil {
				c.ContainerEnvs = map[string]string{}
			}
			for _, envVar := range containerSpec.Env {
				c.ContainerEnvs[envVar.Name] = envVar.Value
			}
			specFromContainer = util.GetContainerExtendedResources(&containerSpec)
			break
		}
	}
	c.PodLabels = podMeta.Pod.Labels
	c.PodAnnotations = podMeta.Pod.Annotations
	c.CgroupParent, _ = koordletutil.GetContainerCgroupPathWithKubeByID(podMeta.CgroupDir, c.ContainerMeta.ID)
	// retrieve ExtendedResources from container spec and pod annotations (prefer container spec)
	specFromAnnotations, err := apiext.GetExtendedResourceSpec(podMeta.Pod.Annotations)
	if err != nil {
		klog.V(4).Infof("failed to get ExtendedResourceSpec from reconciler via annotation, container %s/%s, err: %s",
			c.PodMeta.Namespace, c.PodMeta.Name, c.ContainerMeta.Name, err)
	}
	if specFromContainer != nil {
		c.ExtendedResources = specFromContainer
	} else if specFromAnnotations != nil && specFromAnnotations.Containers != nil { // specFromContainer == nil
		if containerSpec, ok := specFromAnnotations.Containers[c.ContainerMeta.Name]; ok {
			c.ExtendedResources = &containerSpec
		}
	}
}

type ContainerResponse struct {
	Resources        Resources
	AddContainerEnvs map[string]string
}

func (c *ContainerResponse) ProxyDone(resp *runtimeapi.ContainerResourceHookResponse) {
	if c.Resources.IsOriginResSet() && resp.ContainerResources == nil {
		// resource value is injected but origin request is nil, init resource response
		resp.ContainerResources = &runtimeapi.LinuxContainerResources{}
	}
	if c.Resources.CPUSet != nil {
		resp.ContainerResources.CpusetCpus = *c.Resources.CPUSet
	}
	if c.Resources.CFSQuota != nil {
		resp.ContainerResources.CpuQuota = *c.Resources.CFSQuota
	}
	if c.Resources.CPUShares != nil {
		resp.ContainerResources.CpuShares = *c.Resources.CPUShares
	}
	if c.Resources.MemoryLimit != nil {
		resp.ContainerResources.MemoryLimitInBytes = *c.Resources.MemoryLimit
	}
	if c.AddContainerEnvs != nil {
		if resp.ContainerEnvs == nil {
			resp.ContainerEnvs = make(map[string]string)
		}
		for k, v := range c.AddContainerEnvs {
			resp.ContainerEnvs[k] = v
		}
	}
}

type ContainerContext struct {
	Request  ContainerRequest
	Response ContainerResponse
}

func (c *ContainerContext) FromProxy(req *runtimeapi.ContainerResourceHookRequest) {
	c.Request.FromProxy(req)
}

func (c *ContainerContext) ProxyDone(resp *runtimeapi.ContainerResourceHookResponse) {
	c.injectForExt()
	c.Response.ProxyDone(resp)
}

func (c *ContainerContext) FromReconciler(podMeta *statesinformer.PodMeta, containerName string) {
	c.Request.FromReconciler(podMeta, containerName)
}

func (c *ContainerContext) ReconcilerDone() {
	c.injectForExt()
	c.injectForOrigin()
}

func (c *ContainerContext) injectForOrigin() {
	if c.Response.Resources.CPUShares != nil {
		if err := injectCPUShares(c.Request.CgroupParent, *c.Response.Resources.CPUShares); err != nil {
			klog.Infof("set container %v/%v/%v cpu share %v on cgroup parent %v failed, error %v", c.Request.PodMeta.Namespace,
				c.Request.PodMeta.Name, c.Request.ContainerMeta.Name, *c.Response.Resources.CPUShares, c.Request.CgroupParent, err)
		} else {
			klog.V(5).Infof("set container %v/%v/%v cpu share %v on cgroup parent %v",
				c.Request.PodMeta.Namespace, c.Request.PodMeta.Name, c.Request.ContainerMeta.Name,
				*c.Response.Resources.CPUShares, c.Request.CgroupParent)
			audit.V(2).Container(c.Request.ContainerMeta.ID).Reason("runtime-hooks").Message(
				"set container cpu share to %v", *c.Response.Resources.CPUShares).Do()
		}
	}
	if c.Response.Resources.CPUSet != nil {
		if err := injectCPUSet(c.Request.CgroupParent, *c.Response.Resources.CPUSet); err != nil {
			klog.Infof("set container %v/%v/%v cpuset %v on cgroup parent %v failed, error %v", c.Request.PodMeta.Namespace,
				c.Request.PodMeta.Name, c.Request.ContainerMeta.Name, *c.Response.Resources.CPUSet, c.Request.CgroupParent, err)
		} else {
			klog.V(5).Infof("set container %v/%v/%v cpuset %v on cgroup parent %v",
				c.Request.PodMeta.Namespace, c.Request.PodMeta.Name, c.Request.ContainerMeta.Name,
				*c.Response.Resources.CPUSet, c.Request.CgroupParent)
			audit.V(2).Container(c.Request.ContainerMeta.ID).Reason("runtime-hooks").Message(
				"set container cpuset to %v", *c.Response.Resources.CPUSet).Do()
		}
	}
	if c.Response.Resources.CFSQuota != nil {
		if err := injectCPUQuota(c.Request.CgroupParent, *c.Response.Resources.CFSQuota); err != nil {
			klog.Infof("set container %v/%v/%v cfs quota %v on cgroup parent %v failed, error %v", c.Request.PodMeta.Namespace,
				c.Request.PodMeta.Name, c.Request.ContainerMeta.Name, *c.Response.Resources.CFSQuota, c.Request.CgroupParent, err)
		} else {
			klog.V(5).Infof("set container %v/%v/%v cfs quota %v on cgroup parent %v",
				c.Request.PodMeta.Namespace, c.Request.PodMeta.Name, c.Request.ContainerMeta.Name,
				*c.Response.Resources.CFSQuota, c.Request.CgroupParent)
			audit.V(2).Container(c.Request.ContainerMeta.ID).Reason("runtime-hooks").Message(
				"set container cfs quota to %v", *c.Response.Resources.CFSQuota).Do()
		}
	}
	if c.Response.Resources.MemoryLimit != nil {
		if err := injectMemoryLimit(c.Request.CgroupParent, *c.Response.Resources.MemoryLimit); err != nil {
			klog.Infof("set container %v/%v/%v memory limit %v on cgroup parent %v failed, error %v", c.Request.PodMeta.Namespace,
				c.Request.PodMeta.Name, c.Request.ContainerMeta.Name, *c.Response.Resources.MemoryLimit, c.Request.CgroupParent, err)
		} else {
			klog.V(5).Infof("set container %v/%v/%v memory limit %v on cgroup parent %v",
				c.Request.PodMeta.Namespace, c.Request.PodMeta.Name, c.Request.ContainerMeta.Name,
				*c.Response.Resources.MemoryLimit, c.Request.CgroupParent)
			audit.V(2).Container(c.Request.ContainerMeta.ID).Reason("runtime-hooks").Message(
				"set container memory limit to %v", *c.Response.Resources.MemoryLimit).Do()
		}
	}
	// TODO other fields
}

func (c *ContainerContext) injectForExt() {
	// TODO
}

func getContainerID(podAnnotations map[string]string, containerUID string) string {
	// TODO parse from runtime hook request directly
	runtimeType := "containerd"
	if _, exist := podAnnotations["io.kubernetes.docker.type"]; exist {
		runtimeType = "docker"
	}
	return fmt.Sprintf("%s://%s", runtimeType, containerUID)
}
