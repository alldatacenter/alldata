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
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateMemberExtra 创建成员关联关系
func (client *DBClient) CreateMemberExtra(memberExtra *model.MemberExtra) error {
	return client.Create(memberExtra).Error
}

// BatchCreateMemberExtra 批量创建成员关联关系
func (client *DBClient) BatchCreateMemberExtra(memberExtras []model.MemberExtra) error {
	return client.BulkInsert(memberExtras)
}

// GetMemberExtra 根据UserID, Scope和resourceKey获取成员关联关系
func (client *DBClient) GetMemberExtra(userIDs []string, scopeType apistructs.ScopeType, scopeID int64,
	resourceKey []apistructs.ExtraResourceKey) ([]model.MemberExtra, error) {
	var memberExtras []model.MemberExtra
	if err := client.Where("user_id in (?)", userIDs).Where("scope_id = ?", scopeID).Where("scope_type = ?", scopeType).
		Where("resource_key in (?)", resourceKey).Find(&memberExtras).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return memberExtras, nil
}

// GetMemberExtraByIDs 根据多个userid和scopeid查询memberextra
func (client *DBClient) GetMemberExtraByIDs(userIDs []string, scopeType apistructs.ScopeType, scopeIDs []int64,
	resourceKey []apistructs.ExtraResourceKey) ([]model.MemberExtra, error) {
	var memberExtras []model.MemberExtra
	if err := client.Where("user_id in (?)", userIDs).Where("scope_id in (?)", scopeIDs).Where("scope_type = ?", scopeType).
		Where("resource_key in (?)", resourceKey).Find(&memberExtras).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return memberExtras, nil
}

// GetMemberByScopeAndRole 根据Scope和角色获取成员
func (client *DBClient) GetMemberByScopeAndRole(scopeType apistructs.ScopeType, scopeIDs []uint64,
	roles []string) ([]model.MemberExtra, error) {
	var memberExtras []model.MemberExtra
	if err := client.Where("scope_id in (?)", scopeIDs).Where("scope_type = ?", scopeType).
		Where("resource_value in (?)", roles).Find(&memberExtras).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return memberExtras, nil
}

// GetProjectMembersByUser 根据userID查询项目权限，todo delete
func (client *DBClient) GetProjectMembersByUser(userID string) ([]model.MemberExtra, error) {
	var memberExtras []model.MemberExtra
	if err := client.Where("scope_type = ?", apistructs.ProjectScope).
		Where("user_id = ?", userID).
		Find(&memberExtras).Error; err != nil {
		return nil, err
	}
	return memberExtras, nil
}

// GetAppMembersByUser 根据userID查询应用权限
func (client *DBClient) GetAppMembersByUser(userID string) ([]model.MemberExtra, error) {
	var memberExtras []model.MemberExtra
	db := client.Where("scope_type = ?", apistructs.AppScope).
		Where("user_id = ?", userID).
		Find(&memberExtras)

	if err := db.Find(&memberExtras).Error; err != nil {
		return nil, err
	}

	return memberExtras, nil
}

// GetMeberExtraByParentID 通过父scope获取成员extra
func (client *DBClient) GetMeberExtraByParentID(userIDs []string, scopeType apistructs.ScopeType, parentID int64) ([]model.MemberExtra, error) {
	var memberExtras []model.MemberExtra
	if err := client.Where("scope_type = ?", scopeType).Where("parent_id = ?", parentID).
		Where("user_id in (?)", userIDs).Find(&memberExtras).Error; err != nil {
		return nil, err
	}
	return memberExtras, nil
}

// GetMeberRoleByParentID 通过父scope获取成员extra
func (client *DBClient) GetMeberRoleByParentID(userID string, scopeType apistructs.ScopeType, parentID int64) ([]model.MemberExtra, error) {
	var memberExtras []model.MemberExtra
	if err := client.Where("scope_type = ?", scopeType).Where("parent_id = ?", parentID).
		Where("user_id = ?", userID).Where("resource_key = ?", apistructs.RoleResourceKey).
		Find(&memberExtras).Error; err != nil {
		return nil, err
	}
	return memberExtras, nil
}

