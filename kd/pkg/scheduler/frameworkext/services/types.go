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

package services

import (
	"fmt"

	"github.com/gin-gonic/gin"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"
)

type APIServiceProvider interface {
	RegisterEndpoints(group *gin.RouterGroup)
}

type ErrorMessage struct {
	Message string `json:"message,omitempty"`
}

func ResponseErrorMessage(c *gin.Context, statusCode int, format string, args ...interface{}) {
	var e ErrorMessage
	e.Message = fmt.Sprintf(format, args...)
	c.JSON(statusCode, e)
}

type NodeInfo struct {
	// Overall Node information.
	Node *corev1.Node `json:"node,omitempty"`
	// Pods running on the node.
	Pods []*framework.PodInfo `json:"pods,omitempty"`

	// The subset of pods with affinity.
	PodsWithAffinity []*framework.PodInfo `json:"podsWithAffinity,omitempty"`

	// The subset of pods with required anti-affinity.
	PodsWithRequiredAntiAffinity []*framework.PodInfo `json:"podsWithRequiredAntiAffinity,omitempty"`

	// Ports allocated on the node.
	UsedPorts HostPortInfo `json:"usedPorts,omitempty"`

	// Total requested resources of all pods on this node. This includes assumed
	// pods, which scheduler has sent for binding, but may not be scheduled yet.
	Requested *framework.Resource `json:"requested,omitempty"`
	// Total requested resources of all pods on this node with a minimum value
	// applied to each container's CPU and memory requests. This does not reflect
	// the actual resource requests for this node, but is used to avoid scheduling
	// many zero-request pods onto one node.
	NonZeroRequested *framework.Resource `json:"nonZeroRequested,omitempty"`

	// The total amount of remaining resources
	Remaining *framework.Resource `json:"remaining,omitempty"`

	// We store allocatedResources (which is Node.Status.Allocatable.*) explicitly
	// as int64, to avoid conversions and accessing map.
	Allocatable *framework.Resource `json:"allocatable,omitempty"`

	// ImageStates holds the entry of an image if and only if this image is on the node. The entry can be used for
	// checking an image's existence and advanced usage (e.g., image locality scheduling policy) based on the image
	// state information.
	ImageStates map[string]*framework.ImageStateSummary `json:"imageStates,omitempty"`

	// PVCRefCounts contains a mapping of PVC names to the number of pods on the node using it.
	// Keys are in the format "namespace/name".
	PVCRefCounts map[string]int `json:"pvcRefCounts,omitempty"`

	// Whenever NodeInfo changes, generation is bumped.
	// This is used to avoid cloning it if the object didn't change.
	Generation int64 `json:"generation,omitempty"`
}

// HostPortInfo stores mapping from ip to a set of ProtocolPort
type HostPortInfo map[string][]framework.ProtocolPort

func convertNodeInfo(nodeInfo *framework.NodeInfo) *NodeInfo {
	var usedPorts HostPortInfo
	if len(nodeInfo.UsedPorts) > 0 {
		usedPorts = make(HostPortInfo)
		for host, portInfos := range nodeInfo.UsedPorts {
			ports := make([]framework.ProtocolPort, 0, len(portInfos))
			for k := range portInfos {
				ports = append(ports, k)
			}
			usedPorts[host] = ports
		}
	}

	remaining := nodeInfo.Allocatable.Clone()
	remaining.MilliCPU -= nodeInfo.Requested.MilliCPU
	remaining.Memory -= nodeInfo.Requested.Memory
	remaining.EphemeralStorage -= nodeInfo.Requested.EphemeralStorage
	remaining.AllowedPodNumber -= nodeInfo.Requested.AllowedPodNumber
	for k, v := range nodeInfo.Requested.ScalarResources {
		remaining.AddScalar(k, -v)
	}

	return &NodeInfo{
		Node:                         nodeInfo.Node(),
		Pods:                         nodeInfo.Pods,
		PodsWithAffinity:             nodeInfo.PodsWithAffinity,
		PodsWithRequiredAntiAffinity: nodeInfo.PodsWithRequiredAntiAffinity,
		UsedPorts:                    usedPorts,
		Requested:                    nodeInfo.Requested,
		NonZeroRequested:             nodeInfo.NonZeroRequested,
		Remaining:                    remaining,
		Allocatable:                  nodeInfo.Allocatable,
		ImageStates:                  nodeInfo.ImageStates,
		PVCRefCounts:                 nodeInfo.PVCRefCounts,
		Generation:                   nodeInfo.Generation,
	}
}
