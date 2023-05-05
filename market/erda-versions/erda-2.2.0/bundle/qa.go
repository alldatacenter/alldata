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

// ExecuteApiTest 执行接口测试
func (b *Bundle) ExecuteApiTest(req apistructs.ApiTestsActionRequest) (uint64, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var executeResp apistructs.ApiTestsActionResponse
	resp, err := hc.Post(host).Path("/api/apitests/actions/execute-tests").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&executeResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !executeResp.Success {
		return 0, toAPIError(resp.StatusCode(), executeResp.Error)
	}

	return executeResp.Data, nil
}

// InternalRemoveTestPlanCaseRelIssueRelationsByIssueID 内部使用，根据 issueID 删除测试计划用例与 bug 的关联关系
func (b *Bundle) InternalRemoveTestPlanCaseRelIssueRelationsByIssueID(issueID uint64) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var removeResp struct {
		apistructs.Header
	}
	resp, err := hc.Delete(host).Path("/api/testplans/testcase-relations/actions/internal-remove-issue-relations").
		Header(httputil.InternalHeader, "bundle").
		Param("issueID", strconv.FormatUint(issueID, 10)).
		Do().JSON(&removeResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !removeResp.Success {
		return toAPIError(resp.StatusCode(), removeResp.Error)
	}

	return nil
}
