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
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/util/uuid"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingconfig "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

func TestEndpointsQueryCPUTopologyOptions(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	plugin, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
	assert.NoError(t, err)
	assert.NotNil(t, plugin)
	p := plugin.(*Plugin)

	expectedCPUTopologyOptions := CPUTopologyOptions{
		CPUTopology:  buildCPUTopologyForTest(2, 1, 4, 2),
		ReservedCPUs: cpuset.MustParse("0-1"),
		MaxRefCount:  1,
		Policy: &extension.KubeletCPUManagerPolicy{
			Policy: extension.KubeletCPUManagerPolicyStatic,
			Options: map[string]string{
				extension.KubeletCPUManagerPolicyFullPCPUsOnlyOption: "true",
			},
		},
	}
	p.topologyManager.UpdateCPUTopologyOptions("test-node-1", func(options *CPUTopologyOptions) {
		*options = expectedCPUTopologyOptions
	})

	engine := gin.Default()
	p.RegisterEndpoints(engine.Group("/"))
	w := httptest.NewRecorder()
	req, _ := http.NewRequest("GET", "/cpuTopologyOptions/test-node-1", nil)
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Result().StatusCode)
	cpuTopologyOptions := &CPUTopologyOptions{}
	err = json.NewDecoder(w.Result().Body).Decode(cpuTopologyOptions)
	assert.NoError(t, err)
	assert.Equal(t, &expectedCPUTopologyOptions, cpuTopologyOptions)
}

func TestEndpointsQueryAvailableCPUsOptions(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	plugin, err := suit.proxyNew(suit.nodeNUMAResourceArgs, suit.Handle)
	assert.NoError(t, err)
	assert.NotNil(t, plugin)
	p := plugin.(*Plugin)

	cpuTopologyOptions := CPUTopologyOptions{
		CPUTopology:  buildCPUTopologyForTest(2, 1, 4, 2),
		ReservedCPUs: cpuset.MustParse("0-1"),
		MaxRefCount:  1,
		Policy: &extension.KubeletCPUManagerPolicy{
			Policy: extension.KubeletCPUManagerPolicyStatic,
			Options: map[string]string{
				extension.KubeletCPUManagerPolicyFullPCPUsOnlyOption: "true",
			},
		},
	}
	p.topologyManager.UpdateCPUTopologyOptions("test-node-1", func(options *CPUTopologyOptions) {
		*options = cpuTopologyOptions
	})

	p.cpuManager.UpdateAllocatedCPUSet("test-node-1", uuid.NewUUID(), cpuset.MustParse("0,2,4,6"), schedulingconfig.CPUExclusivePolicyNone)

	engine := gin.Default()
	p.RegisterEndpoints(engine.Group("/"))
	w := httptest.NewRecorder()
	req, _ := http.NewRequest("GET", "/availableCPUs/test-node-1", nil)
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Result().StatusCode)
	response := &AvailableCPUsResponse{}
	err = json.NewDecoder(w.Result().Body).Decode(response)
	assert.NoError(t, err)

	expectedResponse := &AvailableCPUsResponse{
		AvailableCPUs: cpuset.MustParse("3,5,7-15"),
		Allocated:     CPUDetails{},
	}
	for _, v := range []int{0, 2, 4, 6} {
		cpuInfo := cpuTopologyOptions.CPUTopology.CPUDetails[v]
		cpuInfo.RefCount++
		cpuInfo.ExclusivePolicy = schedulingconfig.CPUExclusivePolicyNone
		expectedResponse.Allocated[v] = cpuInfo
	}
	assert.Equal(t, expectedResponse, response)
}
