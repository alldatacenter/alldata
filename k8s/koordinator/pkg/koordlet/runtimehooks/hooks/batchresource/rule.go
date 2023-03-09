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
	"reflect"

	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

type batchResourceRule struct {
	enableCFSQuota bool
}

func (r *batchResourceRule) getEnableCFSQuota() bool {
	if r == nil {
		return true
	}
	return r.enableCFSQuota
}

func (p *plugin) parseRule(mergedNodeSLOIf interface{}) (bool, error) {
	mergedNodeSLO := mergedNodeSLOIf.(*slov1alpha1.NodeSLOSpec)

	enableCFSQuota := true
	// NOTE: If CPU Suppress Policy `CPUCfsQuotaPolicy` is enabled for batch pods, batch pods' cfs_quota should be unset
	// since the cfs quota of `kubepods-besteffort` is required to be no less than the children's. Then the cpu usage
	// of Batch is limited by pod-level cpu.shares and qos-level cfs_quota.
	if enable, policy := getCPUSuppressPolicy(mergedNodeSLO); enable && policy == slov1alpha1.CPUCfsQuotaPolicy {
		enableCFSQuota = false
	}

	rule := &batchResourceRule{
		enableCFSQuota: enableCFSQuota,
	}

	updated := p.updateRule(rule)
	klog.V(4).Infof("runtime hook plugin %s update rule %v, new rule %v", name, updated, rule)
	return updated, nil
}

func (p *plugin) ruleUpdateCb(pods []*statesinformer.PodMeta) error {
	r := p.getRule()
	if r == nil {
		klog.V(5).Infof("hook plugin rule is nil, nothing to do for plugin %v", name)
		return nil
	}
	for _, podMeta := range pods {
		podQOS := apiext.GetPodQoSClass(podMeta.Pod)
		if podQOS != apiext.QoSBE {
			continue
		}
		// pod-level
		podCtx := &protocol.PodContext{}
		podCtx.FromReconciler(podMeta)
		if err := p.SetPodCFSQuota(podCtx); err != nil { // only need to change cfs quota
			klog.V(4).Infof("failed to set pod cfs quota during callback %v, err: %v", name, err)
			continue
		}
		podCtx.ReconcilerDone()
		// container-level
		for _, containerStat := range podMeta.Pod.Status.ContainerStatuses {
			containerCtx := &protocol.ContainerContext{}
			containerCtx.FromReconciler(podMeta, containerStat.Name)
			if err := p.SetContainerCFSQuota(containerCtx); err != nil {
				klog.V(4).Infof("failed to set container cfs quota during callback %v, container %v, err: %v",
					name, containerStat.Name, err)
				continue
			}
			containerCtx.ReconcilerDone()
		}
	}
	return nil
}

func (p *plugin) getRule() *batchResourceRule {
	p.ruleRWMutex.RLock()
	defer p.ruleRWMutex.RUnlock()
	if p.rule == nil {
		return nil
	}
	rule := *p.rule
	return &rule
}

func (p *plugin) updateRule(newRule *batchResourceRule) bool {
	p.ruleRWMutex.Lock()
	defer p.ruleRWMutex.Unlock()
	if !reflect.DeepEqual(newRule, p.rule) {
		p.rule = newRule
		return true
	}
	return false
}

func getCPUSuppressPolicy(nodeSLOSpec *slov1alpha1.NodeSLOSpec) (bool, slov1alpha1.CPUSuppressPolicy) {
	if nodeSLOSpec == nil || nodeSLOSpec.ResourceUsedThresholdWithBE == nil ||
		nodeSLOSpec.ResourceUsedThresholdWithBE.CPUSuppressPolicy == "" {
		return *util.DefaultResourceThresholdStrategy().Enable,
			util.DefaultResourceThresholdStrategy().CPUSuppressPolicy
	}
	return *nodeSLOSpec.ResourceUsedThresholdWithBE.Enable,
		nodeSLOSpec.ResourceUsedThresholdWithBE.CPUSuppressPolicy
}
