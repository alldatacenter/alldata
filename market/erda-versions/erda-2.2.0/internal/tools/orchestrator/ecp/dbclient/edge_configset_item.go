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
	"strings"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
)

const (
	ScopePublic = "public"
)

// ListEdgeConfigSetItem List edge configSet item
func (c *DBClient) ListEdgeConfigSetItem(param *apistructs.EdgeCfgSetItemListPageRequest) (int, *[]EdgeConfigSetItem, error) {
	var (
		total       int
		cfgSetItems []EdgeConfigSetItem
		db          = c.Model(&EdgeConfigSetItem{})
	)

	if param.ConfigSetID <= 0 {
		return 0, nil, fmt.Errorf("must provide configset id")
	}

	if (param.Scope == ScopePublic && param.SiteID > 0) || param.SiteID < 0 {
		return 0, nil, fmt.Errorf("illegal site id")
	}

	db = db.Where("configset_id = ?", param.ConfigSetID)

	// If specified scope then query public type only
	// If want to select scope site, please provider site id
	if param.Scope == ScopePublic {
		db = db.Where("scope = ?", ScopePublic)
	}

	if param.SiteID != 0 {
		db = db.Where("site_id = ?", param.SiteID)
	}

	if param.Search != "" {
		cfgSet, err := c.GetEdgeConfigSet(param.ConfigSetID)
		if err != nil {
			return 0, nil, fmt.Errorf("get configset error when search configset item: %v", err)
		}
		// Search name
		_, sites, err := c.ListEdgeSite(&apistructs.EdgeSiteListPageRequest{
			OrgID:     cfgSet.OrgID,
			ClusterID: cfgSet.ClusterID,
			NotPaging: true,
			Search:    param.Search,
		})

		if err != nil {
			return 0, nil, fmt.Errorf("get sites error when search configset item: %v", err)
		}

		candidateSites := make([]uint64, 0)

		for _, site := range *sites {
			candidateSites = append(candidateSites, site.ID)
		}

		if strings.Contains(ScopePublic, param.Search) {
			candidateSites = append(candidateSites, 0)
		}

		db = db.Where("locate(?, item_key) or locate(?, item_value) or site_id in (?)",
			param.Search, param.Search, candidateSites,
		)
	}

	db = db.Order("id")

	if param.NotPaging {
		if err := db.Find(&cfgSetItems).Error; err != nil {
			return 0, nil, err
		}
	} else {
		if err := db.Offset((param.PageNo - 1) * param.PageSize).Limit(param.PageSize).
			Find(&cfgSetItems).Error; err != nil {
			return 0, nil, err
		}
	}

	if err := db.Count(&total).Error; err != nil {
		return 0, nil, err
	}
	return total, &cfgSetItems, nil
}

// GetEdgeConfigSetItem Get edge configSet item by id
func (c *DBClient) GetEdgeConfigSetItem(itemID int64) (*EdgeConfigSetItem, error) {
	var cfgSetItem EdgeConfigSetItem
	if err := c.Where("id = ?", itemID).Find(&cfgSetItem).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
		return nil, err
	}
	return &cfgSetItem, nil
}

// GetEdgeConfigSetItemsBySiteID Get configSet item by site id.
func (c *DBClient) GetEdgeConfigSetItemsBySiteID(siteID int64) (*[]EdgeConfigSetItem, error) {
	var (
		cfgSetItem = make([]EdgeConfigSetItem, 0)
	)

	if err := c.Where("site_id = ?", siteID).Find(&cfgSetItem).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
		return nil, err
	}
	return &cfgSetItem, nil
}

// CreateEdgeConfigSetItem Create edge configSet item
func (c *DBClient) CreateEdgeConfigSetItem(cfgSetItem *EdgeConfigSetItem) error {
	return c.Create(cfgSetItem).Error
}

// UpdateEdgeConfigSetItem Update edge configSet item
func (c *DBClient) UpdateEdgeConfigSetItem(cfgSetItem *EdgeConfigSetItem) error {
	return c.Save(cfgSetItem).Error
}

// DeleteEdgeConfigSetItem Delete edge configSet item
func (c *DBClient) DeleteEdgeConfigSetItem(cfgSetItemID int64) error {
	return c.Where("id = ?", cfgSetItemID).Delete(&EdgeConfigSetItem{}).Error
}

// DeleteEdgeConfigSetItemBySiteID Delete all edge configSet item under provided site id
func (c *DBClient) DeleteEdgeConfigSetItemBySiteID(siteID int64) error {
	return c.Where("site_id = ?", siteID).Delete(&EdgeConfigSetItem{}).Error
}

// DeleteEdgeCfgSetItemByCfgID Delete all edge configSet item under provided configSet id
func (c *DBClient) DeleteEdgeCfgSetItemByCfgID(configSetID int64) error {
	return c.Where("configset_id = ?", configSetID).Delete(&EdgeConfigSetItem{}).Error
}
