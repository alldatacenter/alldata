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

package statesinformer

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"reflect"
	"sort"
	"sync"

	"github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/apis/topology/v1alpha1"
	topov1alpha1 "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/apis/topology/v1alpha1"
	topologyclientset "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/clientset/versioned"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/kubelet/cm/cpumanager"
	"k8s.io/kubernetes/pkg/kubelet/cm/cpumanager/state"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/features"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/kubelet"
	"github.com/koordinator-sh/koordinator/pkg/util"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

const (
	nodeTopoInformerName pluginName = "nodeTopoInformer"
)

type nodeTopoInformer struct {
	config         *Config
	topologyClient topologyclientset.Interface
	nodeTopoMutex  sync.RWMutex
	nodeTopology   *topov1alpha1.NodeResourceTopology

	metricCache    metriccache.MetricCache
	callbackRunner *callbackRunner

	kubelet      KubeletStub
	nodeInformer *nodeInformer
	podsInformer *podsInformer
}

func NewNodeTopoInformer() *nodeTopoInformer {
	return &nodeTopoInformer{}
}

func (s *nodeTopoInformer) GetNodeTopo() *topov1alpha1.NodeResourceTopology {
	s.nodeTopoMutex.RLock()
	defer s.nodeTopoMutex.RUnlock()
	return s.nodeTopology.DeepCopy()
}

func (s *nodeTopoInformer) Setup(ctx *pluginOption, state *pluginState) {
	s.config = ctx.config
	s.topologyClient = ctx.TopoClient
	s.metricCache = state.metricCache
	s.callbackRunner = state.callbackRunner

	nodeInformerIf := state.informerPlugins[nodeInformerName]
	nodeInformer, ok := nodeInformerIf.(*nodeInformer)
	if !ok {
		klog.Fatalf("node informer format error")
	}
	s.nodeInformer = nodeInformer

	podsInformerIf := state.informerPlugins[podsInformerName]
	podsInformer, ok := podsInformerIf.(*podsInformer)
	if !ok {
		klog.Fatalf("pods informer format error")
	}
	s.podsInformer = podsInformer
}

func (s *nodeTopoInformer) Start(stopCh <-chan struct{}) {
	klog.V(2).Infof("starting node topo informer")
	if !cache.WaitForCacheSync(stopCh, s.nodeInformer.HasSynced, s.podsInformer.HasSynced) {
		klog.Fatalf("timed out waiting for pod caches to sync")
	}
	if s.config.NodeTopologySyncInterval <= 0 {
		return
	}
	stub, err := newKubeletStubFromConfig(s.nodeInformer.GetNode(), s.config)
	if err != nil {
		klog.Fatalf("create kubelet stub, %v", err)
	}
	s.kubelet = stub
	go wait.Until(s.reportNodeTopology, s.config.NodeTopologySyncInterval, stopCh)
	klog.V(2).Infof("node topo informer started")
}

func (s *nodeTopoInformer) HasSynced() bool {
	return true
}

func (s *nodeTopoInformer) createNodeTopoIfNotExist() {
	node := s.nodeInformer.GetNode()
	topologyName := node.Name
	ctx := context.TODO()
	_, err := s.topologyClient.TopologyV1alpha1().NodeResourceTopologies().Get(ctx, topologyName, metav1.GetOptions{ResourceVersion: "0"})
	if err == nil {
		return
	}
	if !errors.IsNotFound(err) {
		klog.Errorf("failed to get NodeResourceTopology %s, err: %v", topologyName, err)
		return
	}

	topology := newNodeTopo(node)
	// TODO: add retry if create fail
	_, err = s.topologyClient.TopologyV1alpha1().NodeResourceTopologies().Create(ctx, topology, metav1.CreateOptions{})
	if err != nil {
		klog.Errorf("failed to create NodeResourceTopology %s, err: %v", topologyName, err)
		return
	}
}

