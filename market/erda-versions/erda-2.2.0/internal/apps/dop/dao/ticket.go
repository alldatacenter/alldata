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

package dao

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/model"
)

// CreateTicket 创建工单
func (client *DBClient) CreateTicket(ticket *model.Ticket) error {
	return client.Create(ticket).Error
}

// UpdateTicket 更新工单
func (client *DBClient) UpdateTicket(ticket *model.Ticket) error {
	return client.Save(ticket).Error
}

// GetTicket 获取工单详情
func (client *DBClient) GetTicket(ticketID int64) (*model.Ticket, error) {
	var ticket model.Ticket
	if err := client.Where("id = ?", ticketID).Find(&ticket).Error; err != nil {
		return nil, err
	}
	return &ticket, nil
}

// GetOpenTicketByKey 获取open状态的工单
func (client *DBClient) GetOpenTicketByKey(key string) (*model.Ticket, error) {
	var ticket model.Ticket
	if err := client.Where("`key` = ?", key).Where("status = ?", apistructs.TicketOpen).
		Find(&ticket).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &ticket, nil
}

// GetTicketByRequestID 根据requestID header获取记录
func (client *DBClient) GetTicketByRequestID(requestID string) (*model.Ticket, error) {
	var ticket model.Ticket
	if err := client.Where("request_id = ?", requestID).Find(&ticket).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &ticket, nil
}

// GetClusterOpenTicketsNum 获取指定集群open状态的工单总数
func (client *DBClient) GetClusterOpenTicketsNum(ticketType, targetType, targetID string) (uint64, error) {
	var ticketsCount uint64
	if err := client.Model(&model.Ticket{}).
		Where("type = ?", ticketType).
		Where("target_id = ?", targetID).
		Where("target_type = ?", targetType).
		Where("status = ?", apistructs.TicketOpen).
		Count(&ticketsCount).
		Error; err != nil {
		return 0, err
	}

	return ticketsCount, nil
}

// DeleteTicket 删除工单
func (client *DBClient) DeleteTicket(targetID, targetType, ticketType string) error {
	var m model.Ticket
	//err := client.Model(&model.Ticket{}).
	client.LogMode(true)
	err := client.Debug().Table("ps_tickets").
		Where("type = ?", ticketType).
		Where("target_id = ?", targetID).
		Where("target_type = ?", targetType).
		Delete(&m).Error
	return err
}
