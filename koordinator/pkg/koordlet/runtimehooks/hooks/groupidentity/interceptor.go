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
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"

	ext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/runtimehooks/protocol"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util"
)

func (b *bvtPlugin) SetPodBvtValue(p protocol.HooksProtocol) error {
	if !b.SystemSupported() {
		klog.V(5).Infof("plugin %s is not supported by system", name)
		return nil
	}
	r := b.getRule()
	if r == nil {
		klog.V(5).Infof("hook plugin rule is nil, nothing to do for plugin %v", name)
		return nil
	}
	err := b.initialize()
	if err != nil {
		klog.V(4).Infof("failed to initialize plugin %s, err: %s", name, err)
		return nil
	}

	podCtx := p.(*protocol.PodContext)
	req := podCtx.Request
	podQOS := ext.GetQoSClassByAttrs(req.Labels, req.Annotations)
	podKubeQOS := util.GetKubeQoSByCgroupParent(req.CgroupParent)
	podBvt := r.getPodBvtValue(podQOS, podKubeQOS)
	podCtx.Response.Resources.CPUBvt = pointer.Int64(podBvt)
	return nil
}

func (b *bvtPlugin) SetKubeQOSBvtValue(p protocol.HooksProtocol) error {
	if !b.SystemSupported() {
		klog.V(5).Infof("plugin %s is not supported by system", name)
		return nil
	}
	r := b.getRule()
	if r == nil {
		klog.V(5).Infof("hook plugin rule is nil, nothing to do for plugin %v", name)
		return nil
	}
	err := b.initialize()
	if err != nil {
		klog.V(4).Infof("failed to initialize plugin %s, err: %s", name, err)
		return nil
	}

	kubeQOSCtx := p.(*protocol.KubeQOSContext)
	req := kubeQOSCtx.Request
	bvtValue := r.getKubeQOSDirBvtValue(req.KubeQOSClass)
	kubeQOSCtx.Response.Resources.CPUBvt = pointer.Int64(bvtValue)
	return nil
}
