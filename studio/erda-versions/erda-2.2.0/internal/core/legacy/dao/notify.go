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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

func (client *DBClient) CreateNotify(request *apistructs.CreateNotifyRequest) (int64, error) {
	notify := model.Notify{
		Name:          request.Name,
		ScopeType:     request.ScopeType,
		ScopeID:       request.ScopeID,
		NotifyGroupID: request.NotifyGroupID,
		Enabled:       request.Enabled,
		Channels:      request.Channels,
		OrgID:         request.OrgID,
		Label:         request.Label,
		ClusterName:   request.ClusterName,
		Creator:       request.Creator,
	}

	err := client.Create(&notify).Error
	if err != nil {
		return 0, nil
	}

	for _, itemID := range request.NotifyItemIDs {
		itemRelation := model.NotifyItemRelation{
			NotifyItemID: itemID,
			NotifyID:     notify.ID,
		}
		err := client.Create(&itemRelation).Error
		if err != nil {
			return 0, err
		}
	}
	//默认通知源
	if request.NotifySources == nil {
		request.NotifySources = []apistructs.NotifySource{
			{
				Name:       request.ScopeType + "-" + request.ScopeID,
				SourceID:   request.ScopeID,
				SourceType: request.ScopeType,
			},
		}
	}
	for _, source := range request.NotifySources {
		notifySource := model.NotifySource{
			Name:       source.Name,
			NotifyID:   notify.ID,
			SourceType: source.SourceType,
			SourceID:   source.SourceID,
			OrgID:      request.OrgID,
		}
		err := client.Create(&notifySource).Error
		if err != nil {
			return 0, err
		}
	}
	return notify.ID, nil
}

func (client *DBClient) CheckNotifyNameExist(scopeType, scopeID, name, label string) (bool, error) {
	var count int64
	err := client.Model(&model.Notify{}).Where("scope_type =? and scope_id =? and name = ? and label = ?",
		scopeType, scopeID, name, label).Count(&count).Error
	return count > 0, err
}

func (client *DBClient) UpdateNotifyEnable(id int64, enabled bool, orgID int64) error {
	return client.Model(&model.Notify{}).Where("id = ? and org_id=?", id, orgID).Update("enabled", enabled).Error
}

func (client *DBClient) UpdateNotify(request *apistructs.UpdateNotifyRequest) error {
	var notify model.Notify
	err := client.Where("id = ? ", request.ID).First(&notify).Error
	if err != nil {
		return err
	}

	notify.Channels = request.Channels
	if request.WithGroup {
		err := client.UpdateNotifyGroup(&apistructs.UpdateNotifyGroupRequest{
			ID:      notify.NotifyGroupID,
			Name:    request.GroupName,
			Targets: request.GroupTargets,
			OrgID:   request.OrgID,
		})
		if err != nil {
			return err
		}
	} else {
		notify.NotifyGroupID = request.NotifyGroupID
	}
	client.Delete(model.NotifyItemRelation{}, "notify_id = ?", notify.ID)
	client.Delete(model.NotifySource{}, "notify_id = ?", notify.ID)
	for _, itemID := range request.NotifyItemIDs {
		itemRelation := model.NotifyItemRelation{
			NotifyItemID: itemID,
			NotifyID:     notify.ID,
		}
		err := client.Create(&itemRelation).Error
		if err != nil {
			return err
		}
	}
	//默认通知源
	if request.NotifySources == nil {
		request.NotifySources = []apistructs.NotifySource{
			{
				Name:       notify.ScopeType + "-" + notify.ScopeID,
				SourceID:   notify.ScopeID,
				SourceType: notify.ScopeType,
			},
		}
	}
	for _, source := range request.NotifySources {
		notifySource := model.NotifySource{
			Name:       source.Name,
			NotifyID:   notify.ID,
			SourceType: source.SourceType,
			SourceID:   source.SourceID,
			OrgID:      request.OrgID,
		}
		err := client.Create(&notifySource).Error
		if err != nil {
			return err
		}
	}

	return client.Save(&notify).Error
}

