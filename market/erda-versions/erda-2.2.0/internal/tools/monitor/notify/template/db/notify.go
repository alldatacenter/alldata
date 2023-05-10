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

	"github.com/erda-project/erda/internal/tools/monitor/notify/template/model"
)

type NotifyDB struct {
	DB *gorm.DB
}

func New(db *gorm.DB) *NotifyDB {
	return &NotifyDB{
		DB: db,
	}
}

type Notify struct {
	gorm.Model
	NotifyID   string `gorm:"column:notify_id"`
	NotifyName string `gorm:"column:notify_name"`
	Target     string `gorm:"column:target"`
	Scope      string `gorm:"column:scope"`
	ScopeID    string `gorm:"column:scope_id"`
	Attributes string `json:"attributes"`
	Enable     bool   `gorm:"enable"`
}

func (n *Notify) TableName() string {
	return "sp_notify"
}

func (n *NotifyDB) CheckNotifyNameExist(scopeID, scopeName, notifyName string) (bool, error) {
	var count int64
	err := n.DB.Model(&Notify{}).Where("scope = ? and scope_id = ? and "+
		"notify_name = ?", scopeName, scopeID, notifyName).Count(&count).Error
	if count > 0 {
		return true, err
	}
	return false, err
}

func (n *NotifyDB) CreateNotifyRecord(record *Notify) error {
	err := n.DB.Create(record).Error
	return err
}

func (n *NotifyDB) GetNotify(id int64) (*Notify, error) {
	var notify Notify
	err := n.DB.Model(&Notify{}).Where("id = ?", id).First(&notify).Error
	return &notify, err
}

func (n *NotifyDB) DeleteNotifyRecord(id int64) error {
	err := n.DB.Where("id = ?", id).Delete(&Notify{}).Error
	return err
}

func (n *NotifyDB) UpdateNotify(updateNotify *Notify) error {
	err := n.DB.Model(&Notify{}).Where("id = ?", updateNotify.Model.ID).Update("target", updateNotify.Target).
		Update("notify_id", updateNotify.NotifyID).Update("attribute", updateNotify.Attributes).Error
	return err
}

func (n *NotifyDB) GetNotifyList(notifyListReq *model.QueryNotifyListReq) ([]*Notify, error) {
	notifies := make([]*Notify, 0)
	query := n.DB.Model(&Notify{})
	if notifyListReq.Scope != "" {
		query = query.Where("scope = ?", notifyListReq.Scope)
	}
	if notifyListReq.ScopeID != "" {
		query = query.Where("scope_id = ?", notifyListReq.ScopeID)
	}
	err := query.Order("created_at desc").Find(&notifies).Error
	return notifies, err
}

func (n *NotifyDB) UpdateEnable(id int64) error {
	var notify Notify
	err := n.DB.Model(&Notify{}).Where("id = ?", id).First(&notify).Error
	if err != nil {
		return err
	}
	if notify.Enable == true {
		notify.Enable = false
	} else {
		notify.Enable = true
	}
	err = n.DB.Model(&Notify{}).Where("id = ?", id).Update("enable", notify.Enable).Error
	return err
}

func (n *NotifyDB) CheckNotifyTemplateExist(scope, scopeID string) (*[]NotifyConfig, error) {
	customizeTemplate := make([]NotifyConfig, 0)
	err := n.DB.Model(&NotifyConfig{}).Where("scope = ?", scope).Where("scope_id = ?", scopeID).
		Find(&customizeTemplate).Error
	return &customizeTemplate, err
}
