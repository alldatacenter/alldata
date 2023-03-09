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

package nodenumaresource

import (
	"context"

	nrtv1alpha1 "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/apis/topology/v1alpha1"
	nrtclientset "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/clientset/versioned"
	nrtinformers "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/informers/externalversions"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	"github.com/koordinator-sh/koordinator/apis/extension"
	frameworkexthelper "github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/helper"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

type nodeResourceTopologyEventHandler struct {
	topologyManager CPUTopologyManager
}

func registerNodeResourceTopologyEventHandler(handle framework.Handle, topologyManager CPUTopologyManager) error {
	nrtClient, ok := handle.(nrtclientset.Interface)
	if !ok {
		kubeConfig := *handle.KubeConfig()
		kubeConfig.ContentType = runtime.ContentTypeJSON
		kubeConfig.AcceptContentTypes = runtime.ContentTypeJSON
		var err error
		nrtClient, err = nrtclientset.NewForConfig(&kubeConfig)
		if err != nil {
			return err
		}
	}

	nodeResTopologyInformerFactory := nrtinformers.NewSharedInformerFactoryWithOptions(nrtClient, 0)
	nodeResTopologyInformer := nodeResTopologyInformerFactory.Topology().V1alpha1().NodeResourceTopologies().Informer()
	eventHandler := &nodeResourceTopologyEventHandler{
		topologyManager: topologyManager,
	}
	frameworkexthelper.ForceSyncFromInformer(context.TODO().Done(), nodeResTopologyInformerFactory, nodeResTopologyInformer, eventHandler)
	return nil
}

func (m *nodeResourceTopologyEventHandler) OnAdd(obj interface{}) {
	nodeResTopology, ok := obj.(*nrtv1alpha1.NodeResourceTopology)
	if !ok {
		return
	}
	m.updateNodeResourceTopology(nil, nodeResTopology)
}

func (m *nodeResourceTopologyEventHandler) OnUpdate(oldObj, newObj interface{}) {
	oldNodeResTopology, ok := oldObj.(*nrtv1alpha1.NodeResourceTopology)
	if !ok {
		return
	}

	nodeResTopology, ok := newObj.(*nrtv1alpha1.NodeResourceTopology)
	if !ok {
		return
	}
	m.updateNodeResourceTopology(oldNodeResTopology, nodeResTopology)
}

func (m *nodeResourceTopologyEventHandler) OnDelete(obj interface{}) {
	var nodeResTopology *nrtv1alpha1.NodeResourceTopology
	switch t := obj.(type) {
	case *nrtv1alpha1.NodeResourceTopology:
		nodeResTopology = t
	case cache.DeletedFinalStateUnknown:
		var ok bool
		nodeResTopology, ok = t.Obj.(*nrtv1alpha1.NodeResourceTopology)
		if !ok {
			return
		}
	default:
		break
	}

	if nodeResTopology == nil {
		return
	}
	m.topologyManager.Delete(nodeResTopology.Name)
}

func (m *nodeResourceTopologyEventHandler) updateNodeResourceTopology(oldNodeResTopology, newNodeResTopology *nrtv1alpha1.NodeResourceTopology) {
	podCPUAllocs, err := extension.GetPodCPUAllocs(newNodeResTopology.Annotations)
	if err != nil {
		klog.Errorf("Failed to GetPodCPUAllocs from new NodeResourceTopology %s, err: %v", newNodeResTopology.Name, err)
	}

	kubeletPolicy, err := extension.GetKubeletCPUManagerPolicy(newNodeResTopology.Annotations)
	if err != nil {
		klog.Errorf("Failed to GetKubeletCPUManagerPolicy from NodeResourceTopology %s, err: %v", newNodeResTopology.Name, err)
	}
	var kubeletReservedCPUs cpuset.CPUSet
	if kubeletPolicy != nil {
		kubeletReservedCPUs, err = cpuset.Parse(kubeletPolicy.ReservedCPUs)
		if err != nil {
			klog.Errorf("Failed to Parse kubelet reserved CPUs %s, err: %v", kubeletPolicy.ReservedCPUs, err)
		}
	}

	reportedCPUTopology, err := extension.GetCPUTopology(newNodeResTopology.Annotations)
	if err != nil {
		klog.Errorf("Failed to GetCPUTopology, name: %s, err: %v", newNodeResTopology.Name, err)
	}

	cpuTopology := convertCPUTopology(reportedCPUTopology)
	reservedCPUs := m.getPodAllocsCPUSet(podCPUAllocs)
	reservedCPUs = reservedCPUs.Union(kubeletReservedCPUs)

	nodeName := newNodeResTopology.Name
	m.topologyManager.UpdateCPUTopologyOptions(nodeName, func(options *CPUTopologyOptions) {
		*options = CPUTopologyOptions{
			CPUTopology:  cpuTopology,
			ReservedCPUs: reservedCPUs,
			Policy:       kubeletPolicy,
			MaxRefCount:  options.MaxRefCount,
		}
	})
}

func (m *nodeResourceTopologyEventHandler) getPodAllocsCPUSet(podCPUAllocs extension.PodCPUAllocs) cpuset.CPUSet {
	if len(podCPUAllocs) == 0 {
		return cpuset.CPUSet{}
	}
	builder := cpuset.NewCPUSetBuilder()
	for _, v := range podCPUAllocs {
		if !v.ManagedByKubelet || v.UID == "" || v.CPUSet == "" {
			continue
		}
		cpuset, err := cpuset.Parse(v.CPUSet)
		if err != nil || cpuset.IsEmpty() {
			continue
		}
		builder.Add(cpuset.ToSliceNoSort()...)
	}
	return builder.Result()
}

func convertCPUTopology(reportedCPUTopology *extension.CPUTopology) *CPUTopology {
	builder := NewCPUTopologyBuilder()
	for _, info := range reportedCPUTopology.Detail {
		builder.AddCPUInfo(int(info.Socket), int(info.Node), int(info.Core), int(info.ID))
	}
	return builder.Result()
}
