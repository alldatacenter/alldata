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

package resource_executor

import (
	"github.com/koordinator-sh/koordinator/apis/runtime/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/resexecutor/cri"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/store"
	"github.com/koordinator-sh/koordinator/pkg/runtimeproxy/utils"
)

type RuntimeResourceExecutor interface {
	GetMetaInfo() string
	GenerateHookRequest() interface{}
	// ParseRequest would be the first function after request intercepted, during which,
	// pod/container's meta/resource info would be parsed from request or loaded from local store,
	// and some hint info should also be offered during ParseRequest stage, e.g. to check if executor
	// should call hook plugins when pod/container is system component.
	ParseRequest(request interface{}) (utils.CallHookPluginOperation, error)
	ResourceCheckPoint(response interface{}) error
	DeleteCheckpointIfNeed(request interface{}) error
	UpdateRequest(response interface{}, request interface{}) error
}

type RuntimeResourceType string

const (
	RuntimePodResource       RuntimeResourceType = "RuntimePodResource"
	RuntimeContainerResource RuntimeResourceType = "RuntimeContainerResource"
	RuntimeNoopResource      RuntimeResourceType = "RuntimeNoopResource"
)

func NewRuntimeResourceExecutor(runtimeResourceType RuntimeResourceType) RuntimeResourceExecutor {
	switch runtimeResourceType {
	case RuntimePodResource:
		return cri.NewPodResourceExecutor()
	case RuntimeContainerResource:
		return cri.NewContainerResourceExecutor()
	}
	return &NoopResourceExecutor{}
}

// NoopResourceExecutor means no-operation for cri request,
// where no hook exists like ListContainerStats/ExecSync etc.
type NoopResourceExecutor struct {
}

func (n *NoopResourceExecutor) GetMetaInfo() string {
	return ""
}

func (n *NoopResourceExecutor) GenerateResourceCheckpoint() interface{} {
	return v1alpha1.ContainerResourceHookRequest{}
}

func (n *NoopResourceExecutor) GenerateHookRequest() interface{} {
	return store.ContainerInfo{}
}

func (n *NoopResourceExecutor) ParseRequest(request interface{}) (utils.CallHookPluginOperation, error) {
	return utils.Unknown, nil
}
func (n *NoopResourceExecutor) ResourceCheckPoint(response interface{}) error {
	return nil
}

func (n *NoopResourceExecutor) DeleteCheckpointIfNeed(request interface{}) error {
	return nil
}

func (n *NoopResourceExecutor) UpdateRequest(response interface{}, request interface{}) error {
	return nil
}
