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
	"net/http"

	"github.com/gin-gonic/gin"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/services"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

var _ services.APIServiceProvider = &Plugin{}

type AvailableCPUsResponse struct {
	AvailableCPUs cpuset.CPUSet `json:"availableCPUs,omitempty"`
	Allocated     CPUDetails    `json:"allocated,omitempty"`
}

func (p *Plugin) RegisterEndpoints(group *gin.RouterGroup) {
	group.GET("/cpuTopologyOptions/:nodeName", func(c *gin.Context) {
		nodeName := c.Param("nodeName")
		topologyOptions := p.topologyManager.GetCPUTopologyOptions(nodeName)
		c.JSON(http.StatusOK, topologyOptions)
	})
	group.GET("/availableCPUs/:nodeName", func(c *gin.Context) {
		nodeName := c.Param("nodeName")
		availableCPUs, allocated, err := p.cpuManager.GetAvailableCPUs(nodeName)
		if err != nil {
			services.ResponseErrorMessage(c, http.StatusInternalServerError, err.Error())
			return
		}
		r := &AvailableCPUsResponse{
			AvailableCPUs: availableCPUs,
			Allocated:     allocated,
		}
		c.JSON(http.StatusOK, r)
	})
}