func (s *nodeTopoInformer) calcNodeTopo() (map[string]string, error) {
	nodeCPUInfo, cpuTopology, sharedPoolCPUs, err := s.calCPUTopology()
	if err != nil {
		return nil, fmt.Errorf("failed to calculate cpu topology, err: %v", err)
	}

	var cpuManagerPolicy extension.KubeletCPUManagerPolicy
	if s.config != nil && !s.config.DisableQueryKubeletConfig {
		kubeletConfiguration, err := s.kubelet.GetKubeletConfiguration()
		if err != nil {
			return nil, fmt.Errorf("failed to GetKubeletConfiguration, err: %v", err)
		}
		klog.V(5).Infof("kubelet args: %v", kubeletConfiguration)

		// default policy is none
		cpuManagerPolicy = extension.KubeletCPUManagerPolicy{
			Policy:  kubeletConfiguration.CPUManagerPolicy,
			Options: kubeletConfiguration.CPUManagerPolicyOptions,
		}

		if kubeletConfiguration.CPUManagerPolicy == string(cpumanager.PolicyStatic) {
			topology := kubelet.NewCPUTopology((*koordletutil.LocalCPUInfo)(nodeCPUInfo))
			reservedCPUs, err := kubelet.GetStaticCPUManagerPolicyReservedCPUs(topology, kubeletConfiguration)
			if err != nil {
				klog.Errorf("Failed to GetStaticCPUManagerPolicyReservedCPUs, err: %v", err)
			}
			cpuManagerPolicy.ReservedCPUs = reservedCPUs.String()

			// NOTE: We should not remove reservedCPUs from sharedPoolCPUs to
			//  ensure that Burstable Pods (e.g. Pods request 0C but are limited to 4C)
			//  at least there are reservedCPUs available when nodes are allocated
		}
	}

	cpuManagerPolicyJSON, err := json.Marshal(cpuManagerPolicy)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal cpu manager policy, err: %v", err)
	}

	// Users can specify the kubelet RootDirectory on the host in the koordlet DaemonSet,
	// but inside koordlet it is always mounted to the path /var/lib/kubelet
	stateFilePath := kubelet.GetCPUManagerStateFilePath("/var/lib/kubelet")
	data, err := os.ReadFile(stateFilePath)
	if err != nil {
		if !os.IsNotExist(err) {
			return nil, fmt.Errorf("failed to read state file, err: %v", err)
		}
	}
	// TODO: report lse/lsr pod from cgroup
	var podAllocsJSON []byte
	if len(data) > 0 {
		podAllocs, err := s.calGuaranteedCpu(sharedPoolCPUs, string(data))
		if err != nil {
			return nil, fmt.Errorf("failed to cal GuaranteedCpu, err: %v", err)
		}
		if len(podAllocs) != 0 {
			podAllocsJSON, err = json.Marshal(podAllocs)
			if err != nil {
				return nil, fmt.Errorf("failed to marshal pod allocs, err: %v", err)
			}
		}
	}

	cpuTopologyJSON, err := json.Marshal(cpuTopology)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal cpu topology of node, err: %v", err)
	}

	sharePools := s.calCPUSharePools(sharedPoolCPUs)
	cpuSharePoolsJSON, err := json.Marshal(sharePools)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal cpushare pools of node, err: %v", err)
	}

	annotations := map[string]string{}
	annotations[extension.AnnotationNodeCPUTopology] = string(cpuTopologyJSON)
	annotations[extension.AnnotationNodeCPUSharedPools] = string(cpuSharePoolsJSON)
	annotations[extension.AnnotationKubeletCPUManagerPolicy] = string(cpuManagerPolicyJSON)
	if len(podAllocsJSON) != 0 {
		annotations[extension.AnnotationNodeCPUAllocs] = string(podAllocsJSON)
	}

	return annotations, nil
}

func (s *nodeTopoInformer) calGuaranteedCpu(usedCPUs map[int32]*extension.CPUInfo, stateJSON string) ([]extension.PodCPUAlloc, error) {
	if stateJSON == "" {
		return nil, fmt.Errorf("empty state file")
	}
	checkpoint := &state.CPUManagerCheckpoint{}
	err := json.Unmarshal([]byte(stateJSON), checkpoint)
	if err != nil {
		return nil, err
	}

	pods := make(map[types.UID]*PodMeta)
	managedPods := make(map[types.UID]struct{})
	for _, podMeta := range s.podsInformer.GetAllPods() {
		pods[podMeta.Pod.UID] = podMeta
		qosClass := extension.GetPodQoSClass(podMeta.Pod)
		if qosClass == extension.QoSLS || qosClass == extension.QoSBE {
			managedPods[podMeta.Pod.UID] = struct{}{}
			continue
		}
		resourceStatus, err := extension.GetResourceStatus(podMeta.Pod.Annotations)
		if err == nil {
			set, err := cpuset.Parse(resourceStatus.CPUSet)
			if err == nil && set.Size() > 0 {
				managedPods[podMeta.Pod.UID] = struct{}{}
			}
		}
	}

	var podAllocs []extension.PodCPUAlloc
	for podUID := range checkpoint.Entries {
		if _, ok := managedPods[types.UID(podUID)]; ok {
			continue
		}
		cpuSet := cpuset.NewCPUSet()
		for container, cpuString := range checkpoint.Entries[podUID] {
			if containerCPUSet, err := cpuset.Parse(cpuString); err != nil {
				klog.Errorf("could not parse cpuset %q for container %q in pod %q: %v", cpuString, container, podUID, err)
				continue
			} else if containerCPUSet.Size() > 0 {
				cpuSet = cpuSet.Union(containerCPUSet)
			}
		}
		if cpuSet.IsEmpty() {
			continue
		}

		// TODO: It is possible that the data in the checkpoint file is invalid
		//  and should be checked with the data in the cgroup to determine whether it is consistent
		podCPUAlloc := extension.PodCPUAlloc{
			UID:              types.UID(podUID),
			CPUSet:           cpuSet.String(),
			ManagedByKubelet: true,
		}
		podMeta := pods[types.UID(podUID)]
		if podMeta != nil {
			podCPUAlloc.Namespace = podMeta.Pod.Namespace
			podCPUAlloc.Name = podMeta.Pod.Name
		}
		podAllocs = append(podAllocs, podCPUAlloc)

		for _, cpuID := range cpuSet.ToSliceNoSort() {
			delete(usedCPUs, int32(cpuID))
		}
	}
	return podAllocs, nil
}

