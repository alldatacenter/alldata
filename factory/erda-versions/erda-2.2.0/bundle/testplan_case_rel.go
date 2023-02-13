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

func (b *Bundle) ListTestPlanCaseRel(testCaseIDs []uint64) ([]apistructs.TestPlanCaseRel, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	urlQueryStrings := make(map[string][]string)
	for _, tcID := range testCaseIDs {
		urlQueryStrings["id"] = append(urlQueryStrings["id"], fmt.Sprintf("%d", tcID))
	}

	var listResp apistructs.TestPlanCaseRelListResponse
	resp, err := hc.Get(host).Path("/api/testplans/testcase-relations/actions/internal-list").
		Header(httputil.InternalHeader, "bundle").
		Params(urlQueryStrings).
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}

	return listResp.Data, nil
}
