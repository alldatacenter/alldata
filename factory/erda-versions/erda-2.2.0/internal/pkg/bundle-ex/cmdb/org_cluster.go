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
	"time"

	"github.com/erda-project/erda/apistructs"
)

// OrgClusterRelationDTO 企业对应集群关系结构
type OrgClusterRelationDTO struct {
	ID          uint64    `json:"id"`
	OrgID       uint64    `json:"orgId"`
	OrgName     string    `json:"orgName"`
	ClusterID   uint64    `json:"clusterId"`
	ClusterName string    `json:"clusterName"`
	Creator     string    `json:"creator"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

type orgClusterRelResp struct {
	apistructs.Header
	Data []*OrgClusterRelationDTO `json:"data"`
}

// QueryAllOrgClusterRelation 获取所有的企业集群关联关系
func (c *Cmdb) QueryAllOrgClusterRelation() ([]*OrgClusterRelationDTO, error) {
	var resp orgClusterRelResp
	hresp, err := c.hc.Get(c.url).
		Path("/api/orgs/clusters/relations").
		Header("User-ID", c.operatorID).
		Do().JSON(&resp)
	if err != nil {
		return nil, err
	}
	if !hresp.IsOK() || !resp.Success {
		return nil, fmt.Errorf("status: %d, error: %v", hresp.StatusCode(), resp.Error)
	}
	return resp.Data, nil
}
