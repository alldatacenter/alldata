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
	"fmt"
	"net/http"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

func (e *Endpoints) batchInsertLabels(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var createReq apistructs.PipelineLabelBatchInsertRequest
	if err := json.NewDecoder(r.Body).Decode(&createReq); err != nil {
		logrus.Errorf("[alert] failed to decode request body: %v", err)
		return apierrors.ErrCreatePipelineLabel.InvalidParameter(errors.Errorf("request body: %v", err)).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		return errorresp.ErrResp(fmt.Errorf("auth error: not internal client"))
	}

	if len(createReq.Labels) <= 0 {
		return apierrors.ErrCreatePipelineLabel.InvalidParameter("labels").ToResp(), nil
	}

	for index, label := range createReq.Labels {
		if label.TargetID <= 0 {
			return apierrors.ErrCreatePipelineLabel.InvalidParameter(fmt.Errorf("label index %v missing targetID", index)).ToResp(), nil
		}
		if len(label.PipelineYmlName) <= 0 {
			return apierrors.ErrCreatePipelineLabel.InvalidParameter(fmt.Errorf("label index %v missing pipelineYmlName", index)).ToResp(), nil
		}
		if len(label.PipelineSource) <= 0 {
			return apierrors.ErrCreatePipelineLabel.InvalidParameter(fmt.Errorf("label index %v missing pipelineSource", index)).ToResp(), nil
		}
		if len(label.Type.String()) <= 0 {
			return apierrors.ErrCreatePipelineLabel.InvalidParameter(fmt.Errorf("label index %v missing type", index)).ToResp(), nil
		}
		if len(label.Key) <= 0 {
			return apierrors.ErrCreatePipelineLabel.InvalidParameter(fmt.Errorf("label index %v missing key", index)).ToResp(), nil
		}
	}

	err = e.pipelineSvc.BatchCreateLabels(&createReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp("success")
}

func (e *Endpoints) pipelineLabelList(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	var req apistructs.PipelineLabelListRequest
	err := e.queryStringDecoder.Decode(&req, r.URL.Query())
	if err != nil {
		return apierrors.ErrListPipelineLabel.InvalidParameter(err).ToResp(), nil
	}

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if !identityInfo.IsInternalClient() {
		return errorresp.ErrResp(fmt.Errorf("auth error: not internal client"))
	}

	if len(req.PipelineYmlName) <= 0 {
		return apierrors.ErrListPipelineLabel.InvalidParameter("missing pipelineYmlName").ToResp(), nil
	}

	if len(req.PipelineSource) <= 0 {
		return apierrors.ErrListPipelineLabel.InvalidParameter("missing pipelineSource").ToResp(), nil
	}

	pageResult, err := e.pipelineSvc.ListLabels(&req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(pageResult)
}
