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
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) GetCloudAccount(accountID uint64) (*apistructs.CloudAccountAllInfo, error) {
	host, err := b.urls.CMDB()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var accountResp apistructs.CloudAccountGetResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/cloud-accounts/%d", accountID)).Header(httputil.InternalHeader, "bundle").Do().JSON(&accountResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !accountResp.Success {
		return nil, toAPIError(resp.StatusCode(), accountResp.Error)
	}

	return &accountResp.Data, nil
}