func (client *DBClient) QueryNotifies(request *apistructs.QueryNotifyRequest) (*apistructs.QueryNotifyData, error) {
	var notifies []model.Notify
	query := client.Model(&model.Notify{}).Where("org_id = ?", request.OrgID)
	if request.ScopeType != "" {
		query = query.Where("scope_type = ?", request.ScopeType)
	}
	if request.ScopeID != "" {
		query = query.Where("scope_id = ?", request.ScopeID)
	}
	if request.Label != "" {
		query = query.Where("label = ?", request.Label)
	}
	if request.ClusterName != "" {
		query = query.Where("cluster_name = ?", request.ClusterName)
	}
	var count int
	err := query.Count(&count).Error
	if err != nil {
		return nil, err
	}

	err = query.Order("created_at desc").Offset((request.PageNo - 1) * request.PageSize).
		Limit(request.PageSize).
		Find(&notifies).Error
	if err != nil {
		return nil, err
	}

	result := &apistructs.QueryNotifyData{
		Total: count,
		List:  []*apistructs.NotifyDetail{},
	}
	for _, notify := range notifies {
		apiNotify := &apistructs.NotifyDetail{
			ID:        notify.ID,
			Name:      notify.Name,
			ScopeType: notify.ScopeType,
			ScopeID:   notify.ScopeID,
			Channels:  notify.Channels,
			Enabled:   notify.Enabled,
			UpdatedAt: notify.UpdatedAt,
			CreatedAt: notify.CreatedAt,
			Creator:   notify.Creator,
		}
		apiNotify.NotifyGroup, err = client.GetNotifyGroupByID(notify.NotifyGroupID, request.OrgID)
		apiNotify.NotifyItems, err = client.GetNotifyItemsByNotifyID(notify.ID)
		apiNotify.NotifySources, err = client.GetNotifySourcesByNotifyID(notify.ID)
		result.List = append(result.List, apiNotify)
	}
	return result, nil
}

func (client *DBClient) QueryNotifiesBySource(sourceType, sourceID, itemName string, orgID int64, clusterName string, label string) ([]*apistructs.NotifyDetail, error) {
	var notifies []model.Notify
	query := client.Table("dice_notify_sources").
		Joins("inner join dice_notifies on dice_notifies.id = dice_notify_sources.notify_id").
		Joins("inner join dice_notify_item_relation on dice_notify_item_relation.notify_id = dice_notify_sources.notify_id").
		Joins("inner join dice_notify_items on dice_notify_items.id = dice_notify_item_relation.notify_item_id").
		Where("dice_notifies.enabled =1 and dice_notify_sources.source_type =? and "+
			"(dice_notify_sources.source_id =? or dice_notify_sources.source_id =0 )and dice_notify_items.name = ? and dice_notifies.org_id =? ",
			sourceType, sourceID, itemName, orgID)
	if clusterName != "" {
		query = query.Where("dice_notifies.cluster_name =?", clusterName)
	}
	if label != "" {
		query = query.Where("dice_notifies.label = ?", label)
	}
	query = query.Select("dice_notifies.id,dice_notifies.name,dice_notifies.scope_type,dice_notifies.scope_id,dice_notifies.notify_group_id," +
		"dice_notifies.channels,dice_notifies.enabled,dice_notifies.updated_at,dice_notifies.created_at")
	err := query.Scan(&notifies).Error
	if err != nil {
		return nil, err
	}

	var result []*apistructs.NotifyDetail
	for _, notify := range notifies {
		apiNotify := &apistructs.NotifyDetail{
			ID:        notify.ID,
			Name:      notify.Name,
			ScopeType: notify.ScopeType,
			ScopeID:   notify.ScopeID,
			Channels:  notify.Channels,
			Enabled:   notify.Enabled,
			UpdatedAt: notify.UpdatedAt,
			CreatedAt: notify.CreatedAt,
		}
		apiNotify.NotifyGroup, err = client.GetNotifyGroupByIDWithoutOrgID(notify.NotifyGroupID)
		apiNotify.NotifyItems, err = client.GetNotifyItemsByNotifyIDAndItemName(notify.ID, itemName)
		apiNotify.NotifySources, err = client.GetNotifySourcesByNotifyID(notify.ID)
		result = append(result, apiNotify)
	}
	return result, nil
}

