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

package dbclient

import (
	"fmt"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
)

// ListEdgeConfigSet List edge configSet
func (c *DBClient) ListEdgeConfigSet(param *apistructs.EdgeConfigSetListPageRequest) (int, *[]EdgeConfigSet, error) {
	var (
		total          int
		edgeConfigSets []EdgeConfigSet
		db             = c.Model(&EdgeConfigSet{})
	)

	if param.OrgID < 0 && param.ClusterID < 0 {
		return 0, nil, fmt.Errorf("illegal orgin id and cluster id")
	}

	if param.OrgID > 0 && param.ClusterID > 0 {
		db = db.Where("org_id = ? and cluster_id = ?", param.OrgID, param.ClusterID)
	} else if param.OrgID > 0 && param.ClusterID <= 0 {
		db = db.Where("org_id = ?", param.OrgID)
	} else if param.ClusterID > 0 && param.OrgID <= 0 {
		db = db.Where("cluster_id = ?", param.ClusterID)
	}

	db = db.Order("id")

	if param.NotPaging {
		if err := db.Find(&edgeConfigSets).Error; err != nil {
			return 0, nil, err
		}
	} else {
		if err := db.Offset((param.PageNo - 1) * param.PageSize).Limit(param.PageSize).
			Find(&edgeConfigSets).Error; err != nil {
			return 0, nil, err
		}
	}

	if err := db.Count(&total).Error; err != nil {
		return 0, nil, err
	}
	return total, &edgeConfigSets, nil
}

// GetEdgeConfigSet Get edge configSet by configSet id
func (c *DBClient) GetEdgeConfigSet(configSetID int64) (*EdgeConfigSet, error) {
	var edgeConfigSet EdgeConfigSet

	if err := c.Where("id = ?", configSetID).Find(&edgeConfigSet).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
		return nil, err
	}
	fmt.Println(edgeConfigSet)
	return &edgeConfigSet, nil
}

// CreateEdgeConfigSet Create edge configSet
func (c *DBClient) CreateEdgeConfigSet(edgeConfigSet *EdgeConfigSet) error {
	return c.Create(edgeConfigSet).Error
}

// UpdateEdgeConfigSet Update edge configSet
func (c *DBClient) UpdateEdgeConfigSet(edgeConfigSet *EdgeConfigSet) error {
	return c.Save(edgeConfigSet).Error
}

// DeleteEdgeConfigSet Delete edge configSet
func (c *DBClient) DeleteEdgeConfigSet(edgeConfigSetID int64) error {
	return c.Where("id = ?", edgeConfigSetID).Delete(&EdgeConfigSet{}).Error
}
