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
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) CreateOrEditNotify(submitData *apistructs.EditOrCreateModalData, inParams *apistructs.InParams, userId string) error {
	host, err := b.urls.Monitor()
	if err != nil {
		return err
	}
	hc := b.hc
	var resp apistructs.Header
	var path string
	createMap := make(map[string]interface{})
	createMap["templateId"] = submitData.Items
	createMap["notifyName"] = submitData.Name
	createMap["notifyGroupId"] = submitData.Target
	createMap["channels"] = submitData.Channels
	var httpResp *httpclient.Response
	if submitData.Id != 0 {
		path = fmt.Sprintf("/api/notify/records/%d?scope=%v&scopeId=%v", submitData.Id, inParams.ScopeType, inParams.ScopeId)
		httpResp, err = hc.Put(host).Path(path).JSONBody(createMap).Header(httputil.UserHeader, userId).Do().JSON(&resp)
	} else {
		path = fmt.Sprintf("/api/notify/records?scope=%v&scopeId=%v", inParams.ScopeType, inParams.ScopeId)
		httpResp, err = hc.Post(host).Path(path).JSONBody(createMap).Header(httputil.UserHeader, userId).Do().JSON(&resp)
	}
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return nil
}
