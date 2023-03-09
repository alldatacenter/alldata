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
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
)

type KubeQOSRequet struct {
	KubeQOSClass corev1.PodQOSClass
	CgroupParent string
}

func (r *KubeQOSRequet) FromReconciler(kubeQOS corev1.PodQOSClass) {
	r.KubeQOSClass = kubeQOS
	r.CgroupParent = util.GetKubeQosRelativePath(kubeQOS)
}

type KubeQOSResponse struct {
	Resources Resources
}

type KubeQOSContext struct {
	Request  KubeQOSRequet
	Response KubeQOSResponse
}

func (k *KubeQOSContext) FromReconciler(kubeQOS corev1.PodQOSClass) {
	k.Request.FromReconciler(kubeQOS)
}

func (k *KubeQOSContext) ReconcilerDone() {
	k.injectForOrigin()
	k.injectForExt()
}

func (p *KubeQOSContext) injectForOrigin() {
	// TODO
}

func (p *KubeQOSContext) injectForExt() {
	if p.Response.Resources.CPUBvt != nil {
		if err := injectCPUBvt(p.Request.CgroupParent, *p.Response.Resources.CPUBvt); err != nil {
			klog.Infof("set kubeqos %v bvt %v on cgroup parent %v failed, error %v", p.Request.KubeQOSClass,
				*p.Response.Resources.CPUBvt, p.Request.CgroupParent, err)
		} else {
			klog.V(5).Infof("set kubeqos %v bvt %v on cgroup parent %v", p.Request.KubeQOSClass,
				*p.Response.Resources.CPUBvt, p.Request.CgroupParent)
			audit.V(2).Group(string(p.Request.KubeQOSClass)).Reason("runtime-hooks").Message(
				"set kubeqos bvt to %v", *p.Response.Resources.CPUBvt).Do()
		}
	}
}
