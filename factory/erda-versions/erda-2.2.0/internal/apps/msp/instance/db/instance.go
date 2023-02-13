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

// InstanceDB .
type InstanceDB struct {
	*gorm.DB
}

func (db *InstanceDB) query() *gorm.DB {
	return db.Table(TableInstance).Where("`is_deleted`='N'")
}

func (db *InstanceDB) GetByFields(fields map[string]interface{}) (*Instance, error) {
	query := db.query()
	query, err := gormutil.GetQueryFilterByFields(query, instanceFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*Instance
	if err := query.Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}

func (db *InstanceDB) GetByID(id string) (*Instance, error) {
	return db.GetByFields(map[string]interface{}{
		"ID": id,
	})
}

func (db *InstanceDB) GetByEngineAndVersionAndAz(engine string, version string, az string) (*Instance, error) {
	var list []*Instance
	if err := db.query().
		Where("`engine`=?", engine).
		Where("`version`=?", version).
		Where("`az`=?", az).Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}

	if len(list) <= 0 {
		return nil, nil
	}

	return list[0], nil
}

func (db *InstanceDB) GetByEngineAndTenantGroup(engine string, tenantGroup string) (*Instance, error) {
	var list []*Instance
	if err := db.query().
		Where("`engine`=?", engine).
		Where("`tenant_group`=?", tenantGroup).
		Limit(1).
		Find(&list).Error; err != nil {
		return nil, err
	}

	if len(list) <= 0 {
		return nil, nil
	}

	return list[0], nil
}
