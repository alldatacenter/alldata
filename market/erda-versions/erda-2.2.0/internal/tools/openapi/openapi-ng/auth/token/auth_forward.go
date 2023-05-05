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

package token

import (
	"net/http"
	"net/http/httputil"
	"net/url"
	"os"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/discover"
)

func ForwardAuthToken(w http.ResponseWriter, r *http.Request) {
	forwardCoreService(w, r, "/oauth2/token")
}

func ForwardInvalidateToken(w http.ResponseWriter, r *http.Request) {
	forwardCoreService(w, r, "/oauth2/invalidate_token")
}

func ForwardValidateToken(w http.ResponseWriter, r *http.Request) {
	forwardCoreService(w, r, "/oauth2/validate_token")
}

func forwardCoreService(w http.ResponseWriter, r *http.Request, path string) {
	var host string
	var err error

	host = os.Getenv(discover.EnvCoreServices)
	if host == "" {
		host, err = discover.GetEndpoint(discover.SvcCoreServices)
		if err != nil {
			logrus.Errorf("forwardCoreService failed to get core-service GetEndpoint，error %v", err)
			return
		}
	}

	u := &url.URL{
		Scheme: "http",
		Host:   host,
	}

	proxy := httputil.NewSingleHostReverseProxy(u)
	r.URL.Path = path
	proxy.ServeHTTP(w, r)
}
