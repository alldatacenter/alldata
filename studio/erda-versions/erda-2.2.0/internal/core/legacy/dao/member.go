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
	"github.com/erda-project/erda/internal/core/legacy/types"
	"github.com/erda-project/erda/pkg/strutil"
)

var joinSQL = "LEFT OUTER JOIN dice_member_extra on dice_member.scope_type=dice_member_extra.scope_type and dice_member.user_id=dice_member_extra.user_id and dice_member.scope_id=dice_member_extra.scope_id"

// CreateMember 创建成员
func (client *DBClient) CreateMember(member *model.Member) error {
	return client.Create(member).Error
}

// UpdateMember 更新成员
func (client *DBClient) UpdateMember(member *model.Member) error {
	return client.Save(member).Error
}

// UpdateMemberUserInfo 更新成员 userinfo 信息
func (client *DBClient) UpdateMemberUserInfo(ids []int64, fields map[string]interface{}) error {
	return client.Debug().Model(model.Member{}).Where("id in (?)", ids).Updates(fields).Error
}

// DeleteMember 删除成员
func (client *DBClient) DeleteMember(memberID int64) error {
	return client.Where("id = ?", memberID).Delete(&model.Member{}).Error
}

// DeleteMembersByScope 根据scope删除成员
func (client *DBClient) DeleteMembersByScope(scopeType apistructs.ScopeType, scopeID int64) error {
	return client.Where("scope_type = ?", scopeType).Where("scope_id = ?", scopeID).
		Delete(&model.Member{}).Error
}

// DeleteMembersByOrgAndUsers 根据 userIDs 删除成员信息 (用户踢出企业时使用)
func (client *DBClient) DeleteMembersByOrgAndUsers(orgID string, userIDs []string) error {
	return client.Where("org_id = ?", orgID).Where("user_id in (?)", userIDs).Delete(&model.Member{}).Error
}

// DeleteMembersByScopeAndUsers 根据scope & userID删除成员
func (client *DBClient) DeleteMembersByScopeAndUsers(userIDs []string, scopeType apistructs.ScopeType, scopeID int64) error {
	return client.Where("user_id in (?)", userIDs).Where("scope_type = ?", scopeType).
		Where("scope_id = ?", scopeID).Delete(&model.Member{}).Error
}

// DeleteMemberByScopeAndUsersAndRole 根据scope, userID, role删除成员，多角色状态下，更新用户角色时用到, todo delete
func (client *DBClient) DeleteMemberByScopeAndUsersAndRole(userIDs []string, scopeType apistructs.ScopeType, scopeID int64, role string) error {
	return client.Where("user_id in (?)", userIDs).Where("scope_type = ?", scopeType).
		Where("scope_id = ?", scopeID).Where("role = ?", role).Delete(&model.Member{}).Error
}

// DeleteProjectAppsMembers 删除该用户对应项目下的应用权限
func (client *DBClient) DeleteProjectAppsMembers(userIDs []string, projectID int64) error {
	return client.Where("user_id in (?)", userIDs).Where("scope_type = ?", apistructs.AppScope).
		Where("parent_id = ?", projectID).Delete(&model.Member{}).Error
}

// GetMemberByID 根据memberID获取成员
func (client *DBClient) GetMemberByID(memberID int64) (*model.Member, error) {
	var member *model.Member
	if err := client.Where("id = ?", memberID).Find(member).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, ErrNotFoundMember
		}
		return nil, err
	}
	return member, nil
}

// GetMemberByIDs 根据memberID列表获取成员列表
func (client *DBClient) GetMemberByIDs(memberIDs []int64) ([]model.Member, error) {
	var members []model.Member
	if err := client.Model(&model.Member{}).Where("id in (?)", memberIDs).Find(&members).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, ErrNotFoundMember
		}
		return nil, err
	}
	return members, nil
}

// GetMemberByUserID 通过 user_id 获取成员
func (client *DBClient) GetMemberByUserID(userID string) ([]model.Member, error) {
	var members []model.Member
	if err := client.Model(&model.Member{}).Where("user_id = ?", userID).Find(&members).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, ErrNotFoundMember
		}
		return nil, err
	}
	return members, nil
}

// GetMemberByUserIDsAndScope 通过 user_id 和 scope 信息获取成员
func (client *DBClient) GetMemberByUserIDsAndScope(userIDs []string, scopeType apistructs.ScopeType, scopeID int64) ([]model.Member, error) {
	var members []model.Member
	if err := client.Model(&model.Member{}).Where("user_id in (?)", userIDs).Where("scope_type = ?", scopeType).
		Where("scope_id = ?", scopeID).Find(&members).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, ErrNotFoundMember
		}
		return nil, err
	}
	return members, nil
}

