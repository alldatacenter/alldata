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

// CreateMBox 创建站内信记录
func (b *Bundle) CreateMBox(request *apistructs.CreateMBoxRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var getResp apistructs.CreateMBoxResponse
	resp, err := hc.Post(host).Path("/api/mboxs").JSONBody(request).
		Do().JSON(&getResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return toAPIError(resp.StatusCode(), getResp.Error)
	}
	return nil
}

func (b *Bundle) GetMboxStats(identity apistructs.Identity) (int64, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var qr apistructs.QueryMBoxStatsResponse
	resp, err := hc.Get(host).Path("/api/mboxs/actions/stats").
		Header(httputil.UserHeader, identity.UserID).
		Header(httputil.OrgHeader, identity.OrgID).
		Do().JSON(&qr)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !qr.Success {
		return 0, toAPIError(resp.StatusCode(), qr.Error)
	}

	return qr.Data.UnreadCount, nil
}

func (b *Bundle) ListMbox(identity apistructs.Identity, req apistructs.QueryMBoxRequest) (apistructs.QueryMBoxData, error) {
	data := apistructs.QueryMBoxData{}
	host, err := b.urls.CoreServices()
	if err != nil {
		return data, err
	}
	hc := b.hc

	var qr apistructs.QueryMBoxResponse
	resp, err := hc.Get(host).Path("/api/mboxs").
		Header(httputil.UserHeader, identity.UserID).
		Header(httputil.OrgHeader, identity.OrgID).
		Param("pageNo", strconv.FormatInt(req.PageNo, 10)).
		Param("pageSize", strconv.FormatInt(req.PageSize, 10)).
		Param("status", string(req.Status)).
		Param("type", string(req.Type)).
		Do().JSON(&qr)
	if err != nil {
		return data, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !qr.Success {
		return data, toAPIError(resp.StatusCode(), qr.Error)
	}

	return qr.Data, nil
}

// SetMBoxReadStatus 设置站内信为已读
func (b *Bundle) SetMBoxReadStatus(identity apistructs.Identity, request *apistructs.SetMBoxReadStatusRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var setResp apistructs.Header
	resp, err := hc.Post(host).Path("/api/mboxs/actions/set-read").JSONBody(request).
		Header(httputil.UserHeader, identity.UserID).
		Header(httputil.OrgHeader, identity.OrgID).
		Do().JSON(&setResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !setResp.Success {
		return toAPIError(resp.StatusCode(), setResp.Error)
	}
	return nil
}
