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

package permissionsvc

import (
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/gitflowutil"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
)

type PermissionSvc struct {
	bdl *bundle.Bundle
}

func New(bdl *bundle.Bundle) *PermissionSvc {
	s := PermissionSvc{}
	s.bdl = bdl
	return &s
}

func (s *PermissionSvc) Check(identityInfo apistructs.IdentityInfo, req *apistructs.PermissionCheckRequest) error {
	// 内部调用，无需鉴权
	if identityInfo.InternalClient != "" {
		return nil
	}

	// 鉴权
	req.UserID = identityInfo.UserID
	respData, err := s.bdl.CheckPermission(req)
	if err != nil {
		return apierrors.ErrCheckPermission.InternalError(err)
	}
	if !respData.Access {
		return apierrors.ErrCheckPermission.AccessDenied()
	}

	return nil
}

func (s *PermissionSvc) CheckInternalClient(identityInfo apistructs.IdentityInfo) error {
	return s.Check(identityInfo, &apistructs.PermissionCheckRequest{
		UserID:   identityInfo.UserID,    // just check internal user id
		Scope:    apistructs.OrgScope,    // any valid scope is ok
		Resource: apistructs.OrgResource, // any valid resource is ok
		Action:   apistructs.GetAction,   // any valid action is ok
	})
}

// CheckApp 校验用户在 应用 下是否有 ${action} 权限
func (s *PermissionSvc) CheckApp(identityInfo apistructs.IdentityInfo, appID uint64, action string) error {
	return s.Check(identityInfo, &apistructs.PermissionCheckRequest{
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: apistructs.AppResource,
		Action:   action,
	})
}

// CheckBranch 校验用户在 应用对应分支 下是否有 ${action} 权限
func (s *PermissionSvc) CheckBranch(identityInfo apistructs.IdentityInfo, appIDStr, branch, action string) error {
	if identityInfo.IsInternalClient() {
		return nil
	}
	// 处理分支，获取分支前缀
	branchPrefix, err := gitflowutil.GetReferencePrefix(branch)
	if err != nil {
		return apierrors.ErrCheckPermission.InvalidParameter(err)
	}
	// appID
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCheckPermission.InvalidParameter(err)
	}
	return s.Check(identityInfo, &apistructs.PermissionCheckRequest{
		Scope:    apistructs.AppScope,
		ScopeID:  appID,
		Resource: branchPrefix,
		Action:   action,
	})
}
