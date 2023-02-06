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

package permission

import (
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/services/security"
	"github.com/erda-project/erda/internal/core/legacy/types"
	"github.com/erda-project/erda/pkg/strutil"
)

// Permission 权限操作封装
type Permission struct {
	db  *dao.DBClient
	Bdl *bundle.Bundle
}

// Option 定义 Permission 对象配置选项
type Option func(*Permission)

// New 新建 Permission 实例
func New(options ...Option) *Permission {
	permission := &Permission{}
	for _, op := range options {
		op(permission)
	}
	return permission
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(p *Permission) {
		p.db = db
	}
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(p *Permission) {
		p.Bdl = bdl
	}
}

// StateCheckPermission 事件状态Button鉴权
func (p *Permission) StateCheckPermission(req *apistructs.PermissionCheckRequest) (bool, []string, error) {
	logrus.Debugf("invoke permission, time: %s, req: %+v", time.Now().Format(time.RFC3339), req)
	// 是否是内部服务账号
	if isReservedInternalServiceAccount(req.UserID) {
		return true, nil, nil
	}
	// 用户是否为系统管理员
	if admin, err := p.db.IsSysAdmin(req.UserID); err == nil && admin {
		return true, nil, nil
	}

	//// 管理员角色可继承
	//if ok := p.roleInherit(req.UserID, req.Scope, int64(req.ScopeID)); ok {
	//	return true, nil
	//}

	// 若用户 ID 为 support，则直接赋予 Support 角色，不去获取用户对应的角色
	// 若用户 ID 不是 support，获取用户是否有对应角色
	var roles []string
	if req.UserID != apistructs.SupportID {
		members, err := p.db.GetMemberByScopeAndUserID(req.UserID, req.Scope, int64(req.ScopeID))
		if err != nil {
			return false, nil, err
		}

		// 用户无项目角色时，若用户有项目下应用角色，则返回项目Guest角色
		if len(members) == 0 && req.Scope == apistructs.ProjectScope {
			members, err := p.db.GetMembersByParentID(apistructs.AppScope, int64(req.ScopeID), req.UserID)
			if err != nil || len(members) == 0 {
				return false, nil, nil
			}

			// TODO
			//roles = append(roles, types.GuestRole)
		} else {
			for _, member := range members {
				if member.ResourceKey == apistructs.RoleResourceKey {
					roles = append(roles, member.ResourceValue)
				}
			}
		}
	} else {
		roles = append(roles, types.RoleOrgSupport)
	}
	return false, roles, nil
}

// CheckPublicScope Check current scope public
func (p *Permission) CheckPublicScope(userId string, scopeType apistructs.ScopeType, scopeId int64) (bool, error) {
	switch scopeType {
	case apistructs.OrgScope:
		org, err := p.db.GetOrg(scopeId)
		if err != nil {
			return false, err
		}
		return org.IsPublic, nil
	case apistructs.ProjectScope:
		project, err := p.db.GetProjectByID(scopeId)
		if err != nil || !project.IsPublic {
			return false, err
		}
		// check if in upper level
		member, err := p.db.GetMemberByScopeAndUserID(userId, apistructs.OrgScope, project.OrgID)
		if err != nil {
			return false, err
		}
		if len(member) > 0 {
			return true, nil
		}
		// if not, check upper level isPublic
		org, err := p.db.GetOrg(project.OrgID)
		if err != nil {
			return false, err
		}
		return org.IsPublic, nil
	case apistructs.AppScope:
		app, err := p.db.GetApplicationByID(scopeId)
		if err != nil || !app.IsPublic {
			return false, err
		}
		member, err := p.db.GetMemberByScopeAndUserID(userId, apistructs.ProjectScope, app.ProjectID)
		if err != nil {
			return false, err
		}
		if len(member) > 0 {
			return true, nil
		}
		project, err := p.db.GetProjectByID(app.ProjectID)
		if err != nil || !project.IsPublic {
			return false, err
		}
		member, err = p.db.GetMemberByScopeAndUserID(userId, apistructs.OrgScope, project.OrgID)
		if err != nil {
			return false, err
		}
		if len(member) > 0 {
			return true, nil
		}
		org, err := p.db.GetOrg(project.OrgID)
		if err != nil {
			return false, err
		}
		return org.IsPublic, nil
	}
	return true, nil
}

