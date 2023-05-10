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

const (
	MenuIniName             = "MS_MENU"
	EngineMenuKeyPrefix     = "MK_"
	EngineMenuJumpKeyPrefix = "MK_JUMP_"
)

type MenuConfigDB struct {
	*gorm.DB
}

// GetMicroServiceMenu .
func (db *MenuConfigDB) GetMicroServiceMenu() (*TmcIni, error) {
	var list []*TmcIni
	if err := db.Table(TableTmcIni).
		Where("ini_name=?", MenuIniName).Where("is_deleted = ?", "N").Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}

// GetMicroServiceEngineKey .
func (db *MenuConfigDB) GetMicroServiceEngineKey(engine string) (string, error) {
	var list []*TmcIni
	if err := db.Table(TableTmcIni).
		Where("ini_name=?", EngineMenuKeyPrefix+engine).Where("is_deleted = ?", "N").Limit(1).Find(&list).Error; err != nil {
		return "", err
	}
	if len(list) <= 0 || list[0] == nil {
		return "", nil
	}
	return list[0].IniValue, nil
}

// GetMicroServiceEngineJumpKey .
func (db *MenuConfigDB) GetMicroServiceEngineJumpKey(engine string) (string, error) {
	var list []*TmcIni
	if err := db.Table(TableTmcIni).
		Where("ini_name=?", EngineMenuJumpKeyPrefix+engine).Limit(1).Find(&list).Error; err != nil {
		return "", err
	}
	if len(list) <= 0 || list[0] == nil {
		return "", nil
	}
	return list[0].IniValue, nil
}
