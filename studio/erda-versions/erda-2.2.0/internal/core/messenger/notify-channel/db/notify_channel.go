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
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/common/errors"
)

const TableNotifyChannel = "erda_notify_channel"

type NotifyChannel struct {
	Id              string    `gorm:"column:id" db:"id" json:"id" form:"id"`                                                         //id
	Name            string    `gorm:"column:name" db:"name" json:"name" form:"name"`                                                 //渠道名称
	Type            string    `gorm:"column:type" db:"type" json:"type" form:"type"`                                                 //渠道类型
	Config          string    `gorm:"column:config" db:"config" json:"config" form:"config"`                                         //渠道配置
	ScopeType       string    `gorm:"column:scope_type" db:"scope_type" json:"scope_type" form:"scope_type"`                         //域类型
	ScopeId         string    `gorm:"column:scope_id" db:"scope_id" json:"scope_id" form:"scope_id"`                                 //域id
	CreatorId       string    `gorm:"column:creator_id" db:"creator_id" json:"creator_id" form:"creator_id"`                         //创建人Id
	ChannelProvider string    `gorm:"column:channel_provider" db:"channel_provider" json:"channel_provider" form:"channel_provider"` //渠道提供商类型
	IsEnabled       bool      `gorm:"column:is_enabled" db:"is_enabled" json:"is_enabled" form:"is_enabled"`                         //是否启用
	KmsKey          string    `gorm:"column:kms_key" db:"kms_key" json:"kms_key" form:"kms_key"`                                     //kms key
	CreatedAt       time.Time `gorm:"column:created_at" db:"created_at" json:"created_at" form:"created_at"`                         //创建时间
	UpdatedAt       time.Time `gorm:"column:updated_at" db:"updated_at" json:"updated_at" form:"updated_at"`                         //更新时间
	IsDeleted       bool      `gorm:"column:is_deleted" db:"is_deleted" json:"is_deleted" form:"is_deleted"`                         //是否删除
}

func (NotifyChannel) TableName() string {
	return TableNotifyChannel
}

// NotifyChannelDB erda_notify_channel
type NotifyChannelDB struct {
	*gorm.DB
}

func (db *NotifyChannelDB) db() *gorm.DB {
	return db.Table(TableNotifyChannel)
}

func (db *NotifyChannelDB) Create(notifyChannel *NotifyChannel) (*NotifyChannel, error) {
	err := db.db().Create(notifyChannel).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return notifyChannel, nil
}

func (db *NotifyChannelDB) GetCountByScopeAndType(scopeId, scopeType, channelType string) (int64, error) {
	var count int64
	err := db.db().
		Where("`scope_id` = ?", scopeId).
		Where("`scope_type` = ?", scopeType).
		Where("`type` = ?", channelType).
		Where("`is_enabled` = ?", true).
		Where("`is_deleted` = ?", false).
		Count(&count).Error
	if err == gorm.ErrRecordNotFound {
		return 0, nil
	}
	if err != nil {
		return 0, errors.NewDatabaseError(err)
	}
	return count, err
}

func (db *NotifyChannelDB) GetByScopeAndType(scopeId, scopeType, channelType string) (*NotifyChannel, error) {
	channel := &NotifyChannel{}
	err := db.db().
		Where("`scope_id` = ?", scopeId).
		Where("`scope_type` = ?", scopeType).
		Where("`type` = ?", channelType).
		Where("`is_enabled` = ?", true).
		Where("`is_deleted` = ?", false).
		Find(channel).
		Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return channel, nil
}

func (db *NotifyChannelDB) GetById(id string) (*NotifyChannel, error) {
	channel := &NotifyChannel{}
	err := db.db().Where("`id` = ?", id).Where("`is_deleted` = ?", false).Find(channel).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return channel, nil
}

func (db *NotifyChannelDB) GetByName(name string) (*NotifyChannel, error) {
	channel := &NotifyChannel{}
	err := db.db().Where("`name` = ?", name).Where("`is_deleted` = ?", false).Find(channel).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return channel, nil
}

func (db *NotifyChannelDB) GetCountByName(name string) (int64, error) {
	var count int64
	err := db.db().Where("`name` = ?", name).Where("`is_deleted` = ?", false).Count(&count).Error
	if err == gorm.ErrRecordNotFound {
		return 0, nil
	}
	if err != nil {
		return 0, errors.NewDatabaseError(err)
	}
	return count, nil
}

func (db *NotifyChannelDB) DeleteById(id string) (*NotifyChannel, error) {
	channel, err := db.GetById(id)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if channel == nil {
		return nil, errors.NewDatabaseError(errors.NewNotFoundError(id))
	}
	channel.IsDeleted = true
	err = db.db().Model(channel).Update(channel).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return channel, nil
}

func (db *NotifyChannelDB) UpdateById(notifyChannel *NotifyChannel) (*NotifyChannel, error) {

	err := db.db().Model(notifyChannel).Save(notifyChannel).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return notifyChannel, nil
}

func (db *NotifyChannelDB) SwitchEnable(currentNotifyChannel, switchNotifyChannel *NotifyChannel) error {
	tx := db.db().Begin()
	currentNotifyChannel.IsEnabled = true
	currentNotifyChannel.UpdatedAt = time.Now()
	switchNotifyChannel.IsEnabled = false
	switchNotifyChannel.UpdatedAt = time.Now()
	err := tx.Model(currentNotifyChannel).Save(currentNotifyChannel).Error
	if err != nil {
		tx.Rollback()
		return errors.NewDatabaseError(err)
	}
	err = tx.Model(switchNotifyChannel).Save(switchNotifyChannel).Error
	if err != nil {
		tx.Rollback()
		return errors.NewDatabaseError(err)
	}
	return tx.Commit().Error
}

func (db *NotifyChannelDB) ListByPage(offset, pageSize int64, scopeId, scopeType, channelType string) (int64, []NotifyChannel, error) {
	var channels []NotifyChannel
	whereDB := db.db().
		Where("`is_deleted` = ?", false).
		Where("`scope_id` = ?", scopeId).
		Where("`scope_type` = ?", scopeType).
		Where("`type` = ?", channelType)

	err := whereDB.
		Order("`created_at` DESC", true).
		Offset(offset).
		Limit(pageSize).
		Find(&channels).
		Error
	if err != nil {
		return 0, nil, errors.NewDatabaseError(err)
	}
	var totalCount int64 = 0
	err = whereDB.Count(&totalCount).Error
	if err != nil {
		return 0, nil, errors.NewDatabaseError(err)
	}

	return totalCount, channels, nil
}

func (db *NotifyChannelDB) EnabledChannelList(scopeId, scopeType string) ([]NotifyChannel, error) {
	var channels []NotifyChannel
	err := db.db().Where("`scope_id` = ?", scopeId).
		Where("`scope_type` = ?", scopeType).
		Where("`is_enabled` = ?", true).
		Where("`is_deleted` = ?", false).
		Find(&channels).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return channels, nil
}
