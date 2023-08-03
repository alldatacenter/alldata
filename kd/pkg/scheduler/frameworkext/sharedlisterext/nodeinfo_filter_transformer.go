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

package sharedlisterext

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
)

func init() {
	frameworkext.RegisterDefaultTransformers(NewNodeInfoFilterTransformer())
}

var _ frameworkext.FilterTransformer = &nodeInfoFilterTransformer{}

type nodeInfoFilterTransformer struct{}

func NewNodeInfoFilterTransformer() frameworkext.SchedulingTransformer {
	return &nodeInfoFilterTransformer{}
}

func (h *nodeInfoFilterTransformer) Name() string { return "nodeInfoFilterTransformer" }

// BeforeFilter transforms nodeInfo if needed. When the scheduler executes the Filter, the passed NodeInfos comes from the cache instead of SnapshotSharedLister.
// This means that when these NodeInfos need to be transformed, they can only be done in the execution of Filter.
func (h *nodeInfoFilterTransformer) BeforeFilter(handle frameworkext.ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) (*corev1.Pod, *framework.NodeInfo, bool) {
	transformedNodeInfo := TransformOneNodeInfo(nodeInfo)
	return pod, transformedNodeInfo, true
}
