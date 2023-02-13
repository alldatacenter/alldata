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
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

// ListDomains 查询域名列表
func (e *Endpoints) ListDomains(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	v := vars["runtimeID"]
	runtimeID, err := strutil.Atoi64(v)
	if err != nil {
		return apierrors.ErrListDomain.InvalidParameter(strutil.Concat("runtimeID: ", v)).ToResp(), nil
	}
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrListDomain.InvalidParameter(err).ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListDomain.NotLogin().ToResp(), nil
	}
	data, err := e.domain.List(userID, orgID, uint64(runtimeID))
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

// UpdateDomains 更新域名
func (e *Endpoints) UpdateDomains(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	v := vars["runtimeID"]
	runtimeID, err := strutil.Atoi64(v)
	if err != nil {
		return apierrors.ErrUpdateDomain.InvalidParameter(strutil.Concat("runtimeID: ", v)).ToResp(), nil
	}
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrUpdateDomain.InvalidParameter(err).ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateDomain.NotLogin().ToResp(), nil
	}
	var group apistructs.DomainGroup
	if err := json.NewDecoder(r.Body).Decode(&group); err != nil {
		return apierrors.ErrUpdateDomain.InvalidParameter(err).ToResp(), nil
	}
	if err := e.domain.Update(userID, orgID, uint64(runtimeID), &group); err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(nil)
}
