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

package apidocsvc

import (
	"net/http"
	"path/filepath"
	"strconv"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/websocket"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/cache/org"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// Upgrade updates the scheme http to ws.
// It writes the error to w http.ResponseWriter, so the caller need not to handle the error.
func (svc *Service) Upgrade(w http.ResponseWriter, r *http.Request, req *apistructs.WsAPIDocHandShakeReq) *errorresp.APIError {
	ft, err := bundle.NewGittarFileTree(req.URIParams.Inode)
	if err != nil {
		return apierrors.WsUpgrade.InvalidParameter("invalid inode")
	}
	appID, err := strconv.ParseUint(ft.ApplicationID(), 10, 64)
	if err != nil {
		return apierrors.WsUpgrade.InvalidParameter("invalid inode")
	}

	h := APIDocWSHandler{
		orgID:     req.OrgID,
		userID:    req.Identity.UserID,
		appID:     appID,
		branch:    ft.BranchName(),
		filename:  filepath.Base(ft.PathFromRepoRoot()),
		req:       req,
		svc:       svc,
		sessionID: uuid.New(),
		ft:        ft,
		trans:     svc.trans,
	}
	if orgDTO, ok := org.GetOrgByOrgID(strconv.FormatUint(req.OrgID, 10)); ok {
		h.codes, _ = i18n.ParseLanguageCode(orgDTO.Locale)
	}

	ws := websocket.New()
	ws.Register(heartBeatRequest, h.wrap(h.handleHeartBeat))
	ws.Register(autoSaveRequest, h.wrap(h.handleAutoSave))
	ws.Register(commitRequest, h.wrap(h.handleCommit))
	ws.AfterConnected(h.AfterConnected)
	ws.BeforeClose(h.BeforeClose)
	if err := ws.Upgrade(w, r); err != nil {
		return apierrors.WsUpgrade.InvalidParameter(err)
	}
	ws.Run()

	return nil
}
