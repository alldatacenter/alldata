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

	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
)

var (
	_ framework.SharedLister           = &SharedListerAdapter{}
	_ framework.NodeInfoLister         = &SharedListerAdapter{}
	_ frameworkext.SharedListerAdapter = NewSharedListerAdapter
)

type SharedListerAdapter struct {
	framework.SharedLister
}

func NewSharedListerAdapter(sharedLister framework.SharedLister) framework.SharedLister {
	return &SharedListerAdapter{
		SharedLister: sharedLister,
	}
}

func (s *SharedListerAdapter) NodeInfos() framework.NodeInfoLister {
	return s
}

func (s *SharedListerAdapter) List() ([]*framework.NodeInfo, error) {
	nodeInfos, err := s.SharedLister.NodeInfos().List()
	if err != nil {
		return nil, err
	}
	transformedNodeInfos := TransformNodeInfos(nodeInfos)
	return transformedNodeInfos, nil
}

func (s *SharedListerAdapter) HavePodsWithAffinityList() ([]*framework.NodeInfo, error) {
	nodeInfos, err := s.SharedLister.NodeInfos().HavePodsWithAffinityList()
	if err != nil {
		return nil, err
	}
	transformedNodeInfos := TransformNodeInfos(nodeInfos)
	return transformedNodeInfos, nil
}

func (s *SharedListerAdapter) HavePodsWithRequiredAntiAffinityList() ([]*framework.NodeInfo, error) {
	nodeInfos, err := s.SharedLister.NodeInfos().HavePodsWithRequiredAntiAffinityList()
	if err != nil {
		return nil, err
	}
	transformedNodeInfos := TransformNodeInfos(nodeInfos)
	return transformedNodeInfos, nil
}

func (s *SharedListerAdapter) Get(nodeName string) (*framework.NodeInfo, error) {
	nodeInfo, err := s.SharedLister.NodeInfos().Get(nodeName)
	if err != nil {
		return nil, err
	}
	transformedNodeInfo := TransformOneNodeInfo(nodeInfo)
	return transformedNodeInfo, nil
}
