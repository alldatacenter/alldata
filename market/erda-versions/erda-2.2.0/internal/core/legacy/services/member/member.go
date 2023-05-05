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

// Package member 成员操作封装
package member

import (
	"context"
	"strconv"
	"time"
	"unicode/utf8"

	"github.com/go-redis/redis"
	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/types"
	locale "github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

// Member 成员操作封装
type Member struct {
	db           *dao.DBClient
	uc           *ucauth.UCClient
	redisCli     *redis.Client
	tran         i18n.Translator
	tokenService tokenpb.TokenServiceServer
}

// Option 定义 Member 对象配置选项
type Option func(*Member)

// New 新建 Member 实例
func New(options ...Option) *Member {
	mem := &Member{}
	for _, op := range options {
		op(mem)
	}
	return mem
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(m *Member) {
		m.db = db
	}
}

// WithUCClient 配置 uc client
func WithUCClient(uc *ucauth.UCClient) Option {
	return func(m *Member) {
		m.uc = uc
	}
}

// WithRedisClient 配置 redis client
func WithRedisClient(cli *redis.Client) Option {
	return func(m *Member) {
		m.redisCli = cli
	}
}

func WithTranslator(tran i18n.Translator) Option {
	return func(m *Member) {
		m.tran = tran
	}
}

func WithTokenSvc(tokenService tokenpb.TokenServiceServer) Option {
	return func(m *Member) {
		m.tokenService = tokenService
	}
}

// CreateOrUpdate 创建/更新成员
func (m *Member) CreateOrUpdate(userID string, req apistructs.MemberAddRequest) error {
	// 参数校验
	if err := m.checkCreateParam(req); err != nil {
		return err
	}

	langCodes, _ := i18n.ParseLanguageCode(locale.GetGoroutineBindLang())
	scopeID, err := strconv.ParseInt(req.Scope.ID, 10, 64)
	if err != nil {
		return errors.Errorf("failed to create permission(invalid scope id)")
	}

	users, err := m.uc.FindUsers(req.UserIDs)
	if err != nil {
		logrus.Warnf("failed to get user info, (%v)", err)
		return errors.Errorf("failed to get user info")
	}
	if err := m.checkUCUserInfo(users); err != nil {
		return err
	}

	for _, role := range req.Roles {
		if types.CheckIfRoleIsOwner(role) {
			if req.Scope.Type == apistructs.ProjectScope || req.Scope.Type == apistructs.AppScope {
				ownerCount, err := m.db.GetOwnerMembersCount(scopeID, req.Scope.Type)
				if err != nil {
					return err
				}
				oldCount, err := m.getMemberOwnerCount(users, scopeID, req.Scope.Type)
				if err != nil {
					return err
				}
				if ownerCount+uint64(len(req.UserIDs))-oldCount > 1 {
					return errors.Errorf(m.tran.Text(langCodes, "ErrAddMemberOwner"))
				}
			}
		}
	}
	// 管理员最多允许5个
	// for _, role := range req.Roles {
	//	if types.CheckIfRoleIsManager(role) {
	// 		// 企业管理员
	// 		if req.Scope.Type == apistructs.OrgScope {
	// 			orgManagerCount, err := m.db.GetManagerMembersCount(scopeID, apistructs.OrgScope)
	// 			if err != nil {
	// 				return err
	// 			}
	//
	//			oldCount, err := m.getMemberManagerCount(users, scopeID, apistructs.OrgScope)
	// 			if err != nil {
	// 				return err
	// 			}
	//
	// 			if orgManagerCount+uint64(len(req.UserIDs))-oldCount > apistructs.MaxOrgNum {
	// 				return errors.Errorf("企业管理员最多不能超过5个")
	// 			}
	// 		}
	//
	// 		// 项目管理员
	// 		if req.Scope.Type == apistructs.ProjectScope {
	// 			projectManagerCount, err := m.db.GetManagerMembersCount(scopeID, apistructs.ProjectScope)
	// 			if err != nil {
	// 				return err
	// 			}
	//
	// 			oldCount, err := m.getMemberManagerCount(users, scopeID, apistructs.ProjectScope)
	// 			if err != nil {
	// 				return err
	// 			}
	//
	// 			if projectManagerCount+uint64(len(req.UserIDs))-oldCount > apistructs.MaxProjectNum {
	// 				return errors.Errorf("项目管理员最多不能超过5个")
	// 			}
	// 		}
	//
	// 		// Publisher 管理员
	// 		if req.Scope.Type == apistructs.PublisherScope {
	// 			pubManagerCount, err := m.db.GetManagerMembersCount(scopeID, apistructs.PublisherScope)
	// 			if err != nil {
	// 				return err
	// 			}
	//
	// 			oldCount, err := m.getMemberManagerCount(users, scopeID, apistructs.PublisherScope)
	// 			if err != nil {
	// 				return err
	// 			}
	//
	// 			if pubManagerCount+uint64(len(req.UserIDs))-oldCount > apistructs.MaxProjectNum {
	// 				return errors.Errorf("Publisher管理员最多不能超过5个")
	// 			}
	// 		}
	//
	// 		break
	// 	}
	// }

	// 操作鉴权
	if err = m.CheckPermission(userID, req.Scope.Type, scopeID); err != nil {
		return err
	}

	var (
		targetScopeIDs  []int64
		targetScopeType apistructs.ScopeType
	)
	if len(req.TargetScopeIDs) != 0 && req.TargetScopeType != "" {
		targetScopeIDs, targetScopeType = req.TargetScopeIDs, req.TargetScopeType
	} else {
		targetScopeIDs, targetScopeType = []int64{scopeID}, req.Scope.Type
	}

	// 添加成员信息至DB
	var userIDStrs []string
	parentIDMap := make(map[int64]int64)
	for _, user := range users {
		for _, targetScopeID := range targetScopeIDs {
			members, err := m.db.GetMemberByScopeAndUserID(user.ID, targetScopeType, targetScopeID)
			if err != nil {
				logrus.Infof("failed to get members, (%v)", err)
				continue
			}
			parentID := m.getParentScopeID(targetScopeType, targetScopeID)
			parentIDMap[targetScopeID] = parentID
			// 创建成员
			if len(members) == 0 {
				orgID, projectID, applicationID := m.getIDs(req, targetScopeID)
				// create PAT when user is invited to org.
				// TODO: After we expose PAT management on UI, this can be removed.
				if req.Scope.Type == apistructs.OrgScope {
					_, err := m.tokenService.CreateToken(context.Background(), &tokenpb.CreateTokenRequest{
						Scope:     string(apistructs.OrgScope),
						ScopeId:   req.Scope.ID,
						Type:      mysqltokenstore.PAT.String(),
						CreatorId: user.ID,
					})
					if err != nil {
						return err
					}
				}
				member := &model.Member{
					ScopeType:     targetScopeType,
					ScopeID:       targetScopeID,
					ScopeName:     m.db.GetScopeNameByScope(req.Scope.Type, targetScopeID),
					ParentID:      parentID,
					UserID:        user.ID,
					Email:         user.Email,
					Mobile:        user.Phone,
					Name:          user.Name,
					Nick:          user.Nick,
					Avatar:        user.AvatarURL,
					UserSyncAt:    time.Now(),
					OrgID:         orgID,
					ProjectID:     projectID,
					ApplicationID: applicationID,
				}
				if err := m.db.CreateMember(member); err != nil {
					return errors.Errorf("failed to add member, (%v)", err)
				}
			}
		}

		userIDStrs = append(userIDStrs, user.ID)
	}

	// 更新member的extra
	if err := m.syncReqExtra2DB(userIDStrs, targetScopeType, targetScopeIDs, req, parentIDMap); err != nil {
		return err
	}

	return nil
}

// UpdateMemberUserInfo 更新成员的user info 和 uc 同步
func (m *Member) UpdateMemberUserInfo(req apistructs.MemberUserInfoUpdateRequest) error {
	for _, member := range req.Members {
		oldMembers, err := m.db.GetMemberByUserID(member.UserID)
		if err != nil {
			continue
		}
		memberIDs := make([]int64, 0, len(oldMembers))
		for _, v := range oldMembers {
			memberIDs = append(memberIDs, v.ID)
		}
		if err := m.db.UpdateMemberUserInfo(memberIDs, map[string]interface{}{
			"email":  member.Email,
			"mobile": member.Mobile,
			"name":   member.Name,
			"nick":   member.Nick,
			"avatar": member.Avatar,
		}); err != nil {
			return err
		}
	}

	return nil
}

// List 成员列表/查询
func (m *Member) List(param *apistructs.MemberListRequest) (int, []model.Member, error) {
	return m.db.GetMembersByParam(param)
}

// Delete 删除成员
func (m *Member) Delete(userID string, req apistructs.MemberRemoveRequest) error {
	// 参数校验
	if err := m.checkDeleteParam(req); err != nil {
		return err
	}

	// 操作鉴权
	scopeID, err := strconv.ParseInt(req.Scope.ID, 10, 64)
	if err != nil {
		return errors.Errorf("failed to delete member(invalid scope id)")
	}

	if len(req.UserIDs) > 1 || req.UserIDs[0] != userID { // 若自己退出，无需鉴权；若移除其他成员，需要管理员权限
		if err := m.CheckPermission(userID, req.Scope.Type, scopeID); err != nil {
			return err
		}
	}

	// 用户退出scope时，删除当前用户该scope下的extra
	if err := m.deleteMemberExtra(req.UserIDs, req.Scope.Type, scopeID); err != nil {
		return err
	}

	if req.Scope.Type == apistructs.OrgScope { // 若用户退出企业时，删除当前用户所有权限
		if err := m.db.DeleteMembersByOrgAndUsers(req.Scope.ID, req.UserIDs); err != nil {
			return err
		}
		return nil
	}

	// 若用户退出项目/应用/Publisher时
	if err := m.db.DeleteMembersByScopeAndUsers(req.UserIDs, req.Scope.Type, scopeID); err != nil {
		return errors.Errorf("failed to delete members, (%v)", err)
	}

	// 若用户退出项目时，删除该用户对应项目下的应用权限
	if req.Scope.Type == apistructs.ProjectScope {
		if err := m.db.DeleteProjectAppsMembers(req.UserIDs, scopeID); err != nil {
			return errors.Errorf("failed to delete project applications members list, (%v)", err)
		}
	}

	return nil
}

// ListMemberRolesByUser 根据用户查看角色
func (m *Member) ListMemberRolesByUser(l *locale.LocaleResource, identityInfo apistructs.IdentityInfo, pageReq apistructs.ListMemberRolesByUserRequest) (int,
	[]apistructs.UserScopeRole, error) {
	// 鉴权
	if !identityInfo.IsInternalClient() {
		admin, err := m.db.IsSysAdmin(identityInfo.UserID)
		if err != nil {
			return 0, nil, err
		}
		if !admin {
			return 0, nil, errors.Errorf("无权限")
		}
	}

	//
	total, members, err := m.db.PageMeberRoleByParentID(pageReq)
	if err != nil {
		return 0, nil, err
	}
	userScopeRoles := make([]apistructs.UserScopeRole, 0, total)
	for _, member := range members {
		roles := make([]string, 0, len(member.Roles))
		for _, role := range member.Roles {
			roles = append(roles, l.Get(types.AllScopeRoleMap[pageReq.ScopeType][role].I18nKey))
		}
		scopeName := member.ScopeName
		if scopeName == "" {
			scopeName = m.db.GetScopeNameByScope(member.ScopeType, member.ScopeID)
		}
		userScopeRoles = append(userScopeRoles, apistructs.UserScopeRole{
			ScopeType: member.ScopeType,
			ScopeID:   member.ScopeID,
			ScopeName: scopeName,
			Roles:     roles,
		})
	}

	return total, userScopeRoles, nil
}

// ListByOrgAndUser 根据用户获取成员列表
func (m *Member) ListByOrgAndUser(orgID int64, userID string) ([]model.MemberJoin, error) {
	return m.db.GetMembersByOrgAndUser(orgID, userID)
}

// GetByUserAndScope 根据用户 & scope获取成员
func (m *Member) GetByUserAndScope(userID string, scopeType apistructs.ScopeType, scopeID int64) ([]model.MemberJoin, error) {
	return m.db.GetMemberByScopeAndUserID(userID, scopeType, scopeID)
}

// GetByToken 根据用户token获取成员
func (m *Member) GetByToken(token string) (*model.Member, error) {
	return m.db.GetMemberByToken(token)
}

// GetScopeManagersByScopeID 根据 scopeID 获取 scope 对应的管理员列表
func (m *Member) GetScopeManagersByScopeID(scopeType apistructs.ScopeType, scopeID int64) ([]apistructs.Member, error) {
	return m.db.GetScopeManagersByScopeID(scopeType, scopeID)
}

// GetMembersByParent 根据parentID查看成员
// func (m *Member) GetMembersByParent(scopeType apistructs.ScopeType, parentID int64, userID string) ([]model.Member, error) {
// 	return m.db.GetMembersByParent(scopeType, parentID, userID)
// }

// ListMemberLabel 查看一个成员的label
func (m *Member) ListMemberLabel(userID string, scopeID int64) ([]string, error) {
	labels := make([]string, 0)
	mlRelations, err := m.db.GetMemberExtra([]string{userID}, apistructs.OrgScope, scopeID,
		[]apistructs.ExtraResourceKey{apistructs.LabelResourceKey})
	if err != nil {
		return nil, err
	}

	for _, mlr := range mlRelations {
		labels = append(labels, mlr.ResourceValue)
	}

	return labels, nil
}

// 添加/修改成员时，参数检查
func (m *Member) checkCreateParam(memberCreateReq apistructs.MemberAddRequest) error {
	// Role合法性校验
	for _, role := range memberCreateReq.Roles {
		if !types.CheckIfRoleIsValid(role) {
			return errors.Errorf("invalid request, role is invalid")
		}
	}
	// Scope合法性校验
	if _, ok := types.AllScopeRoleMap[memberCreateReq.Scope.Type]; !ok {
		return errors.Errorf("invalid request, scope type is invalid")
	}

	if memberCreateReq.TargetScopeType != "" {
		if _, ok := types.AllScopeRoleMap[memberCreateReq.TargetScopeType]; !ok {
			return errors.Errorf("invalid request, scope type is invalid")
		}
	}

	// UserIds不可为空
	if len(memberCreateReq.UserIDs) == 0 {
		return errors.Errorf("invalid request, userIds is empty")
	}
	// TODO userIds真实性检查
	return nil
}

// 删除成员时，参数检查
func (m *Member) checkDeleteParam(memberDeleteReq apistructs.MemberRemoveRequest) error {
	// Scope合法性校验
	if _, ok := types.AllScopeRoleMap[memberDeleteReq.Scope.Type]; !ok {
		return errors.Errorf("invalid request, scope type is invalid")
	}

	// UserIds不可为空
	if len(memberDeleteReq.UserIDs) == 0 {
		return errors.Errorf("invalid request, userIds is empty")
	}
	return nil
}

// 更新请求里的成员角色，标签到dice_member_extra里
func (m *Member) syncReqExtra2DB(userIDs []string, scopeType apistructs.ScopeType, scopeIDs []int64,
	req apistructs.MemberAddRequest, parentIDMap map[int64]int64) error {
	// newUserIDRLMap是请求里的角色列表
	newUserIDRLMap := make(map[string]map[string]string)
	for _, userID := range userIDs {
		for _, v := range req.Roles {
			if _, ok := newUserIDRLMap[userID]; !ok {
				newUserIDRLMap[userID] = make(map[string]string, 0)
			}
			newUserIDRLMap[userID][v] = "role"
		}
		for _, v := range req.Labels {
			if _, ok := newUserIDRLMap[userID]; !ok {
				newUserIDRLMap[userID] = make(map[string]string, 0)
			}
			newUserIDRLMap[userID][v] = "label"
		}
	}

	// oldExtras是数据库里的角色和标签列表
	oldUserIDRLMap, needDeleteUserIDRLMap := make(map[string][]string, 0), make(map[string][]string, 0)
	oldExtras, err := m.db.GetMemberExtraByIDs(userIDs, scopeType, scopeIDs,
		[]apistructs.ExtraResourceKey{apistructs.RoleResourceKey, apistructs.LabelResourceKey})
	if err != nil {
		return err
	}
	for _, v := range oldExtras {
		oldUserIDRLMap[v.UserID] = append(oldUserIDRLMap[v.UserID], v.ResourceValue)
	}
	for k, v := range oldUserIDRLMap {
		oldUserIDRLMap[k] = strutil.DedupSlice(v)
	}

	// 两边同步过程
	for userID, oldRLs := range oldUserIDRLMap {
		for _, oldRL := range oldRLs {
			if _, ok := newUserIDRLMap[userID][oldRL]; !ok {
				// 需要删除的成员角色
				needDeleteUserIDRLMap[userID] = append(needDeleteUserIDRLMap[userID], oldRL)
			} else {
				// 用户新的角色以前就有的，不作处理
				delete(newUserIDRLMap[userID], oldRL)
			}
		}
	}

	// 删除不要了的角色
	for userID, rl := range needDeleteUserIDRLMap {
		if len(rl) > 0 {
			if err := m.db.DeleteMemberExtraByUxerIDANDResourceValues(userID, scopeType, scopeIDs, rl); err != nil {
				return err
			}
		}
	}

	// 创建新增的角色
	var mes []model.MemberExtra
	for userID, rlMap := range newUserIDRLMap {
		for k, v := range rlMap {
			for _, scopeID := range scopeIDs {
				me := model.MemberExtra{
					UserID:        userID,
					ScopeType:     scopeType,
					ScopeID:       scopeID,
					ParentID:      parentIDMap[scopeID],
					ResourceValue: k,
				}
				switch v {
				case "role":
					me.ResourceKey = apistructs.RoleResourceKey
				case "label":
					me.ResourceKey = apistructs.LabelResourceKey
				default:
					continue
				}
				mes = append(mes, me)
			}
		}
	}

	// 新增成员角色和标签
	if len(mes) > 0 {
		if err := m.db.BatchCreateMemberExtra(mes); err != nil {
			return err
		}
	}

	return nil
}

// 在判断最大管理员数量时，若是成员原本就有manager的角色，这次更新只是添加一个非管理员的角色，
// 则不应该算作占用最大管理员数量中的一个
func (m *Member) getMemberManagerCount(users []ucauth.User, scopeID int64, scopeType apistructs.ScopeType) (uint64, error) {
	var userIDs []string
	for _, user := range users {
		userIDs = append(userIDs, user.ID)
	}

	count, err := m.db.GetManagerMemberCountByUserID(userIDs, scopeID, scopeType)
	if err != nil {
		return 0, err
	}

	return count, nil
}

// 在判断最大管理员数量时，若是成员原本就有owner的角色，这次更新只是添加一个非owner的角色，
// 则不应该算作占用最大所有者数量中的一个
func (m *Member) getMemberOwnerCount(users []ucauth.User, scopeID int64, scopeType apistructs.ScopeType) (uint64, error) {
	var userIDs []string
	for _, user := range users {
		userIDs = append(userIDs, user.ID)
	}

	count, err := m.db.GetOwnerMemberCountByUserID(userIDs, scopeID, scopeType)
	if err != nil {
		return 0, err
	}

	return count, nil
}

// 删除成员所有关联信息
func (m *Member) deleteMemberExtra(userIDs []string, scopeType apistructs.ScopeType, scopeID int64) error {
	if scopeType == apistructs.OrgScope {
		// 获取该企业下该成员加入的所有项目
		var projectIDs []int64
		projects, err := m.db.GetMeberExtraByParentID(userIDs, apistructs.ProjectScope, scopeID)
		if err != nil && !gorm.IsRecordNotFoundError(err) {
			return err
		}
		for _, p := range projects {
			projectIDs = append(projectIDs, p.ScopeID)
		}

		// 删除项目的权限
		if err := m.db.DeleteMemberExtraByUserIDsAndScopeIDs(apistructs.ProjectScope, projectIDs, userIDs); err != nil {
			return err
		}
		// 删除应用的权限
		if err := m.db.DeleteMemberExtraByParentIDs(userIDs, apistructs.AppScope, projectIDs); err != nil {
			return err
		}
	}

	if scopeType == apistructs.ProjectScope {
		if err := m.db.DeleteMemberExtraByParentID(userIDs, apistructs.AppScope, scopeID); err != nil {
			return err
		}
	}

	if err := m.db.DeleteMemberExtraByUserIDsAndScope(scopeType, scopeID, userIDs); err != nil {
		return err
	}

	return nil
}

// CheckPermission 成员相关API鉴权， userID: 当前操作用户
func (m *Member) CheckPermission(userID string, scopeType apistructs.ScopeType, scopeID int64) error {
	if userID == "onlyYou" {
		return nil
	}
	// TODO: ugly code
	// all (1000,5000) users is reserved as internal service account
	// support 角色无权限添加和删除成员
	if v, err := strutil.Atoi64(userID); err == nil {
		if v > 1000 && v < 5000 && userID != apistructs.SupportID {
			return nil
		}
	}

	admin, err := m.db.IsSysAdmin(userID)
	if err != nil {
		return err
	}

	switch scopeType {
	case apistructs.SysScope:
		// 系统管理员
		if admin {
			return nil
		}
	case apistructs.OrgScope: // 企业级鉴权
		// 企业管理员
		members, err := m.db.GetMemberByScopeAndUserID(userID, scopeType, scopeID)
		if err != nil {
			return err
		}
		for _, member := range members {
			if member.ResourceKey == apistructs.RoleResourceKey && types.CheckIfRoleIsManager(member.ResourceValue) {
				return nil
			}
		}

		// 系统管理员
		if admin {
			return nil
		}
	case apistructs.ProjectScope: // 项目级鉴权
		// 项目管理员
		members, err := m.db.GetMemberByScopeAndUserID(userID, scopeType, scopeID)
		if err != nil {
			return err
		}
		for _, member := range members {
			if member.ResourceKey == apistructs.RoleResourceKey && types.CheckIfRoleIsManager(member.ResourceValue) {
				return nil
			}
		}

		// 企业管理员
		project, err := m.db.GetProjectByID(scopeID)
		if err != nil {
			return err
		}
		members, err = m.db.GetMemberByScopeAndUserID(userID, apistructs.OrgScope, project.OrgID)
		if err != nil {
			return err
		}
		for _, member := range members {
			if member.ResourceKey == apistructs.RoleResourceKey && types.CheckIfRoleIsManager(member.ResourceValue) {
				return nil
			}
		}
	case apistructs.AppScope: // 应用级鉴权
		// 应用管理员
		members, err := m.db.GetMemberByScopeAndUserID(userID, scopeType, scopeID)
		if err != nil {
			return err
		}
		for _, member := range members {
			if member.ResourceKey == apistructs.RoleResourceKey && types.CheckIfRoleIsManager(member.ResourceValue) {
				return nil
			}
		}

		// 项目管理员
		application, err := m.db.GetApplicationByID(scopeID)
		if err != nil {
			return err
		}
		members, err = m.db.GetMemberByScopeAndUserID(userID, apistructs.ProjectScope, application.ProjectID)
		if err != nil {
			return err
		}
		for _, member := range members {
			if member.ResourceKey == apistructs.RoleResourceKey && types.CheckIfRoleIsManager(member.ResourceValue) {
				return nil
			}
		}
	}
	return errors.Errorf("failed to check permission")
}

// 获取orgID, projectID, applicationID
func (m *Member) getIDs(req apistructs.MemberAddRequest, scopeID int64) (orgID, projectID, applicationID int64) {
	switch req.Scope.Type {
	case apistructs.OrgScope:
		orgID = scopeID
	case apistructs.ProjectScope:
		project, _ := m.db.GetProjectByID(scopeID)
		orgID = project.OrgID
		projectID = scopeID
	case apistructs.AppScope:
		app, _ := m.db.GetApplicationByID(scopeID)
		orgID = app.OrgID
		projectID = app.ProjectID
		applicationID = scopeID
	}

	return orgID, projectID, applicationID
}

// 获取当前scope的父scopeID
func (m *Member) getParentScopeID(scopeType apistructs.ScopeType, scopeID int64) int64 {
	switch scopeType {
	case apistructs.OrgScope:
		return 0
	case apistructs.ProjectScope:
		project, err := m.db.GetProjectByID(scopeID)
		if err != nil {
			logrus.Infof("failed to get project, (%v)", err)
			return 0
		}
		return project.OrgID
	case apistructs.AppScope:
		application, err := m.db.GetApplicationByID(scopeID)
		if err != nil {
			logrus.Infof("failed to get application, (%v)", err)
			return 0
		}
		return application.ProjectID
	default:
		logrus.Infof("unknown scopeType")
		return 0
	}
}

// IsAdmin 检查用户是否为系统管理员
func (m *Member) IsAdmin(userID string) bool {
	var member model.Member
	if err := m.db.Where("scope_type = ?", apistructs.SysScope).
		Where("user_id = ?", userID).Find(&member).Error; err != nil {
		if err != gorm.ErrRecordNotFound {
			return false
		}
	}
	if member.ID == 0 {
		logrus.Debugf("CAUTION: user(%s) currently not an admin, will become soon if no one admin exist", userID)
		// TODO: some risk
		// TODO: just a magic value, kratos' user_id is UUID, it is significantly larger than 11
		if len(userID) > 11 && m.noOneAdminForKratos() { // len > 11 imply that is kratos user
			logrus.Debugf("CAUTION: firstUserBecomeAdmin: %s, there may some risk", userID)
			if err := m.firstUserBecomeAdmin(userID); err != nil {
				return false
			}
			return true
		}
		return false
	}
	return true
}

func (m *Member) noOneAdminForKratos() bool {
	cnt := 1
	if err := m.db.Model(&model.Member{}).
		// only kratos user_id length greater than 11, add this check to prevent init sql's data: user_id=1 admin
		// TODO: just a magic value, kratos' user_id is UUID, it is significantly larger than 11
		Where("scope_type = ? AND length(user_id) > 11", apistructs.SysScope).
		Count(&cnt).Error; err != nil {
		return false
	}
	logrus.Debugf("CAUTION: there are %d admins", cnt)
	return cnt == 0
}

func (m *Member) firstUserBecomeAdmin(userID string) error {
	return m.db.Create(&model.Member{
		ScopeType: apistructs.SysScope,
		UserID:    userID,
	}).Error
}

// ListByScopeTypeAndUser 根据scopeType & user 获取成员
func (m *Member) ListByScopeTypeAndUser(scopeType apistructs.ScopeType, userID string) ([]model.Member, error) {
	return m.db.GetMembersByScopeTypeAndUser(scopeType, userID)
}

// ListOrgByUserIDs 获取userid对应的企业关系
func (m *Member) ListOrgByUserIDs(userIDs []string) ([]model.Member, error) {
	return m.db.GetOrgByUserIDs(userIDs)
}

// CheckInviteCode 校验邀请码
func (m *Member) CheckInviteCode(inviteCode, orgID string) (string, error) {
	if inviteCode == "" || utf8.RuneCount([]byte(inviteCode)) < 7 {
		return "", errors.New("inviteCode is invalid")
	}

	userID, err := apistructs.DecodeUserID(inviteCode[5:])
	if err != nil {
		return "", errors.Errorf("inviteCode is invalid err: %v", err)
	}
	now := time.Now()
	code1, err := m.redisCli.Get(apistructs.OrgInviteCodeRedisKey.GetKey(now.Day(), userID, orgID)).Result()
	if err == redis.Nil {
	} else if err != nil {
		return "", err
	}

	code2, err := m.redisCli.Get(apistructs.OrgInviteCodeRedisKey.GetKey(now.AddDate(0, 0, -1).Day(), userID, orgID)).Result()
	if err == redis.Nil {
	} else if err != nil {
		return "", err
	}

	if code1 == "" && code2 == "" {
		return "", errors.New("The verification code has expired")
	}

	if inviteCode != code1 && inviteCode != code2 {
		return "", errors.New("The verification code error")
	}

	logrus.Info("The verification code is correct")
	return userID, nil
}

// GetAllOrganizational 获取所有组织架构
func (m *Member) GetAllOrganizational() (*apistructs.GetAllOrganizationalData, error) {
	// list all org
	orgs, err := m.db.GetOrgList()
	if err != nil {
		return nil, err
	}

	organization := make(map[string]map[string][]string, 0)
	members := make(map[string]map[string]map[string]apistructs.Member, 0)
	for _, org := range orgs {
		organization[org.Name] = make(map[string][]string, 0)
		// 获取企业下的成员
		orgMembers, err := m.getMember(apistructs.OrgScope, org.ID)
		if err != nil {
			return nil, err
		}
		for userID, userDATA := range orgMembers {
			if _, ok := members[userID]; !ok {
				members[userID] = make(map[string]map[string]apistructs.Member, 0)
			}
			if _, ok := members[userID]["org"]; !ok {
				members[userID]["org"] = make(map[string]apistructs.Member, 0)
			}
			members[userID]["org"][org.Name] = userDATA
		}
		// 获取企业下的项目
		prjs, err := m.db.ListProjectByOrgID(uint64(org.ID))
		if err != nil {
			return nil, err
		}
		for _, prj := range prjs {
			organization[org.Name][prj.Name] = []string{}
			// 获取项目下的成员
			prjMembers, err := m.getMember(apistructs.ProjectScope, prj.ID)
			if err != nil {
				return nil, err
			}
			for userID, userDATA := range prjMembers {
				if _, ok := members[userID]; !ok {
					members[userID] = make(map[string]map[string]apistructs.Member, 0)
				}
				if _, ok := members[userID]["prj"]; !ok {
					members[userID]["prj"] = make(map[string]apistructs.Member, 0)
				}
				members[userID]["prj"][prj.Name] = userDATA
			}
			apps, err := m.db.GetApplicationsByProjectID(prj.ID, 1, 1000)
			if err != nil {
				return nil, err
			}
			for _, app := range apps {
				organization[org.Name][prj.Name] = append(organization[org.Name][prj.Name], app.Name)
				// 获取应用下的成员
				appMembers, err := m.getMember(apistructs.AppScope, app.ID)
				if err != nil {
					return nil, err
				}
				for userID, userDATA := range appMembers {
					if _, ok := members[userID]; !ok {
						members[userID] = make(map[string]map[string]apistructs.Member, 0)
					}
					if _, ok := members[userID]["app"]; !ok {
						members[userID]["app"] = make(map[string]apistructs.Member, 0)
					}
					members[userID]["app"][app.Name] = userDATA
				}
			}
		}
	}

	return &apistructs.GetAllOrganizationalData{Organization: organization, Members: members}, nil
}

func (m *Member) getMember(scopeType apistructs.ScopeType, scopeID int64) (map[string]apistructs.Member, error) {
	_, tmpMembers, err := m.db.GetMembersByParam(&apistructs.MemberListRequest{
		PageNo:    1,
		PageSize:  5000,
		ScopeType: scopeType,
		ScopeID:   scopeID,
	})
	if err != nil {
		return nil, err
	}
	members := make(map[string]apistructs.Member, 0)
	for _, v := range tmpMembers {
		members[v.UserID] = v.Convert2APIDTO()
	}

	return members, nil
}

func (m *Member) checkUCUserInfo(users []ucauth.User) error {
	for _, user := range users {
		if user.ID == "" {
			return errors.Errorf("failed to get user info")
		}
	}
	return nil
}
