// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package resourceinfo

import (
	"fmt"
	"net"
	"strconv"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/node"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/pod"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/strutil"
)

type ResourceInfo struct {
	podutil  *pod.Pod
	nodeutil *node.Node
	addr     string
	client   *httpclient.HTTPClient
}

func New(addr string, client *httpclient.HTTPClient) *ResourceInfo {
	podutil := pod.New(pod.WithCompleteParams(addr, client))
	nodeutil := node.New(addr, client)
	return &ResourceInfo{addr: addr, client: client, podutil: podutil, nodeutil: nodeutil}
}

// PARAM brief: Does not provide cpuusage, memusage data, reducing the overhead of calling k8sapi
func (ri *ResourceInfo) Get(brief bool) (apistructs.ClusterResourceInfoData, error) {
	podlist := &v1.PodList{Items: nil}
	if !brief {
		var err error
		podlist, err = ri.podutil.ListAllNamespace([]string{"status.phase!=Succeeded", "status.phase!=Failed"})
		if err != nil {
			return apistructs.ClusterResourceInfoData{}, nil
		}
	}
	nodelist, err := ri.nodeutil.List()
	if err != nil {
		logrus.Errorf("failed to list nodes: %v", err)
		return apistructs.ClusterResourceInfoData{}, nil
	}
	podmap := splitPodsByNodeName(podlist)
	nodeResourceInfoMap := map[string]*apistructs.NodeResourceInfo{}
	for _, node := range nodelist.Items {
		var ip net.IP
		for _, addr := range node.Status.Addresses {
			if addr.Type == v1.NodeInternalIP {
				ip = net.ParseIP(addr.Address)
			}
		}
		if ip == nil {
			// ignore, if internalIP not found
			continue
		}
		nodeResourceInfoMap[ip.String()] = &apistructs.NodeResourceInfo{}
		info := nodeResourceInfoMap[ip.String()]
		info.Labels = nodeLabels(&node)
		info.Ready = nodeReady(&node)
		cpuAllocatable, err := strconv.ParseFloat(fmt.Sprintf("%f", node.Status.Allocatable.Cpu().AsDec()), 64)
		if err != nil {
			return apistructs.ClusterResourceInfoData{}, err
		}
		memAllocatable, _ := node.Status.Allocatable.Memory().AsInt64()
		info.CPUAllocatable = cpuAllocatable
		info.MemAllocatable = memAllocatable
		pods := podmap[node.Name]
		podlist := &v1.PodList{Items: pods}
		reqs, limits := getPodsTotalRequestsAndLimits(podlist)
		cpuReqs, cpuLimit, memReqs, memLimit := reqs[v1.ResourceCPU], limits[v1.ResourceCPU], reqs[v1.ResourceMemory], limits[v1.ResourceMemory]
		cpuReqsNum, err := strconv.ParseFloat(fmt.Sprintf("%f", cpuReqs.AsDec()), 64)
		if err != nil {
			return apistructs.ClusterResourceInfoData{}, err
		}
		memReqsNum, _ := memReqs.AsInt64()
		cpuLimitNum, err := strconv.ParseFloat(fmt.Sprintf("%f", cpuLimit.AsDec()), 64)
		if err != nil {
			return apistructs.ClusterResourceInfoData{}, err
		}
		memLimitNum, _ := memLimit.AsInt64()
		info.CPUReqsUsage = cpuReqsNum
		info.CPULimitUsage = cpuLimitNum
		info.MemReqsUsage = memReqsNum
		info.MemLimitUsage = memLimitNum
	}

	return apistructs.ClusterResourceInfoData{Nodes: nodeResourceInfoMap}, nil
}

func splitPodsByNodeName(podlist *v1.PodList) map[string][]v1.Pod {
	podmap := map[string][]v1.Pod{}
	for i := range podlist.Items {
		if _, ok := podmap[podlist.Items[i].Spec.NodeName]; ok {
			podmap[podlist.Items[i].Spec.NodeName] = append(podmap[podlist.Items[i].Spec.NodeName], podlist.Items[i])
		} else {
			podmap[podlist.Items[i].Spec.NodeName] = []v1.Pod{podlist.Items[i]}
		}
	}
	return podmap
}

func nodeLabels(n *v1.Node) []string {
	r := []string{}
	for k := range n.ObjectMeta.Labels {
		if strutil.HasPrefixes(k, "dice/") {
			r = append(r, k)
		}
	}
	return r
}

func nodeReady(n *v1.Node) bool {
	for _, cond := range n.Status.Conditions {
		if cond.Type == v1.NodeReady {
			if cond.Status == "True" {
				return true
			}
		}
	}
	return false
}

// copy from kubectl
func getPodsTotalRequestsAndLimits(podList *v1.PodList) (reqs map[v1.ResourceName]resource.Quantity, limits map[v1.ResourceName]resource.Quantity) {
	reqs, limits = map[v1.ResourceName]resource.Quantity{}, map[v1.ResourceName]resource.Quantity{}
	for _, pod := range podList.Items {
		podReqs, podLimits := PodRequestsAndLimits(&pod)
		for podReqName, podReqValue := range podReqs {
			if value, ok := reqs[podReqName]; !ok {
				reqs[podReqName] = podReqValue.DeepCopy()
			} else {
				value.Add(podReqValue)
				reqs[podReqName] = value
			}
		}
		for podLimitName, podLimitValue := range podLimits {
			if value, ok := limits[podLimitName]; !ok {
				limits[podLimitName] = podLimitValue.DeepCopy()
			} else {
				value.Add(podLimitValue)
				limits[podLimitName] = value
			}
		}
	}
	return
}

// copy from kubectl
// PodRequestsAndLimits returns a dictionary of all defined resources summed up for all
// containers of the pod.
func PodRequestsAndLimits(pod *v1.Pod) (reqs, limits v1.ResourceList) {
	reqs, limits = v1.ResourceList{}, v1.ResourceList{}
	for _, container := range pod.Spec.Containers {
		addResourceList(reqs, container.Resources.Requests)
		addResourceList(limits, container.Resources.Limits)
	}
	// init containers define the minimum of any resource
	for _, container := range pod.Spec.InitContainers {
		maxResourceList(reqs, container.Resources.Requests)
		maxResourceList(limits, container.Resources.Limits)
	}
	return
}

// copy from kubectl
// addResourceList adds the resources in newList to list
func addResourceList(list, new v1.ResourceList) {
	for name, quantity := range new {
		if value, ok := list[name]; !ok {
			list[name] = quantity.DeepCopy()
		} else {
			value.Add(quantity)
			list[name] = value
		}
	}
}

// copy from kubectl
// maxResourceList sets list to the greater of list/newList for every resource
// either list
func maxResourceList(list, new v1.ResourceList) {
	for name, quantity := range new {
		if value, ok := list[name]; !ok {
			list[name] = quantity.DeepCopy()
			continue
		} else {
			if quantity.Cmp(value) > 0 {
				list[name] = quantity.DeepCopy()
			}
		}
	}
}
