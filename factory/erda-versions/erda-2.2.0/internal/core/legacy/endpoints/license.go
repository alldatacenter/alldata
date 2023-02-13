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
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// GetLicense 获取授权情况
func (e *Endpoints) GetLicense(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	if e.license == nil {
		return apierrors.ErrGetLicense.InvalidState("license is empty").ToResp(), nil
	}

	resp := apistructs.LicenseResponse{
		License: e.license,
		Valid:   true,
	}
	var hostCount uint64
	// TODO 1 refactor
	//hostCount, err := e.host.GetHostNumber()
	//if err != nil {
	//	return apierrors.ErrGetLicense.InternalError(err).ToResp(), nil
	//}
	resp.CurrentHostCount = hostCount

	if e.license.IsExpired() {
		resp.Valid = false
		resp.Message = "已过期"
		return httpserver.OkResp(resp)
	}
	if e.license.Data.MaxHostCount < hostCount {
		resp.Valid = false
		resp.Message = "超过最大host数"
		return httpserver.OkResp(resp)
	}
	return httpserver.OkResp(resp)
}
