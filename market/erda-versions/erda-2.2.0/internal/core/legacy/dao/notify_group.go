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
	"encoding/json"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

// CreateNotifyGroup 创建通知组
func (client *DBClient) CreateNotifyGroup(request *apistructs.CreateNotifyGroupRequest) (int64, error) {
	targetJson, err := json.Marshal(request.Targets)
	if err != nil {
		return 0, err
	}
	notifyGroup := model.NotifyGroup{
		Name:        request.Name,
		ScopeType:   request.ScopeType,
		ScopeID:     request.ScopeID,
		TargetData:  string(targetJson),
		OrgID:       request.OrgID,
		Creator:     request.Creator,
		Label:       request.Label,
		ClusterName: request.ClusterName,
		AutoCreate:  request.AutoCreate,
	}

	err = client.Create(&notifyGroup).Error
	if err != nil {
		return 0, nil
	}
	return notifyGroup.ID, nil
}

// UpdateNotifyGroup 更新通知组
func (client *DBClient) UpdateNotifyGroup(request *apistructs.UpdateNotifyGroupRequest) error {
	targetJson, err := json.Marshal(request.Targets)
	if err != nil {
		return err
	}
	var notifyGroup model.NotifyGroup
	err = client.Where("id = ? and org_id = ?", request.ID, request.OrgID).First(&notifyGroup).Error
	if err != nil {
		return err
	}

	notifyGroup.Name = request.Name
	notifyGroup.TargetData = string(targetJson)

	return client.Save(&notifyGroup).Error
}

func (client *DBClient) CheckNotifyGroupNameExist(scopeType, scopeID, name string) (bool, error) {
	var count int64
	err := client.Model(&model.NotifyGroup{}).Where("scope_type =? and scope_id =? and name = ?",
		scopeType, scopeID, name).Count(&count).Error
	return count > 0, err
}

// QueryNotifyGroup 查询通知组
func (client *DBClient) QueryNotifyGroup(request *apistructs.QueryNotifyGroupRequest, orgID int64) (*apistructs.QueryNotifyGroupData, error) {
	var notifyGroups []model.NotifyGroup
	query := client.Model(&model.NotifyGroup{}).Where("org_id =? and auto_create= ?", orgID, false)
	var count int
	if request.ScopeType != "" && request.ScopeID != "" {
		query = query.Where("scope_type = ? and scope_id =?", request.ScopeType, request.ScopeID)
	}
	if request.Label != "" {
		query = query.Where("label =? ", request.Label)
	}
	if request.ClusterName != "" {
		query = query.Where("cluster_name =? ", request.ClusterName)
	}
	if request.Name != "" {
		query = query.Where("name like ?", "%"+request.Name+"%")
	}
	err := query.Count(&count).Error
	if err != nil {
		return nil, err
	}
	err = query.Order("updated_at desc").
		Offset((request.PageNo - 1) * request.PageSize).
		Limit(request.PageSize).
		Find(&notifyGroups).Error
	if err != nil {
		return nil, err
	}

	result := &apistructs.QueryNotifyGroupData{
		Total: count,
		List:  []*apistructs.NotifyGroup{},
	}
	for _, notifyGroup := range notifyGroups {
		result.List = append(result.List, notifyGroup.ToApiData())
	}
	return result, nil
}

// GetNotifyGroupByID 获取通知组
func (client *DBClient) GetNotifyGroupByID(id int64, orgID int64) (*apistructs.NotifyGroup, error) {
	var notifyGroup model.NotifyGroup
	err := client.Where("id = ? and org_id =? ", id, orgID).Find(&notifyGroup).Error
	if err != nil {
		return nil, err
	}
	return notifyGroup.ToApiData(), nil
}

// GetNotifyGroupByIDWithoutOrgID 直接通过ID获取通知组
func (client *DBClient) GetNotifyGroupByIDWithoutOrgID(id int64) (*apistructs.NotifyGroup, error) {
	var notifyGroup model.NotifyGroup
	err := client.Where("id = ?", id).Find(&notifyGroup).Error
	if err != nil {
		return nil, err
	}
	return notifyGroup.ToApiData(), nil
}

// BatchGetNotifyGroup 批量获取通知组
func (client *DBClient) BatchGetNotifyGroup(ids []int64) ([]*apistructs.NotifyGroup, error) {
	var notifyGroups []model.NotifyGroup
	err := client.Where("id in (?) ", ids).Find(&notifyGroups).Error
	if err != nil {
		return nil, err
	}
	result := []*apistructs.NotifyGroup{}
	for _, notifyGroup := range notifyGroups {
		result = append(result, notifyGroup.ToApiData())
	}
	return result, nil
}

func (client *DBClient) GetNotifyGroupNameByID(id int64, orgID int64) (*apistructs.NotifyGroup, error) {
	var notifyGroup model.NotifyGroup
	err := client.Where("id = ? and org_id =? ", id, orgID).Select("id,name").Find(&notifyGroup).Error
	if err != nil {
		return nil, err
	}
	return notifyGroup.ToApiData(), nil
}

func (client *DBClient) DeleteNotifyGroup(id int64, orgID int64) error {
	var notifyGroup model.NotifyGroup
	err := client.Where("id = ? and org_id = ? ", id, orgID).Find(&notifyGroup).Error
	if err != nil {
		return err
	}
	return client.Delete(&notifyGroup).Error
}

// DeleteNotifyRelationsByScope 删除对应关联的通知信息
func (client *DBClient) DeleteNotifyRelationsByScope(scopeType apistructs.ScopeType, scopeID string) error {
	var notifies []model.Notify
	err := client.Where("scope_type = ? and scope_id = ? ", scopeType, scopeID).Find(&notifies).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return err
	}
	notifiyIDs := []int64{}
	for _, notify := range notifies {
		notifiyIDs = append(notifiyIDs, notify.ID)
	}
	err = client.Where("notify_id in (?)", notifiyIDs).Delete(&model.NotifyItemRelation{}).Error
	if err != nil {
		return err
	}
	err = client.Where("notify_id in (?)", notifiyIDs).Delete(&model.NotifySource{}).Error
	if err != nil {
		return err
	}

	err = client.Where("scope_type = ? and scope_id = ? ", scopeType, scopeID).Delete(&model.NotifyGroup{}).Error
	if err != nil {
		return err
	}
	err = client.Where("scope_type = ? and scope_id = ? ", scopeType, scopeID).Delete(&model.Notify{}).Error
	return err
}
