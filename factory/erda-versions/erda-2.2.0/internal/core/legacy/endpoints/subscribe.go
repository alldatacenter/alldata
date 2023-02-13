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
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func GetUserInfo(r *http.Request) (string, uint64, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return "", 0, err
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return "", 0, err
	}
	return userID.String(), orgID, err
}

func (e *Endpoints) Subscribe(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// request check
	uid, oid, err := GetUserInfo(r)
	if err != nil {
		return apierrors.ErrCreateSubscribe.InvalidParameter(err).ToResp(), nil
	}
	if r.Body == nil {
		return apierrors.ErrCreateSubscribe.MissingParameter("body is nil").ToResp(), nil
	}
	var req apistructs.CreateSubscribeReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateSubscribe.InvalidParameter("can't decode body").ToResp(), nil
	}
	req.UserID = uid
	req.OrgID = oid
	if err := req.Validate(); err != nil {
		return apierrors.ErrCreateSubscribe.InvalidParameter(err.Error()).ToResp(), nil
	}
	logrus.Infof("request body: %+v", req)

	// permission check
	pReq := apistructs.PermissionCheckRequest{
		UserID:   uid,
		Scope:    apistructs.OrgScope,
		ScopeID:  oid,
		Resource: apistructs.SubscribeResource,
		Action:   apistructs.CreateAction,
	}
	if access, err := e.permission.CheckPermission(&pReq); err != nil || !access {
		return apierrors.ErrCreateApplication.AccessDenied().ToResp(), nil
	}

	// request process
	subscribeID, err := e.subscribe.Subscribe(req)
	if err != nil {
		return apierrors.ErrCreateApplication.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(subscribeID)
}

func (e *Endpoints) UnSubscribe(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// request check
	uid, oid, err := GetUserInfo(r)
	if err != nil {
		return apierrors.ErrDeleteSubscribe.InvalidParameter(err).ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrDeleteSubscribe.MissingParameter("body is nil").ToResp(), nil
	}

	var req apistructs.UnSubscribeReq
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrDeleteSubscribe.InvalidParameter("can't decode body").ToResp(), nil
	}
	req.UserID = uid
	req.OrgID = oid
	logrus.Infof("request body: %+v", req)

	// permission check
	pReq := apistructs.PermissionCheckRequest{
		UserID:   uid,
		Scope:    apistructs.OrgScope,
		ScopeID:  oid,
		Resource: apistructs.SubscribeResource,
		Action:   apistructs.DeleteAction,
	}
	if access, err := e.permission.CheckPermission(&pReq); err != nil || !access {
		return apierrors.ErrDeleteSubscribe.AccessDenied().ToResp(), nil
	}

	// request process
	err = e.subscribe.UnSubscribe(req)
	if err != nil {
		return apierrors.ErrDeleteSubscribe.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

func (e *Endpoints) GetSubscribes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// request check
	uid, oid, err := GetUserInfo(r)
	if err != nil {
		return apierrors.ErrGetSubscribe.InvalidParameter(err).ToResp(), nil
	}

	var req apistructs.GetSubscribeReq
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrGetSubscribe.InvalidParameter(err).ToResp(), nil
	}

	req.UserID = uid
	req.OrgID = oid
	if err := req.Validate(); err != nil {
		return apierrors.ErrCreateSubscribe.InvalidParameter(err.Error()).ToResp(), nil
	}
	logrus.Infof("request body: %+v", req)

	// permission check
	pReq := apistructs.PermissionCheckRequest{
		UserID:   uid,
		Scope:    apistructs.OrgScope,
		ScopeID:  oid,
		Resource: apistructs.SubscribeResource,
		Action:   apistructs.GetAction,
	}
	if access, err := e.permission.CheckPermission(&pReq); err != nil || !access {
		return apierrors.ErrGetSubscribe.AccessDenied().ToResp(), nil
	}

	// request process
	items, err := e.subscribe.GetSubscribes(req)
	if err != nil {
		return apierrors.ErrGetSubscribe.InternalError(err).ToResp(), nil
	}

	data := apistructs.SubscribeDTO{
		Total: len(items),
		List:  items,
	}

	return httpserver.OkResp(data)
}

func getListSubscribeParam(r *http.Request) (*apistructs.GetSubscribeReq, error) {
	req := apistructs.GetSubscribeReq{}
	req.Type = apistructs.SubscribeType(r.URL.Query().Get("type"))

	id := r.URL.Query().Get("typeID")
	if id != "" {
		typeID, err := strconv.Atoi(id)
		if err != nil {
			return nil, fmt.Errorf("invalid param, typeID is invalid")
		}
		req.TypeID = uint64(typeID)
	}
	return &req, nil
}
