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

package deviceshare

import (
	"testing"

	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/types"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func Test_newNodeDeviceCache(t *testing.T) {
	expectNodeDeviceCache := &nodeDeviceCache{
		nodeDeviceInfos: map[string]*nodeDevice{},
	}
	assert.Equal(t, expectNodeDeviceCache, newNodeDeviceCache())
}

func Test_newNodeDevice(t *testing.T) {
	expectNodeDevice := &nodeDevice{
		deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{},
		deviceFree:  map[schedulingv1alpha1.DeviceType]deviceResources{},
		deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
		allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]v1.ResourceList{},
	}
	assert.Equal(t, expectNodeDevice, newNodeDevice())
}
