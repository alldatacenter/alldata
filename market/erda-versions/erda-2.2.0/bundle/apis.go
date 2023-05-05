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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// CreateTestAPI 创建API 测试
func (b *Bundle) CreateTestAPI(req apistructs.ApiTestInfo) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var createResp apistructs.ApiTestsCreateResponse
	httpResp, err := hc.Post(host).Path("/api/apitests").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&createResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !createResp.Success {
		return toAPIError(httpResp.StatusCode(), createResp.Error)
	}
	return nil
}

// UpdateTestAPI 更新 API 测试
func (b *Bundle) UpdateTestAPI(req apistructs.ApiTestInfo) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var updateResp apistructs.ApiTestsUpdateResponse
	httpResp, err := hc.Put(host).Path(fmt.Sprintf("/api/apitests/%d", req.ApiID)).
		Header(httputil.InternalHeader, "bundle").
		JSONBody(&req).
		Do().JSON(&updateResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !updateResp.Success {
		return toAPIError(httpResp.StatusCode(), updateResp.Error)
	}
	return nil
}

// DeleteTestAPI 删除 API 测试
func (b *Bundle) DeleteTestAPI(apiID int64) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var delResp apistructs.ApiTestsDeleteResponse
	httpResp, err := hc.Delete(host).Path(fmt.Sprintf("/api/apitests/%d", apiID)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&delResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !delResp.Success {
		return toAPIError(httpResp.StatusCode(), delResp.Error)
	}
	return nil
}

// GetTestAPI 获取指定 API 测试信息
func (b *Bundle) GetTestAPI(apiID int64) (*apistructs.ApiTestInfo, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.ApiTestsGetResponse
	httpResp, err := hc.Get(host).Path(fmt.Sprintf("/api/apitests/%d", apiID)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !getResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), getResp.Error)
	}
	return getResp.Data, nil
}

// ListTestAPI 获取 API 测试列表
func (b *Bundle) ListTestAPI(usecaseID int64) ([]*apistructs.ApiTestInfo, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.ApiTestsListResponse
	httpResp, err := hc.Get(host).Path("/api/apitests/actions/list-apis").
		Header(httputil.InternalHeader, "bundle").
		Param("usecaseID", strconv.FormatInt(usecaseID, 10)).
		Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !getResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), getResp.Error)
	}
	return getResp.Data, nil
}
