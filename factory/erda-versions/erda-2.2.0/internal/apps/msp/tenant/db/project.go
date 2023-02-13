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

	"github.com/erda-project/erda/pkg/common/errors"
)

// MSPProjectDB msp_project
type MSPProjectDB struct {
	*gorm.DB
}

func (db *MSPProjectDB) db() *gorm.DB {
	return db.Table(TableMSPProject)
}

func (db *MSPProjectDB) Create(project *MSPProject) (*MSPProject, error) {
	result := db.db().Create(project)

	if result.Error != nil {
		return nil, errors.NewDatabaseError(result.Error)
	}
	value := result.Value.(*MSPProject)
	return value, nil
}

func (db *MSPProjectDB) Update(project *MSPProject) (*MSPProject, error) {
	err := db.db().Model(project).Update(project).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return project, nil
}

func (db *MSPProjectDB) Query(id string) (*MSPProject, error) {
	project := MSPProject{}
	err := db.db().Where("`id` = ?", id).Where("`is_deleted` = ?", false).Find(&project).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return &project, err
}

func (db *MSPProjectDB) Delete(id string) (*MSPProject, error) {
	project, err := db.Query(id)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	project.IsDeleted = true
	err = db.Model(&project).Update(&project).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return project, err
}
