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

package elasticquota

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/elasticquota/core"
)

// todo the eventHandler's operation should be a complete transaction in the future work.

func (g *Plugin) OnPodAdd(obj interface{}) {
	pod, ok := obj.(*corev1.Pod)
	if !ok {
		return
	}

	pod = core.RunDecoratePod(pod)
	quotaName := g.getPodAssociateQuotaName(pod)
	g.groupQuotaManager.OnPodAdd(quotaName, pod)
	klog.V(5).Infof("OnPodAddFunc %v.%v add success, quotaName:%v", pod.Namespace, pod.Name, quotaName)
}

func (g *Plugin) OnPodUpdate(oldObj, newObj interface{}) {
	oldPod := oldObj.(*corev1.Pod)
	newPod := newObj.(*corev1.Pod)

	if oldPod.ResourceVersion == newPod.ResourceVersion {
		klog.Warningf("update pod warning, update version for the same:%v", newPod.Name)
		return
	}

	oldPod = core.RunDecoratePod(oldPod)
	newPod = core.RunDecoratePod(newPod)
	oldQuotaName := g.getPodAssociateQuotaName(oldPod)
	newQuotaName := g.getPodAssociateQuotaName(newPod)
	g.groupQuotaManager.OnPodUpdate(newQuotaName, oldQuotaName, newPod, oldPod)
	klog.V(5).Infof("OnPodUpdateFunc %v.%v update success, quotaName:%v", newPod.Namespace, newPod.Name, newQuotaName)
}

func (g *Plugin) OnPodDelete(obj interface{}) {
	pod, ok := obj.(*corev1.Pod)
	if !ok {
		return
	}

	pod = core.RunDecoratePod(pod)
	quotaName := g.getPodAssociateQuotaName(pod)
	g.groupQuotaManager.OnPodDelete(quotaName, pod)
	klog.V(5).Infof("OnPodDeleteFunc %v.%v delete success", pod.Namespace, pod.Name)
}