// GetMemberByUserIDAndScopeIDs 通过 user_id 和 scopeIDs 信息获取成员
func (client *DBClient) GetMemberByUserIDAndScopeIDs(userID string, scopeType apistructs.ScopeType, scopeIDs []int64) ([]model.Member, error) {
	var members []model.Member
	if err := client.Model(&model.Member{}).Where("user_id = ?", userID).Where("scope_type = ?", scopeType).
		Where("scope_id in (?)", scopeIDs).Find(&members).Error; err != nil {
		return nil, err
	}
	return members, nil
}

// GetMembersByParam 根据查询参数获取企业成员列表
func (client *DBClient) GetMembersByParam(param *apistructs.MemberListRequest) (
	int, []model.Member, error) {
	var (
		tmpMembers []model.MemberJoin
		total      int
	)
	db := client.Table("dice_member").Select("distinct dice_member.user_id").Joins(joinSQL) //条件查询
	if param.ScopeType != "" {
		db = db.Where("dice_member.scope_type = ?", param.ScopeType)
	}
	if param.ScopeID != 0 {
		db = db.Where("dice_member.scope_id = ?", param.ScopeID)
	}
	if len(param.Roles) > 0 {
		db = db.Where("dice_member_extra.resource_key = ?", apistructs.RoleResourceKey).
			Where("dice_member_extra.resource_value in (?)", param.Roles)
	}
	if len(param.Labels) > 0 {
		db = db.Where("dice_member_extra.resource_key = ?", apistructs.LabelResourceKey).
			Where("dice_member_extra.resource_value in (?)", param.Labels)
	}
	if param.Q != "" {
		db = db.Where("(email LIKE ? OR mobile LIKE ? OR nick LIKE ? OR name LIKE ?)",
			"%"+param.Q+"%", "%"+param.Q+"%", "%"+param.Q+"%", "%"+param.Q+"%")
	}

	if err := db.Offset((param.PageNo - 1) * param.PageSize).
		Limit(param.PageSize).Scan(&tmpMembers).Offset(0).Limit(-1).
		Select("count(distinct(dice_member.user_id))").
		Count(&total).Error; err != nil {
		return 0, nil, err
	}

	var userIDs []string
	for _, v := range tmpMembers {
		userIDs = append(userIDs, v.UserID)
	}
	userIDs = strutil.DedupSlice(userIDs)

	tmpMembers2, err := client.GetMemberByUserIDsAndScope(userIDs, param.ScopeType, param.ScopeID)
	if err != nil {
		return 0, nil, err
	}

	tmpRolesAndLabels, err := client.GetMemberExtra(userIDs, param.ScopeType, param.ScopeID,
		[]apistructs.ExtraResourceKey{apistructs.RoleResourceKey, apistructs.LabelResourceKey})
	if err != nil {
		return 0, nil, err
	}

	members := []model.Member{}
	if len(tmpMembers2) > 0 {
		for _, v := range tmpMembers2 {
			var roles, labels []string
			// 通过用户id聚合角色和标签
			for _, rl := range tmpRolesAndLabels {
				if rl.UserID == v.UserID {
					switch rl.ResourceKey {
					case apistructs.RoleResourceKey:
						roles = append(roles, rl.ResourceValue)
					case apistructs.LabelResourceKey:
						labels = append(labels, rl.ResourceValue)
					default:
						continue
					}
				}
			}

			members = append(members, model.Member{
				BaseModel:     v.BaseModel,
				ScopeID:       v.ScopeID,
				ScopeName:     v.ScopeName,
				ScopeType:     v.ScopeType,
				ParentID:      v.ParentID,
				Roles:         roles,
				UserID:        v.UserID,
				Email:         v.Email,
				Mobile:        v.Mobile,
				Name:          v.Name,
				Avatar:        v.Avatar,
				Token:         v.Token,
				UserSyncAt:    v.UserSyncAt,
				OrgID:         v.OrgID,
				ProjectID:     v.ProjectID,
				ApplicationID: v.ApplicationID,
				Nick:          v.Nick,
				Labels:        labels,
			})
		}
	}

	return total, members, nil
}

// GetMemberByScopeAndUserID 根据scope & userID过滤
func (client *DBClient) GetMemberByScopeAndUserID(userID string, scopeType apistructs.ScopeType,
	scopeID int64) ([]model.MemberJoin, error) {
	var members []model.MemberJoin
	db := client.Table("dice_member").Select("*").Joins(joinSQL)
	if err := db.Where("dice_member.user_id = ?", userID).Where("dice_member.scope_type = ?", scopeType).Where("dice_member.scope_id = ?", scopeID).
		Scan(&members).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return members, nil
}

