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

// Create or update component ingress
func (b *Bundle) CreateOrUpdateComponentIngress(req apistructs.ComponentIngressUpdateRequest) error {
	host, err := b.urls.Hepa()
	if err != nil {
		return err
	}
	var fetchResp apistructs.ComponentIngressUpdateResponse
	resp, err := b.hc.Put(host).Path("/api/gateway/component-ingress").Header(httputil.InternalHeader, "bundle").JSONBody(req).Do().JSON(&fetchResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return toAPIError(resp.StatusCode(), fetchResp.Error)
	}
	return nil
}
