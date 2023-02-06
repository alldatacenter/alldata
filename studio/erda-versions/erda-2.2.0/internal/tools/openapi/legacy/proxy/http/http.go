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

package http

import (
	"net/http"
	"net/http/httputil"
	"runtime/debug"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api"
	util "github.com/erda-project/erda/pkg/http/httputil"
)

func NewReverseProxy(director func(*http.Request),
	modifyResponse func(*http.Response) error) http.Handler {
	return &httputil.ReverseProxy{
		FlushInterval:  -1,
		Director:       director,
		ModifyResponse: modifyResponse,
	}
}

type ReverseProxyWithCustom struct {
	reverseProxy http.Handler
}

func NewReverseProxyWithCustom(director func(*http.Request),
	modifyResponse func(*http.Response) error) http.Handler {
	r := NewReverseProxy(director, modifyResponse)
	return &ReverseProxyWithCustom{reverseProxy: r}
}

func (r *ReverseProxyWithCustom) ServeHTTP(rw http.ResponseWriter, req *http.Request) {
	defer func() {
		if err := recover(); err != nil {
			debug.PrintStack()
			logrus.Errorf("[alert] openapi http proxy recover from panic: %v", err)
		}
	}()
	spec := api.API.Find(req)
	req.Header.Add(util.UserInfoDesensitizedHeader, strconv.FormatBool(spec.NeedDesensitize))
	if spec != nil && spec.Custom != nil {
		spec.Custom(rw, req)
		return
	}
	r.reverseProxy.ServeHTTP(rw, req)
}
