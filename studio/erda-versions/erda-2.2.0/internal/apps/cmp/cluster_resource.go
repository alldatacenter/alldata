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

package cmp

import (
	"context"
	"fmt"
	"time"

	"github.com/rancher/apiserver/pkg/types"
	"github.com/rancher/wrangler/pkg/data"
	"github.com/sirupsen/logrus"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/cache"
)

func (p *provider) GetClustersResources(ctx context.Context, cReq *pb.GetClustersResourcesRequest) (*pb.GetClusterResourcesResponse, error) {
	resp := &pb.GetClusterResourcesResponse{}
	for _, clusterName := range cReq.ClusterNames {
		detail := &pb.ClusterResourceDetail{ClusterName: clusterName}
		nodesList, err := p.ListSteveResource(ctx, &apistructs.SteveRequest{
			NoAuthentication: true,
			Type:             apistructs.K8SNode,
			ClusterName:      clusterName,
		})
		if err != nil {
			logrus.Errorf("failed to get cluster resource for cluster %s, %v", clusterName, err)
			detail.Err = err.Error()
			resp.List = append(resp.List, detail)
			continue
		}
		var nodes []data.Object
		for _, obj := range nodesList {
			nodes = append(nodes, obj.Data())
		}

		nodesAllocatedRes, err := GetNodesAllocatedRes(ctx, p, true, clusterName, "", "", nodes)
		if err != nil {
			logrus.Errorf("failed to get nodes allocated resource for cluster %s, %v", clusterName, err)
			detail.Err = err.Error()
			resp.List = append(resp.List, detail)
			continue
		}
		for _, node := range nodes {
			if IsVirtualNode(node) {
				continue
			}
			nodeName := node.String("metadata", "name")
			allocatableCPUQty, _ := resource.ParseQuantity(node.String("status", "allocatable", "cpu"))
			allocatableMemQty, _ := resource.ParseQuantity(node.String("status", "allocatable", "memory"))
			capacityCPUQty, _ := resource.ParseQuantity(node.String("status", "capacity", "cpu"))
			capacityMemQty, _ := resource.ParseQuantity(node.String("status", "capacity", "memory"))

			allocatableCPU := allocatableCPUQty.MilliValue()
			allocatableMem := allocatableMemQty.Value()
			capacityCPU := capacityCPUQty.MilliValue()
			capacityMem := capacityMemQty.Value()
			allocatedCPU := nodesAllocatedRes[nodeName].CPU
			allocatedMem := nodesAllocatedRes[nodeName].Mem

			labels := node.Map("metadata", "labels")
			var labelArr []string
			for k, v := range labels {
				labelArr = append(labelArr, fmt.Sprintf("%s=%s", k, v))
			}
			detail.Hosts = append(detail.Hosts, &pb.HostResourceDetail{
				Host:           nodeName,
				CpuAllocatable: uint64(allocatableCPU),
				CpuTotal:       uint64(capacityCPU),
				CpuRequest:     uint64(allocatedCPU),
				MemAllocatable: uint64(allocatableMem),
				MemTotal:       uint64(capacityMem),
				MemRequest:     uint64(allocatedMem),
				Labels:         labelArr,
			})
		}
		detail.Success = true
		resp.List = append(resp.List, detail)
	}
	resp.Total = uint32(len(resp.List))
	return resp, nil
}

func (p *provider) GetNamespacesResources(ctx context.Context, nReq *pb.GetNamespacesResourcesRequest) (*pb.GetNamespacesResourcesResponse, error) {
	resp := &pb.GetNamespacesResourcesResponse{}
	nss := make(map[string][]string)
	for _, req := range nReq.Namespaces {
		nss[req.ClusterName] = append(nss[req.ClusterName], req.Namespace)
	}

	for cluster, namespaces := range nss {
		item := &pb.ClusterResourceItem{ClusterName: cluster}
		nsAllocatableRes, err := GetNamespaceAllocatedRes(ctx, p, true, cluster, "", "", namespaces)
		if err != nil {
			logrus.Errorf("failed to get namespace allocated resource for cluster %s", cluster)
			item.Err = err.Error()
			resp.List = append(resp.List, item)
			continue
		}
		for _, namespace := range namespaces {
			item.List = append(item.List, &pb.NamespaceResourceDetail{
				Namespace:  namespace,
				CpuRequest: uint64(nsAllocatableRes[namespace].CPU),
				MemRequest: uint64(nsAllocatableRes[namespace].Mem),
			})
		}
		item.Success = true
		resp.List = append(resp.List, item)
	}
	resp.Total = uint32(len(resp.List))
	return resp, nil
}

