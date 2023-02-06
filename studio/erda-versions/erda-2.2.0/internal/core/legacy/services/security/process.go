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

package security

import (
	"context"
	"sync"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/types"
	"github.com/erda-project/erda/pkg/strutil"
)

type PermissionProcess interface {
	permissionList(ctx context.Context) (*apistructs.PermissionList, error)
	check(ctx context.Context) (bool, error)
}

type DefaultPermissionProcess struct {
}

func (d DefaultPermissionProcess) permissionList(ctx context.Context) (*apistructs.PermissionList, error) {
	var permissionList = apistructs.PermissionList{
		Access:           false,
		PermissionList:   make([]apistructs.ScopeResource, 0),
		ResourceRoleList: make([]apistructs.ScopeResource, 0),
		Roles:            make([]string, 0),
		Exist:            true,
	}
	return &permissionList, nil
}

func (d DefaultPermissionProcess) check(ctx context.Context) (bool, error) {
	return false, nil
}

type PermissionProcessMiddleware struct {
	RealProcess  PermissionProcess
	AfterProcess func(context.Context, *apistructs.PermissionList) (*apistructs.PermissionList, error)
	AfterCheck   func(ctx context.Context, check bool) (bool, error)
}

func (d PermissionProcessMiddleware) permissionList(ctx context.Context) (*apistructs.PermissionList, error) {
	list, err := d.RealProcess.permissionList(ctx)
	if err != nil {
		return nil, err
	}
	if d.AfterProcess != nil {
		return d.AfterProcess(ctx, list)
	}
	return list, nil
}

func (d PermissionProcessMiddleware) check(ctx context.Context) (bool, error) {
	check, err := d.RealProcess.check(ctx)
	if err != nil {
		return false, err
	}

	if d.AfterCheck != nil {
		return d.AfterCheck(ctx, check)
	}
	return check, err
}

type RolePermissionProcess struct {
	Adaptor *PermissionAdaptor
	roles   []string
}

func (r RolePermissionProcess) permissionList(ctx context.Context) (*apistructs.PermissionList, error) {
	permissionList, err := DefaultPermissionProcess{}.permissionList(ctx)
	if err != nil {
		return nil, err
	}

	if len(r.roles) <= 0 {
		return permissionList, nil
	}

	var pml = make([]model.RolePermission, 0)
	// get permissions from db
	pmlDb, resourceRoleDb := r.Adaptor.Db.GetPermissionList(r.roles)

	// get permissions from permission.yml
	pmlYml, resourceRoles := conf.RolePermissions(r.roles)
	if len(pmlDb) > 0 {
		for _, v := range pmlDb {
			k := strutil.Concat(v.Scope, v.Resource, v.Action)
			pmlYml[k] = v
		}
	}

	// merge permission list
	for _, v := range pmlYml {
		pml = append(pml, v)
	}

	permissions := make([]apistructs.ScopeResource, 0)
	for _, v := range pml {
		permission := apistructs.ScopeResource{
			Resource:     v.Resource,
			Action:       v.Action,
			ResourceRole: v.ResourceRole,
		}
		permissions = append(permissions, permission)
	}

	resourceRoles = append(resourceRoles, resourceRoleDb...)
	resourceRoleList := make([]apistructs.ScopeResource, 0)
	for _, v := range resourceRoles {
		rr := apistructs.ScopeResource{
			Resource:     v.Resource,
			Action:       v.Action,
			ResourceRole: v.ResourceRole,
		}
		resourceRoleList = append(resourceRoleList, rr)
	}

	permissionList.Access = true
	permissionList.Roles = r.roles
	permissionList.PermissionList = permissions
	permissionList.ResourceRoleList = resourceRoleList
	return permissionList, nil
}

func (d RolePermissionProcess) check(ctx context.Context) (bool, error) {
	if len(d.roles) <= 0 {
		return false, nil
	}

	req := d.Adaptor.GetCheckRequest(ctx)

	rp, err := d.Adaptor.Db.GetRolePermission(d.roles, &req)
	if err != nil {
		return false, err
	}
	return rp != nil, nil
}

type GetByUserAndScopePermissionProcess struct {
	Adaptor *PermissionAdaptor
}

func (g GetByUserAndScopePermissionProcess) permissionList(ctx context.Context) (*apistructs.PermissionList, error) {
	permissionList, err := DefaultPermissionProcess{}.permissionList(ctx)
	if err != nil {
		return nil, err
	}

	roles, err := g.GetAllRoles(ctx)
	if err != nil {
		return permissionList, err
	}
	if len(roles) <= 0 {
		return permissionList, nil
	}

	return RolePermissionProcess{
		Adaptor: g.Adaptor,
		roles:   roles,
	}.permissionList(ctx)
}

