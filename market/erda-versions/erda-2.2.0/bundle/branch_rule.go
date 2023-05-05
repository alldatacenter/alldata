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

package bundle

import (
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// GetProjectBranchRules 查询项目分支规则
func (b *Bundle) GetProjectBranchRules(projectId uint64) ([]*apistructs.BranchRule, error) {
	devFlowRule, err := b.GetDevFlowRule(projectId)
	if err != nil {
		return nil, err
	}

	return devFlowRule.MakeBranchRules()
}

// GetAppBranchRules 查询应用分支规则
func (b *Bundle) GetAppBranchRules(appId uint64) ([]*apistructs.BranchRule, error) {
	return b.GetBranchRules(apistructs.AppScope, appId)
}

// GetBranchRules 查询分支规则
func (b *Bundle) GetBranchRules(scopeType apistructs.ScopeType, scopeID uint64) ([]*apistructs.BranchRule, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.QueryBranchRuleResponse
	resp, err := hc.Get(host).Path("/api/branch-rules").
		Param("scopeId", strconv.FormatUint(scopeID, 10)).
		Param("scopeType", string(scopeType)).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return fetchResp.Data, nil
}

func (b *Bundle) GetAllValidBranchWorkspace(appId uint64, userID string) ([]apistructs.ValidBranch, error) {
	host, err := b.urls.DOP()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.PipelineAppAllValidBranchWorkspaceResponse
	resp, err := hc.Get(host).Path("/api/branch-rules/actions/app-all-valid-branch-workspaces").
		Param("appId", strconv.FormatUint(appId, 10)).
		Header(httputil.UserHeader, userID).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return fetchResp.Data, nil
}

func (b *Bundle) GetBranchWorkspaceConfig(appId uint64, branch string) (*apistructs.ValidBranch, error) {
	app, err := b.GetApp(appId)
	if err != nil {
		return nil, err
	}
	return b.GetBranchWorkspaceConfigByProject(app.ProjectID, branch)
}

func (b *Bundle) GetPermissionByGitReference(branch *apistructs.ValidBranch) string {
	resource := "normalBranch"
	if branch.IsProtect {
		resource = "protectedBranch"
	}
	return resource
}

func (b *Bundle) GetBranchWorkspaceConfigByProject(projectID uint64, branch string) (*apistructs.ValidBranch, error) {
	rules, err := b.GetProjectBranchRules(projectID)
	if err != nil {
		return nil, err
	}
	validBranch := diceworkspace.GetValidBranchByGitReference(branch, rules)
	return validBranch, nil
}
