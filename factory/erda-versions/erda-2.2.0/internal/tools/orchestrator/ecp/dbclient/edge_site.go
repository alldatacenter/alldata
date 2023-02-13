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

// ListEdgeSite List edge site
func (c *DBClient) ListEdgeSite(param *apistructs.EdgeSiteListPageRequest) (int, *[]EdgeSite, error) {
	var (
		total     int
		edgeSites []EdgeSite
		db        = c.Model(&EdgeSite{})
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

	if param.Search != "" {
		db = db.Where("locate(?, name)", param.Search)
	}

	db = db.Order("id")

	if param.NotPaging {
		if err := db.Find(&edgeSites).Error; err != nil {
			return 0, nil, err
		}
	} else {
		if err := db.Offset((param.PageNo - 1) * param.PageSize).Limit(param.PageSize).
			Find(&edgeSites).Error; err != nil {
			return 0, nil, err
		}
	}

	if err := db.Count(&total).Error; err != nil {
		return 0, nil, err
	}
	return total, &edgeSites, nil
}

// GetEdgeSite Get edge site
func (c *DBClient) GetEdgeSite(edgeSiteID int64) (*EdgeSite, error) {
	var (
		edgeSite EdgeSite
	)
	if err := c.Where("id = ?", edgeSiteID).Find(&edgeSite).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
		return nil, err
	}
	return &edgeSite, nil
}

// CreateEdgeSite Create edge site record
func (c *DBClient) CreateEdgeSite(edgeSite *EdgeSite) error {
	return c.Create(edgeSite).Error
}

// UpdateEdgeSite Update edge site
func (c *DBClient) UpdateEdgeSite(edgeSite *EdgeSite) error {
	return c.Save(edgeSite).Error
}

// DeleteEdgeSite Delete edge site
func (c *DBClient) DeleteEdgeSite(edgeSiteID int64) error {
	return c.Where("id = ?", edgeSiteID).Delete(&EdgeSite{}).Error
}