var checkAdaptor security.PermissionAdaptor

// CheckPermission 鉴权Z
func (p *Permission) CheckPermission(req *apistructs.PermissionCheckRequest) (bool, error) {
	logrus.Debugf("invoke permission, time: %s, req: %+v", time.Now().Format(time.RFC3339), req)

	checkAdaptor.Once.Do(func() {
		checkAdaptor.Db = p.db
		checkAdaptor.Bdl = p.Bdl
		checkAdaptor.RegisterHandler(security.InternalUserPermissionHandler{Adaptor: &checkAdaptor})
		checkAdaptor.RegisterHandler(security.AdminUserPermissionHandler{Adaptor: &checkAdaptor})
		checkAdaptor.RegisterHandler(security.SupportUserPermissionHandler{Adaptor: &checkAdaptor})
		checkAdaptor.RegisterHandler(security.NormalUserPermissionHandler{Adaptor: &checkAdaptor})
	})

	return checkAdaptor.CheckPermission(*req)
}

// isReservedInternalServiceAccount 是否为内部服务账号
func isReservedInternalServiceAccount(userID string) bool {
	// TODO: ugly code
	// all (1000,5000) users is reserved as internal service account
	if v, err := strutil.Atoi64(userID); err == nil {
		if v > 1000 && v < 5000 && userID != apistructs.SupportID {
			return true
		}
	}
	return false
}

// CheckInternalPermission 鉴权内部服务账户
func (p *Permission) CheckInternalPermission(identityInfo apistructs.IdentityInfo) bool {
	if identityInfo.IsInternalClient() {
		return true
	}
	return isReservedInternalServiceAccount(identityInfo.UserID)
}

//func (p *Permission) roleInherit(userID string, scopeType apistructs.ScopeType, scopeID int64) bool {
//	switch scopeType {
//	case apistructs.OrgScope: // 企业级鉴权
//		// 企业管理员
//		members, err := p.db.GetMemberByScopeAndUserID(userID, scopeType, scopeID)
//		if err != nil || len(members) == 0 {
//			return false
//		}
//
//		for _, member := range members {
//			if member.Role == types.RoleOrgManager {
//				return true
//			}
//		}
//
//		// 系统管理员
//		if admin, err := p.db.IsSysAdmin(userID); err != nil || admin {
//			return false
//		}
//	case apistructs.ProjectScope: // 项目级鉴权
//		// 项目管理员
//		members, err := p.db.GetMemberByScopeAndUserID(userID, scopeType, scopeID)
//		if err != nil || len(members) == 0 {
//			return false
//		}
//
//		for _, member := range members {
//			if member.Role == types.RoleProjectOwner{
//				return true
//			}
//		}
//
//		// 企业管理员
//		project, err := p.db.GetProjectByID(scopeID)
//		if err != nil {
//			return false
//		}
//		members, err = p.db.GetMemberByScopeAndUserID(userID, apistructs.OrgScope, project.OrgID)
//		if err != nil || len(members) == 0 {
//			return false
//		}
//
//		for _, member := range members {
//			if member.Role == types.ManagerRole {
//				return true
//			}
//		}
//	case apistructs.AppScope: // 应用级鉴权
//		// 应用管理员
//		members, err := p.db.GetMemberByScopeAndUserID(userID, scopeType, scopeID)
//		if err != nil || len(members) == 0 {
//			return false
//		}
//		for _, member := range members {
//			if member.Role == types.ManagerRole {
//				return true
//			}
//		}
//
//		// 项目管理员
//		application, err := p.db.GetApplicationByID(scopeID)
//		if err != nil {
//			return false
//		}
//		members, err = p.db.GetMemberByScopeAndUserID(userID, apistructs.ProjectScope, application.ProjectID)
//		if err != nil || len(members) == 0 {
//			return false
//		}
//		for _, member := range members {
//			if member.Role == types.ManagerRole {
//				return true
//			}
//		}
//	case apistructs.PublisherScope: // Publisher级鉴权
//		// Publisher管理员
//		members, err := p.db.GetMemberByScopeAndUserID(userID, scopeType, scopeID)
//		if err != nil || len(members) == 0 {
//			return false
//		}
//		for _, member := range members {
//			if member.Role == types.ManagerRole {
//				return true
//			}
//		}
//	}
//	return false
//}
