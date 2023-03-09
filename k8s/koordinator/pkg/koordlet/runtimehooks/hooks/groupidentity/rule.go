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
	"reflect"
	"strconv"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"

	ext "github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	sysutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

type bvtRule struct {
	enable           bool
	podQOSParams     map[ext.QoSClass]int64
	kubeQOSDirParams map[corev1.PodQOSClass]int64
	kubeQOSPodParams map[corev1.PodQOSClass]int64
}

func (r *bvtRule) getEnable() bool {
	if r == nil {
		return false
	}
	return r.enable
}

func (r *bvtRule) getPodBvtValue(podQoSClass ext.QoSClass, podKubeQOS corev1.PodQOSClass) int64 {
	if val, exist := r.podQOSParams[podQoSClass]; exist {
		return val
	}
	if val, exist := r.kubeQOSPodParams[podKubeQOS]; exist {
		return val
	}
	return *util.NoneCPUQOS().GroupIdentity
}

func (r *bvtRule) getKubeQOSDirBvtValue(kubeQOS corev1.PodQOSClass) int64 {
	if bvtValue, exist := r.kubeQOSDirParams[kubeQOS]; exist {
		return bvtValue
	}
	return *util.NoneCPUQOS().GroupIdentity
}

func (b *bvtPlugin) parseRule(mergedNodeSLOIf interface{}) (bool, error) {
	mergedNodeSLO := mergedNodeSLOIf.(*slov1alpha1.NodeSLOSpec)

	// check if bvt is enabled
	enable := *mergedNodeSLO.ResourceQOSStrategy.LSRClass.CPUQOS.Enable ||
		*mergedNodeSLO.ResourceQOSStrategy.LSClass.CPUQOS.Enable ||
		*mergedNodeSLO.ResourceQOSStrategy.BEClass.CPUQOS.Enable

	// setting pod rule by qos config
	lsrValue := *mergedNodeSLO.ResourceQOSStrategy.LSRClass.CPUQOS.CPUQOS.GroupIdentity
	lsValue := *mergedNodeSLO.ResourceQOSStrategy.LSClass.CPUQOS.GroupIdentity
	beValue := *mergedNodeSLO.ResourceQOSStrategy.BEClass.CPUQOS.GroupIdentity

	// setting besteffort according to BE
	besteffortDirVal := beValue
	besteffortPodVal := beValue

	// setting burstable according to LS
	burstableDirVal := lsValue
	burstablePodVal := lsValue

	// NOTICE guaranteed root dir must set as 0 until kernel supported
	guaranteedDirVal := *util.NoneCPUQOS().GroupIdentity
	// setting guaranteed pod enabled if LS or LSR enabled
	guaranteedPodVal := *util.NoneCPUQOS().GroupIdentity
	if *mergedNodeSLO.ResourceQOSStrategy.LSRClass.CPUQOS.Enable {
		guaranteedPodVal = lsrValue
	} else if *mergedNodeSLO.ResourceQOSStrategy.LSClass.CPUQOS.Enable {
		guaranteedPodVal = lsValue
	}

	newRule := &bvtRule{
		enable: enable,
		podQOSParams: map[ext.QoSClass]int64{
			ext.QoSLSR: lsrValue,
			ext.QoSLS:  lsValue,
			ext.QoSBE:  beValue,
		},
		kubeQOSDirParams: map[corev1.PodQOSClass]int64{
			corev1.PodQOSGuaranteed: guaranteedDirVal,
			corev1.PodQOSBurstable:  burstableDirVal,
			corev1.PodQOSBestEffort: besteffortDirVal,
		},
		kubeQOSPodParams: map[corev1.PodQOSClass]int64{
			corev1.PodQOSGuaranteed: guaranteedPodVal,
			corev1.PodQOSBurstable:  burstablePodVal,
			corev1.PodQOSBestEffort: besteffortPodVal,
		},
	}

	updated := b.updateRule(newRule)
	klog.Infof("runtime hook plugin %s update rule %v, new rule %v", name, updated, newRule)
	return updated, nil
}

func (b *bvtPlugin) ruleUpdateCb(pods []*statesinformer.PodMeta) error {
	if !b.SystemSupported() {
		klog.V(5).Infof("plugin %s is not supported by system", name)
		return nil
	}
	r := b.getRule()
	if r == nil {
		klog.V(5).Infof("hook plugin rule is nil, nothing to do for plugin %v", name)
		return nil
	}
	for _, kubeQOS := range []corev1.PodQOSClass{
		corev1.PodQOSGuaranteed, corev1.PodQOSBurstable, corev1.PodQOSBestEffort} {
		bvtValue := r.getKubeQOSDirBvtValue(kubeQOS)
		kubeQOSCgroupPath := koordletutil.GetKubeQosRelativePath(kubeQOS)
		if err := sysutil.CgroupFileWrite(kubeQOSCgroupPath, sysutil.CPUBVTWarpNs, strconv.FormatInt(bvtValue, 10)); err != nil {
			klog.Infof("update kube qos %v cpu bvt failed, dir %v, error %v", kubeQOS, kubeQOSCgroupPath, err)
		} else {
			audit.V(2).Group(string(kubeQOS)).Reason(name).Message("set bvt to %v", bvtValue)
		}
	}
	for _, podMeta := range pods {
		podQOS := ext.GetPodQoSClass(podMeta.Pod)
		podKubeQOS := podMeta.Pod.Status.QOSClass
		podBvt := r.getPodBvtValue(podQOS, podKubeQOS)
		podCgroupPath := koordletutil.GetPodCgroupDirWithKube(podMeta.CgroupDir)
		if err := sysutil.CgroupFileWrite(podCgroupPath, sysutil.CPUBVTWarpNs, strconv.FormatInt(podBvt, 10)); err != nil {
			klog.Infof("update pod %s cpu bvt failed, dir %v, error %v",
				util.GetPodKey(podMeta.Pod), podCgroupPath, err)
		} else {
			audit.V(2).Pod(podMeta.Pod.Namespace, podMeta.Pod.Name).Reason(name).Message("set bvt to %v", podBvt).Do()
		}
	}
	return nil
}

func (b *bvtPlugin) getRule() *bvtRule {
	b.ruleRWMutex.RLock()
	defer b.ruleRWMutex.RUnlock()
	if b.rule == nil {
		return nil
	}
	rule := *b.rule
	return &rule
}

func (b *bvtPlugin) updateRule(newRule *bvtRule) bool {
	b.ruleRWMutex.Lock()
	defer b.ruleRWMutex.Unlock()
	if !reflect.DeepEqual(newRule, b.rule) {
		b.rule = newRule
		return true
	}
	return false
}
