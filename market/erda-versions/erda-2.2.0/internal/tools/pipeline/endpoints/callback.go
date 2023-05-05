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
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) pipelineCallback(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var req apistructs.PipelineCallbackRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCallback.InvalidParameter(err).ToResp(), nil
	}

	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if err := e.permissionSvc.CheckInternalClient(identityInfo); err != nil {
		return errorresp.ErrResp(err)
	}

	if err := e.edgeRegister.CheckAccessTokenFromHttpRequest(r); err != nil {
		return errorresp.ErrResp(apierrors.ErrCheckPermission.AccessDenied())
	}

	switch req.Type {
	case apistructs.PipelineCallbackTypeOfAction.String():
		if err := e.pipelineSvc.DealPipelineCallbackOfAction(req.Data); err != nil {
			return apierrors.ErrCallback.InternalError(err).ToResp(), nil
		}
	case apistructs.PipelineCallbackTypeOfEdgeTaskReport.String():
		if err := e.pipelineSvc.DealPipelineCallbackOfTask(req.Data); err != nil {
			return apierrors.ErrCallback.InternalError(err).ToResp(), nil
		}
	case apistructs.PipelineCallbackTypeOfEdgePipelineReport.String():
		if err := e.pipelineSvc.DealPipelineCallbackOfPipeline(req.Data); err != nil {
			return apierrors.ErrCallback.InternalError(err).ToResp(), nil
		}
	case apistructs.PipelineCallbackTypeOfEdgeCronReport.String():
		if err := e.pipelineSvc.DealPipelineCallbackOfCron(req.Data); err != nil {
			return apierrors.ErrCallback.InternalError(err).ToResp(), nil
		}
	default:
		return apierrors.ErrCallback.InvalidParameter(strutil.Concat("invalid callback type: ", req.Type)).ToResp(), nil
	}

	return httpserver.OkResp(nil)
}
