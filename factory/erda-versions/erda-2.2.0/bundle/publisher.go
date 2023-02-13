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

// FetchPublisher 获取 publisher 详情
func (b *Bundle) FetchPublisher(publisherID uint64) (*apistructs.PublisherDTO, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var publisherResp apistructs.PublisherDetailResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/publishers/%d", publisherID)).
		Header("Internal-Client", "bundle").
		Do().JSON(&publisherResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !publisherResp.Success {
		return nil, toAPIError(resp.StatusCode(), publisherResp.Error)
	}
	return &publisherResp.Data, nil
}

func (b *Bundle) GetUserRelationPublisher(userID string, orgID string) (*apistructs.PagingPublisherDTO, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var publisherResp apistructs.PublisherListResponse
	resp, err := hc.Get(host).Path(fmt.Sprintf("/api/publishers/actions/list-my-publishers")).
		Header("Internal-Client", "bundle").
		Header("USER-ID", userID).
		Header(httputil.OrgHeader, orgID).
		Do().JSON(&publisherResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !publisherResp.Success {
		return nil, toAPIError(resp.StatusCode(), publisherResp.Error)
	}
	return &publisherResp.Data, nil
}
