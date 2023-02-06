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

package cmdb

import (
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
)

type notifyGroupResp struct {
	apistructs.Header
	Data []*apistructs.NotifyGroup `json:"data"`
}

// QueryNotifyGroup .
func (c *Cmdb) QueryNotifyGroup(groupIDs []string) ([]*apistructs.NotifyGroup, error) {
	var resp *notifyGroupResp
	hresp, err := c.hc.Get(c.url).
		Path("/api/notify-groups/actions/batch-get").
		Header("User-ID", c.operatorID).
		Param("ids", strings.Join(groupIDs, ",")).
		Do().JSON(&resp)
	if err != nil {
		return nil, err
	}
	if !hresp.IsOK() || !resp.Success {
		return nil, fmt.Errorf("status: %d, error: %v", hresp.StatusCode(), resp.Error)
	}
	return resp.Data, nil
}
