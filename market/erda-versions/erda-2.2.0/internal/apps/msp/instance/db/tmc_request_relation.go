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

package db

import "github.com/jinzhu/gorm"

// TmcRequestRelationDB .
type TmcRequestRelationDB struct {
	*gorm.DB
}

func (db *TmcRequestRelationDB) GetChildRequestIdsByParentId(parentId string) ([]string, error) {
	var list []TmcRequestRelation
	if err := db.Table(TableRequestRelation).
		Where("`parent_request_id`=? AND is_deleted='N'", parentId).Find(&list).Error; err != nil {
		return nil, err
	}

	var ids []string
	for _, item := range list {
		ids = append(ids, item.ChildRequestId)
	}

	return ids, nil
}

func (db *TmcRequestRelationDB) DeleteRequestRelation(parentId string, childId string) error {
	resp := db.Table(TableRequestRelation).
		Where("`parent_request_id`=? AND `child_request_id`=?", parentId, childId).
		Update("is_deleted", "Y")

	return resp.Error
}
