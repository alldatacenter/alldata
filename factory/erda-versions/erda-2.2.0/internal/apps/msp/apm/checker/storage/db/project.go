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

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/database/gormutil"
)

// ProjectDB .
type ProjectDB struct {
	*gorm.DB
}

func (db *ProjectDB) query() *gorm.DB {
	return db.Table(TableProject).Where("`is_deleted`=?", "N")
}

func (db *ProjectDB) GetByFields(fields map[string]interface{}) (*Project, error) {
	query, err := gormutil.GetQueryFilterByFields(db.query(), projectFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*Project
	if err := query.Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}

func (db *ProjectDB) GetByID(id int64) (*Project, error) {
	return db.GetByFields(map[string]interface{}{
		"ID": id,
	})
}

func (db *ProjectDB) GetByProjectID(projectID int64) (*Project, error) {
	return db.GetByFields(map[string]interface{}{
		"ProjectID": projectID,
	})
}
