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
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

// GetApplicationPublishItemRelationsGroupByENV 根据环境分组应用和发布内容关联
func (e *Endpoints) GetApplicationPublishItemRelationsGroupByENV(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	applicationID, err := strutil.Atoi64(vars["applicationID"])
	if err != nil {
		return apierrors.ErrGetApplicationPublishItemRelation.InvalidParameter(err).ToResp(), nil
	}

	result, err := e.app.GetPublishItemRelationsMap(apistructs.QueryAppPublishItemRelationRequest{AppID: applicationID})
	if err != nil {
		return apierrors.ErrGetApplicationPublishItemRelation.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(result)
}

// QueryApplicationPublishItemRelations 查询应用和发布内容关联
func (e *Endpoints) QueryApplicationPublishItemRelations(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	var req apistructs.QueryAppPublishItemRelationRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrPagingIssues.InvalidParameter(err).ToResp(), nil
	}

	relations, err := e.app.QueryPublishItemRelations(req)
	if err != nil {
		return apierrors.ErrGetApplicationPublishItemRelation.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(relations)
}

// UpdateApplicationPublishItemRelations 更新应用发布内容关联
func (e *Endpoints) UpdateApplicationPublishItemRelations(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	applicationID, err := strutil.Atoi64(vars["applicationID"])
	if err != nil {
		return apierrors.ErrUpdateApplicationPublishItemRelation.InvalidParameter(err).ToResp(), nil
	}

	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateApplicationPublishItemRelation.InvalidParameter(err).ToResp(), nil
	}

	var request apistructs.UpdateAppPublishItemRelationRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrUpdateApplicationPublishItemRelation.InvalidParameter("can't decode body").ToResp(), nil
	}
	request.AppID = applicationID
	request.UserID = userID.String()
	request.AKAIMap = make(map[apistructs.DiceWorkspace]apistructs.MonitorKeys, 0)
	err = e.app.UpdatePublishItemRelations(&request)
	if err != nil {
		return apierrors.ErrUpdateApplicationPublishItemRelation.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("")
}

// RemoveApplicationPublishItemRelations 删除应用发布内容关联
func (e *Endpoints) RemoveApplicationPublishItemRelations(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	var request apistructs.RemoveAppPublishItemRelationsRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrRemoveApplicationPublishItemRelation.InvalidParameter("can't decode body").ToResp(), nil
	}
	err := e.app.RemovePublishItemRelations(&request)
	if err != nil {
		return apierrors.ErrRemoveApplicationPublishItemRelation.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("")
}
