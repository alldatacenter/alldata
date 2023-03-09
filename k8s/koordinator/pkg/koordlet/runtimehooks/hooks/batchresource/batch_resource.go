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

package batchresource

import (
	"fmt"
	"sync"

	utilerrors "k8s.io/apimachinery/pkg/util/errors"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/reconciler"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/rule"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	rmconfig "github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	name        = "BatchResource"
	description = "set fundamental cgroups value for batch pod"
)

type plugin struct {
	rule        *batchResourceRule
	ruleRWMutex sync.RWMutex
}

var podQOSConditions = []string{string(apiext.QoSBE), string(apiext.QoSLS), string(apiext.QoSNone)}

func (p *plugin) Register() {
	klog.V(5).Infof("register hook %v", name)
	rule.Register(name, description,
		rule.WithParseFunc(statesinformer.RegisterTypeNodeSLOSpec, p.parseRule),
		rule.WithUpdateCallback(p.ruleUpdateCb))
	hooks.Register(rmconfig.PreRunPodSandbox, name, description+" (pod)", p.SetPodResources)
	hooks.Register(rmconfig.PreCreateContainer, name, description+" (container)", p.SetContainerResources)
	hooks.Register(rmconfig.PreUpdateContainerResources, name, description+" (container)", p.SetContainerResources)
	reconciler.RegisterCgroupReconciler(reconciler.PodLevel, sysutil.CPUShares, description+" (pod cpu shares)",
		p.SetPodCPUShares, reconciler.PodQOSFilter(), podQOSConditions...)
	reconciler.RegisterCgroupReconciler(reconciler.PodLevel, sysutil.CPUCFSQuota, description+" (pod cfs quota)",
		p.SetPodCFSQuota, reconciler.PodQOSFilter(), podQOSConditions...)
	reconciler.RegisterCgroupReconciler(reconciler.PodLevel, sysutil.MemoryLimit, description+" (pod memory limit)",
		p.SetPodMemoryLimit, reconciler.PodQOSFilter(), podQOSConditions...)
	reconciler.RegisterCgroupReconciler(reconciler.ContainerLevel, sysutil.CPUShares, description+" (container cpu shares)",
		p.SetContainerCPUShares, reconciler.PodQOSFilter(), podQOSConditions...)
	reconciler.RegisterCgroupReconciler(reconciler.ContainerLevel, sysutil.CPUCFSQuota, description+" (container cfs quota)",
		p.SetContainerCFSQuota, reconciler.PodQOSFilter(), podQOSConditions...)
	reconciler.RegisterCgroupReconciler(reconciler.ContainerLevel, sysutil.MemoryLimit, description+" (container memory limit)",
		p.SetContainerMemoryLimit, reconciler.PodQOSFilter(), podQOSConditions...)
}

var singleton *plugin

func Object() *plugin {
	if singleton == nil {
		singleton = &plugin{}
	}
	return singleton
}

func (p *plugin) SetPodResources(proto protocol.HooksProtocol) error {
	podCtx := proto.(*protocol.PodContext)
	if podCtx == nil {
		return fmt.Errorf("pod protocol is nil for plugin %v", name)
	}

	err := p.SetPodCPUShares(proto)
	if err != nil {
		klog.V(5).Infof("failed to set pod cpu shares in plugin %s, pod %s/%s, err: %v",
			name, podCtx.Request.PodMeta.Namespace, podCtx.Request.PodMeta.Name, err)
	}

	err1 := p.SetPodCFSQuota(proto)
	if err1 != nil {
		klog.V(5).Infof("failed to set pod cfs quota in plugin %s, pod %s/%s, err: %v",
			name, podCtx.Request.PodMeta.Namespace, podCtx.Request.PodMeta.Name, err1)
	}

	err2 := p.SetPodMemoryLimit(proto)
	if err2 != nil {
		klog.V(5).Infof("failed to set pod memory limit in plugin %s, pod %s/%s, err: %v",
			name, podCtx.Request.PodMeta.Namespace, podCtx.Request.PodMeta.Name, err2)
	}

	return utilerrors.NewAggregate([]error{err, err1, err2})
}

