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

import "time"

const (
	TableTmcIni = "tb_tmc_ini"
)

type TmcIni struct {
	ID         int       `gorm:"column:id;primary_key"`
	IniName    string    `gorm:"column:ini_name"`
	IniDesc    string    `gorm:"column:ini_desc"`
	IniValue   string    `gorm:"column:ini_value"`
	CreateTime time.Time `gorm:"column:create_time"`
	UpdateTime time.Time `gorm:"column:update_time"`
	IsDeleted  string    `gorm:"column:is_deleted"`
}

func (TmcIni) TableName() string { return TableTmcIni }
