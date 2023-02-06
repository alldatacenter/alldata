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

package edge

import (
	"context"
	"fmt"
	"strings"

	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/dbclient"
)

const (
	ScopePublic            = "public"
	ScopePublicDisplayName = "通用"
	ScopeSite              = "site"
	cmAPIVersion           = "v1"
	cmKind                 = "ConfigMap"
)

// ListConfigSetItem List configSet item paging or search condition.
func (e *Edge) ListConfigSetItem(param *apistructs.EdgeCfgSetItemListPageRequest) (int, *[]apistructs.EdgeCfgSetItemInfo, error) {
	total, cfgItems, err := e.db.ListEdgeConfigSetItem(param)
	if err != nil {
		return 0, nil, err
	}

	cfgSetItemInfos := make([]apistructs.EdgeCfgSetItemInfo, 0, len(*cfgItems))

	for _, cfgItem := range *cfgItems {
		if cfgItem.SiteID == 0 {
			cfgSetItemInfos = append(cfgSetItemInfos, *convertToEdgeCfgSetItemInfo(cfgItem, ScopePublic, ScopePublicDisplayName))
			continue
		}
		siteInfo, err := e.db.GetEdgeSite(cfgItem.SiteID)
		if err != nil {
			return 0, nil, fmt.Errorf("failed to get releated edge site(%d): %v", cfgItem.SiteID, err)
		}
		cfgSetItemInfos = append(cfgSetItemInfos, *convertToEdgeCfgSetItemInfo(cfgItem, siteInfo.Name, siteInfo.DisplayName))
	}
	return total, &cfgSetItemInfos, nil
}

// GetConfigSetItem Get configSet item by item id.
func (e *Edge) GetConfigSetItem(itemID int64) (*apistructs.EdgeCfgSetItemInfo, error) {
	var (
		itemInfo *apistructs.EdgeCfgSetItemInfo
	)

	item, err := e.db.GetEdgeConfigSetItem(itemID)
	if err != nil {
		return nil, err
	}

	if item.Scope == ScopePublic {
		itemInfo = convertToEdgeCfgSetItemInfo(*item, ScopePublic, ScopePublicDisplayName)
		return itemInfo, nil
	}

	site, err := e.db.GetEdgeSite(item.SiteID)
	if err != nil {
		return nil, err
	}

	itemInfo = convertToEdgeCfgSetItemInfo(*item, site.Name, site.DisplayName)

	return itemInfo, nil
}

