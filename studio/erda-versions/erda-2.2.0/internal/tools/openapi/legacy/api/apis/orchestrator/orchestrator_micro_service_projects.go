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

package orchestrator

import (
	"encoding/json"
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

var ORCHESTRATOR_MICRO_SERVICE_PROJECTS = apis.ApiSpec{
	Path:         "/api/microservice/projects",
	BackendPath:  "/api/microservice/projects",
	Host:         "orchestrator.marathon.l4lb.thisdcos.directory:8081",
	Scheme:       "http",
	Method:       "GET",
	ResponseType: apistructs.MicroServiceProjectResponse{},
	Custom:       attachProjectParams,
	CheckLogin:   true,
	CheckToken:   true,
	IsOpenAPI:    true,
	Doc:          `summary: 微服务项目列表`,
	Group:        "addons",
}

func attachProjectParams(w http.ResponseWriter, r *http.Request) {
	userID := r.Header.Get("User-ID")
	orgID := r.Header.Get("Org-ID")

	// get permissions
	client := httpclient.New()
	var perms apistructs.ScopeRoleList
	cr := client.Get(discover.CoreServices()).
		Path("/api/permissions").
		Header("User-ID", userID).
		Header("Org-ID", orgID)
	if err := utils.DoJson(cr, &perms); err != nil {
		ErrFromError(w, err)
	}

	params := r.URL.Query()
	params.Del("projectId")
	for _, p := range perms.List {
		if p.Scope.Type == apistructs.ProjectScope && p.Access {
			params.Add("projectId", p.Scope.ID)
		}
	}

	var data json.RawMessage
	cr = client.Get(discover.Orchestrator()).
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		Path(r.URL.Path).Params(params)

	if err := utils.DoJson(cr, &data); err != nil {
		ErrFromError(w, err)
		return
	}

	Succ(w, data)
}

func Succ(w http.ResponseWriter, data interface{}) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	b, _ := json.Marshal(utils.Resp{
		Success: true,
		Data:    data,
	})
	w.Write(b)
}
func ErrFromError(w http.ResponseWriter, error error) {
	Err(w, &apistructs.ErrorResponse{Code: "InternalError", Msg: error.Error()}, http.StatusInternalServerError)
}
func Err(w http.ResponseWriter, err *apistructs.ErrorResponse, httpCode int) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(httpCode)
	b, _ := json.Marshal(utils.Resp{
		Success: false,
		Err:     err,
	})
	w.Write(b)
}
