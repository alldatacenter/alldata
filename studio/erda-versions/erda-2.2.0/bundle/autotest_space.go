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

// ListTestSpace 获取测试空间列表
func (b *Bundle) ListTestSpace(req apistructs.AutoTestSpaceListRequest) (*apistructs.AutoTestSpaceList, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var listResp apistructs.AutoTestSpaceListResponse
	resp, err := hc.Get(host).Path("/api/autotests/spaces").
		Params(req.URLQueryString()).
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}

	return listResp.Data, nil
}

// DeleteTestSpace 删除测试空间
func (b *Bundle) DeleteTestSpace(id uint64, userID string) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var listResp apistructs.AutoTestSpaceListResponse
	resp, err := hc.Delete(host).Path("/api/autotests/spaces/"+strconv.FormatUint(id, 10)).
		// Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Do().JSON(&listResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return toAPIError(resp.StatusCode(), listResp.Error)
	}

	return nil
}

// CreateTestSpace 创建测试空间
func (b *Bundle) CreateTestSpace(req *apistructs.AutoTestSpaceCreateRequest, userID string) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var listResp apistructs.AutoTestSpaceResponse
	resp, err := hc.Post(host).Path("/api/autotests/spaces").
		// Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(req).
		Do().JSON(&listResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return toAPIError(resp.StatusCode(), listResp.Error)
	}

	return nil
}

// UpdateTestSpace 更新测试空间
func (b *Bundle) UpdateTestSpace(space *apistructs.AutoTestSpace, userID string) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var listResp apistructs.AutoTestSpaceResponse
	resp, err := hc.Put(host).Path("/api/autotests/spaces").
		// Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(&space).
		Do().JSON(&listResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return toAPIError(resp.StatusCode(), listResp.Error)
	}

	return nil
}

// ListTestSpace 获取测试空间列表
func (b *Bundle) GetTestSpace(id uint64) (*apistructs.AutoTestSpace, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var res apistructs.AutoTestSpaceResponse
	resp, err := hc.Get(host).Path("/api/autotests/spaces/"+strconv.FormatUint(id, 10)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&res)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !res.Success {
		return nil, toAPIError(resp.StatusCode(), res.Error)
	}

	return res.Data, nil
}

// CopyTestSpace 复制测试空间
func (b *Bundle) CopyTestSpace(spaceID uint64, userID string) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc

	var listResp apistructs.AutoTestSpaceResponse
	resp, err := hc.Post(host).Path("/api/autotests/spaces/actions/copy").
		// Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		JSONBody(apistructs.AutoTestSpace{
			ID: spaceID,
		}).
		Do().JSON(&listResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return toAPIError(resp.StatusCode(), listResp.Error)
	}

	return nil
}

// ExportTestSpace export autotest space
func (b *Bundle) ExportTestSpace(userID string, req apistructs.AutoTestSpaceExportRequest) error {
	host, err := b.urls.DOP()
	if err != nil {
		return err
	}
	hc := b.hc
	var exportID uint64
	_, err = hc.Post(host).Path("/api/autotests/spaces/actions/export").
		Header(httputil.UserHeader, userID).
		JSONBody(req).Do().JSON(&exportID)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	return nil
}

func (b *Bundle) GetAutoTestSpaceStats(spaceIDs []string) (map[uint64]*apistructs.AutoTestSpaceStats, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var listResp apistructs.AutoTestSpaceStatsResponse
	resp, err := hc.Get(host).Path("/api/autotests/spaces/actions/stats").
		Params(map[string][]string{
			"spaceIDs": spaceIDs,
		}).
		Do().JSON(&listResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !listResp.Success {
		return nil, toAPIError(resp.StatusCode(), listResp.Error)
	}

	return listResp.Data, nil
}
