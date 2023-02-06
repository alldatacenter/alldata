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

package openapi

import (
	"bytes"
	"fmt"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

var OPENAPI_LIST_CLIENT = apis.ApiSpec{
	Path:   "/api/openapi/manager/clients",
	Scheme: "http",
	Method: "GET",
	Custom: func(rw http.ResponseWriter, req *http.Request) {
		token, err := auth.GetDiceClientToken()
		if err != nil {
			errStr := fmt.Sprintf("get token fail: %v", err)
			logrus.Error(errStr)
			http.Error(rw, errStr, http.StatusForbidden)
			return
		}
		logrus.Infof("diceclienttoken: %+v", token)
		var body bytes.Buffer
		r, err := httpclient.New(httpclient.WithCompleteRedirect()).Get(discover.UC()).Path("/api/open-client/manager/clients").
			Header("Authorization", "Bearer "+token.AccessToken).Do().Body(&body)
		if err != nil {
			errStr := fmt.Sprintf("list client fail: %v", err)
			logrus.Error(errStr)
			http.Error(rw, errStr, http.StatusForbidden)
			return
		}
		if !r.IsOK() {
			errStr := fmt.Sprintf("list client fail, statuscode: %d, body: %v", r.StatusCode(), body.String())
			logrus.Error(errStr)
			http.Error(rw, errStr, http.StatusForbidden)
			return
		}
		rw.Write([]byte(body.String()))
	},
	CheckLogin: false, // TODO:
	Doc: `
summary: 获取client列表
description: 认证： 通过 Authorization 头信息进行认证。 格式为“Bearer <token>”, 注意空格
produces:
  - application/json
`,
}
