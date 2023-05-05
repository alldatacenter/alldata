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

func (client *DBClient) QueryAppPublishItemRelations(req apistructs.QueryAppPublishItemRelationRequest) ([]apistructs.AppPublishItemRelation, error) {
	var items []apistructs.AppPublishItemRelation
	db := client.Table("dice_app_publish_item_relation as relation").
		Joins("inner join dice_app on dice_app.id = relation.app_id").
		Joins("inner join dice_publish_items on dice_publish_items.id = relation.publish_item_id").
		Joins("inner join dice_publishers on dice_publishers.id = dice_publish_items.publisher_id").
		Select("dice_app.name as app_name ,dice_publish_items.name as publish_item_name,dice_publishers.name as publisher_name," +
			"relation.env,relation.app_id,relation.publish_item_id,relation.ak,relation.ai,dice_publish_items.publisher_id as publisher_id")

	if req.AppID > 0 {
		db = db.Where("relation.app_id = ?", req.AppID)
	}
	if req.PublishItemID > 0 {
		db = db.Where("relation.publish_item_id = ?", req.PublishItemID)
	}
	if req.AK != "" && req.AI != "" {
		db = db.Where("relation.ak = ? and relation.ai = ?", req.AK, req.AI)
	}

	err := db.Scan(&items).Error

	return items, err
}

func (client *DBClient) RemovePublishItemRelations(request *apistructs.RemoveAppPublishItemRelationsRequest) error {
	return client.Where("publish_item_id=?", request.PublishItemId).Delete(&model.ApplicationPublishItemRelation{}).Error
}

func (client *DBClient) UpdateAppPublishItemRelations(request *apistructs.UpdateAppPublishItemRelationRequest) error {
	if request.DEVItemID > 0 {
		r := &model.ApplicationPublishItemRelation{
			Env:           apistructs.DevWorkspace,
			AppID:         request.AppID,
			PublishItemID: request.DEVItemID,
			Creator:       request.UserID,
		}
		mk, ok := request.AKAIMap[apistructs.DevWorkspace]
		if ok {
			r.AK, r.AI = mk.AK, mk.AI
		}
		if client.Model(model.ApplicationPublishItemRelation{}).Where("app_id = ? and env = ?",
			request.AppID, apistructs.DevWorkspace).Updates(r).RowsAffected == 0 {
			if err := client.Create(r).Error; err != nil {
				return err
			}
		}
	}

	if request.TESTItemID > 0 {
		r := &model.ApplicationPublishItemRelation{
			Env:           apistructs.TestWorkspace,
			AppID:         request.AppID,
			PublishItemID: request.TESTItemID,
			Creator:       request.UserID,
		}
		mk, ok := request.AKAIMap[apistructs.TestWorkspace]
		if ok {
			r.AK, r.AI = mk.AK, mk.AI
		}
		if client.Model(model.ApplicationPublishItemRelation{}).Where("app_id = ? and env = ?",
			request.AppID, apistructs.TestWorkspace).Updates(r).RowsAffected == 0 {
			if err := client.Create(r).Error; err != nil {
				return err
			}
		}
	}

	if request.STAGINGItemID > 0 {
		r := &model.ApplicationPublishItemRelation{
			Env:           apistructs.StagingWorkspace,
			AppID:         request.AppID,
			PublishItemID: request.STAGINGItemID,
			Creator:       request.UserID,
		}
		mk, ok := request.AKAIMap[apistructs.StagingWorkspace]
		if ok {
			r.AK, r.AI = mk.AK, mk.AI
		}
		if client.Model(model.ApplicationPublishItemRelation{}).Where("app_id = ? and env = ?",
			request.AppID, apistructs.StagingWorkspace).Updates(r).RowsAffected == 0 {
			if err := client.Create(r).Error; err != nil {
				return err
			}
		}
	}

	if request.ProdItemID > 0 {
		r := &model.ApplicationPublishItemRelation{
			Env:           apistructs.WORKSPACE_PROD,
			AppID:         request.AppID,
			PublishItemID: request.ProdItemID,
			Creator:       request.UserID,
		}
		mk, ok := request.AKAIMap[apistructs.ProdWorkspace]
		if ok {
			r.AK, r.AI = mk.AK, mk.AI
		}
		if client.Model(model.ApplicationPublishItemRelation{}).Where("app_id = ? and env = ?",
			request.AppID, apistructs.WORKSPACE_PROD).Updates(r).RowsAffected == 0 {
			if err := client.Create(r).Error; err != nil {
				return err
			}
		}
	}
	return nil
}
