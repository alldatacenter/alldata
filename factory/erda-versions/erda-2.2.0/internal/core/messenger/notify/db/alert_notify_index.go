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
	"strconv"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/core/messenger/notify/model"
)

type AlertNotifyIndexDB struct {
	*gorm.DB
}

type AlertNotifyIndex struct {
	ID         int64     `json:"id" gorm:"column:id"`
	NotifyID   int64     `json:"notifyID" gorm:"column:notify_id"`
	NotifyName string    `json:"notifyName" gorm:"column:notify_name"`
	Status     string    `json:"status" gorm:"column:status"`
	Channel    string    `json:"channel" gorm:"column:channel"`
	Attributes string    `json:"attributes" gorm:"column:attributes"`
	ScopeType  string    `json:"scopeType" gorm:"column:scope_type"`
	ScopeID    string    `json:"scopeID" gorm:"column:scope_id"`
	OrgID      int64     `json:"org_id" gorm:"column:org_id"`
	CreatedAt  time.Time `json:"created_at" gorm:"column:created_at"`
	SendTime   time.Time `json:"sendTime" gorm:"column:send_time"`
	AlertId    int64     `json:"alertId,gorm:"column:alert_id"`
}

func (AlertNotifyIndex) TableName() string {
	return "alert_notify_index"
}

func (db *AlertNotifyIndexDB) CreateAlertNotifyIndex(alertNotifyIndex *AlertNotifyIndex) (int64, error) {
	err := db.Create(&alertNotifyIndex).Error
	if err != nil {
		return 0, err
	}
	return alertNotifyIndex.ID, nil
}

func (db *AlertNotifyIndexDB) QueryAlertNotifyHistories(queryRequest *model.QueryAlertNotifyIndexRequest) ([]AlertNotifyIndex, int64, error) {
	var alertNotifyIndex []AlertNotifyIndex
	query := db.Model(&AlertNotifyIndex{}).Where("org_id = ?", queryRequest.OrgID).
		Where("scope_type = ?", queryRequest.ScopeType).
		Where("scope_id = ?", queryRequest.ScopeID)
	if queryRequest.NotifyName != "" {
		query = query.Where("notify_name like ?", "%"+queryRequest.NotifyName+"%")
	}
	if queryRequest.Status != "" {
		query = query.Where("status = ?", queryRequest.Status)
	}
	if queryRequest.Channel != "" {
		query = query.Where("channel = ?", queryRequest.Channel)
	}
	if queryRequest.AlertID != 0 {
		query = query.Where("alert_id = ?", queryRequest.AlertID)
	}
	if len(queryRequest.SendTime) > 0 {
		timeFormat := "2006-01-02 15:04:05"
		msInt, _ := strconv.ParseInt(queryRequest.SendTime[0], 10, 64)
		tm := time.Unix(0, msInt*int64(time.Millisecond))
		startSendTime := tm.Format(timeFormat)
		msInt, _ = strconv.ParseInt(queryRequest.SendTime[1], 10, 64)
		tm = time.Unix(0, msInt*int64(time.Millisecond))
		endSendTime := tm.Format(timeFormat)
		query = query.Where("send_time >= ?", startSendTime).Where("send_time <= ?", endSendTime)
	}
	var count int64
	err := query.Count(&count).Error
	if err != nil {
		return nil, 0, err
	}
	if queryRequest.TimeOrder {
		query = query.Order("send_time asc")
	} else {
		query = query.Order("send_time desc")
	}
	err = query.Offset((queryRequest.PageNo - 1) * queryRequest.PageSize).
		Limit(queryRequest.PageSize).Find(&alertNotifyIndex).Error
	if err != nil {
		return nil, 0, err
	}
	return alertNotifyIndex, count, nil
}

func (db *AlertNotifyIndexDB) GetAlertNotifyIndex(id int64) (*AlertNotifyIndex, error) {
	var alertIndex AlertNotifyIndex
	err := db.Where("id = ?", id).Find(&alertIndex).Error
	if err != nil {
		return nil, err
	}
	return &alertIndex, nil
}
