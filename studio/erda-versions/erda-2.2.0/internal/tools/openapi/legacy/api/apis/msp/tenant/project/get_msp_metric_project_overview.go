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
	"fmt"
	"net/http"
	"net/url"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

var MSP_METRIC_PROJECT_OVERVIEW = apis.ApiSpec{
	Path:        "/api/msp/metrics/tenant/project/overview",
	BackendPath: "/api/msp/metrics/tenant/project/overview",
	Host:        "msp.marathon.l4lb.thisdcos.directory:8080",
	Scheme:      "http",
	Method:      "POST",
	CheckLogin:  true,
	CheckToken:  true,
	Custom:      attachMetricProjectParams,
	IsOpenAPI:   true,
	Doc:         "GET MSP project overview for dashboard",
}

func attachMetricProjectParams(w http.ResponseWriter, r *http.Request) {
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
	key := params.Get("project_key")
	params.Del("project_key")
	var projectIDs []string
	for _, p := range perms.List {
		if p.Scope.Type == apistructs.ProjectScope && p.Access {
			projectIDs = append(projectIDs, p.Scope.ID)
		}
	}

	if projectIDs == nil || len(projectIDs) == 0 {
		Success(w, nil)
		return
	}
	if key == "gateway" {
		paramsForProject := url.Values{}
		paramsForProject.Del("projectId")
		for _, id := range projectIDs {
			paramsForProject.Add("projectId", id)
		}
		var data []string
		cr = client.Get(discover.MSP()).
			Header("lang", lang).
			Header("User-ID", userID).
			Header("Org-ID", orgID).
			Path("/api/msp/tenant/projects/tenants/ids").
			Params(paramsForProject)
		if err := utils.DoJson(cr, &data); err != nil {
			ErrFromError(w, err)
			return
		}

		for _, id := range data {
			params.Add("in_target_terminus_key", id)
		}
	} else {
		fk := fmt.Sprintf("in_%s", key)
		params.Del(fk)
		for _, id := range projectIDs {
			params.Add(fk, id)
		}
	}

	var data json.RawMessage
	cr = client.Post(discover.MSP()).
		Header("lang", lang).
		Header("User-ID", userID).
		Header("Org-ID", orgID).
		Path(r.URL.Path).
		Params(params).
		RawBody(r.Body)

	if err := utils.DoJson(cr, &data); err != nil {
		ErrFromError(w, err)
		return
	}

	Success(w, data)
}