// GetScopeManagersByScopeID 根据 scopeID 获取 scope 对应的管理员列表
func (client *DBClient) GetScopeManagersByScopeID(scopeType apistructs.ScopeType, scopeID int64) ([]apistructs.Member, error) {
	var member []apistructs.Member
	db := client.Table("dice_member").Select("*").Joins(joinSQL)
	if err := db.Where("dice_member.scope_type = ?", scopeType).Where("dice_member.scope_id = ?", scopeID).
		Where("resource_key = ?", apistructs.RoleResourceKey).
		Where("resource_value IN (?)", types.GetScopeManagerRoleNames(scopeType)).
		Find(&member).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return member, nil
}

// GetScopeNameByScope 根据scope获取名称
func (client *DBClient) GetScopeNameByScope(scopeType apistructs.ScopeType, scopeID int64) string {
	switch scopeType {
	case apistructs.OrgScope:
		org, err := client.GetOrg(scopeID)
		if err != nil {
			return ""
		}
		return org.Name
	case apistructs.ProjectScope:
		project, err := client.GetProjectByID(scopeID)
		if err != nil {
			return ""
		}
		return project.Name
	case apistructs.AppScope:
		app, err := client.GetApplicationByID(scopeID)
		if err != nil {
			return ""
		}
		return app.Name
	default:
		return ""
	}
}

// GetMembersByOrgAndUser 根据userID查询成员
func (client *DBClient) GetMembersByOrgAndUser(orgID int64, userID string) ([]model.MemberJoin, error) {
	var members []model.MemberJoin
	db := client.Table("dice_member").Select("*").Joins(joinSQL)
	if err := db.Where("dice_member.org_id = ?", orgID).Where("dice_member.user_id = ?", userID).
		Order("dice_member.updated_at DESC").Find(&members).Error; err != nil {
		return nil, err
	}
	return members, nil
}

// GetMembersByScopeTypeAndUser 根据scopeType & user获取成员
func (client *DBClient) GetMembersByScopeTypeAndUser(scopeType apistructs.ScopeType, userID string) ([]model.Member, error) {
	var members []model.Member
	if err := client.Where("user_id = ?", userID).Where("scope_type = ?", scopeType).
		Find(&members).Error; err != nil {
		return nil, err
	}
	return members, nil
}

// GetMemberByToken 根据token获取成员
func (client *DBClient) GetMemberByToken(token string) (*model.Member, error) {
	var members model.Member
	if err := client.Where("token = ?", token).
		First(&members).Error; err != nil {
		return nil, err
	}
	return &members, nil
}

// GetMembersByParentID 根据parentID & userID等信息查询权限
func (client *DBClient) GetMembersByParentID(scopeType apistructs.ScopeType, parentID int64, userID string) (
	[]model.MemberJoin, error) {
	var members []model.MemberJoin
	db := client.Table("dice_member").Select("*").Joins(joinSQL)
	if err := db.Where("dice_member.scope_type = ?", scopeType).Where("dice_member.user_id = ?", userID).
		Where("dice_member.parent_id = ?", parentID).Scan(&members).Error; err != nil {
		return nil, err
	}
	return members, nil
}

// GetMembersWithoutExtraByScope 根据parentID 信息查询成员,不带角色label等信息
func (client *DBClient) GetMembersWithoutExtraByScope(scopeType apistructs.ScopeType, scopeID int64) (
	int, []model.Member, error) {
	var (
		members []model.Member
		total   int
	)
	if err := client.Table("dice_member").Where("scope_type = ?", scopeType).Where("scope_id = ?", scopeID).
		Scan(&members).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, members, nil
}

type MemberCount struct {
	ScopeID int64
	Count   int
}

func (client *DBClient) GetMemberCountByScopeIDs(scopeType apistructs.ScopeType, scopeIDs []int64) ([]MemberCount, error) {
	var counts []MemberCount
	if err := client.Table(model.Member{}.TableName()).Select("scope_id, COUNT(scope_id) as count").Where("scope_type = ? AND scope_id IN (?)", scopeType, scopeIDs).
		Group("scope_id").Find(&counts).Error; err != nil {
		return nil, err
	}
	return counts, nil
}

