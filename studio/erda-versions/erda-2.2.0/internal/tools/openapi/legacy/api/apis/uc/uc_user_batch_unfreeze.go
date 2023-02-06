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

package uc

import (
	"encoding/json"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apierrors"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

var UC_USER_BATCH_UNFREEZE = apis.ApiSpec{
	Path:         "/api/users/actions/batch-unfreeze",
	Scheme:       "http",
	Method:       "PUT",
	Custom:       batchUnFreezeUser,
	CheckLogin:   true,
	CheckToken:   true,
	RequestType:  apistructs.UserBatchUnFreezeRequest{},
	ResponseType: apistructs.UserBatchUnFreezeResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 批量用户解冻",
}

func batchUnFreezeUser(w http.ResponseWriter, r *http.Request) {
	operatorID, err := user.GetUserID(r)
	if err != nil {
		apierrors.ErrAdminUser.NotLogin().Write(w)
		return
	}

	if err := checkPermission(operatorID, apistructs.UpdateAction); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	token, err := auth.GetDiceClientToken()
	if err != nil {
		logrus.Errorf("failed to get token: %v", err)
		apierrors.ErrBatchUnFreezeUser.InternalError(err).
			Write(w)
		return
	}
	// check login & permission
	_, err = mustManageUsersPerm(r, apierrors.ErrBatchUnFreezeUser)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	// get req
	var req apistructs.UserBatchUnFreezeRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	userIDs := req.UserIDs
	logrus.Debugf("to unfreeze userID: %v", userIDs)
	// handle
	operatorIDStr := operatorID.String()
	for _, userID := range userIDs {
		if err := handleUnfreezeUser(userID, operatorIDStr, token); err != nil {
			errorresp.ErrWrite(err, w)
			return
		}
	}
	httpserver.WriteData(w, nil)
}