func (client *DBClient) FuzzyQueryNotifiesBySource(req apistructs.FuzzyQueryNotifiesBySourceRequest) ([]*apistructs.NotifyDetail, int, error) {
	var notifies []model.Notify
	var total int
	query := client.Table("dice_notify_sources").
		Joins("inner join dice_notifies on dice_notifies.id = dice_notify_sources.notify_id").
		Joins("inner join dice_notify_item_relation on dice_notify_item_relation.notify_id = dice_notify_sources.notify_id").
		Joins("inner join dice_notify_items on dice_notify_items.id = dice_notify_item_relation.notify_item_id").
		Where("dice_notifies.org_id = ? and dice_notifies.label = ?", req.OrgID, req.Label)
	if req.SourceType != "" {
		query = query.Where("dice_notify_sources.source_type = ?", req.SourceType)
	}
	if req.ClusterName != "" {
		query = query.Where("dice_notifies.cluster_name LIKE ?", genFuzzyQuery(req.ClusterName))
	}
	if req.SourceName != "" {
		query = query.Where("dice_notify_sources.name LIKE ?", genFuzzyQuery(req.SourceName))
	}
	if req.NotifyName != "" {
		query = query.Where("dice_notifies.name LIKE ?", genFuzzyQuery(req.NotifyName))
	}
	if req.ItemName != "" {
		query = query.Where("dice_notify_items.name LIKE ?", genFuzzyQuery(req.ItemName))
	}
	if req.Channel != "" {
		query = query.Where("dice_notifies.channels LIKE ?", genFuzzyQuery(req.Channel))
	}
	query = query.Select("dice_notifies.id,dice_notifies.name,dice_notifies.scope_type,dice_notifies.scope_id,dice_notifies.notify_group_id," +
		"dice_notifies.channels,dice_notifies.enabled,dice_notifies.updated_at,dice_notifies.created_at").Group("dice_notifies.id").
		Order("created_at desc").Offset((req.PageNo - 1) * req.PageSize).Count(&total).Limit(req.PageSize)
	err := query.Scan(&notifies).Error
	if err != nil {
		return nil, 0, err
	}

	var result []*apistructs.NotifyDetail
	for _, notify := range notifies {
		apiNotify := &apistructs.NotifyDetail{
			ID:        notify.ID,
			Name:      notify.Name,
			ScopeType: notify.ScopeType,
			ScopeID:   notify.ScopeID,
			Channels:  notify.Channels,
			Enabled:   notify.Enabled,
			UpdatedAt: notify.UpdatedAt,
			CreatedAt: notify.CreatedAt,
		}
		apiNotify.NotifyGroup, _ = client.GetNotifyGroupByIDWithoutOrgID(notify.NotifyGroupID)
		apiNotify.NotifyItems, _ = client.QuerytNotifyItemsByNotifyIDAndItemName(notify.ID, req.ItemName)
		apiNotify.NotifySources, _ = client.GetNotifySourcesByNotifyID(notify.ID)
		result = append(result, apiNotify)
	}
	return result, total, nil
}

func (client *DBClient) GetNotifyByGroupID(groupID int64) (*model.Notify, error) {
	var notify model.Notify
	err := client.Where("notify_group_id = ?", groupID).Find(&notify).Error
	if err != nil {
		return nil, err
	}
	return &notify, nil
}

func (client *DBClient) GetNotifyDetail(id int64, orgID int64) (*apistructs.NotifyDetail, error) {
	var notify model.Notify
	err := client.Where("id = ? and org_id =?", id, orgID).Find(&notify).Error
	if err != nil {
		return nil, err
	}
	notifyGroup, err := client.GetNotifyGroupByID(notify.NotifyGroupID, orgID)
	if err != nil {
		return nil, err
	}
	notifyItems, err := client.GetNotifyItemsByNotifyID(notify.ID)
	if err != nil {
		return nil, err
	}
	notifySources, err := client.GetNotifySourcesByNotifyID(notify.ID)
	if err != nil {
		return nil, err
	}
	notifyDetail := &apistructs.NotifyDetail{
		ID:            notify.ID,
		Name:          notify.Name,
		ScopeType:     notify.ScopeType,
		ScopeID:       notify.ScopeID,
		Channels:      notify.Channels,
		NotifyGroup:   notifyGroup,
		NotifyItems:   notifyItems,
		NotifySources: notifySources,
		Enabled:       notify.Enabled,
		UpdatedAt:     notify.UpdatedAt,
		CreatedAt:     notify.CreatedAt,
		Creator:       notify.Creator,
	}

	return notifyDetail, nil
}

func (client *DBClient) DeleteNotify(id int64, withGroup bool, orgID int64) error {
	var notify model.Notify
	err := client.Where("id = ? and org_id= ?", id, orgID).Find(&notify).Error
	if err != nil {
		return err
	}
	client.Delete(model.NotifyItemRelation{}, "notify_id = ?", notify.ID)
	client.Delete(model.NotifySource{}, "notify_id = ?", notify.ID)
	if withGroup {
		client.DeleteNotifyGroup(notify.NotifyGroupID, orgID)
	}
	return client.Delete(&notify).Error
}

func genFuzzyQuery(cond string) string {
	return "%" + cond + "%"
}