// CreateConfigSetItem  Create configSet item.
func (e *Edge) CreateConfigSetItem(ctx context.Context, req *apistructs.EdgeCfgSetItemCreateRequest) ([]uint64, error) {
	var (
		configSetItemIDs = make([]uint64, 0)
		err              error
	)

	cfgSet, err := e.db.GetEdgeConfigSet(req.ConfigSetID)
	if err != nil {
		return nil, err
	}

	nsName, err := e.getConfigSetName(cfgSet.OrgID, cfgSet.Name)
	if err != nil {
		return nil, err
	}

	clusterInfo, err := e.getClusterInfo(ctx, cfgSet.ClusterID)
	if err != nil {
		return nil, err
	}

	if req.Scope == ScopePublic {
		err = e.createItemToConfigMap(clusterInfo.Name, nsName, ScopePublic, req.ItemKey, req.ItemValue)
		if err != nil {
			return nil, err
		}
		cfgSetItem := &dbclient.EdgeConfigSetItem{
			ConfigsetID: int64(cfgSet.ID),
			ItemKey:     req.ItemKey,
			ItemValue:   req.ItemValue,
		}

		cfgSetItem.Scope = ScopePublic

		_, res, err := e.db.ListEdgeConfigSetItem(&apistructs.EdgeCfgSetItemListPageRequest{
			ConfigSetID: int64(cfgSet.ID),
			Search:      req.ItemKey,
			SiteID:      0,
		})

		if err != nil {
			return nil, err
		}
		if len(*res) == 0 {
			err = e.db.CreateEdgeConfigSetItem(cfgSetItem)
			if err != nil {
				// If have errors when create record, rollback configMap resource.
				err = e.deleteItemToConfigMap(clusterInfo.Name, nsName, ScopePublic, req.ItemKey)
				if err != nil {
					return nil, err
				}
				return nil, err
			}
		} else {
			return nil, fmt.Errorf("item had exsited in database")
		}

		configSetItemIDs = append(configSetItemIDs, cfgSetItem.ID)
		return configSetItemIDs, nil
	}

	if req.Scope == ScopeSite {
		for _, siteID := range req.SiteIDs {
			site, err := e.db.GetEdgeSite(siteID)
			if err != nil {
				return nil, err
			}
			cm, err := e.k8s.GetConfigMap(clusterInfo.Name, nsName, site.Name)

			if isResourceNotFound(err) {
				continue
			} else if err != nil {
				return nil, err
			} else {
				if _, ok := cm.Data[req.ItemKey]; ok {
					return nil, fmt.Errorf("item key %s for site %s already existed in configset %s (scope: %s), please update it", req.ItemKey, site.Name, cfgSet.Name, ScopeSite)
				}
			}
		}

		createdCm := make([]string, 0)
		for _, siteID := range req.SiteIDs {
			site, err := e.db.GetEdgeSite(siteID)
			if err != nil {
				return nil, err
			}
			err = e.createItemToConfigMap(clusterInfo.Name, nsName, site.Name, req.ItemKey, req.ItemValue)
			if err != nil {
				return nil, err
			}
			createdCm = append(createdCm, site.Name)

			cfgSetItem := &dbclient.EdgeConfigSetItem{
				ConfigsetID: int64(cfgSet.ID),
				ItemKey:     req.ItemKey,
				ItemValue:   req.ItemValue,
			}
			cfgSetItem.Scope = ScopeSite
			cfgSetItem.SiteID = int64(site.ID)

			createError := e.db.CreateEdgeConfigSetItem(cfgSetItem)
			if createError != nil {
				backError := make([]string, 0)
				for _, cmName := range createdCm {
					err = e.deleteItemToConfigMap(clusterInfo.Name, nsName, cmName, req.ItemKey)
					if err != nil {
						backError = append(backError, err.Error())
						continue
					}
				}
				for _, cfgSetItemID := range configSetItemIDs {
					err = e.db.DeleteEdgeConfigSetItem(int64(cfgSetItemID))
					if err != nil {
						backError = append(backError, err.Error())
						continue
					}
				}
				if len(backError) != 0 {
					return nil, fmt.Errorf(fmt.Sprint(backError))
				}
				return nil, createError
			}
			configSetItemIDs = append(configSetItemIDs, cfgSetItem.ID)
		}
		return configSetItemIDs, nil
	}

	return configSetItemIDs, nil
}

// createItemToConfigMap Create configSet item(K-V) in kubernetes configMap resource.
func (e *Edge) createItemToConfigMap(clusterName, namespace, cmName, itemKey, itemValue string) error {
	var (
		defaultCM = &v1.ConfigMap{
			TypeMeta: metav1.TypeMeta{
				Kind:       cmKind,
				APIVersion: cmAPIVersion,
			},
			Data: map[string]string{itemKey: itemValue},
		}
	)
	cm, err := e.k8s.GetConfigMap(clusterName, namespace, cmName)

	if isResourceNotFound(err) {
		defaultCM.Name = cmName
		err = e.k8s.CreateConfigMap(clusterName, namespace, defaultCM)
		if err != nil {
			return err
		}
		return nil
	}

	if err != nil {
		return err
	}

	if _, ok := cm.Data[itemKey]; ok {
		return fmt.Errorf("item key %s already existed in %s (scope: %s), please update it", itemKey, cmName, ScopePublic)
	} else {
		if cm.Data == nil {
			cm.Data = make(map[string]string, 0)
		}
		cm.Data[itemKey] = itemValue
		err = e.k8s.UpdateConfigMap(clusterName, namespace, cm)
		if err != nil {
			return err
		}
	}

	return nil
}