func (s *nodeTopoInformer) reportNodeTopology() {
	klog.Info("start to report node topology")
	// do not CREATE if reporting is disabled,
	// but update the node topo object internally
	if features.DefaultKoordletFeatureGate.Enabled(features.NodeTopologyReport) {
		s.createNodeTopoIfNotExist()
	} else {
		klog.V(5).Infof("feature %v not enabled, node topo will not be reported", features.NodeTopologyReport)
	}

	nodeTopoAnnotations, err := s.calcNodeTopo()
	if err != nil {
		klog.Errorf("failed to calculate node topology, err: %v", err)
		return
	}

	ctx := context.TODO()
	node := s.nodeInformer.GetNode()
	err = util.RetryOnConflictOrTooManyRequests(func() error {
		var nodeResourceTopology *v1alpha1.NodeResourceTopology
		if features.DefaultKoordletFeatureGate.Enabled(features.NodeTopologyReport) {
			nodeResourceTopology, err = s.topologyClient.TopologyV1alpha1().NodeResourceTopologies().Get(ctx, node.Name, metav1.GetOptions{ResourceVersion: "0"})
			if err != nil {
				klog.Errorf("failed to get node resource topology %s, err: %v", node.Name, err)
				return err
			}
		} else {
			nodeResourceTopology = newNodeTopo(node)
		}

		// set fields
		if nodeResourceTopology.Annotations == nil {
			nodeResourceTopology.Annotations = make(map[string]string)
		}
		for k, v := range nodeTopoAnnotations {
			nodeResourceTopology.Annotations[k] = v
		}

		if isSyncNeeded(s.nodeTopology, nodeResourceTopology, node.Name) {
			// TODO: use a NodeResourceTopology informer
			s.updateNodeTopo(nodeResourceTopology)

			// do UPDATE
			if features.DefaultKoordletFeatureGate.Enabled(features.NodeTopologyReport) {
				_, err = s.topologyClient.TopologyV1alpha1().NodeResourceTopologies().Update(context.TODO(), nodeResourceTopology, metav1.UpdateOptions{})
				if err != nil {
					klog.Errorf("failed to update cpu info of node %s, err: %v", node.Name, err)
					return err
				}
			}
			return nil
		}
		return nil
	})
	if err != nil {
		klog.Errorf("failed to update NodeResourceTopology, err: %v", err)
	}
}

func isSyncNeeded(oldNRT, newNRT *v1alpha1.NodeResourceTopology, nodename string) bool {
	if oldNRT == nil || oldNRT.Annotations == nil || newNRT.Annotations == nil {
		return true
	}
	if isEqualTopo(oldNRT.Annotations, newNRT.Annotations) {
		// do nothing
		klog.V(4).Infof("all good, no need to report nodetopo  %s", nodename)
		return false
	}
	//not equal
	klog.V(4).Infof("node %s topology is changed, need sync", nodename)
	return true
}

// IsequalTopo returns whether the new topology has difference with the old one or not
func isEqualTopo(OldTopo map[string]string, NewTopo map[string]string) bool {
	var (
		OldData interface{}
		NewData interface{}
	)
	keyslice := []string{extension.AnnotationKubeletCPUManagerPolicy, extension.AnnotationNodeCPUSharedPools,
		extension.AnnotationNodeCPUTopology, extension.AnnotationNodeCPUAllocs}
	for _, key := range keyslice {
		oldValue, oldExist := OldTopo[key]
		newValue, newExist := NewTopo[key]
		if !oldExist && !newExist {
			// both not exist, no need to compare this key
			continue
		} else if oldExist != newExist {
			// (oldExist = true, newExist = false) OR (oldExist = false, newExist = true), node topo not equal
			return false
		} // else both exist in new and old, compare value
		err := json.Unmarshal([]byte(oldValue), &OldData)
		if err != nil {
			klog.Errorf("failed to unmarshal, err: %v,and key: %v", err, key)
		}
		err1 := json.Unmarshal([]byte(newValue), &NewData)
		if err1 != nil {
			klog.Errorf("failed to unmarshal, err: %v,and key: %v", err1, key)
		}
		if !reflect.DeepEqual(OldData, NewData) {
			return false
		}
	}
	return true
}

