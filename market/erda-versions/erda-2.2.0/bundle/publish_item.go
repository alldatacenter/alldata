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

// QueryPublishItems queries publish items from dicehub
func (b *Bundle) QueryPublishItems(req *apistructs.QueryPublishItemRequest) (*apistructs.QueryPublishItemData, error) {
	host, err := b.urls.DiceHub()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var getResp apistructs.QueryPublishItemResponse
	resp, err := hc.Get(host).Path("/api/publish-items").
		Param("pageSize", strconv.FormatInt(req.PageSize, 10)).
		Param("type", req.Type).
		Param("ids", req.Ids).
		Param("q", req.Q).
		Param("pageNo", strconv.FormatInt(req.PageNo, 10)).
		Param("publisherId", strconv.FormatInt(req.PublisherId, 10)).
		Param("name", req.Name).
		Header("Internal-Client", "bundle").
		Header("Org-ID", strconv.FormatInt(req.OrgID, 10)).
		Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return &getResp.Data, nil
}

// QueryMyPublishItem queries my publishing items from dicehub
func (b *Bundle) QueryMyPublishItem(userID string, req *apistructs.QueryPublishItemRequest) (*apistructs.QueryPublishItemData, error) {
	host, err := b.urls.DiceHub()
	if err != nil {
		return nil, err
	}
	var values = make(url.Values)
	req.ToValues(values)
	request := b.hc.Get(host).Path("/api/my-publish-items").
		Header("Internal-Client", "bundle").
		Header("User-ID", userID).
		Header(httputil.OrgHeader, strconv.FormatInt(req.OrgID, 10))
	for k := range values {
		request.Param(k, values.Get(k))
	}
	var response apistructs.QueryPublishItemResponse
	resp, err := request.Do().JSON(&response)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !response.Success {
		return nil, toAPIError(resp.StatusCode(), response.Error)
	}
	return &response.Data, nil
}