func (d GetByUserAndScopePermissionProcess) check(ctx context.Context) (bool, error) {
	roles, err := d.GetAllRoles(ctx)
	if err != nil {
		return false, err
	}
	if len(roles) <= 0 {
		return false, nil
	}

	return RolePermissionProcess{
		Adaptor: d.Adaptor,
		roles:   roles,
	}.check(ctx)
}

func (g GetByUserAndScopePermissionProcess) GetAllRoles(ctx context.Context) ([]string, error) {
	var roles []string

	members, err := g.Adaptor.Db.GetMemberByScopeAndUserID(g.Adaptor.GetUserID(ctx), g.Adaptor.GetScopeType(ctx), g.Adaptor.GetScopeID(ctx))
	if err != nil {
		logrus.Infof("failed to get permission, (%v)", err)
		return nil, errors.Errorf("failed to Access permission")
	}

	if len(members) == 0 {
		if g.Adaptor.GetScopeType(ctx) == apistructs.SysScope {
			return nil, nil
		}
		isPublic, err := g.Adaptor.CheckPublicScope(g.Adaptor.GetUserID(ctx), g.Adaptor.GetScopeType(ctx), g.Adaptor.GetScopeID(ctx))
		if err != nil || !isPublic {
			return nil, err
		}
		roles = append(roles, types.RoleGuest)
	}

	for _, member := range members {
		if member.ResourceKey == apistructs.RoleResourceKey {
			roles = append(roles, member.ResourceValue)
		}
	}
	if len(roles) == 0 {
		logrus.Warningf("nil role, scope: %s, scopeID: %d, memberInfo: %v", g.Adaptor.GetScopeType(ctx), g.Adaptor.GetScopeID(ctx), members)
		// members without resource value, give guest permission if scope is public
		isPublic, err := g.Adaptor.CheckPublicScope(g.Adaptor.GetUserID(ctx), g.Adaptor.GetScopeType(ctx), g.Adaptor.GetScopeID(ctx))
		if err != nil || !isPublic {
			return nil, err
		}
		roles = append(roles, types.RoleGuest)
	}
	return roles, nil
}

type AppPermissionProcess struct {
	Adaptor *PermissionAdaptor
}

func (a AppPermissionProcess) permissionList(ctx context.Context) (*apistructs.PermissionList, error) {

	roles, err := a.GetAllRoles(ctx)
	if err != nil {
		return nil, err
	}

	return RolePermissionProcess{
		Adaptor: a.Adaptor,
		roles:   roles,
	}.permissionList(ctx)
}

func (d AppPermissionProcess) check(ctx context.Context) (bool, error) {
	roles, err := d.GetAllRoles(ctx)
	if err != nil {
		return false, err
	}

	return RolePermissionProcess{
		Adaptor: d.Adaptor,
		roles:   roles,
	}.check(ctx)
}

func (a AppPermissionProcess) GetAllRoles(ctx context.Context) ([]string, error) {
	dto, err := a.Adaptor.Db.GetApplicationByID(a.Adaptor.GetScopeID(ctx))
	if err != nil {
		return nil, err
	}
	// get all project-level application
	_, data, err := a.Adaptor.Db.GetApplicationsByIDs(&dto.OrgID, &dto.ProjectID, nil, &apistructs.ApplicationListRequest{
		ProjectID: uint64(dto.ProjectID),
		PageSize:  9999,
		PageNo:    1,
		Mode:      string(apistructs.ApplicationModeProjectService),
	})
	if err != nil {
		return nil, err
	}

	var appIDList []int64
	for _, app := range data {
		appIDList = append(appIDList, app.ID)
	}
	appIDList = append(appIDList, a.Adaptor.GetScopeID(ctx))

	var group sync.WaitGroup
	var roles []string
	var lock sync.Mutex

	for _, app := range appIDList {
		group.Add(1)
		go func(appID int64) {
			defer group.Done()

			var queryCtx = context.Background()
			queryCtx = a.Adaptor.SetScopeID(queryCtx, appID)
			queryCtx = a.Adaptor.SetScopeType(queryCtx, a.Adaptor.GetScopeType(ctx))
			queryCtx = a.Adaptor.SetUserID(queryCtx, a.Adaptor.GetUserID(ctx))

			result, err := GetByUserAndScopePermissionProcess{Adaptor: a.Adaptor}.GetAllRoles(queryCtx)
			if err != nil {
				return
			}
			if len(result) <= 0 {
				return
			}

			lock.Lock()
			roles = append(roles, result...)
			lock.Unlock()

		}(app)
	}

	group.Wait()

	return roles, nil
}
