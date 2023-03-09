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

package elasticquota

import (
	"net/http"

	"github.com/gin-gonic/gin"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/services"
)

var _ services.APIServiceProvider = &Plugin{}

func (g *Plugin) RegisterEndpoints(group *gin.RouterGroup) {
	group.GET("/quota/:name", func(c *gin.Context) {
		quotaName := c.Param("name")
		quotaSummary, exist := g.GetQuotaSummary(quotaName)
		if !exist {
			services.ResponseErrorMessage(c, http.StatusNotFound, "cannot find quota %s", quotaName)
			return
		}
		c.JSON(http.StatusOK, quotaSummary)
	})
	group.GET("/quotas", func(c *gin.Context) {
		quotaSummaries := g.GetQuotaSummaries()
		c.JSON(http.StatusOK, quotaSummaries)
	})
}
