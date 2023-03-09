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

package groupidentity

import (
	"fmt"
	"sync"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/hooks"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/reconciler"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/rule"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	rmconfig "github.com/koordinator-sh/koordinator/pkg/runtimeproxy/config"
)

const (
	name        = "GroupIdentity"
	description = "set bvt value by priority and qos class"
)

type bvtPlugin struct {
	rule             *bvtRule
	ruleRWMutex      sync.RWMutex
	sysSupported     *bool
	hasKernelEnabled *bool // whether kernel is configurable for enabling bvt (via `kernel.sched_group_identity_enabled`)
	kernelEnabled    *bool // if not nil, indicates whether bvt feature is enabled via `kernel.sched_group_identity_enabled`
}

func (b *bvtPlugin) Register() {
	klog.V(5).Infof("register hook %v", name)
	hooks.Register(rmconfig.PreRunPodSandbox, name, description, b.SetPodBvtValue)
	rule.Register(name, description,
		rule.WithParseFunc(statesinformer.RegisterTypeNodeSLOSpec, b.parseRule),
		rule.WithUpdateCallback(b.ruleUpdateCb),
		rule.WithSystemSupported(b.SystemSupported))
	reconciler.RegisterCgroupReconciler(reconciler.PodLevel, sysutil.CPUBVTWarpNs, "reconcile pod level cpu bvt value",
		b.SetPodBvtValue, reconciler.NoneFilter())
	reconciler.RegisterCgroupReconciler(reconciler.KubeQOSLevel, sysutil.CPUBVTWarpNs, "reconcile kubeqos level cpu bvt value",
		b.SetKubeQOSBvtValue, reconciler.NoneFilter())
}

func (b *bvtPlugin) SystemSupported() bool {
	if b.sysSupported == nil {
		isBVTSupported, msg := false, "resource not found"
		bvtResource, err := sysutil.GetCgroupResource(sysutil.CPUBVTWarpNsName)
		if err == nil {
			isBVTSupported, msg = bvtResource.IsSupported(util.GetKubeQosRelativePath(corev1.PodQOSGuaranteed))
		}
		bvtConfigPath := sysutil.GetProcSysFilePath(sysutil.KernelSchedGroupIdentityEnable)
		b.sysSupported = pointer.BoolPtr(isBVTSupported || sysutil.FileExists(bvtConfigPath))
		klog.Infof("update system supported info to %v for plugin %v, supported msg %s",
			*b.sysSupported, name, msg)
	}
	return *b.sysSupported
}

func (b *bvtPlugin) hasKernelEnable() bool {
	if b.hasKernelEnabled == nil {
		bvtConfigPath := sysutil.GetProcSysFilePath(sysutil.KernelSchedGroupIdentityEnable)
		b.hasKernelEnabled = pointer.BoolPtr(sysutil.FileExists(bvtConfigPath))
	}
	return *b.hasKernelEnabled
}

// initKernelEnable checks if the kernel supports sysctl configuration for bvt (group identity).
// It returns:
// 1. whether the feature is enabled after initialization (set sysctl config if the kernel supports)
// 2. any error for the initialization
func (b *bvtPlugin) initialize() (bool, error) {
	// NOTE: bvt (group identity) is supported and can be initialized in the system if:
	// 1. anolis os kernel (<26.4): cgroup cpu.bvt_warp_ns exists but sysctl kernel.sched_group_identity_enabled no exist,
	//    the bvt feature is enabled by default, no need to set sysctl.
	// 2. anolis os kernel (>=26.4): both cgroup cpu.bvt_warp_ns and sysctl kernel.sched_group_identity_enabled exist,
	//    the bvt feature is enabled when kernel.sched_group_identity_enabled is set as `1`.
	enable := b.getRule().getEnable()
	if !b.hasKernelEnable() {
		return enable, nil
	}

	// if cpu qos is enabled/disabled in rule, check if we need to change the sysctl config for bvt (group identity)
	if b.kernelEnabled != nil && *b.kernelEnabled == enable {
		klog.V(6).Infof("skip initialize bvt to %v, hook plugin rule not change", enable)
		return enable, nil
	}

	// try to set bvt kernel enabled via sysctl
	err := sysutil.SetSchedGroupIdentity(enable)
	if err != nil {
		return false, fmt.Errorf("cannot enable kernel sysctl for bvt, err: %v", err)
	}
	b.kernelEnabled = pointer.BoolPtr(enable)
	klog.V(4).Infof("hook plugin %s is successfully initialized to %v", name, enable)
	return enable, nil
}

var singleton *bvtPlugin

func Object() *bvtPlugin {
	if singleton == nil {
		singleton = &bvtPlugin{rule: &bvtRule{}}
	}
	return singleton
}
