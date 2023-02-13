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

package project

import (
	"encoding/json"
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

var MSP_PROJECT_LIST = apis.ApiSpec{
	Path:        "/api/msp/tenant/projects",
	BackendPath: "/api/msp/tenant/projects",
	Host:        "msp.marathon.l4lb.thisdcos.directory:8080",
	Scheme:      "http",
	Method:      "GET",
	CheckLogin:  true,
	CheckToken:  true,
	Custom:      attachProjectParams,
	IsOpenAPI:   true,
	Doc:         "GET MSP projects",
}

func attachProjectParams(w http.ResponseWriter, r *http.Request) {
	userID := r.Header.Get("User-ID")
	orgID := r.Header.Get("Org-ID")
	lang := r.Header.Get("lang")

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
	cr = client.Get(discover.MSP()).
		Header("lang", lang).
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		Path(r.URL.Path).Params(params)

	if err := utils.DoJson(cr, &data); err != nil {
		ErrFromError(w, err)
		return
	}

	Success(w, data)
}

func Success(w http.ResponseWriter, data interface{}) {
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