// PageMeberRoleByParentID 通过用户和父scope分页查询成员角色
func (client *DBClient) PageMeberRoleByParentID(req apistructs.ListMemberRolesByUserRequest) (int, []model.Member, error) {
	var (
		tmpMembers []model.Member
		members    []model.Member
		total      int
	)

	sql := client.Table("dice_member").Select("distinct scope_id").Where("user_id = ?", req.UserID).
		Where("scope_type = ?", req.ScopeType)

	if req.ParentID != 0 {
		sql = sql.Where("parent_id = ?", req.ParentID)
	}

	if err := sql.Offset((req.PageNo - 1) * req.PageSize).Limit(req.PageSize).Find(&tmpMembers).
		Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	var scopeIDs []int64
	for _, v := range tmpMembers {
		scopeIDs = append(scopeIDs, v.ScopeID)
	}
	scopeIDs = strutil.DedupInt64Slice(scopeIDs)

	members, err := client.GetMemberByUserIDAndScopeIDs(req.UserID, req.ScopeType, scopeIDs)
	if err != nil {
		return 0, nil, err
	}

	tmpRoles, err := client.GetMeberRoleByParentID(req.UserID, req.ScopeType, req.ParentID)
	if err != nil {
		return 0, nil, err
	}

	for i := range members {
		roles := make([]string, 0)
		for _, role := range tmpRoles {
			if members[i].UserID == role.UserID && members[i].ScopeType == role.ScopeType && members[i].ScopeID == role.ScopeID {
				roles = append(roles, role.ResourceValue)
			}
		}
		members[i].Roles = roles
	}

	return total, members, nil
}

// DeleteMemberExtraByScope 根据scope信息删除成员
func (client *DBClient) DeleteMemberExtraByScope(scopeType apistructs.ScopeType, scopeID int64) error {
	return client.Where("scope_type = ?", scopeType).Where("scope_id in (?)", scopeID).
		Delete(&model.MemberExtra{}).Error
}

// DeleteMemberExtraByUserIDsAndScopeIDs 根据userID和多个scopeID删除成员关联关系
func (client *DBClient) DeleteMemberExtraByUserIDsAndScopeIDs(scopeType apistructs.ScopeType, scopeIDs []int64,
	userIDs []string) error {
	return client.Where("user_id in (?)", userIDs).Where("scope_type = ?", scopeType).
		Where("scope_id in (?)", scopeIDs).Delete(&model.MemberExtra{}).Error
}

// DeleteMemberExtraByUserIDsAndScope 根据userID和scope删除成员关联关系
func (client *DBClient) DeleteMemberExtraByUserIDsAndScope(scopeType apistructs.ScopeType, scopeID int64,
	userIDs []string) error {
	return client.Where("user_id in (?)", userIDs).Where("scope_type = ?", scopeType).
		Where("scope_id = ?", scopeID).Delete(&model.MemberExtra{}).Error
}

// DeleteMemberExtraByParentID 根据parentID删除成员关联关系
func (client *DBClient) DeleteMemberExtraByParentID(userIDs []string, scopeType apistructs.ScopeType, parentID int64) error {
	return client.Where("user_id in (?)", userIDs).Where("scope_type = ?", scopeType).Where("parent_id = ?", parentID).
		Delete(&model.MemberExtra{}).Error
}

// DeleteMemberExtraByParentIDs 根据多个parentID删除成员关联关系
func (client *DBClient) DeleteMemberExtraByParentIDs(userID []string, scopeType apistructs.ScopeType, parentIDs []int64) error {
	return client.Where("user_id in (?)", userID).Where("scope_type = ?", scopeType).Where("parent_id in (?)", parentIDs).
		Delete(&model.MemberExtra{}).Error
}

// DeleteMemberExtraByIDsANDResourceValues 根据userIDs, scope和resourceValues删除成员标签
func (client *DBClient) DeleteMemberExtraByIDsANDResourceValues(userIDs []string, scopeType apistructs.ScopeType,
	scopeIDs []int64, resourceValues []string) error {
	return client.Where("user_id in (?)", userIDs).Where("scope_id in (?)", scopeIDs).Where("scope_type = ?", scopeType).
		Where("resource_value in (?)", resourceValues).Delete(&model.MemberExtra{}).Error
}

// DeleteMemberExtraByUxerIDANDResourceValues 根据userID, scope和resourceValues删除成员标签
func (client *DBClient) DeleteMemberExtraByUxerIDANDResourceValues(userID string, scopeType apistructs.ScopeType,
	scopeIDs []int64, resourceValues []string) error {
	return client.Where("user_id = ?", userID).Where("scope_id in (?)", scopeIDs).Where("scope_type = ?", scopeType).
		Where("resource_value in (?)", resourceValues).Delete(&model.MemberExtra{}).Error
}
