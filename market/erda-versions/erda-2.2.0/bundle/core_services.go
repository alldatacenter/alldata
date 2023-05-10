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
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) ProjectClusterReferred(userID, orgID, clusterName string) (referred bool, err error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return
	}
	hc := b.hc

	var rsp struct {
		apistructs.Header
		Data bool `json:"data"`
	}

	resp, err := hc.Get(host).
		Path(fmt.Sprintf("/api/projects/actions/refer-cluster")).
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Param("cluster", clusterName).
		Do().JSON(&rsp)

	if err != nil {
		return
	}
	if !resp.IsOK() || !rsp.Success {
		err = toAPIError(resp.StatusCode(), rsp.Error)
		return
	}

	return rsp.Data, nil
}