func (p *plugin) SetPodCPUShares(proto protocol.HooksProtocol) error {
	podCtx := proto.(*protocol.PodContext)
	if podCtx == nil {
		return fmt.Errorf("pod protocol is nil for plugin %v", name)
	}

	if !isPodQoSBEByAttr(podCtx.Request.Labels, podCtx.Request.Annotations) {
		return nil
	}

	extendedResourceSpec := podCtx.Request.ExtendedResources
	// if the extendedResourceSpec is empty, do nothing and keep the original cgroup configs
	if extendedResourceSpec == nil {
		return nil
	}

	milliCPURequest := int64(0)
	// TODO: count init container and pod overhead
	for _, c := range extendedResourceSpec.Containers {
		if c.Requests == nil {
			continue
		}
		containerRequest := util.GetBatchMilliCPUFromResourceList(c.Requests)
		if containerRequest <= 0 {
			continue
		}
		milliCPURequest += containerRequest
	}
	cpuShares := milliCPURequest * sysutil.CPUShareUnitValue / 1000
	if cpuShares < sysutil.CPUSharesMinValue {
		cpuShares = sysutil.CPUSharesMinValue
	}

	podCtx.Response.Resources.CPUShares = pointer.Int64Ptr(cpuShares)
	return nil
}

func (p *plugin) SetPodCFSQuota(proto protocol.HooksProtocol) error {
	podCtx := proto.(*protocol.PodContext)
	if podCtx == nil {
		return fmt.Errorf("pod protocol is nil for plugin %v", name)
	}

	if !isPodQoSBEByAttr(podCtx.Request.Labels, podCtx.Request.Annotations) {
		return nil
	}

	extendedResourceSpec := podCtx.Request.ExtendedResources
	// if the extendedResourceSpec is empty, do nothing and keep the original cgroup configs
	if extendedResourceSpec == nil {
		return nil
	}

	// if cfs quota is disabled, set as -1
	if !p.getRule().getEnableCFSQuota() {
		podCtx.Response.Resources.CFSQuota = pointer.Int64Ptr(-1)
		klog.V(5).Infof("try to unset pod-level cfs quota since it is disabled in rule of plugin %v", name)
		return nil
	}

	milliCPULimit := int64(0)
	// TODO: count init container and pod overhead
	for _, c := range extendedResourceSpec.Containers {
		if c.Limits == nil {
			continue
		}
		containerLimit := util.GetBatchMilliCPUFromResourceList(c.Limits)
		if containerLimit <= 0 { // pod unlimited once a container is unlimited
			milliCPULimit = -1
			break
		}
		milliCPULimit += containerLimit
	}

	cfsQuota := milliCPULimit * sysutil.CFSBasePeriodValue / 1000 // TBD: assert base cfs period not changed
	if cfsQuota <= 0 {                                            // pod unlimited
		cfsQuota = -1
	}
	if cfsQuota < sysutil.CFSQuotaMinValue { // cfs_quota_us should be no less than 1000
		cfsQuota = sysutil.CFSQuotaMinValue
	}

	podCtx.Response.Resources.CFSQuota = pointer.Int64Ptr(cfsQuota)
	return nil
}

func (p *plugin) SetPodMemoryLimit(proto protocol.HooksProtocol) error {
	podCtx := proto.(*protocol.PodContext)
	if podCtx == nil {
		return fmt.Errorf("pod protocol is nil for plugin %v", name)
	}

	if !isPodQoSBEByAttr(podCtx.Request.Labels, podCtx.Request.Annotations) {
		return nil
	}

	extendedResourceSpec := podCtx.Request.ExtendedResources
	// if the extendedResourceSpec is empty, do nothing and keep the original cgroup configs
	if extendedResourceSpec == nil {
		return nil
	}

	memoryLimit := int64(0)
	// TODO: count init container and pod overhead
	for _, c := range extendedResourceSpec.Containers {
		if c.Limits == nil {
			continue
		}
		containerLimit := util.GetBatchMemoryFromResourceList(c.Limits)
		if containerLimit <= 0 { // pod unlimited once a container is unlimited
			memoryLimit = -1
			break
		}
		memoryLimit += containerLimit
	}

	podCtx.Response.Resources.MemoryLimit = pointer.Int64Ptr(memoryLimit)
	return nil
}

func (p *plugin) SetContainerResources(proto protocol.HooksProtocol) error {
	containerCtx := proto.(*protocol.ContainerContext)
	if containerCtx == nil {
		return fmt.Errorf("container protocol is nil for plugin %v", name)
	}

	err := p.SetContainerCPUShares(proto)
	if err != nil {
		klog.V(5).Infof("failed to set container cpu shares in plugin %s, container %s/%s/%s, err: %v",
			name, containerCtx.Request.PodMeta.Namespace, containerCtx.Request.PodMeta.Name,
			containerCtx.Request.ContainerMeta.Name, err)
	}

	err1 := p.SetContainerCFSQuota(proto)
	if err1 != nil {
		klog.V(5).Infof("failed to set container cfs quota in plugin %s, container %s/%s/%s, err: %v",
			name, containerCtx.Request.PodMeta.Namespace, containerCtx.Request.PodMeta.Name,
			containerCtx.Request.ContainerMeta.Name, err)
	}

	err2 := p.SetContainerMemoryLimit(proto)
	if err2 != nil {
		klog.V(5).Infof("failed to set container memory limit in plugin %s, container %s/%s/%s, err: %v",
			name, containerCtx.Request.PodMeta.Namespace, containerCtx.Request.PodMeta.Name,
			containerCtx.Request.ContainerMeta.Name, err)
	}

	return utilerrors.NewAggregate([]error{err, err1, err2})
}

