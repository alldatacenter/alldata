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
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) ListFileRecords(userID string, req apistructs.ListTestFileRecordsRequest) (*apistructs.ListFileRecordsResponse, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var rsp apistructs.ListFileRecordsResponse
	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/test-file-records")).
		Header(httputil.UserHeader, userID).
		Params(req.ConvertToQueryParams()).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrListFileRecord.InternalError(err)
	}
	if !httpResp.IsOK() || !rsp.Success {
		return nil, toAPIError(httpResp.StatusCode(), rsp.Error)
	}

	return &rsp, nil
}
