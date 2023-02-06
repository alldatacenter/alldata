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
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/pkg/ucauth"
)

var OPENAPI_NEW_CLIENT = apis.ApiSpec{
	Path:   "/api/openapi/manager/clients",
	Scheme: "http",
	Method: "POST",
	Custom: func(rw http.ResponseWriter, req *http.Request) {
		var newClientReq ucauth.NewClientRequest
		d := json.NewDecoder(req.Body)
		if err := d.Decode(&newClientReq); err != nil {
			errStr := fmt.Sprintf("new client fail: %v, buffered: %v", err, d.Buffered())
			logrus.Error(errStr)
			http.Error(rw, errStr, http.StatusBadRequest)
			return
		}
		res, err := auth.NewUCTokenClient(&newClientReq)
		if err != nil {
			errStr := fmt.Sprintf("new client fail: %v", err)
			logrus.Error(errStr)
			http.Error(rw, errStr, http.StatusBadGateway)
			return
		}
		resBody, err := json.Marshal(res)
		if err != nil {
			errStr := fmt.Sprintf("new client marshal fail: %v", err)
			logrus.Error(errStr)
			http.Error(rw, errStr, http.StatusBadGateway)
			return
		}
		rw.Write(resBody)
	},
	CheckLogin: false, // TODO:
	Doc: `
summary: 创建新client
description: 认证： 通过 Authorization 头信息进行认证。 格式为“Bearer <token>”, 注意空格
parameters:
  - in: body
    name: request-json
    description: request json body
    schema:
      type: object
      properties:
        accessTokenValiditySeconds:
          type: integer
        autoApprove:
          type: boolean
        clientId:
          type: string
        clientLogoUrl:
          type: string
        clientName:
          type: string
        clientSecret:
          type: string
        refreshTokenValiditySeconds:
          type: integer
        userId:
          type: int

produces:
  - application/json

responses:
  '200':
    description: OK
`,
}
