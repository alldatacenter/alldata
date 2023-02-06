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
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// CreateLabel 创建标签
func (e *Endpoints) CreateLabel(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.ProjectLabelCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateLabel.InvalidParameter(err).ToResp(), nil
	}
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateLabel.NotLogin().ToResp(), nil
	}
	req.IdentityInfo = identityInfo
	logrus.Debugf("create label request body: %+v", req)

	labelID, err := e.label.Create(&req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(labelID)
}

// DeleteLabel 删除标签
func (e *Endpoints) DeleteLabel(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	labelIDStr := vars["id"]
	labelID, err := strconv.ParseInt(labelIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeleteLabel.InvalidParameter(err).ToResp(), nil
	}

	label, err := e.label.GetByID(labelID)
	if err != nil {
		logrus.Errorf("when get label for audit faild %v", err)
	}

	if err := e.label.Delete(labelID); err != nil {
		return apierrors.ErrDeleteLabel.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(label)
}

// UpdateLabel 更新标签
func (e *Endpoints) UpdateLabel(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	if _, err := user.GetIdentityInfo(r); err != nil {
		return apierrors.ErrUpdateLabel.NotLogin().ToResp(), nil
	}

	labelIDStr := vars["id"]
	labelID, err := strconv.ParseInt(labelIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrUpdateLabel.InvalidParameter(err).ToResp(), nil
	}

	// 检查body合法性
	if r.Body == nil {
		return apierrors.ErrUpdateLabel.MissingParameter("body is nil").ToResp(), nil
	}
	var req apistructs.ProjectLabelUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateLabel.InvalidParameter(err).ToResp(), nil
	}
	logrus.Debugf("update label request body: %+v", req)

	req.ID = labelID

	// 更新label至DB
	if err = e.label.Update(&req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(labelID)
}

// ListLabel 获取标签列表
func (e *Endpoints) ListLabel(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	if _, err := user.GetIdentityInfo(r); err != nil {
		return apierrors.ErrGetLabels.NotLogin().ToResp(), nil
	}

	var req apistructs.ProjectLabelListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrGetLabels.InvalidParameter(err).ToResp(), nil
	}

	total, labels, err := e.label.List(&req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	userIDs := make([]string, 0, len(labels))
	for _, v := range labels {
		userIDs = append(userIDs, v.Creator)
	}

	return httpserver.OkResp(apistructs.ProjectLabelListResponseData{
		Total: total,
		List:  labels,
	}, userIDs)
}

// GetLabel 通过id获取label
func (e *Endpoints) GetLabel(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	if _, err := user.GetIdentityInfo(r); err != nil {
		return apierrors.ErrGetLabels.NotLogin().ToResp(), nil
	}

	labelIDStr := vars["id"]
	labelID, err := strconv.ParseInt(labelIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetLabels.InvalidParameter(err).ToResp(), nil
	}

	label, err := e.label.GetByID(labelID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(label)
}

// ListByNamesAndProjectID list label by names and projectID
func (e *Endpoints) ListByNamesAndProjectID(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	if _, err := user.GetIdentityInfo(r); err != nil {
		return apierrors.ErrListByNamesAndProjectID.NotLogin().ToResp(), nil
	}

	var req apistructs.ListByNamesAndProjectIDRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListByNamesAndProjectID.InvalidParameter(err).ToResp(), nil
	}

	labels, err := e.label.ListByNamesAndProjectID(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(labels)
}

// ListLabelByIDs list label by ids
func (e *Endpoints) ListLabelByIDs(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	if _, err := user.GetIdentityInfo(r); err != nil {
		return apierrors.ErrListLabelByIDs.NotLogin().ToResp(), nil
	}

	var req apistructs.ListLabelByIDsRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListLabelByIDs.InvalidParameter(err).ToResp(), nil
	}

	labels, err := e.label.ListLabelByIDs(req.IDs)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(labels)
}
