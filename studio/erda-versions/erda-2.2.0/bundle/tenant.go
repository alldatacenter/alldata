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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) TenantGroupInfo(scopeId string) (*apistructs.TenantGroupInfo, error) {
	host, err := b.urls.MSP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var infoResp apistructs.GetTenantGroupInfoResponse
	resp, err := hc.Get(host).Path("/api/msp/tenant/projectInfo").
		Header(httputil.InternalHeader, "bundle").
		Param("scopeId", scopeId).
		Do().JSON(&infoResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !infoResp.Success {
		return nil, toAPIError(resp.StatusCode(), infoResp.Error)
	}
	return infoResp.Data, nil
}
