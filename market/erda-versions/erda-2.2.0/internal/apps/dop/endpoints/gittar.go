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
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (e *Endpoints) GittarWebHookCallback(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	if r.ContentLength == 0 {
		return apierrors.ErrDoGittarWebHookCallback.MissingParameter(apierrors.MissingRequestBody).ToResp(), nil
	}
	var req apistructs.GittarPushEventRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrDoGittarWebHookCallback.InvalidParameter(err).ToResp(), nil
	}

	go func() {
		TaskQueue <- &req
	}()

	return httpserver.OkResp(nil)
}
