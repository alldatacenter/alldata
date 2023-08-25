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
	"errors"
	"fmt"
	"reflect"
	"sort"
	"testing"

	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/client-go/tools/events"
	utilnode "k8s.io/component-helpers/node/topology"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/profile"
	st "k8s.io/kubernetes/pkg/scheduler/testing"
)

var _ framework.SharedLister = &fakeSharedListerWithDump{}

type fakeSharedListerWithDump struct {
	nodeInfos []*framework.NodeInfo
	nodes     map[string]*framework.NodeInfo
	nodeNames []string
}

func newFakeSharedListerWithDump(sched *scheduler.Scheduler) *fakeSharedListerWithDump {
	dump := sched.SchedulerCache.Dump()
	nodes := make([]*v1.Node, 0, len(dump.Nodes))
	for _, v := range dump.Nodes {
		if n := v.Node(); n != nil {
			nodes = append(nodes, n)
		}
	}
	sort.Slice(nodes, func(i, j int) bool {
		return nodes[i].Name < nodes[j].Name
	})
	tree := newNodeTree(nodes)
	nodeNames, err := tree.list()
	if err != nil {
		panic(err.Error())
	}

	nodeInfos := make([]*framework.NodeInfo, 0, len(dump.Nodes))
	for _, v := range nodeNames {
		ni := dump.Nodes[v]
		if ni != nil {
			nodeInfos = append(nodeInfos, ni)
		}
	}

	return &fakeSharedListerWithDump{
		nodeInfos: nodeInfos,
		nodes:     dump.Nodes,
		nodeNames: nodeNames,
	}
}

func (f *fakeSharedListerWithDump) NodeInfos() framework.NodeInfoLister {
	return f
}

func (f *fakeSharedListerWithDump) List() ([]*framework.NodeInfo, error) {
	return f.nodeInfos, nil
}

func (f *fakeSharedListerWithDump) HavePodsWithAffinityList() ([]*framework.NodeInfo, error) {
	nodeInfos := make([]*framework.NodeInfo, 0, len(f.nodeInfos))
	for _, v := range f.nodeInfos {
		if len(v.PodsWithAffinity) > 0 {
			nodeInfos = append(nodeInfos, v)
		}
	}
	return nodeInfos, nil
}

func (f *fakeSharedListerWithDump) HavePodsWithRequiredAntiAffinityList() ([]*framework.NodeInfo, error) {
	nodeInfos := make([]*framework.NodeInfo, 0, len(f.nodeInfos))
	for _, v := range f.nodeInfos {
		if len(v.PodsWithRequiredAntiAffinity) > 0 {
			nodeInfos = append(nodeInfos, v)
		}
	}
	return nodeInfos, nil
}

func (f *fakeSharedListerWithDump) Get(nodeName string) (*framework.NodeInfo, error) {
	if v, ok := f.nodes[nodeName]; ok && v.Node() != nil {
		return v, nil
	}
	return nil, fmt.Errorf("nodeinfo not found for node name %q", nodeName)
}

// nodeTree is a tree-like data structure that holds node names in each zone. Zone names are
// keys to "NodeTree.tree" and values of "NodeTree.tree" are arrays of node names.
// NodeTree is NOT thread-safe, any concurrent updates/reads from it must be synchronized by the caller.
// It is used only by schedulerCache, and should stay as such.
type nodeTree struct {
	tree     map[string][]string // a map from zone (region-zone) to an array of nodes in the zone.
	zones    []string            // a list of all the zones in the tree (keys)
	numNodes int
}

// newNodeTree creates a NodeTree from nodes.
func newNodeTree(nodes []*v1.Node) *nodeTree {
	nt := &nodeTree{
		tree: make(map[string][]string, len(nodes)),
	}
	for _, n := range nodes {
		nt.addNode(n)
	}
	return nt
}

// addNode adds a node and its corresponding zone to the tree. If the zone already exists, the node
// is added to the array of nodes in that zone.
func (nt *nodeTree) addNode(n *v1.Node) {
	zone := utilnode.GetZoneKey(n)
	if na, ok := nt.tree[zone]; ok {
		for _, nodeName := range na {
			if nodeName == n.Name {
				klog.InfoS("Node already exists in the NodeTree", "node", klog.KObj(n))
				return
			}
		}
		nt.tree[zone] = append(na, n.Name)
	} else {
		nt.zones = append(nt.zones, zone)
		nt.tree[zone] = []string{n.Name}
	}
	klog.V(2).InfoS("Added node in listed group to NodeTree", "node", klog.KObj(n), "zone", zone)
	nt.numNodes++
}

