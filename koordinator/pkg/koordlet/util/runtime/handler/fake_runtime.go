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

package handler

import (
	"k8s.io/cri-api/pkg/apis/testing"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/statesinformer"
)

type FakeRuntimeHandler struct {
	fakeRuntimeService *testing.FakeRuntimeService
	PodMetas           map[string]*statesinformer.PodMeta
}

func NewFakeRuntimeHandler() ContainerRuntimeHandler {
	return &FakeRuntimeHandler{fakeRuntimeService: testing.NewFakeRuntimeService(), PodMetas: make(map[string]*statesinformer.PodMeta)}
}

func (f *FakeRuntimeHandler) StopContainer(containerID string, timeout int64) error {
	return f.fakeRuntimeService.StopContainer(containerID, timeout)
}

func (f *FakeRuntimeHandler) SetFakeContainers(containers []*testing.FakeContainer) {
	f.fakeRuntimeService.SetFakeContainers(containers)
}

func (f *FakeRuntimeHandler) UpdateContainerResources(containerID string, opts UpdateOptions) error {
	return nil
}
