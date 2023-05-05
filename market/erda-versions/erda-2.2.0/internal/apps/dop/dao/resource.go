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

package dao

import (
	"github.com/erda-project/erda/internal/core/legacy/model"
)

// GetOrgClusterRelationsByOrg 根据 orgID 获取企业对应集群关系
func (client *DBClient) GetOrgClusterRelationsByOrg(orgID uint64) ([]model.OrgClusterRelation, error) {
	var relations []model.OrgClusterRelation
	if err := client.Where("org_id = ?", orgID).Find(&relations).Error; err != nil {
		return nil, err
	}
	return relations, nil
}
