/*
Copyright 2022 The Koordinator Authors.
Copyright 2015 The Kubernetes Authors.

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

package frameworkext

import (
	"fmt"

	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"
)

var (
	_ framework.SharedLister   = &TemporarySnapshot{}
	_ framework.NodeInfoLister = &TemporarySnapshot{}
)

type TemporarySnapshot struct {
	// nodeInfoMap a map of node name to a snapshot of its NodeInfo.
	nodeInfoMap map[string]*framework.NodeInfo
	// nodeInfoList is the list of nodes as ordered in the cache's nodeTree.
	nodeInfoList []*framework.NodeInfo
	// havePodsWithAffinityNodeInfoList is the list of nodes with at least one pod declaring affinity terms.
	havePodsWithAffinityNodeInfoList []*framework.NodeInfo
	// havePodsWithRequiredAntiAffinityNodeInfoList is the list of nodes with at least one pod declaring
	// required anti-affinity terms.
	havePodsWithRequiredAntiAffinityNodeInfoList []*framework.NodeInfo
}

func NewTemporarySnapshot() *TemporarySnapshot {
	snapshot := &TemporarySnapshot{
		nodeInfoMap: make(map[string]*framework.NodeInfo),
	}
	return snapshot
}

// UpdateSnapshot takes a snapshot of cached NodeInfo map. This is called at
// beginning of every scheduling cycle.
// The snapshot only includes Nodes that are not deleted at the time this function is called.
// nodeinfo.Node() is guaranteed to be not nil for all the nodes in the snapshot.
// This function tracks generation number of NodeInfo and updates only the
// entries of an existing snapshot that have changed after the snapshot was taken.
func (snapshot *TemporarySnapshot) UpdateSnapshot(sharedLister framework.SharedLister) error {
	nodeInfos, err := sharedLister.NodeInfos().List()
	if err != nil {
		klog.Error(err)
		return err
	}

	// NodeInfoList and HavePodsWithAffinityNodeInfoList must be re-created if a node was added
	// or removed from the cache.
	updateAllLists := false
	// HavePodsWithAffinityNodeInfoList must be re-created if a node changed its
	// status from having pods with affinity to NOT having pods with affinity or the other
	// way around.
	updateNodesHavePodsWithAffinity := false
	// HavePodsWithRequiredAntiAffinityNodeInfoList must be re-created if a node changed its
	// status from having pods with required anti-affinity to NOT having pods with required
	// anti-affinity or the other way around.
	updateNodesHavePodsWithRequiredAntiAffinity := false

	// Start from the head of the NodeInfo doubly linked list and update snapshot
	// of NodeInfos updated after the last snapshot.
	for _, node := range nodeInfos {
		if np := node.Node(); np != nil {
			existing, ok := snapshot.nodeInfoMap[np.Name]
			if !ok {
				updateAllLists = true
				existing = &framework.NodeInfo{}
				snapshot.nodeInfoMap[np.Name] = existing
			} else if existing.Generation > 0 && existing.Generation == node.Generation {
				// NOTE: There is currently no better way to implement incremental updates.
				// If we directly modify the NodeInfo of TemporarySnapshot during scheduling, we need to set NodeInfo.Generation to -1.
				// Such cases currently only occur in the Reservation Scheduling.
				continue
			}
			clone := node.Clone()
			// We track nodes that have pods with affinity, here we check if this node changed its
			// status from having pods with affinity to NOT having pods with affinity or the other
			// way around.
			if (len(existing.PodsWithAffinity) > 0) != (len(clone.PodsWithAffinity) > 0) {
				updateNodesHavePodsWithAffinity = true
			}
			if (len(existing.PodsWithRequiredAntiAffinity) > 0) != (len(clone.PodsWithRequiredAntiAffinity) > 0) {
				updateNodesHavePodsWithRequiredAntiAffinity = true
			}
			// We need to preserve the original pointer of the NodeInfo struct since it
			// is used in the NodeInfoList, which we may not update.
			*existing = *clone
		}
	}

	// Comparing to pods in nodeTree.
	// Deleted nodes get removed from the tree, but they might remain in the nodes map
	// if they still have non-deleted Pods.
	if len(snapshot.nodeInfoMap) > len(nodeInfos) {
		snapshot.removeDeletedNodesFromSnapshot(sharedLister, len(nodeInfos))
		updateAllLists = true
	}

	if updateAllLists || updateNodesHavePodsWithAffinity || updateNodesHavePodsWithRequiredAntiAffinity {
		snapshot.updateNodeInfoSnapshotList(nodeInfos, updateAllLists)
	}

	if len(snapshot.nodeInfoList) != len(nodeInfos) {
		errMsg := fmt.Sprintf("snapshot state is not consistent, length of NodeInfoMap=%v, length of nodes in cache=%v, trying to recover",
			len(snapshot.nodeInfoMap), len(nodeInfos))
		klog.Error(errMsg)
		// We will try to recover by re-creating the lists for the next scheduling cycle, but still return an
		// error to surface the problem, the error will likely cause a failure to the current scheduling cycle.
		snapshot.updateNodeInfoSnapshotList(nodeInfos, true)
		return fmt.Errorf(errMsg)
	}

	return nil
}

func (snapshot *TemporarySnapshot) updateNodeInfoSnapshotList(nodeInfos []*framework.NodeInfo, updateAll bool) {
	numNodes := len(nodeInfos)
	snapshot.havePodsWithAffinityNodeInfoList = make([]*framework.NodeInfo, 0, numNodes)
	snapshot.havePodsWithRequiredAntiAffinityNodeInfoList = make([]*framework.NodeInfo, 0, numNodes)
	if updateAll {
		// Take a snapshot of the nodes order in the tree
		snapshot.nodeInfoList = make([]*framework.NodeInfo, 0, numNodes)
		for _, node := range nodeInfos {
			np := node.Node()
			if nodeInfo := snapshot.nodeInfoMap[np.Name]; nodeInfo != nil {
				snapshot.nodeInfoList = append(snapshot.nodeInfoList, nodeInfo)
				if len(nodeInfo.PodsWithAffinity) > 0 {
					snapshot.havePodsWithAffinityNodeInfoList = append(snapshot.havePodsWithAffinityNodeInfoList, nodeInfo)
				}
				if len(nodeInfo.PodsWithRequiredAntiAffinity) > 0 {
					snapshot.havePodsWithRequiredAntiAffinityNodeInfoList = append(snapshot.havePodsWithRequiredAntiAffinityNodeInfoList, nodeInfo)
				}
			} else {
				klog.Errorf("node %q exist in nodeTree but not in NodeInfoMap, this should not happen.", np.Name)
			}
		}
	} else {
		for _, nodeInfo := range snapshot.nodeInfoList {
			if len(nodeInfo.PodsWithAffinity) > 0 {
				snapshot.havePodsWithAffinityNodeInfoList = append(snapshot.havePodsWithAffinityNodeInfoList, nodeInfo)
			}
			if len(nodeInfo.PodsWithRequiredAntiAffinity) > 0 {
				snapshot.havePodsWithRequiredAntiAffinityNodeInfoList = append(snapshot.havePodsWithRequiredAntiAffinityNodeInfoList, nodeInfo)
			}
		}
	}
}

// If certain nodes were deleted after the last snapshot was taken, we should remove them from the snapshot.
func (snapshot *TemporarySnapshot) removeDeletedNodesFromSnapshot(sharedLister framework.SharedLister, numNodes int) {
	toDelete := len(snapshot.nodeInfoMap) - numNodes
	for name := range snapshot.nodeInfoMap {
		if toDelete <= 0 {
			break
		}
		if n, err := sharedLister.NodeInfos().Get(name); err != nil || n.Node() == nil {
			delete(snapshot.nodeInfoMap, name)
			toDelete--
		}
	}
}

func (snapshot *TemporarySnapshot) NodeInfos() framework.NodeInfoLister {
	return snapshot
}

func (snapshot *TemporarySnapshot) List() ([]*framework.NodeInfo, error) {
	return snapshot.nodeInfoList, nil
}

func (snapshot *TemporarySnapshot) HavePodsWithAffinityList() ([]*framework.NodeInfo, error) {
	return snapshot.havePodsWithAffinityNodeInfoList, nil
}

func (snapshot *TemporarySnapshot) HavePodsWithRequiredAntiAffinityList() ([]*framework.NodeInfo, error) {
	return snapshot.havePodsWithRequiredAntiAffinityNodeInfoList, nil
}

func (snapshot *TemporarySnapshot) Get(nodeName string) (*framework.NodeInfo, error) {
	if v, ok := snapshot.nodeInfoMap[nodeName]; ok && v.Node() != nil {
		return v, nil
	}
	return nil, fmt.Errorf("nodeinfo not found for node name %q", nodeName)
}
