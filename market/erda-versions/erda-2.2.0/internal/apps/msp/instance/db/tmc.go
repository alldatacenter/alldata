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

// TmcDB .
type TmcDB struct {
	*gorm.DB
}

func (db *TmcDB) GetByEngine(engine string) (*Tmc, error) {
	if len(engine) <= 0 {
		return nil, nil
	}
	var list []*Tmc
	if err := db.Table(TableTmc).
		Where("`engine`=?", engine).Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}
