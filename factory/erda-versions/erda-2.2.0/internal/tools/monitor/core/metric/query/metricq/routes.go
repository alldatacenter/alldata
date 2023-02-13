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

package metricq

import (
	"io/ioutil"
	"net/http"

	"github.com/erda-project/erda-infra/providers/httpserver"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) initRoutes(routes httpserver.Router) error {
	p.initRoutesV1(routes)
	// metric query apis
	routes.GET("/api/query", p.queryMetrics)  // for tsql
	routes.POST("/api/query", p.queryMetrics) // for tsql

	// Data export, temporary solution.
	routes.GET("/api/metrics/:scope/export", p.exportMetrics)
	routes.POST("/api/metrics/:scope/export", p.exportMetrics)
	return nil
}

// queryMetrics .
func (p *provider) queryMetrics(r *http.Request) interface{} {
	err := r.ParseForm()
	if err != nil {
		return api.Errors.InvalidParameter(err)
	}
	ql, q, format := r.Form.Get("ql"), r.Form.Get("q"), r.Form.Get("format")
	r.Form.Del("ql")
	r.Form.Del("q")
	r.Form.Del("format")
	if len(format) == 0 {
		format = "influxdb"
	}
	if len(ql) == 0 {
		ql = "influxql"
	}
	if len(q) == 0 {
		byts, err := ioutil.ReadAll(r.Body)
		if err == nil {
			q = string(byts)
		}
	}
	resp, data, err := p.q.QueryWithFormat(ql, q, format, api.Language(r), nil, nil, r.Form)
	if err != nil {
		return api.Errors.InvalidParameter(err)
	}
	if resp.Details != nil {
		return resp.Details
	}
	if response, ok := data.(httpserver.Response); ok {
		return response
	} else if response, ok := data.(httpserver.ResponseGetter); ok {
		return response
	}
	return api.Success(data)
}
