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
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func init() {
	RegisterNodeInfoTransformer(nodeReservationTransformer)
}

// node.alloc = node.alloc - node.anno.reserved
func nodeReservationTransformer(nodeInfo *framework.NodeInfo) {
	if nodeInfo == nil || nodeInfo.Node() == nil {
		return
	}

	node := nodeInfo.Node()
	resourceListReservedByNode := util.GetNodeReservationFromAnnotation(node.Annotations)
	if resourceListReservedByNode == nil {
		return
	}

	originAlloc := node.Status.Allocatable.DeepCopy()
	currentAlloc := quotav1.Subtract(originAlloc, resourceListReservedByNode)

	// node.alloc(batch-memory) and node.alloc(batch-memory) have subtracted the reserved resources from the koord-manager,
	// so we should keep the original data here.
	currentAlloc[extension.BatchMemory] = originAlloc[extension.BatchMemory]
	currentAlloc[extension.BatchCPU] = originAlloc[extension.BatchCPU]

	if !quotav1.Equals(originAlloc, currentAlloc) {
		nodeInfo.Allocatable = framework.NewResource(currentAlloc)
	}
}
