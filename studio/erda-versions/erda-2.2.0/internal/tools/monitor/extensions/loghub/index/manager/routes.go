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

package manager

import (
	"net/http"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	routes.GET("/api/logs-query/indices", p.inspectIndices)
	routes.POST("/api/logs-manager/indices/:addon", p.createByAddonIndex)
	return nil
}

func (p *provider) inspectIndices(r *http.Request) interface{} {
	return api.Success(p.indices.Load())
}

func (p *provider) createByAddonIndex(params struct {
	Addon string `param:"addon" validate:"required"`
}) interface{} {
	resp, err := p.createIndex(params.Addon)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(resp)
}
