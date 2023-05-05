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
)

type NotifyConfig struct {
	gorm.Model
	NotifyID  string `gorm:"column:notify_id"`
	Metadata  string `gorm:"column:metadata"`
	Behavior  string `gorm:"column:behavior"`
	Templates string `gorm:"column:templates"`
	Scope     string `gorm:"column:scope"`
	ScopeID   string `gorm:"column:scope_id"`
}

func (c *NotifyConfig) TableName() string {
	return "sp_notify_user_define"
}

func (n *NotifyDB) GetAllUserDefineNotify(scope, scopeID string) (*[]NotifyConfig, error) {
	allNotifies := make([]NotifyConfig, 0)
	err := n.DB.Model(&allNotifies).Where("scope = ?", scope).
		Where("scope_id = ?", scopeID).Find(&allNotifies).Error
	return &allNotifies, err
}

func (n *NotifyDB) CreateUserDefineNotifyTemplate(customize *NotifyConfig) error {
	err := n.DB.Create(customize).Error
	return err
}

func (n *NotifyDB) GetUserDefine(notifyId string) (*NotifyConfig, error) {
	var userDefine NotifyConfig
	err := n.DB.Model(&NotifyConfig{}).Where("notify_id = ?", notifyId).Find(&userDefine).Error
	return &userDefine, err
}

func (n *NotifyDB) GetAllUserDefineTemplates() (*[]NotifyConfig, error) {
	allTemplates := make([]NotifyConfig, 0)
	err := n.DB.Model(&NotifyConfig{}).Find(&allTemplates).Error
	return &allTemplates, err
}
