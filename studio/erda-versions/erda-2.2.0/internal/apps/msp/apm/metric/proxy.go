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

package metric

import (
	"context"
	"fmt"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strconv"

	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) proxy(hostpath string, header http.Header, params url.Values, rw http.ResponseWriter, r *http.Request) error {
	target, err := url.Parse(hostpath)
	if err != nil {
		return err
	}
	scopeId := r.URL.Query().Get("scopeId")
	var project *ObjData
	if scopeId != "" {
		project, err = p.getProjectResult(context.Background(), scopeId)
		if err != nil {
			return err
		}
	}
	path, rawpath := target.Path, target.EscapedPath()
	rp := &httputil.ReverseProxy{Director: func(req *http.Request) {
		for k, vals := range header {
			req.Header.Del(k)
			for _, val := range vals {
				req.Header.Add(k, val)
			}
		}
		query := req.URL.Query()
		if project != nil {
			req.Header.Add("erda-projectId", strconv.Itoa(int(project.ProjectId)))
			req.Header.Add("erda-projectName", project.ProjectName)
			req.Header.Add("erda-workspace", project.Workspace)
		}
		for k, vals := range params {
			query.Del(k)
			for _, val := range vals {
				query.Add(k, val)
			}
		}
		req.URL.Scheme = target.Scheme
		req.URL.Host = target.Host
		req.URL.Path, req.URL.RawPath = path, rawpath
		req.URL.RawQuery = query.Encode()
		p.Log.Debugf("proxy %s %s -> %s", req.Method, rawpath, req.URL)
		if _, ok := req.Header["User-Agent"]; !ok {
			// explicitly disable User-Agent so it's not set to default value
			req.Header.Set("User-Agent", "")
		}
	},
	}
	rp.ServeHTTP(rw, r)
	return nil
}

func (p *provider) proxyMonitor(path string, params url.Values, rw http.ResponseWriter, r *http.Request) interface{} {
	err := p.proxy(fmt.Sprintf("%s%s", p.Cfg.MonitorURL, path), nil, params, rw, r)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return nil
}
