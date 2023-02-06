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

func (client *DBClient) CreateNotifyItem(request *apistructs.CreateNotifyItemRequest) (int64, error) {
	notifyItem := model.NotifyItem{
		Name:           request.Name,
		DisplayName:    request.DisplayName,
		Category:       request.Category,
		MobileTemplate: request.MobileTemplate,
		EmailTemplate:  request.EmailTemplate,
	}

	err := client.Create(&notifyItem).Error
	if err != nil {
		return 0, nil
	}
	return notifyItem.ID, nil
}

func (client *DBClient) UpdateNotifyItem(request *apistructs.UpdateNotifyItemRequest) error {
	var notifyItem model.NotifyItem
	err := client.Where("id = ? ", request.ID).First(&notifyItem).Error
	if err != nil {
		return err
	}
	notifyItem.MobileTemplate = request.MobileTemplate
	return client.Save(&notifyItem).Error
}

func (client *DBClient) DeleteNotifyItem(id int64) error {
	var notifyItem model.NotifyItem
	err := client.Where("id = ?", id).Find(&notifyItem).Error
	if err != nil {
		return err
	}
	return client.Delete(&notifyItem).Error
}

func (client *DBClient) QueryNotifyItems(request *apistructs.QueryNotifyItemRequest) (*apistructs.QueryNotifyItemData, error) {
	var notifyItems []model.NotifyItem
	query := client.Model(&model.NotifyItem{})
	if request.Category != "" {
		query = query.Where("category = ?", request.Category)
	}
	if request.Label != "" {
		query = query.Where("label = ?", request.Label)
	}
	if request.ScopeType != "" {
		query = query.Where("scope_type =?", request.ScopeType)
	}
	var count int
	err := query.Count(&count).Error
	if err != nil {
		return nil, err
	}

	err = query.Offset((request.PageNo - 1) * request.PageSize).
		Limit(request.PageSize).
		Find(&notifyItems).Error
	if err != nil {
		return nil, err
	}

	result := &apistructs.QueryNotifyItemData{
		Total: count,
		List:  []*apistructs.NotifyItem{},
	}
	for _, notifyItem := range notifyItems {
		result.List = append(result.List, notifyItem.ToApiData())
	}
	return result, nil
}

func (client *DBClient) GetNotifyItemsByNotifyID(notifyID int64) ([]*apistructs.NotifyItem, error) {
	var items []*apistructs.NotifyItem
	err := client.Table("dice_notify_item_relation").
		Joins("inner join dice_notify_items on dice_notify_items.id = dice_notify_item_relation.notify_item_id").
		Select("dice_notify_items.id, dice_notify_items.name,dice_notify_items.display_name,dice_notify_items.category,dice_notify_items.label,dice_notify_items.scope_type,"+
			"dice_notify_items.mobile_template,dice_notify_items.email_template,dice_notify_items.dingding_template,dice_notify_items.mbox_template").
		Where("dice_notify_item_relation.notify_id = ?", notifyID).
		Scan(&items).Error
	return items, err
}

func (client *DBClient) GetNotifyItemsByNotifyIDAndItemName(notifyID int64, itemName string) ([]*apistructs.NotifyItem, error) {
	var items []*apistructs.NotifyItem
	err := client.Table("dice_notify_item_relation").
		Joins("inner join dice_notify_items on dice_notify_items.id = dice_notify_item_relation.notify_item_id").
		Select("dice_notify_items.id, dice_notify_items.name,dice_notify_items.display_name,dice_notify_items.category,dice_notify_items.label,dice_notify_items.scope_type,"+
			"dice_notify_items.mobile_template,dice_notify_items.email_template,dice_notify_items.dingding_template,"+
			"dice_notify_items.mbox_template,dice_notify_items.vms_template").
		Where("dice_notify_item_relation.notify_id = ? and dice_notify_items.name = ?", notifyID, itemName).
		Scan(&items).Error
	return items, err
}

func (client *DBClient) QuerytNotifyItemsByNotifyIDAndItemName(notifyID int64, itemName string) ([]*apistructs.NotifyItem, error) {
	var items []*apistructs.NotifyItem
	sql := client.Table("dice_notify_item_relation").
		Joins("inner join dice_notify_items on dice_notify_items.id = dice_notify_item_relation.notify_item_id").
		Select("dice_notify_items.id, dice_notify_items.name,dice_notify_items.display_name,dice_notify_items.category,dice_notify_items.label,dice_notify_items.scope_type,"+
			"dice_notify_items.mobile_template,dice_notify_items.email_template,dice_notify_items.dingding_template,"+
			"dice_notify_items.mbox_template,dice_notify_items.vms_template").
		Where("dice_notify_item_relation.notify_id = ?", notifyID)
	if itemName != "" {
		sql.Where("dice_notify_items.name = ?", itemName)
	}
	err := sql.Scan(&items).Error
	return items, err
}