// removeNode removes a node from the NodeTree.
func (nt *nodeTree) removeNode(n *v1.Node) error {
	zone := utilnode.GetZoneKey(n)
	if na, ok := nt.tree[zone]; ok {
		for i, nodeName := range na {
			if nodeName == n.Name {
				nt.tree[zone] = append(na[:i], na[i+1:]...)
				if len(nt.tree[zone]) == 0 {
					nt.removeZone(zone)
				}
				klog.V(2).InfoS("Removed node in listed group from NodeTree", "node", klog.KObj(n), "zone", zone)
				nt.numNodes--
				return nil
			}
		}
	}
	klog.ErrorS(nil, "Node in listed group was not found", "node", klog.KObj(n), "zone", zone)
	return fmt.Errorf("node %q in group %q was not found", n.Name, zone)
}

// removeZone removes a zone from tree.
// This function must be called while writer locks are hold.
func (nt *nodeTree) removeZone(zone string) {
	delete(nt.tree, zone)
	for i, z := range nt.zones {
		if z == zone {
			nt.zones = append(nt.zones[:i], nt.zones[i+1:]...)
			return
		}
	}
}

// updateNode updates a node in the NodeTree.
func (nt *nodeTree) updateNode(old, new *v1.Node) {
	var oldZone string
	if old != nil {
		oldZone = utilnode.GetZoneKey(old)
	}
	newZone := utilnode.GetZoneKey(new)
	// If the zone ID of the node has not changed, we don't need to do anything. Name of the node
	// cannot be changed in an update.
	if oldZone == newZone {
		return
	}
	nt.removeNode(old) // No error checking. We ignore whether the old node exists or not.
	nt.addNode(new)
}

// list returns the list of names of the node. NodeTree iterates over zones and in each zone iterates
// over nodes in a round robin fashion.
func (nt *nodeTree) list() ([]string, error) {
	if len(nt.zones) == 0 {
		return nil, nil
	}
	nodesList := make([]string, 0, nt.numNodes)
	numExhaustedZones := 0
	nodeIndex := 0
	for len(nodesList) < nt.numNodes {
		if numExhaustedZones >= len(nt.zones) { // all zones are exhausted.
			return nodesList, errors.New("all zones exhausted before reaching count of nodes expected")
		}
		for zoneIndex := 0; zoneIndex < len(nt.zones); zoneIndex++ {
			na := nt.tree[nt.zones[zoneIndex]]
			if nodeIndex >= len(na) { // If the zone is exhausted, continue
				if nodeIndex == len(na) { // If it is the first time the zone is exhausted
					numExhaustedZones++
				}
				continue
			}
			nodesList = append(nodesList, na[nodeIndex])
		}
		nodeIndex++
	}
	return nodesList, nil
}

