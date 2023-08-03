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
	"k8s.io/kubernetes/pkg/scheduler/framework"
)

type NodeInfoTransformerFn func(nodeInfo *framework.NodeInfo)

var nodeInfoTransformerFns []NodeInfoTransformerFn

func RegisterNodeInfoTransformer(fn NodeInfoTransformerFn) {
	nodeInfoTransformerFns = append(nodeInfoTransformerFns, fn)
}

func TransformNodeInfos(nodeInfos []*framework.NodeInfo) (transformedNodeInfos []*framework.NodeInfo) {
	for _, nodeInfo := range nodeInfos {
		n := TransformOneNodeInfo(nodeInfo)
		transformedNodeInfos = append(transformedNodeInfos, n)
	}
	return
}

func TransformOneNodeInfo(nodeInfo *framework.NodeInfo) (transformedNodeInfo *framework.NodeInfo) {
	node := nodeInfo.Node()
	if node == nil || len(nodeInfoTransformerFns) == 0 {
		return nodeInfo
	}

	transformedNodeInfo = nodeInfo.Clone()
	for _, fn := range nodeInfoTransformerFns {
		fn(transformedNodeInfo)
	}
	return transformedNodeInfo
}
