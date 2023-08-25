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
	"strconv"

	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/resourceexecutor"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

type HooksProtocol interface {
	ReconcilerDone(executor resourceexecutor.ResourceUpdateExecutor)
}

type hooksProtocolBuilder struct {
	KubeQOS   func(kubeQOS corev1.PodQOSClass) HooksProtocol
	Pod       func(podMeta *statesinformer.PodMeta) HooksProtocol
	Container func(podMeta *statesinformer.PodMeta, containerName string) HooksProtocol
}

var HooksProtocolBuilder = hooksProtocolBuilder{
	KubeQOS: func(kubeQOS corev1.PodQOSClass) HooksProtocol {
		k := &KubeQOSContext{}
		k.FromReconciler(kubeQOS)
		return k
	},
	Pod: func(podMeta *statesinformer.PodMeta) HooksProtocol {
		p := &PodContext{}
		p.FromReconciler(podMeta)
		return p
	},
	Container: func(podMeta *statesinformer.PodMeta, containerName string) HooksProtocol {
		c := &ContainerContext{}
		c.FromReconciler(podMeta, containerName)
		return c
	},
}

type Resources struct {
	// origin resources
	CPUShares   *int64
	CFSQuota    *int64
	CPUSet      *string
	MemoryLimit *int64

	// extended resources
	CPUBvt *int64
}

func (r *Resources) IsOriginResSet() bool {
	return r.CPUShares != nil || r.CFSQuota != nil || r.CPUSet != nil || r.MemoryLimit != nil
}

func injectCPUShares(cgroupParent string, cpuShares int64, a *audit.EventHelper, e resourceexecutor.ResourceUpdateExecutor) error {
	cpuShareStr := strconv.FormatInt(cpuShares, 10)
	updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(sysutil.CPUSharesName, cgroupParent, cpuShareStr, a)
	if err != nil {
		return err
	}
	_, err = e.Update(true, updater)
	return err
}

func injectCPUSet(cgroupParent string, cpuset string, a *audit.EventHelper, e resourceexecutor.ResourceUpdateExecutor) error {
	updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(sysutil.CPUSetCPUSName, cgroupParent, cpuset, a)
	if err != nil {
		return err
	}
	_, err = e.Update(true, updater)
	return err
}

func injectCPUQuota(cgroupParent string, cpuQuota int64, a *audit.EventHelper, e resourceexecutor.ResourceUpdateExecutor) error {
	cpuQuotaStr := strconv.FormatInt(cpuQuota, 10)
	updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(sysutil.CPUCFSQuotaName, cgroupParent, cpuQuotaStr, a)
	if err != nil {
		return err
	}
	_, err = e.Update(true, updater)
	return err
}

func injectMemoryLimit(cgroupParent string, memoryLimit int64, a *audit.EventHelper, e resourceexecutor.ResourceUpdateExecutor) error {
	memoryLimitStr := strconv.FormatInt(memoryLimit, 10)
	updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(sysutil.MemoryLimitName, cgroupParent, memoryLimitStr, a)
	if err != nil {
		return err
	}
	_, err = e.Update(true, updater)
	return err
}

func injectCPUBvt(cgroupParent string, bvtValue int64, a *audit.EventHelper, e resourceexecutor.ResourceUpdateExecutor) error {
	bvtValueStr := strconv.FormatInt(bvtValue, 10)
	updater, err := resourceexecutor.DefaultCgroupUpdaterFactory.New(sysutil.CPUBVTWarpNsName, cgroupParent, bvtValueStr, a)
	if err != nil {
		return err
	}
	_, err = e.Update(true, updater)
	return err
}