func (s *nodeTopoInformer) calCPUSharePools(sharedPoolCPUs map[int32]*extension.CPUInfo) []extension.CPUSharedPool {
	podMetas := s.podsInformer.GetAllPods()
	for _, podMeta := range podMetas {
		status, err := extension.GetResourceStatus(podMeta.Pod.Annotations)
		if err != nil {
			klog.Errorf("failed to get resource status of pod %s, err: %v", podMeta.Pod.Name, err)
			continue
		}
		if status.CPUSet == "" {
			continue
		}

		set, err := cpuset.Parse(status.CPUSet)
		if err != nil {
			klog.Errorf("failed to parse cpuset info of pod %s, err: %v", podMeta.Pod.Name, err)
			continue
		}
		for _, cpuID := range set.ToSliceNoSort() {
			delete(sharedPoolCPUs, int32(cpuID))
		}
	}

	// nodeID -> cpulist
	nodeIDToCpus := make(map[int32][]int)
	for cpuID, info := range sharedPoolCPUs {
		if info != nil {
			nodeIDToCpus[info.Node] = append(nodeIDToCpus[info.Node], int(cpuID))
		}
	}

	var sharePools []extension.CPUSharedPool
	for nodeID, cpus := range nodeIDToCpus {
		if len(cpus) <= 0 {
			continue
		}
		set := cpuset.NewCPUSet(cpus...)
		sharePools = append(sharePools, extension.CPUSharedPool{
			CPUSet: set.String(),
			Node:   nodeID,
			Socket: sharedPoolCPUs[int32(cpus[0])].Socket,
		})
	}
	sort.Slice(sharePools, func(i, j int) bool {
		iPool := sharePools[i]
		jPool := sharePools[j]
		iID := int(iPool.Socket)<<32 | int(iPool.Node)
		jID := int(jPool.Socket)<<32 | int(jPool.Node)
		return iID < jID
	})
	return sharePools
}

func (s *nodeTopoInformer) calCPUTopology() (*metriccache.NodeCPUInfo, *extension.CPUTopology, map[int32]*extension.CPUInfo, error) {
	nodeCPUInfo, err := s.metricCache.GetNodeCPUInfo(&metriccache.QueryParam{})
	if err != nil {
		klog.V(4).Infof("failed to get node cpu info")
		return nil, nil, nil, err
	}

	cpus := make(map[int32]*extension.CPUInfo)
	cpuTopology := &extension.CPUTopology{}
	for _, cpu := range nodeCPUInfo.ProcessorInfos {
		info := extension.CPUInfo{
			ID:     cpu.CPUID,
			Core:   cpu.CoreID,
			Socket: cpu.SocketID,
			Node:   cpu.NodeID,
		}
		cpuTopology.Detail = append(cpuTopology.Detail, info)
		cpus[cpu.CPUID] = &info
	}
	return nodeCPUInfo, cpuTopology, cpus, nil
}

func (s *nodeTopoInformer) updateNodeTopo(newTopo *v1alpha1.NodeResourceTopology) {
	s.setNodeTopo(newTopo)
	klog.V(5).Infof("local node topology info updated %v", newTopo)
	s.callbackRunner.SendCallback(RegisterTypeNodeTopology)
}

func (s *nodeTopoInformer) setNodeTopo(newTopo *v1alpha1.NodeResourceTopology) {
	s.nodeTopoMutex.Lock()
	defer s.nodeTopoMutex.Unlock()
	s.nodeTopology = newTopo.DeepCopy()
}

func newNodeTopo(node *corev1.Node) *v1alpha1.NodeResourceTopology {
	blocker := true
	return &v1alpha1.NodeResourceTopology{
		ObjectMeta: metav1.ObjectMeta{
			Name: node.Name,
			Labels: map[string]string{
				extension.LabelManagedBy: "Koordinator",
			},
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion:         "v1",
					Kind:               "Node",
					Name:               node.Name,
					UID:                node.GetUID(),
					Controller:         &blocker,
					BlockOwnerDeletion: &blocker,
				},
			},
		},
		// fields are required
		TopologyPolicies: []string{string(v1alpha1.None)},
		Zones:            v1alpha1.ZoneList{v1alpha1.Zone{Name: "fake-name", Type: "fake-type"}},
	}
}
