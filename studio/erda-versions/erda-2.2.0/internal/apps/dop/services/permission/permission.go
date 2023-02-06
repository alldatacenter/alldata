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
	"errors"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/strutil"
)

type Permission struct {
	bdl        *bundle.Bundle
	branchRule *branchrule.BranchRule
}

type Option func(*Permission)

// New Permission
func New(options ...Option) *Permission {
	p := &Permission{}
	for _, op := range options {
		op(p)
	}

	return p
}

func WithBundle(bdl *bundle.Bundle) Option {
	return func(p *Permission) {
		p.bdl = bdl
	}
}

func WithBranchRule(branchRule *branchrule.BranchRule) Option {
	return func(p *Permission) {
		p.branchRule = branchRule
	}
}

func (p *Permission) check(identityInfo apistructs.IdentityInfo, req *apistructs.PermissionCheckRequest) error {
	// 内部调用，无需鉴权
	if identityInfo.InternalClient != "" {
		return nil
	}

	// 鉴权
	req.UserID = identityInfo.UserID
	respData, err := p.bdl.CheckPermission(req)
	if err != nil {
		return apierrors.ErrCheckPermission.InternalError(err)
	}
	if !respData.Access {
		return apierrors.ErrCheckPermission.AccessDenied()
	}

	return nil
}

// CheckAppAction 校验用户在应用下是否有 ${action} 权限
func (p *Permission) CheckAppAction(identityInfo apistructs.IdentityInfo, appID uint64, action string) error {
	return p.check(identityInfo, &apistructs.PermissionCheckRequest{
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: apistructs.AppResource,
		Action:   action,
	})
}

// CheckAppConfig 校验用户在应用配置管理下是否有 ${action} 权限
func (p *Permission) CheckAppConfig(identityInfo apistructs.IdentityInfo, appID uint64, action string) error {
	return p.check(identityInfo, &apistructs.PermissionCheckRequest{
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: apistructs.ConfigResource,
		Action:   action,
	})
}

// CheckBranch 校验用户在 应用对应分支 下是否有 ${action} 权限
func (p *Permission) CheckBranchAction(identityInfo apistructs.IdentityInfo, appIDStr, branch, action string) error {
	if identityInfo.IsInternalClient() {
		return nil
	}
	// appID
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCheckPermission.InvalidParameter(err)
	}
	app, err := p.bdl.GetApp(appID)
	if err != nil {
		return apierrors.ErrCheckPermission.InternalError(err)
	}
	rules, err := p.branchRule.Query(apistructs.ProjectScope, int64(app.ProjectID))
	if err != nil {
		return apierrors.ErrCheckPermission.InternalError(err)
	}
	validBranch := diceworkspace.GetValidBranchByGitReference(branch, rules)
	if validBranch.Workspace == "" {
		return apierrors.ErrCheckPermission.InternalError(errors.New("no branch rule match"))
	}

	return p.check(identityInfo, &apistructs.PermissionCheckRequest{
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: validBranch.GetPermissionResource(),
		Action:   action,
	})
}

func (p *Permission) CheckRuntimeBranch(identityInfo apistructs.IdentityInfo, appID uint64, branch string, action string) error {
	app, err := p.bdl.GetApp(appID)
	if err != nil {
		return apierrors.ErrCheckPermission.InternalError(err)
	}
	rules, err := p.branchRule.Query(apistructs.ProjectScope, int64(app.ProjectID))
	if err != nil {
		return apierrors.ErrCheckPermission.InternalError(err)
	}
	validBranch := diceworkspace.GetValidBranchByGitReference(branch, rules)

	perm, err := p.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   identityInfo.UserID,
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: "runtime-" + strutil.ToLower(validBranch.Workspace),
		Action:   action,
	})
	if err != nil {
		return apierrors.ErrCheckPermission.InternalError(err)
	}
	if !perm.Access {
		return apierrors.ErrCheckPermission.AccessDenied()
	}

	return nil
}
