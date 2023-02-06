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
	"net/http"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/ucauth"
)

var OPENAPI_GEN_CLIENT_TOKEN = apis.ApiSpec{
	Path:       "/api/openapi/client-token",
	Scheme:     "http",
	Method:     "POST",
	CheckLogin: false,
	Custom: func(rw http.ResponseWriter, req *http.Request) {
		basic := req.Header.Get("Authorization")
		if basic == "" {
			http.Error(rw, "not provide Basic Authorization header", http.StatusBadRequest)
			return
		}

		oauthToken, err := ucauth.GenClientToken(discover.UC(), basic)
		if err != nil {
			http.Error(rw, err.Error(), http.StatusInternalServerError)
			return
		}
		res, err := json.Marshal(oauthToken)
		if err != nil {
			http.Error(rw, err.Error(), http.StatusForbidden)
			return
		}
		rw.Header().Set("Content-Type", "application/json")
		rw.Write(res)
	},
	Doc: `
summary: client token 发放接口
description: 通过 header Basic 认证. Basic Header："Basic " + base64(<clientid>+":"+<clientsecret>)
produces:
  - application/json
responses:
  '200':
    description: OK
    schema:
      type: object
      properties:
        access_token:
          type: string
        token_type:
          type: string
        refresh_token:
          type: string
        expires_in:
          type: int64
        scope:
          type: string
        jti:
          type: string

  '400':
    description: 没有提供 Authorization header`,
}
