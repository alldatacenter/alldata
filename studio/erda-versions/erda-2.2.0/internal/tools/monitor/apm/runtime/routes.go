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

package runtime

import (
	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (runtime *provider) initRoutes(routes httpserver.Router) error {

	routes.GET("/api/apm/runtime", runtime.runtime, getRuntimePermission(runtime.db))
	routes.GET("/api/apm/tk", runtime.tkByProjectIdAndWorkspace, getProjectPermission())
	routes.GET("/api/apm/instance", runtime.instanceByTk, getInstancePermission(runtime.db))

	return nil
}

func (runtime *provider) instanceByTk(params struct {
	TerminusKey string `query:"terminusKey" validate:"required"`
}) interface{} {
	instance, err := runtime.getInstanceByTk(params.TerminusKey)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(instance)
}

func (runtime *provider) tkByProjectIdAndWorkspace(params struct {
	ProjectId string `query:"projectId" validate:"required"`
	Workspace string `query:"workspace" validate:"required"`
}) interface{} {
	tk, err := runtime.getTkByProjectIdAndWorkspace(params.ProjectId, params.Workspace)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(tk)
}

func (runtime *provider) runtime(params Vo) interface{} {
	runtimeInfo, err := runtime.getRuntime(params)
	if err != nil {
		return api.Success(nil)
	}

	return api.Success(runtimeInfo)
}