func TestSchedulerCache_UpdateSnapshot(t *testing.T) {
	// Create a few nodes to be used in tests.
	var nodes []*v1.Node
	for i := 0; i < 10; i++ {
		node := &v1.Node{
			ObjectMeta: metav1.ObjectMeta{
				Name: fmt.Sprintf("test-node%v", i),
			},
			Status: v1.NodeStatus{
				Allocatable: v1.ResourceList{
					v1.ResourceCPU:    resource.MustParse("1000m"),
					v1.ResourceMemory: resource.MustParse("100m"),
				},
			},
		}
		nodes = append(nodes, node)
	}
	// Create a few nodes as updated versions of the above nodes
	var updatedNodes []*v1.Node
	for _, n := range nodes {
		updatedNode := n.DeepCopy()
		updatedNode.Status.Allocatable = v1.ResourceList{
			v1.ResourceCPU:    resource.MustParse("2000m"),
			v1.ResourceMemory: resource.MustParse("500m"),
		}
		updatedNodes = append(updatedNodes, updatedNode)
	}

	// Create a few pods for tests.
	var pods []*v1.Pod
	for i := 0; i < 20; i++ {
		pod := st.MakePod().Name(fmt.Sprintf("test-pod%v", i)).Namespace("test-ns").UID(fmt.Sprintf("test-puid%v", i)).
			Node(fmt.Sprintf("test-node%v", i%10)).Obj()
		pods = append(pods, pod)
	}

	// Create a few pods as updated versions of the above pods.
	var updatedPods []*v1.Pod
	for _, p := range pods {
		updatedPod := p.DeepCopy()
		priority := int32(1000)
		updatedPod.Spec.Priority = &priority
		updatedPods = append(updatedPods, updatedPod)
	}

	// Add a couple of pods with affinity, on the first and seconds nodes.
	var podsWithAffinity []*v1.Pod
	for i := 0; i < 2; i++ {
		pod := st.MakePod().Name(fmt.Sprintf("p-affinity-%v", i)).Namespace("test-ns").UID(fmt.Sprintf("puid-affinity-%v", i)).
			PodAffinityExists("foo", "", st.PodAffinityWithRequiredReq).Node(fmt.Sprintf("test-node%v", i)).Obj()
		podsWithAffinity = append(podsWithAffinity, pod)
	}

	var sched *scheduler.Scheduler
	var snapshot *TemporarySnapshot
	type operation = func(t *testing.T)

	addNode := func(i int) operation {
		return func(t *testing.T) {
			sched.SchedulerCache.AddNode(nodes[i])
		}
	}
	removeNode := func(i int) operation {
		return func(t *testing.T) {
			if err := sched.SchedulerCache.RemoveNode(nodes[i]); err != nil {
				t.Error(err)
			}
		}
	}
	updateNode := func(i int) operation {
		return func(t *testing.T) {
			sched.SchedulerCache.UpdateNode(nodes[i], updatedNodes[i])
		}
	}
	addPod := func(i int) operation {
		return func(t *testing.T) {
			if err := sched.SchedulerCache.AddPod(pods[i]); err != nil {
				t.Error(err)
			}
		}
	}
	addPodWithAffinity := func(i int) operation {
		return func(t *testing.T) {
			if err := sched.SchedulerCache.AddPod(podsWithAffinity[i]); err != nil {
				t.Error(err)
			}
		}
	}
	removePod := func(i int) operation {
		return func(t *testing.T) {
			if err := sched.SchedulerCache.RemovePod(pods[i]); err != nil {
				t.Error(err)
			}
		}
	}
	removePodWithAffinity := func(i int) operation {
		return func(t *testing.T) {
			if err := sched.SchedulerCache.RemovePod(podsWithAffinity[i]); err != nil {
				t.Error(err)
			}
		}
	}
	updatePod := func(i int) operation {
		return func(t *testing.T) {
			if err := sched.SchedulerCache.UpdatePod(pods[i], updatedPods[i]); err != nil {
				t.Error(err)
			}
		}
	}
	updateSnapshot := func() operation {
		return func(t *testing.T) {
			fsl := newFakeSharedListerWithDump(sched)
			snapshot.UpdateSnapshot(fsl)
			if err := compareCacheWithNodeInfoSnapshot(t, fsl, snapshot); err != nil {
				t.Error(err)
			}
		}
	}

	tests := []struct {
		name                         string
		operations                   []operation
		expected                     []*v1.Node
		expectedHavePodsWithAffinity int
		expectedUsedPVCSet           sets.String
	}{
		{
			name:               "Empty cache",
			operations:         []operation{},
			expected:           []*v1.Node{},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name:               "Single node",
			operations:         []operation{addNode(1)},
			expected:           []*v1.Node{nodes[1]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add node, remove it, add it again",
			operations: []operation{
				addNode(1), updateSnapshot(), removeNode(1), addNode(1),
			},
			expected:           []*v1.Node{nodes[1]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add node and remove it in the same cycle, add it again",
			operations: []operation{
				addNode(1), updateSnapshot(), addNode(2), removeNode(1),
			},
			expected:           []*v1.Node{nodes[2]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add a few nodes, and snapshot in the middle",
			operations: []operation{
				addNode(0), updateSnapshot(), addNode(1), updateSnapshot(), addNode(2),
				updateSnapshot(), addNode(3),
			},
			expected:           []*v1.Node{nodes[3], nodes[2], nodes[1], nodes[0]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add a few nodes, and snapshot in the end",
			operations: []operation{
				addNode(0), addNode(2), addNode(5), addNode(6),
			},
			expected:           []*v1.Node{nodes[6], nodes[5], nodes[2], nodes[0]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Update some nodes",
			operations: []operation{
				addNode(0), addNode(1), addNode(5), updateSnapshot(), updateNode(1),
			},
			expected:           []*v1.Node{nodes[1], nodes[5], nodes[0]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add a few nodes, and remove all of them",
			operations: []operation{
				addNode(0), addNode(2), addNode(5), addNode(6), updateSnapshot(),
				removeNode(0), removeNode(2), removeNode(5), removeNode(6),
			},
			expected:           []*v1.Node{},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add a few nodes, and remove some of them",
			operations: []operation{
				addNode(0), addNode(2), addNode(5), addNode(6), updateSnapshot(),
				removeNode(0), removeNode(6),
			},
			expected:           []*v1.Node{nodes[5], nodes[2]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add a few nodes, remove all of them, and add more",
			operations: []operation{
				addNode(2), addNode(5), addNode(6), updateSnapshot(),
				removeNode(2), removeNode(5), removeNode(6), updateSnapshot(),
				addNode(7), addNode(9),
			},
			expected:           []*v1.Node{nodes[9], nodes[7]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Update nodes in particular order",
			operations: []operation{
				addNode(8), updateNode(2), updateNode(8), updateSnapshot(),
				addNode(1),
			},
			expected:           []*v1.Node{nodes[1], nodes[8], nodes[2]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add some nodes and some pods",
			operations: []operation{
				addNode(0), addNode(2), addNode(8), updateSnapshot(),
				addPod(8), addPod(2),
			},
			expected:           []*v1.Node{nodes[2], nodes[8], nodes[0]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Updating a pod moves its node to the head",
			operations: []operation{
				addNode(0), addPod(0), addNode(2), addNode(4), updatePod(0),
			},
			expected:           []*v1.Node{nodes[0], nodes[4], nodes[2]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add pod before its node",
			operations: []operation{
				addNode(0), addPod(1), updatePod(1), addNode(1),
			},
			expected:           []*v1.Node{nodes[1], nodes[0]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Remove node before its pods",
			operations: []operation{
				addNode(0), addNode(1), addPod(1), addPod(11), updateSnapshot(),
				removeNode(1), updateSnapshot(),
				updatePod(1), updatePod(11), removePod(1), removePod(11),
			},
			expected:           []*v1.Node{nodes[0]},
			expectedUsedPVCSet: sets.NewString(),
		},
		{
			name: "Add Pods with affinity",
			operations: []operation{
				addNode(0), addPodWithAffinity(0), updateSnapshot(), addNode(1),
			},
			expected:                     []*v1.Node{nodes[1], nodes[0]},
			expectedHavePodsWithAffinity: 1,
			expectedUsedPVCSet:           sets.NewString(),
		},
		{
			name: "Add multiple nodes with pods with affinity",
			operations: []operation{
				addNode(0), addPodWithAffinity(0), updateSnapshot(), addNode(1), addPodWithAffinity(1), updateSnapshot(),
			},
			expected:                     []*v1.Node{nodes[1], nodes[0]},
			expectedHavePodsWithAffinity: 2,
			expectedUsedPVCSet:           sets.NewString(),
		},
		{
			name: "Add then Remove pods with affinity",
			operations: []operation{
				addNode(0), addNode(1), addPodWithAffinity(0), updateSnapshot(), removePodWithAffinity(0), updateSnapshot(),
			},
			expected:                     []*v1.Node{nodes[0], nodes[1]},
			expectedHavePodsWithAffinity: 0,
			expectedUsedPVCSet:           sets.NewString(),
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			client := kubefake.NewSimpleClientset()
			informerFactory := informers.NewSharedInformerFactory(client, 0)
			eventBroadcaster := events.NewBroadcaster(&events.EventSinkImpl{Interface: client.EventsV1()})

			stopCh := make(chan struct{})
			defer close(stopCh)
			s, err := scheduler.New(
				client,
				informerFactory,
				profile.NewRecorderFactory(eventBroadcaster),
				stopCh,
			)
			assert.NoError(t, err)
			assert.NotNil(t, s)

			sched = s
			snapshot = NewTemporarySnapshot()

			for _, op := range test.operations {
				op(t)
			}

			if len(test.expected) != sched.SchedulerCache.NodeCount() {
				t.Errorf("unexpected number of nodes. Expected: %v, got: %v", len(test.expected), sched.SchedulerCache.NodeCount())
			}

			// Check number of nodes with pods with affinity
			if len(snapshot.havePodsWithAffinityNodeInfoList) != test.expectedHavePodsWithAffinity {
				t.Errorf("unexpected number of HavePodsWithAffinity nodes. Expected: %v, got: %v", test.expectedHavePodsWithAffinity, len(snapshot.havePodsWithAffinityNodeInfoList))
			}

			// Always update the snapshot at the end of operations and compare it.
			fsl := newFakeSharedListerWithDump(sched)
			if err := snapshot.UpdateSnapshot(fsl); err != nil {
				t.Error(err)
			}
			if err := compareCacheWithNodeInfoSnapshot(t, fsl, snapshot); err != nil {
				t.Error(err)
			}
		})
	}
}

func compareCacheWithNodeInfoSnapshot(t *testing.T, fsl *fakeSharedListerWithDump, snapshot *TemporarySnapshot) error {
	// Compare the map.
	if len(snapshot.nodeInfoMap) != len(fsl.nodeInfos) {
		return fmt.Errorf("unexpected number of nodes in the snapshot. Expected: %v, got: %v", len(fsl.nodes), len(snapshot.nodeInfoMap))
	}
	for name, ni := range fsl.nodes {
		want := ni
		if want.Node() == nil {
			want = nil
		}
		if !reflect.DeepEqual(snapshot.nodeInfoMap[name], want) {
			return fmt.Errorf("unexpected node info for node %q.Expected:\n%v, got:\n%v", name, ni, snapshot.nodeInfoMap[name])
		}
	}

	// Compare the lists.
	if len(snapshot.nodeInfoList) != len(fsl.nodeInfos) {
		return fmt.Errorf("unexpected number of nodes in NodeInfoList. Expected: %v, got: %v", len(fsl.nodes), len(snapshot.nodeInfoList))
	}

	expectedNodeInfoList := make([]*framework.NodeInfo, 0, len(fsl.nodeInfos))
	expectedHavePodsWithAffinityNodeInfoList := make([]*framework.NodeInfo, 0, len(fsl.nodeInfos))
	for _, nodeName := range fsl.nodeNames {
		if n := snapshot.nodeInfoMap[nodeName]; n != nil {
			expectedNodeInfoList = append(expectedNodeInfoList, n)
			if len(n.PodsWithAffinity) > 0 {
				expectedHavePodsWithAffinityNodeInfoList = append(expectedHavePodsWithAffinityNodeInfoList, n)
			}
		} else {
			return fmt.Errorf("node %q exist in nodeTree but not in NodeInfoMap, this should not happen", nodeName)
		}
	}

	for i, expected := range expectedNodeInfoList {
		got := snapshot.nodeInfoList[i]
		if expected != got {
			return fmt.Errorf("unexpected NodeInfo pointer in NodeInfoList. Expected: %p, got: %p", expected, got)
		}
	}

	for i, expected := range expectedHavePodsWithAffinityNodeInfoList {
		got := snapshot.havePodsWithAffinityNodeInfoList[i]
		if expected != got {
			return fmt.Errorf("unexpected NodeInfo pointer in HavePodsWithAffinityNodeInfoList. Expected: %p, got: %p", expected, got)
		}
	}

	return nil
}

func TestSchedulerCache_updateNodeInfoSnapshotList(t *testing.T) {
	// Create a few nodes to be used in tests.
	var nodes []*v1.Node
	i := 0
	// List of number of nodes per zone, zone 0 -> 2, zone 1 -> 6
	for zone, nb := range []int{2, 6} {
		for j := 0; j < nb; j++ {
			nodes = append(nodes, &v1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: fmt.Sprintf("node-%d", i),
					Labels: map[string]string{
						v1.LabelTopologyRegion: fmt.Sprintf("region-%d", zone),
						v1.LabelTopologyZone:   fmt.Sprintf("zone-%d", zone),
					},
				},
			})
			i++
		}
	}

	var sched *scheduler.Scheduler
	var snapshot *TemporarySnapshot

	addNode := func(t *testing.T, i int) {
		nodeInfo := sched.SchedulerCache.AddNode(nodes[i])
		_, ok := snapshot.nodeInfoMap[nodes[i].Name]
		if !ok {
			snapshot.nodeInfoMap[nodes[i].Name] = nodeInfo
		}
	}

	updateSnapshot := func(t *testing.T) {
		fsl := newFakeSharedListerWithDump(sched)
		snapshot.updateNodeInfoSnapshotList(fsl.nodeInfos, true)
		if err := compareCacheWithNodeInfoSnapshot(t, fsl, snapshot); err != nil {
			t.Error(err)
		}
	}

	tests := []struct {
		name       string
		operations func(t *testing.T)
		expected   []string
	}{
		{
			name:       "Empty cache",
			operations: func(t *testing.T) {},
			expected:   []string{},
		},
		{
			name: "Single node",
			operations: func(t *testing.T) {
				addNode(t, 0)
			},
			expected: []string{"node-0"},
		},
		{
			name: "Two nodes",
			operations: func(t *testing.T) {
				addNode(t, 0)
				updateSnapshot(t)
				addNode(t, 1)
			},
			expected: []string{"node-0", "node-1"},
		},
		{
			name: "bug 91601, two nodes, update the snapshot and add two nodes in different zones",
			operations: func(t *testing.T) {
				addNode(t, 2)
				addNode(t, 3)
				updateSnapshot(t)
				addNode(t, 4)
				addNode(t, 0)
			},
			expected: []string{"node-0", "node-2", "node-3", "node-4"},
		},
		{
			name: "bug 91601, 6 nodes, one in a different zone",
			operations: func(t *testing.T) {
				addNode(t, 2)
				addNode(t, 3)
				addNode(t, 4)
				addNode(t, 5)
				updateSnapshot(t)
				addNode(t, 6)
				addNode(t, 0)
			},
			expected: []string{"node-0", "node-2", "node-3", "node-4", "node-5", "node-6"},
		},
		{
			name: "bug 91601, 7 nodes, two in a different zone",
			operations: func(t *testing.T) {
				addNode(t, 2)
				updateSnapshot(t)
				addNode(t, 3)
				addNode(t, 4)
				updateSnapshot(t)
				addNode(t, 5)
				addNode(t, 6)
				addNode(t, 0)
				addNode(t, 1)
			},
			expected: []string{"node-0", "node-2", "node-1", "node-3", "node-4", "node-5", "node-6"},
		},
		{
			name: "bug 91601, 7 nodes, two in a different zone, different zone order",
			operations: func(t *testing.T) {
				addNode(t, 2)
				addNode(t, 1)
				updateSnapshot(t)
				addNode(t, 3)
				addNode(t, 4)
				updateSnapshot(t)
				addNode(t, 5)
				addNode(t, 6)
				addNode(t, 0)
			},
			expected: []string{"node-0", "node-2", "node-1", "node-3", "node-4", "node-5", "node-6"},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			client := kubefake.NewSimpleClientset()
			informerFactory := informers.NewSharedInformerFactory(client, 0)
			eventBroadcaster := events.NewBroadcaster(&events.EventSinkImpl{Interface: client.EventsV1()})

			stopCh := make(chan struct{})
			defer close(stopCh)
			s, err := scheduler.New(
				client,
				informerFactory,
				profile.NewRecorderFactory(eventBroadcaster),
				stopCh,
			)
			assert.NoError(t, err)
			assert.NotNil(t, s)

			sched = s
			snapshot = NewTemporarySnapshot()

			test.operations(t)

			// Always update the snapshot at the end of operations and compare it.
			fsl := newFakeSharedListerWithDump(sched)
			snapshot.updateNodeInfoSnapshotList(fsl.nodeInfos, true)
			if err := compareCacheWithNodeInfoSnapshot(t, fsl, snapshot); err != nil {
				t.Error(err)
			}
			nodeNames := make([]string, len(snapshot.nodeInfoList))
			for i, nodeInfo := range snapshot.nodeInfoList {
				nodeNames[i] = nodeInfo.Node().Name
			}
			if !reflect.DeepEqual(nodeNames, test.expected) {
				t.Errorf("The nodeInfoList is incorrect. Expected %v , got %v", test.expected, nodeNames)
			}
		})
	}
}