func (p *plugin) SetContainerCPUShares(proto protocol.HooksProtocol) error {
	containerCtx := proto.(*protocol.ContainerContext)
	if containerCtx == nil {
		return fmt.Errorf("container protocol is nil for plugin %v", name)
	}

	if !isPodQoSBEByAttr(containerCtx.Request.PodLabels, containerCtx.Request.PodAnnotations) {
		return nil
	}

	containerSpec := containerCtx.Request.ExtendedResources
	// if the extendedResourceSpec is empty, do nothing and keep the original cgroup configs
	if containerSpec == nil {
		return nil
	}

	milliCPURequest := int64(0)
	if containerSpec.Requests != nil {
		containerRequest := util.GetBatchMilliCPUFromResourceList(containerSpec.Requests)
		if containerRequest > 0 {
			milliCPURequest = containerRequest
		}
	}
	cpuShares := milliCPURequest * sysutil.CPUShareUnitValue / 1000
	if cpuShares < sysutil.CPUSharesMinValue {
		cpuShares = sysutil.CPUSharesMinValue
	}

	containerCtx.Response.Resources.CPUShares = pointer.Int64Ptr(cpuShares)
	return nil
}

func (p *plugin) SetContainerCFSQuota(proto protocol.HooksProtocol) error {
	containerCtx := proto.(*protocol.ContainerContext)
	if containerCtx == nil {
		return fmt.Errorf("container protocol is nil for plugin %v", name)
	}

	if !isPodQoSBEByAttr(containerCtx.Request.PodLabels, containerCtx.Request.PodAnnotations) {
		return nil
	}

	containerSpec := containerCtx.Request.ExtendedResources
	// if the extendedResourceSpec is empty, do nothing and keep the original cgroup configs
	if containerSpec == nil {
		return nil
	}

	// if cfs quota is disabled, set as -1
	if !p.getRule().getEnableCFSQuota() {
		containerCtx.Response.Resources.CFSQuota = pointer.Int64Ptr(-1)
		klog.V(5).Infof("try to unset container-level cfs quota since it is disabled in rule of plugin %v", name)
		return nil
	}

	milliCPULimit := int64(0)
	if containerSpec.Limits != nil {
		containerLimit := util.GetBatchMilliCPUFromResourceList(containerSpec.Limits)
		if containerLimit > 0 {
			milliCPULimit = containerLimit
		}
	}
	cfsQuota := milliCPULimit * sysutil.CFSBasePeriodValue / 1000
	if cfsQuota <= 0 {
		cfsQuota = -1
	} else if cfsQuota < sysutil.CFSQuotaMinValue {
		cfsQuota = sysutil.CFSQuotaMinValue
	}

	containerCtx.Response.Resources.CFSQuota = pointer.Int64Ptr(cfsQuota)
	return nil
}

func (p *plugin) SetContainerMemoryLimit(proto protocol.HooksProtocol) error {
	containerCtx := proto.(*protocol.ContainerContext)
	if containerCtx == nil {
		return fmt.Errorf("container protocol is nil for plugin %v", name)
	}

	if !isPodQoSBEByAttr(containerCtx.Request.PodLabels, containerCtx.Request.PodAnnotations) {
		return nil
	}

	containerSpec := containerCtx.Request.ExtendedResources
	// if the extendedResourceSpec is empty, do nothing and keep the original cgroup configs
	if containerSpec == nil {
		return nil
	}

	memoryLimit := int64(0)
	if containerSpec.Limits != nil {
		containerLimit := util.GetBatchMemoryFromResourceList(containerSpec.Limits)
		if containerLimit > 0 {
			memoryLimit = containerLimit
		}
	}
	if memoryLimit <= 0 {
		memoryLimit = -1
	}

	containerCtx.Response.Resources.MemoryLimit = pointer.Int64Ptr(memoryLimit)
	return nil
}

func isPodQoSBEByAttr(labels map[string]string, annotations map[string]string) bool {
	return apiext.GetQoSClassByAttrs(labels, annotations) == apiext.QoSBE
}
