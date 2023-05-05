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
	"net/url"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) GetCurrentUser(userID string) (*apistructs.UserInfo, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var userResp apistructs.UserCurrentResponse
	resp, err := hc.Get(host).Path("/api/users/current").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Do().JSON(&userResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !userResp.Success {
		return nil, toAPIError(resp.StatusCode(), userResp.Error)
	}
	return &userResp.Data, nil
}

func (b *Bundle) ListUsers(req apistructs.UserListRequest) (*apistructs.UserListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var userResp apistructs.UserListResponse
	resp, err := hc.Get(host).Path("/api/users").
		Header(httputil.InternalHeader, "bundle").
		Param("q", req.Query).
		Param("plaintext", strconv.FormatBool(req.Plaintext)).
		Params(url.Values{"userID": req.UserIDs}).
		Do().JSON(&userResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !userResp.Success {
		return nil, toAPIError(resp.StatusCode(), userResp.Error)
	}
	return &userResp.Data, nil
}

func (b *Bundle) GetUcUserID(uuid string) (string, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return "", err
	}
	hc := b.hc

	var userResp apistructs.UserIDResponse
	resp, err := hc.Get(host).Path("/api/users/actions/get-uc-user-id").
		Header(httputil.InternalHeader, "bundle").
		Param("id", uuid).
		Do().JSON(&userResp)
	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !userResp.Success {
		return "", toAPIError(resp.StatusCode(), userResp.Error)
	}
	return userResp.Data, nil
}

func (b *Bundle) SearchUser(params url.Values) (*apistructs.UserListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var userResp apistructs.UserListResponse
	resp, err := hc.Get(host).
		Path("/api/users/actions/search").
		Header(httputil.InternalHeader, "bundle").
		Params(params).
		Do().JSON(&userResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !userResp.Success {
		return nil, toAPIError(resp.StatusCode(), userResp.Error)
	}
	return &userResp.Data, nil
}