// GetMembersByScope 查询指定scope和role的用户，todo delete
func (client *DBClient) GetMembersByScope(scopeType apistructs.ScopeType, scopeIDs []string, roles []string) ([]model.Member, error) {
	var members []model.Member
	if scopeType == apistructs.SysScope {
		if err := client.Where("scope_type = ?", scopeType).
			Where("roles in (?)", roles).
			Find(&members).Error; err != nil {
			return nil, err
		}
	} else {
		if err := client.Where("scope_type = ?", scopeType).
			Where("scope_id in (?)", scopeIDs).
			Where("roles in (?)", roles).
			Find(&members).Error; err != nil {
			return nil, err
		}
	}
	return members, nil
}

// IsSysAdmin 判断用户是否为系统管理员
func (client *DBClient) IsSysAdmin(userID string) (bool, error) {
	var count int
	db := client.Table("dice_member").Select("*").Joins(joinSQL)

	if err := db.Where("dice_member.scope_type = ?", apistructs.SysScope).
		Where("resource_value = ?", types.RoleSysManager).
		Where("resource_key = ?", apistructs.RoleResourceKey).
		Where("dice_member.user_id = ?", userID).Count(&count).Error; err != nil {
		return false, err
	}

	return count > 0, nil
}

// GetOwnerMembersCount 根据scopeID获取项目/应用所有者的数量
func (client *DBClient) GetOwnerMembersCount(scopeID int64, scopeType apistructs.ScopeType) (uint64, error) {
	var managerCount uint64
	db := client.Table("dice_member").Select("*").Joins(joinSQL)
	if err := db.Where("resource_value = ?", types.RoleProjectOwner).
		Where("dice_member.scope_type = ?", scopeType).Where("dice_member.scope_id = ?", scopeID).
		Count(&managerCount).Error; err != nil {
		return 0, err
	}
	return managerCount, nil
}

// GetOwnerMemberCountByUserID 获取某个用户的项目/应用所有者的数量
func (client *DBClient) GetOwnerMemberCountByUserID(userIDs []string, scopeID int64,
	scopeType apistructs.ScopeType) (uint64, error) {
	var managerCount uint64

	db := client.Table("dice_member").Select("*").Joins(joinSQL)
	db = db.Where("dice_member.user_id in (?)", userIDs).Where("dice_member.scope_type = ?", scopeType).
		Where("dice_member.scope_id = ?", scopeID)
	if err := db.Where("resource_value = ?", types.RoleProjectOwner).
		Count(&managerCount).Error; err != nil {
		return 0, err
	}

	return managerCount, nil
}

// GetManagerMembersCount 根据scopeID获取企业/项目/应用管理员的数量，todo delete
func (client *DBClient) GetManagerMembersCount(scopeID int64, scopeType apistructs.ScopeType) (uint64, error) {
	var managerCount uint64

	db := client.DB.Model(&model.Member{})
	db = db.Where("scope_type = ? AND scope_id = ?", scopeType, scopeID)

	if err := db.Where("role in (?)", types.GetScopeManagerRoleNames(scopeType)).Where("scope_type = ?", scopeType).
		Count(&managerCount).Error; err != nil {
		return 0, err
	}
	return managerCount, nil
}

// GetManagerMemberCountByUserID 获取某个用户的企业/项目/应用管理员数量
func (client *DBClient) GetManagerMemberCountByUserID(userIDs []string, scopeID int64,
	scopeType apistructs.ScopeType) (uint64, error) {
	var managerCount uint64

	db := client.Table("dice_member").Select("*").Joins(joinSQL)
	db = db.Where("dice_member.user_id in (?)", userIDs).Where("dice_member.scope_type = ?", scopeType).
		Where("dice_member.scope_id = ?", scopeID)
	if err := db.Where("resource_value in (?)", types.GetScopeManagerRoleNames(scopeType)).
		Count(&managerCount).Error; err != nil {
		return 0, err
	}

	return managerCount, nil
}

// GetMembersByScopeType 根据scopeType获取所有对应的成员列表，todo delete
func (client *DBClient) GetMembersByScopeType(scopeType apistructs.ScopeType) ([]model.Member, error) {
	var members []model.Member
	if err := client.Model(&model.Member{}).Where("scope_type = ?", scopeType).Find(&members).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, ErrNotFoundMember
		}
		return nil, err
	}
	return members, nil
}

// GetOrgByUserIDs 根据用户id获取对应的企业
func (client *DBClient) GetOrgByUserIDs(userIDs []string) ([]model.Member, error) {
	var members []model.Member
	if err := client.Model(&model.Member{}).Where("scope_type = 'org'").Where("user_id in ( ? )", userIDs).
		Find(&members).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, ErrNotFoundMember
		}
		return nil, err
	}
	return members, nil
}
