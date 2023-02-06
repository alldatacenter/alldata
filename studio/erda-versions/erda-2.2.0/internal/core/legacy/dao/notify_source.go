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

func (client *DBClient) GetNotifySourcesByNotifyID(notifyID int64) ([]*apistructs.NotifySource, error) {
	var items []*apistructs.NotifySource
	err := client.Table("dice_notify_sources").
		Joins("inner join dice_notifies on dice_notifies.id = dice_notify_sources.notify_id").
		Select("dice_notify_sources.id, dice_notify_sources.name,dice_notify_sources.source_type,dice_notify_sources.source_id").
		Where("dice_notify_sources.notify_id = ?", notifyID).
		Scan(&items).Error
	return items, err
}

func (client *DBClient) DeleteNotifySource(request *apistructs.DeleteNotifySourceRequest) error {
	err := client.Where("source_type =? and source_id= ? and org_id = ?",
		request.SourceType, request.SourceID, request.OrgID).
		Delete(&model.NotifySource{}).Error
	return err
}
