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

func (b *Bundle) ListSubscribes(userID, orgID string, req apistructs.GetSubscribeReq) (data *apistructs.SubscribeDTO, err error) {
	data = &apistructs.SubscribeDTO{}
	host, err := b.urls.CoreServices()
	if err != nil {
		return
	}
	hc := b.hc

	var rsp apistructs.GetSubscribesResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/subscribe")).
		Param("type", req.Type.String()).
		Param("typeID", strconv.FormatUint(req.TypeID, 10)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Header(httputil.OrgHeader, orgID).
		Do().JSON(&rsp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !rsp.Success {
		err = toAPIError(resp.StatusCode(), rsp.Error)
		return
	}

	return &rsp.Data, nil
}

func (b *Bundle) CreateSubscribe(userID, orgID string, req apistructs.CreateSubscribeReq) (string, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return "", err
	}
	hc := b.hc

	var rsp apistructs.CreateSubscribeRsp
	resp, err := hc.Post(host).Path(fmt.Sprintf("/api/subscribe")).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Header(httputil.OrgHeader, orgID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return "", toAPIError(resp.StatusCode(), rsp.Error)
	}
	return rsp.Data, nil
}

func (b *Bundle) DeleteSubscribe(userID, orgID string, req apistructs.UnSubscribeReq) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var rsp apistructs.Header
	resp, err := hc.Delete(host).Path(fmt.Sprintf("/api/subscribe")).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.UserHeader, userID).
		Header(httputil.OrgHeader, orgID).
		JSONBody(&req).
		Do().JSON(&rsp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !rsp.Success {
		return toAPIError(resp.StatusCode(), rsp.Error)
	}
	return nil
}