type AllocatedRes struct {
	CPU    int64 `json:"cpu"`
	Mem    int64 `json:"mem"`
	PodNum int64 `json:"podNum"`
}

const (
	nodeCacheType = "nodeAllocatedRes"
	nsCacheType   = "nsAllocatedRes"
)

// GetNamespaceAllocatedRes get nodes allocated resource from cache, and update cache in goroutine
func GetNamespaceAllocatedRes(ctx context.Context, server SteveServer, noAuthentication bool, clusterName, userID, orgID string, namespaces []string) (map[string]AllocatedRes, error) {
	var pods []types.APIObject
	hasExpired := false
	nsAllocatedRes := make(map[string]AllocatedRes)
	for _, namespace := range namespaces {
		value, expired, err := cache.GetFreeCache().Get(cache.GenerateKey(clusterName, namespace, nsCacheType))
		if err != nil {
			return nil, err
		}
		if expired {
			hasExpired = true
		}
		if value != nil {
			nar := value[0].Value().(AllocatedRes)
			nsAllocatedRes[namespace] = nar
			continue
		}
		if pods == nil {
			req := &apistructs.SteveRequest{
				NoAuthentication: noAuthentication,
				UserID:           userID,
				OrgID:            orgID,
				Type:             apistructs.K8SPod,
				ClusterName:      clusterName,
			}
			pods, err = server.ListSteveResource(ctx, req)
			if err != nil {
				return nil, err
			}
		}
		cpu, mem, podNum := CalculateNamespaceAllocatedRes(namespace, pods)
		nar := AllocatedRes{
			CPU:    cpu,
			Mem:    mem,
			PodNum: podNum,
		}
		value, err = cache.GetInterfaceValue(nar)
		if err != nil {
			logrus.Errorf("failed to marshal value for namespace %s in GetNamespaceAllocatedRes, %v", namespace, err)
			continue
		}
		if err = cache.GetFreeCache().Set(cache.GenerateKey(clusterName, namespace, nsCacheType), value, time.Minute.Nanoseconds()*5); err != nil {
			logrus.Errorf("failed to set cache for namespace %s in GetNamespaceAllocatedRes, %v", namespace, err)
			continue
		}

		nar = value[0].Value().(AllocatedRes)
		nsAllocatedRes[namespace] = nar
	}
	if hasExpired {
		go func() {
			var err error
			if pods == nil {
				req := &apistructs.SteveRequest{
					NoAuthentication: noAuthentication,
					UserID:           userID,
					OrgID:            orgID,
					Type:             apistructs.K8SPod,
					ClusterName:      clusterName,
				}

				ctx, cancel := context.WithTimeout(context.Background(), time.Second*30)
				defer cancel()
				pods, err = server.ListSteveResource(ctx, req)
				if err != nil {
					logrus.Errorf("failed to list pods in GetNamespaceAllocatedRes goroutine, %v", err)
					return
				}
			}
			for _, namespace := range namespaces {
				cpu, mem, podNum := CalculateNamespaceAllocatedRes(namespace, pods)
				nar := AllocatedRes{
					CPU:    cpu,
					Mem:    mem,
					PodNum: podNum,
				}
				value, err := cache.GetInterfaceValue(nar)
				if err != nil {
					logrus.Errorf("failed to marshal value for namespace %s in GetNamespaceAllocatedRes goroutine, %v", namespace, err)
					continue
				}
				if err = cache.GetFreeCache().Set(cache.GenerateKey(clusterName, namespace, nsCacheType), value, time.Minute.Nanoseconds()*5); err != nil {
					logrus.Errorf("failed to set cache for namespace %s in GetNamespaceAllocatedRes goroutine, %v", namespace, err)
					continue
				}
			}
			logrus.Infof("update namespace allocated resource cache succeeded")
		}()
	}
	return nsAllocatedRes, nil
}