// DeleteConfigSetItem Delete configSet item.
func (e *Edge) DeleteConfigSetItem(ctx context.Context, itemID int64) error {
	cfgSetItem, err := e.db.GetEdgeConfigSetItem(itemID)
	if err != nil {
		return err
	}
	cfgSet, err := e.db.GetEdgeConfigSet(cfgSetItem.ConfigsetID)
	if err != nil {
		return err
	}
	nsName, err := e.getConfigSetName(cfgSet.OrgID, cfgSet.Name)
	if err != nil {
		return err
	}

	clusterInfo, err := e.getClusterInfo(ctx, cfgSet.ClusterID)
	if err != nil {
		return err
	}

	if cfgSetItem.Scope == ScopePublic {
		err = e.deleteItemToConfigMap(clusterInfo.Name, nsName, ScopePublic, cfgSetItem.ItemKey)
		if err != nil {
			return err
		}
	} else if cfgSetItem.Scope == ScopeSite {
		site, err := e.db.GetEdgeSite(cfgSetItem.SiteID)
		if err != nil {
			return err
		}

		err = e.deleteItemToConfigMap(clusterInfo.Name, nsName, site.Name, cfgSetItem.ItemKey)
		if err != nil {
			return err
		}
	}

	err = e.db.DeleteEdgeConfigSetItem(itemID)
	if err != nil {
		// TODO: Update configmap which deleted.
		return err
	}

	return nil
}

// deleteItemToConfigMap Delete configSet item in kubernetes configMap resource.
func (e *Edge) deleteItemToConfigMap(clusterName, namespace, cmName, itemKey string) error {
	cm, err := e.k8s.GetConfigMap(clusterName, namespace, cmName)
	if isResourceNotFound(err) {
		return fmt.Errorf("configmap %s not found in cluster: %s, namespace: %s", cmName, clusterName, namespace)
	}
	if err != nil {
		return err
	}

	delete(cm.Data, itemKey)
	err = e.k8s.UpdateConfigMap(clusterName, namespace, cm)
	if err != nil {
		return err
	}
	return nil
}

// UpdateConfigSetItem Update configSet item.
func (e *Edge) UpdateConfigSetItem(ctx context.Context, itemID int64, req *apistructs.EdgeCfgSetItemUpdateRequest) error {
	var cm *v1.ConfigMap

	cfgSetItem, err := e.db.GetEdgeConfigSetItem(itemID)
	if err != nil {
		return err
	}

	cfgSet, err := e.db.GetEdgeConfigSet(cfgSetItem.ConfigsetID)
	if err != nil {
		return err
	}

	nsName, err := e.getConfigSetName(cfgSet.OrgID, cfgSet.Name)
	if err != nil {
		return err
	}

	clusterInfo, err := e.getClusterInfo(ctx, cfgSet.ClusterID)
	if err != nil {
		return err
	}

	if cfgSetItem.Scope == ScopePublic {
		cm, err = e.k8s.GetConfigMap(clusterInfo.Name, nsName, ScopePublic)
		if err != nil {
			return err
		}

	} else if cfgSetItem.Scope == ScopeSite {
		site, err := e.db.GetEdgeSite(cfgSetItem.SiteID)
		if err != nil {
			return err
		}

		cm, err = e.k8s.GetConfigMap(clusterInfo.Name, nsName, site.Name)
		if err != nil {
			return err
		}

	} else {
		return fmt.Errorf("no scope name %s", cfgSetItem.Scope)
	}

	cm.Data[cfgSetItem.ItemKey] = req.ItemValue

	err = e.k8s.UpdateConfigMap(clusterInfo.Name, nsName, cm)
	if err != nil {
		return err
	}

	cfgSetItem.ItemValue = req.ItemValue

	err = e.db.UpdateEdgeConfigSetItem(cfgSetItem)
	if err != nil {
		return err
	}
	return nil
}

// convertToEdgeCfgSetItemInfo Convert type EdgeConfigSetItem to type EdgeCfgSetItemInfo.
func convertToEdgeCfgSetItemInfo(item dbclient.EdgeConfigSetItem, siteName, siteDisplayName string) *apistructs.EdgeCfgSetItemInfo {
	return &apistructs.EdgeCfgSetItemInfo{
		ID:              int64(item.ID),
		ConfigSetID:     item.ConfigsetID,
		SiteID:          item.SiteID,
		SiteName:        siteName,
		SiteDisplayName: siteDisplayName,
		ItemKey:         item.ItemKey,
		ItemValue:       item.ItemValue,
		Scope:           item.Scope,
		CreatedAt:       item.CreatedAt,
		UpdatedAt:       item.UpdatedAt,
	}
}

// isResourceNotFound Parse kubernetes errors response, to determine whether or not "not found" error.
func isResourceNotFound(err error) bool {
	if err == nil {
		return false
	}
	if strings.Contains(err.Error(), "not found") {
		return true
	}
	return false
}
