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

package branchrule

import (
	"context"
	"fmt"
	"strings"

	dfrpb "github.com/erda-project/erda-proto-go/dop/devflowrule/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/model"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/ucauth"
)

type BranchRule struct {
	db  *dao.DBClient
	uc  *ucauth.UCClient
	bdl *bundle.Bundle

	devFlowRule dfrpb.DevFlowRuleServiceServer
}

type Option func(*BranchRule)

func New(options ...Option) *BranchRule {
	o := &BranchRule{}
	for _, op := range options {
		op(o)
	}
	return o
}

// WithDBClient 配置 db client
func WithDBClient(db *dao.DBClient) Option {
	return func(o *BranchRule) {
		o.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(o *BranchRule) {
		o.bdl = bdl
	}
}

func WithDevFlowRule(svc dfrpb.DevFlowRuleServiceServer) Option {
	return func(o *BranchRule) {
		o.devFlowRule = svc
	}
}

func (branchRule *BranchRule) Count(scopeType apistructs.ScopeType) (int64, error) {
	return branchRule.db.GetBranchRulesCount(scopeType)
}

func (branchRule *BranchRule) Query(scopeType apistructs.ScopeType, scopeID int64) ([]*apistructs.BranchRule, error) {
	if scopeType == apistructs.ProjectScope {
		return branchRule.GetBranchRuleFromDevFlowRule(uint64(scopeID))
	}

	rules, err := branchRule.db.QueryBranchRules(scopeType, scopeID)
	if err != nil {
		return nil, err
	}
	var result []*apistructs.BranchRule
	for _, rule := range rules {
		result = append(result, rule.ToApiData())
	}
	return result, nil
}

func (branchRule *BranchRule) GetBranchRuleFromDevFlowRule(projectID uint64) ([]*apistructs.BranchRule, error) {
	devFlowRuleRsp, err := branchRule.devFlowRule.GetDevFlowRulesByProjectID(apis.WithInternalClientContext(context.Background(), "bundle"), &dfrpb.GetDevFlowRuleRequest{ProjectID: projectID})
	if err != nil {
		return nil, err
	}
	flows := make([]apistructs.Flow, 0, len(devFlowRuleRsp.Data.Flows))
	for _, v := range devFlowRuleRsp.Data.Flows {
		hints := make([]apistructs.StartWorkflowHint, 0, len(v.StartWorkflowHints))
		for _, hint := range v.StartWorkflowHints {
			hints = append(hints, apistructs.StartWorkflowHint{
				Place:            hint.Place,
				ChangeBranchRule: hint.ChangeBranchRule,
			})
		}
		flows = append(flows, apistructs.Flow{
			Name:               v.Name,
			FlowType:           v.FlowType,
			TargetBranch:       v.TargetBranch,
			ChangeFromBranch:   v.ChangeFromBranch,
			ChangeBranch:       v.ChangeBranch,
			EnableAutoMerge:    v.EnableAutoMerge,
			AutoMergeBranch:    v.AutoMergeBranch,
			Artifact:           v.Artifact,
			Environment:        v.Environment,
			StartWorkflowHints: hints,
		})
	}
	devFlowRule := apistructs.DevFlowRule{
		ID:          devFlowRuleRsp.Data.ID,
		Flows:       flows,
		OrgID:       devFlowRuleRsp.Data.OrgID,
		OrgName:     devFlowRuleRsp.Data.OrgName,
		ProjectID:   devFlowRuleRsp.Data.ProjectID,
		ProjectName: devFlowRuleRsp.Data.ProjectName,
	}
	return devFlowRule.MakeBranchRules()
}

func (branchRule *BranchRule) GetAllProjectRulesMap() (map[int64][]*apistructs.BranchRule, error) {
	rules, err := branchRule.db.QueryBranchRulesByScope(apistructs.ProjectScope)
	if err != nil {
		return nil, err
	}
	result := map[int64][]*apistructs.BranchRule{}
	for _, rule := range rules {
		if result[rule.ScopeID] != nil {
			result[rule.ScopeID] = append(result[rule.ScopeID], rule.ToApiData())
		} else {
			result[rule.ScopeID] = []*apistructs.BranchRule{rule.ToApiData()}
		}
	}
	return result, nil
}

func (branchRule *BranchRule) Get(ID int64) (*apistructs.BranchRule, error) {
	rule, err := branchRule.db.GetBranchRule(ID)
	if err != nil {
		return nil, err
	}
	return rule.ToApiData(), nil
}

func (branchRule *BranchRule) Update(request apistructs.UpdateBranchRuleRequest) (*apistructs.BranchRule, error) {
	var rule model.BranchRule
	err := branchRule.db.Model(&model.BranchRule{}).Where("id=?", request.ID).Find(&rule).Error
	if err != nil {
		return nil, err
	}
	rule.Rule = request.Rule
	rule.Desc = request.Desc
	rule.IsTriggerPipeline = request.IsTriggerPipeline
	rule.IsProtect = request.IsProtect
	rule.Workspace = request.Workspace
	rule.ArtifactWorkspace = request.ArtifactWorkspace
	rule.NeedApproval = request.NeedApproval
	err = branchRule.CheckRuleValid(&rule)
	if err != nil {
		return nil, err
	}
	err = branchRule.db.Save(&rule).Error
	if err != nil {
		return nil, err
	}
	return rule.ToApiData(), nil
}

func (branchRule *BranchRule) Create(request apistructs.CreateBranchRuleRequest) (*apistructs.BranchRule, error) {
	rule := model.BranchRule{
		ScopeType:         request.ScopeType,
		ScopeID:           request.ScopeID,
		Rule:              request.Rule,
		IsProtect:         request.IsProtect,
		IsTriggerPipeline: request.IsTriggerPipeline,
		Workspace:         request.Workspace,
		ArtifactWorkspace: request.ArtifactWorkspace,
		NeedApproval:      request.NeedApproval,
		Desc:              request.Desc,
	}
	err := branchRule.CheckRuleValid(&rule)
	if err != nil {
		return nil, err
	}
	err = branchRule.db.CreateBranchRule(&rule)
	if err != nil {
		return nil, err
	}
	return rule.ToApiData(), nil
}

func (branchRule *BranchRule) Delete(id int64) (*apistructs.BranchRule, error) {
	var rule model.BranchRule
	err := branchRule.db.Model(&model.BranchRule{}).Where("id=?", id).Find(&rule).Error
	if err != nil {
		return nil, err
	}
	err = branchRule.db.DeleteBranchRule(id)
	if err != nil {
		return nil, err
	}
	return rule.ToApiData(), nil
}

func isRuleMatch(sourceRule, targetRule string) bool {
	if strings.HasSuffix(sourceRule, "*") {
		//通配符匹配
		sourceRule = strings.TrimSuffix(sourceRule, "*")
		if strings.HasPrefix(targetRule, sourceRule) {
			return true
		}
	} else {
		//完整路径匹配
		if sourceRule == targetRule {
			return true
		}
	}
	return false
}

func (branchRule *BranchRule) CheckRuleValid(newBranchRule *model.BranchRule) error {
	// check duplicate
	currentRules, err := branchRule.Query(newBranchRule.ScopeType, newBranchRule.ScopeID)
	if err != nil {
		return err
	}
	for _, currentRule := range currentRules {
		if currentRule.ID == int64(newBranchRule.ID) {
			continue
		}
		rules := strings.Split(currentRule.Rule, ",")
		newRules := strings.Split(newBranchRule.Rule, ",")
		for _, rule := range rules {
			for _, checkRule := range newRules {
				if isRuleMatch(rule, checkRule) || isRuleMatch(checkRule, rule) {
					return fmt.Errorf("duplicate branch rule %s", checkRule)
				}
			}
		}
	}
	return nil
}

func (branchRule *BranchRule) GetAllValidBranchWorkspaces(appID int64, userID string) ([]*apistructs.ValidBranch, error) {
	var result []*apistructs.ValidBranch

	app, err := branchRule.bdl.GetApp(uint64(appID))
	if err != nil {
		return nil, err
	}
	rules, err := branchRule.Query(apistructs.ProjectScope, int64(app.ProjectID))
	if err != nil {
		return nil, err
	}
	appRules, err := branchRule.Query(apistructs.AppScope, appID)
	if err != nil {
		return nil, err
	}
	repoStats, err := branchRule.bdl.GetGittarStats(int64(app.ID), userID)
	if err != nil {
		return nil, err
	}
	// project rule取部署信息 app rule取保护分支
	for _, branch := range repoStats.Branches {
		branchRule := diceworkspace.GetValidBranchByGitReference(branch, rules)
		branchRule.IsProtect = diceworkspace.GetValidBranchByGitReference(branch, appRules).IsProtect
		result = append(result, branchRule)
	}

	for _, tag := range repoStats.Tags {
		branchRule := diceworkspace.GetValidBranchByGitReference(tag, rules)
		branchRule.IsProtect = diceworkspace.GetValidBranchByGitReference(tag, appRules).IsProtect
		result = append(result, branchRule)
	}

	return result, nil
}

func (branchRule *BranchRule) InitProjectRules(projectID int64) error {
	rules := []model.BranchRule{
		{
			ScopeType:         apistructs.ProjectScope,
			ScopeID:           projectID,
			Rule:              "master,support/*",
			Desc:              "PROD",
			Workspace:         "PROD",
			ArtifactWorkspace: "PROD",
		},
		{
			ScopeType:         apistructs.ProjectScope,
			ScopeID:           projectID,
			Rule:              "release/*,hotfix/*",
			Desc:              "STAGING",
			Workspace:         "STAGING",
			ArtifactWorkspace: "STAGING",
		},
		{
			ScopeType:         apistructs.ProjectScope,
			ScopeID:           projectID,
			Rule:              "develop",
			Workspace:         "TEST",
			ArtifactWorkspace: "TEST",
		},
		{
			ScopeType:         apistructs.ProjectScope,
			ScopeID:           projectID,
			Rule:              "feature/*",
			Workspace:         "DEV",
			ArtifactWorkspace: "DEV",
		},
	}
	for _, rule := range rules {
		err := branchRule.db.Create(&rule).Error
		if err != nil {
			return err
		}
	}
	return nil
}

func (branchRule *BranchRule) InitAppRules(appID int64) error {
	rules := []model.BranchRule{
		{
			ScopeType: apistructs.AppScope,
			ScopeID:   appID,
			Rule:      "master,support/*,release/*,hotfix/*",
			IsProtect: true,
		},
	}
	for _, rule := range rules {
		err := branchRule.db.Create(&rule).Error
		if err != nil {
			return err
		}
	}
	return nil
}

func (branchRule *BranchRule) InitAppRulesWithData(appID int64, rules []*apistructs.BranchRule) error {
	for _, rule := range rules {
		// 只迁移配置了保护分支的
		if rule.IsProtect {
			rule.ScopeID = appID
			rule.ScopeType = apistructs.AppScope
			err := branchRule.db.Create(&model.BranchRule{
				ScopeType: apistructs.AppScope,
				ScopeID:   appID,
				Rule:      rule.Rule,
				IsProtect: rule.IsProtect,
				Desc:      rule.Desc,
			}).Error
			if err != nil {
				return err
			}
		}
	}
	return nil
}