// CalculateNamespaceAllocatedRes calculate allocated cpu, memory and pods for target namespaces
func CalculateNamespaceAllocatedRes(name string, pods []types.APIObject) (cpu, mem, podNum int64) {
	cpuQty := resource.NewQuantity(0, resource.DecimalSI)
	memQty := resource.NewQuantity(0, resource.BinarySI)
	for _, obj := range pods {
		pod := obj.Data()
		status := pod.String("status", "phase")
		if pod.String("metadata", "namespace") != name || status == "Pending" ||
			status == "Failed" || status == "Succeeded" {
			continue
		}
		podNum++
		containers := pod.Slice("spec", "containers")
		for _, container := range containers {
			requestsCPU := resource.NewQuantity(0, resource.DecimalSI)
			requestsMem := resource.NewQuantity(0, resource.BinarySI)
			requests := container.String("resources", "requests", "cpu")
			if requests != "" {
				*requestsCPU, _ = resource.ParseQuantity(requests)
			}
			requests = container.String("resources", "requests", "memory")
			if requests != "" {
				*requestsMem, _ = resource.ParseQuantity(requests)
			}
			cpuQty.Add(*requestsCPU)
			memQty.Add(*requestsMem)
		}
	}
	return cpuQty.MilliValue(), memQty.Value(), podNum
}

func IsVirtualNode(node data.Object) bool {
	labels := node.Map("metadata", "labels")
	v, ok := labels["type"]
	if !ok {
		return false
	}
	s, ok := v.(string)
	if !ok {
		return false
	}
	return s == "virtual-kubelet"
}

// GetNodesAllocatedRes get nodes allocated resource from cache, and update cache in goroutine
func GetNodesAllocatedRes(ctx context.Context, server SteveServer, noAuthentication bool, clusterName, userID, orgID string, nodes []data.Object) (map[string]AllocatedRes, error) {
	var pods []types.APIObject
	hasExpired := false
	nodesAllocatedRes := make(map[string]AllocatedRes)
	for _, node := range nodes {
		if IsVirtualNode(node) {
			continue
		}
		nodeName := node.String("metadata", "name")
		value, expired, err := cache.GetFreeCache().Get(cache.GenerateKey(clusterName, nodeName, nodeCacheType))
		if err != nil {
			return nil, err
		}
		if expired {
			hasExpired = true
		}
		if value != nil {
			nar := value[0].Value().(AllocatedRes)
			nodesAllocatedRes[nodeName] = nar
			continue
		}
		if pods == nil {
			req := &apistructs.SteveRequest{
				NoAuthentication: noAuthentication,
				UserID:           userID,
				OrgID:            orgID,
				Type:             apistructs.K8SPod,
				ClusterName:      clusterName,
			}
			pods, err = server.ListSteveResource(ctx, req)
			if err != nil {
				return nil, err
			}
		}
		cpu, mem, podNum := CalculateNodeAllocatedRes(nodeName, pods)
		nar := AllocatedRes{
			CPU:    cpu,
			Mem:    mem,
			PodNum: podNum,
		}
		value, err = cache.GetInterfaceValue(nar)
		if err != nil {
			logrus.Errorf("failed to marshal value for node %s in GetNodeAllocatedRes, %v", nodeName, err)
			continue
		}
		if err = cache.GetFreeCache().Set(cache.GenerateKey(clusterName, nodeName, nodeCacheType), value, time.Minute.Nanoseconds()*5); err != nil {
			logrus.Errorf("failed to set cache for node %s in GetNodeAllocatedRes, %v", nodeName, err)
			continue
		}

		nar = value[0].Value().(AllocatedRes)
		nodesAllocatedRes[nodeName] = nar
	}
	if hasExpired {
		go func() {
			var err error
			if pods == nil {
				req := &apistructs.SteveRequest{
					NoAuthentication: noAuthentication,
					UserID:           userID,
					OrgID:            orgID,
					Type:             apistructs.K8SPod,
					ClusterName:      clusterName,
				}

				ctx, cancel := context.WithTimeout(context.Background(), time.Second*30)
				defer cancel()
				pods, err = server.ListSteveResource(ctx, req)
				if err != nil {
					logrus.Errorf("failed to list pods in GetNodeAllocatedRes goroutine, %v", err)
					return
				}
			}
			for _, node := range nodes {
				if IsVirtualNode(node) {
					continue
				}
				nodeName := node.String("metadata", "name")
				cpu, mem, podNum := CalculateNodeAllocatedRes(nodeName, pods)
				nar := AllocatedRes{
					CPU:    cpu,
					Mem:    mem,
					PodNum: podNum,
				}
				value, err := cache.GetInterfaceValue(nar)
				if err != nil {
					logrus.Errorf("failed to marshal value for node %s in GetNodeAllocatedRes goroutine, %v", nodeName, err)
					continue
				}
				if err = cache.GetFreeCache().Set(cache.GenerateKey(clusterName, nodeName, nodeCacheType), value, time.Minute.Nanoseconds()*5); err != nil {
					logrus.Errorf("failed to set cache for node %s in GetNodeAllocatedRes goroutine, %v", nodeName, err)
					continue
				}
			}
			logrus.Infof("update node allocated resource cache succeeded")
		}()
	}
	return nodesAllocatedRes, nil
}

