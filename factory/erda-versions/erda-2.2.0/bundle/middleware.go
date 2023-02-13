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

package bundle

import (
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// ListMiddleware list real addon instances by params
func (b *Bundle) ListMiddleware(orgID, userID string, r *apistructs.MiddlewareListRequest) (*apistructs.MiddlewareListResponse, error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	path := "/api/middlewares"
	projectID := strconv.FormatInt(int64(r.ProjectID), 10)

	var data apistructs.MiddlewareListResponse
	resp, err := hc.Post(host).Path(path).
		Param("projectId", projectID).
		Param("addonName", r.AddonName).
		Param("workspace", r.Workspace).
		Param("instanceId", r.InstanceID).
		Param("ip", r.InstanceID).
		Header("org-id", orgID).
		Header("user-id", userID).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&data)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, toAPIError(resp.StatusCode(), data.Error)
	}
	return &data, nil
}
