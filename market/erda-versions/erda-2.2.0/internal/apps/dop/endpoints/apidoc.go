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

package endpoints

import (
	"context"
	"encoding/json"
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/swagger"
	"github.com/erda-project/erda/pkg/swagger/oas3"
)

// APIDocWebsocket is the handler for the editor session,
// all edtion in the same websocket session.
func (e *Endpoints) APIDocWebsocket(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) error {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return err
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return err
	}

	apiErr := e.fileTreeSvc.Upgrade(w, r, &apistructs.WsAPIDocHandShakeReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.FileTreeDetailURI{
			TreeName: "api-docs",
			Inode:    vars["inode"],
		},
	})
	if apiErr != nil {
		httpserver.WriteErr(w, apiErr.Code(), apiErr.Error())
	}

	return nil
}

func (e *Endpoints) ValidateSwagger(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var body map[string]string
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		return errorresp.New().InvalidParameter(err).ToResp(), nil
	}

	var response = map[string]interface{}{
		"success": true,
	}

	content, ok := body["content"]
	if !ok || content == "" {
		response["success"] = false
		response["err"] = "no content"
		return httpserver.OkResp(response)
	}

	v3, err := swagger.LoadFromData([]byte(content))
	if err != nil {
		response["success"] = false
		response["err"] = err.Error()
		return httpserver.OkResp(response)
	}

	if err = oas3.ValidateOAS3(context.TODO(), *v3); err != nil {
		response["success"] = false
		response["err"] = err.Error()
		return httpserver.OkResp(response)
	}

	return httpserver.OkResp(response)
}