// CalculateNodeAllocatedRes calculate allocated cpu, memory and pods for target node
func CalculateNodeAllocatedRes(nodeName string, pods []types.APIObject) (cpu, mem, podNum int64) {
	cpuQty := resource.NewQuantity(0, resource.DecimalSI)
	memQty := resource.NewQuantity(0, resource.BinarySI)
	for _, obj := range pods {
		pod := obj.Data()
		status := pod.String("status", "phase")
		if pod.String("spec", "nodeName") != nodeName || status == "Pending" ||
			status == "Failed" || status == "Succeeded" {
			continue
		}
		podNum++
		containers := pod.Slice("spec", "containers")
		for _, container := range containers {
			requestsCPU := resource.NewQuantity(0, resource.DecimalSI)
			requestsMem := resource.NewQuantity(0, resource.BinarySI)
			requests := container.String("resources", "requests", "cpu")
			if requests != "" {
				*requestsCPU, _ = resource.ParseQuantity(requests)
			}
			requests = container.String("resources", "requests", "memory")
			if requests != "" {
				*requestsMem, _ = resource.ParseQuantity(requests)
			}
			cpuQty.Add(*requestsCPU)
			memQty.Add(*requestsMem)
		}
	}
	return cpuQty.MilliValue(), memQty.Value(), podNum
}

func (p *provider) GetPodsByLabels(ctx context.Context, pReq *pb.GetPodsByLabelsRequest) (*pb.GetPodsByLabelsResponse, error) {
	req := &apistructs.SteveRequest{
		NoAuthentication: true,
		Type:             apistructs.K8SPod,
		ClusterName:      pReq.Cluster,
		LabelSelector:    pReq.Labels,
	}
	pods, err := p.ListSteveResource(ctx, req)
	if err != nil {
		return nil, err
	}

	var targetsPods []*pb.GetPodsByLabelsItem
	for _, pod := range pods {
		obj := pod.Data()
		status := obj.String("status", "phase")
		name := obj.String("metadata", "name")
		namespace := obj.String("metadata", "namespace")

		cpuQty := resource.NewQuantity(0, resource.DecimalSI)
		memQty := resource.NewQuantity(0, resource.BinarySI)
		containers := obj.Slice("spec", "containers")
		for _, container := range containers {
			cpuQty.Add(*parseResource(container.String("resources", "requests", "cpu"), resource.DecimalSI))
			memQty.Add(*parseResource(container.String("resources", "requests", "memory"), resource.BinarySI))
		}
		cpu := uint64(cpuQty.MilliValue())
		mem := uint64(memQty.Value())

		targetsPods = append(targetsPods, &pb.GetPodsByLabelsItem{
			Cluster:    pReq.Cluster,
			Status:     status,
			Name:       name,
			Namespace:  namespace,
			CpuRequest: cpu,
			MemRequest: mem,
		})
	}
	return &pb.GetPodsByLabelsResponse{
		Total: uint64(len(targetsPods)),
		List:  targetsPods,
	}, nil
}

func parseResource(str string, format resource.Format) *resource.Quantity {
	if str == "" {
		return resource.NewQuantity(0, format)
	}
	res, _ := resource.ParseQuantity(str)
	return &res
}
