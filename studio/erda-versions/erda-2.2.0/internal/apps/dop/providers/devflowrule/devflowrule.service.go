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

package devflowrule

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/erda-project/erda-proto-go/dop/devflowrule/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/devflowrule/db"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/common/apis"
)

const resource = "devFlowRule"

type GetFlowByRuleRequest struct {
	ProjectID    uint64
	FlowType     string
	ChangeBranch string
	TargetBranch string
}

type Interface interface {
	pb.DevFlowRuleServiceServer
	GetFlowByRule(context.Context, GetFlowByRuleRequest) (*pb.Flow, error)
}

func (p *provider) CreateDevFlowRule(ctx context.Context, request *pb.CreateDevFlowRuleRequest) (*pb.CreateDevFlowRuleResponse, error) {
	project, err := p.bundle.GetProject(request.ProjectID)
	if err != nil {
		return nil, apierrors.ErrCreateDevFlowRule.InternalError(err)
	}
	org, err := p.bundle.GetOrg(project.OrgID)
	if err != nil {
		return nil, apierrors.ErrCreateDevFlowRule.InternalError(err)
	}

	flows := p.InitFlows()
	b, err := json.Marshal(&flows)
	if err != nil {
		return nil, apierrors.ErrCreateDevFlowRule.InternalError(err)
	}
	devFlow := db.DevFlowRule{
		Scope: db.Scope{
			OrgID:       org.ID,
			OrgName:     org.Name,
			ProjectID:   project.ID,
			ProjectName: project.Name,
		},
		Operator: db.Operator{
			Creator: request.UserID,
			Updater: request.UserID,
		},
		Flows: b,
	}
	err = p.dbClient.CreateDevFlowRule(&devFlow)
	if err != nil {
		return nil, apierrors.ErrCreateDevFlowRule.InternalError(err)
	}
	return &pb.CreateDevFlowRuleResponse{Data: devFlow.Convert()}, nil
}

func (p *provider) InitFlows() db.Flows {
	return db.Flows{
		{
			Name:             "DEV",
			FlowType:         "multi_branch",
			TargetBranch:     "develop",
			ChangeFromBranch: "develop",
			ChangeBranch:     "feature/*,bugfix/*",
			EnableAutoMerge:  false,
			AutoMergeBranch:  "next/develop",
			Artifact:         "alpha",
			Environment:      "DEV",
			StartWorkflowHints: []db.StartWorkflowHint{
				{
					Place:            "TASK",
					ChangeBranchRule: "feature/*",
				},
				{
					Place:            "BUG",
					ChangeBranchRule: "bugfix/*",
				},
			},
		},
		{
			Name:               "TEST",
			FlowType:           "single_branch",
			TargetBranch:       "develop",
			ChangeFromBranch:   "",
			ChangeBranch:       "",
			EnableAutoMerge:    false,
			AutoMergeBranch:    "",
			Artifact:           "beta",
			Environment:        "TEST",
			StartWorkflowHints: nil,
		},
		{
			Name:               "STAGING",
			FlowType:           "multi_branch",
			TargetBranch:       "master",
			ChangeFromBranch:   "develop",
			ChangeBranch:       "release/*",
			EnableAutoMerge:    false,
			AutoMergeBranch:    "",
			Artifact:           "rc",
			Environment:        "STAGING",
			StartWorkflowHints: nil,
		},
		{
			Name:               "PROD",
			FlowType:           "single_branch",
			TargetBranch:       "master",
			ChangeFromBranch:   "",
			ChangeBranch:       "",
			EnableAutoMerge:    false,
			AutoMergeBranch:    "",
			Artifact:           "stable",
			Environment:        "PROD",
			StartWorkflowHints: nil,
		},
	}

}

func (p *provider) UpdateDevFlowRule(ctx context.Context, request *pb.UpdateDevFlowRuleRequest) (*pb.UpdateDevFlowRuleResponse, error) {
	if err := request.Validate(); err != nil {
		return nil, apierrors.ErrUpdateDevFlowRule.InvalidParameter(err)
	}

	devFlow, err := p.dbClient.GetDevFlowRule(request.ID)
	if err != nil {
		return nil, apierrors.ErrUpdateDevFlowRule.InternalError(err)
	}

	access, err := p.bundle.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   apis.GetUserID(ctx),
		Scope:    apistructs.ProjectScope,
		ScopeID:  devFlow.ProjectID,
		Resource: resource,
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return nil, apierrors.ErrUpdateDevFlowRule.InternalError(err)
	}
	if !access.Access {
		return nil, apierrors.ErrUpdateDevFlowRule.AccessDenied()
	}

	if err = p.CheckFlow(request.Flows); err != nil {
		return nil, apierrors.ErrUpdateDevFlowRule.InternalError(err)
	}

	b, err := json.Marshal(request.Flows)
	if err != nil {
		return nil, apierrors.ErrUpdateDevFlowRule.InternalError(err)
	}
	devFlow.Flows = b
	devFlow.Operator.Updater = apis.GetUserID(ctx)
	if err = p.dbClient.UpdateDevFlowRule(devFlow); err != nil {
		return nil, apierrors.ErrUpdateDevFlowRule.InternalError(err)
	}

	return &pb.UpdateDevFlowRuleResponse{Data: devFlow.Convert()}, nil
}

func (p *provider) CheckFlow(flows []*pb.Flow) error {
	nameMap := make(map[string]struct{})
	for _, v := range flows {
		if _, ok := nameMap[v.Name]; ok {
			return fmt.Errorf("the name %s is duplicate", v.Name)
		} else {
			nameMap[v.Name] = struct{}{}
		}
	}
	return nil
}

func (p *provider) GetDevFlowRulesByProjectID(ctx context.Context, request *pb.GetDevFlowRuleRequest) (*pb.GetDevFlowRuleResponse, error) {
	if !apis.IsInternalClient(ctx) {
		access, err := p.bundle.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   apis.GetUserID(ctx),
			Scope:    apistructs.ProjectScope,
			ScopeID:  request.ProjectID,
			Resource: resource,
			Action:   apistructs.ListAction,
		})
		if err != nil {
			return nil, apierrors.ErrGetDevFlowRule.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrGetDevFlowRule.AccessDenied()
		}
	}

	wfs, err := p.dbClient.GetDevFlowRuleByProjectID(request.ProjectID)
	if err != nil {
		return nil, apierrors.ErrGetDevFlowRule.InternalError(err)
	}

	return &pb.GetDevFlowRuleResponse{Data: wfs.Convert()}, nil
}

func (p *provider) DeleteDevFlowRule(ctx context.Context, request *pb.DeleteDevFlowRuleRequest) (*pb.DeleteDevFlowRuleResponse, error) {
	if err := p.dbClient.DeleteDevFlowRuleByProjectID(request.ProjectID); err != nil {
		return nil, apierrors.ErrDeleteDevFlowRule.InternalError(err)
	}
	return &pb.DeleteDevFlowRuleResponse{}, nil
}

func (p *provider) GetFlowByRule(ctx context.Context, request GetFlowByRuleRequest) (*pb.Flow, error) {
	wfs, err := p.dbClient.GetDevFlowRuleByProjectID(request.ProjectID)
	if err != nil {
		return nil, err
	}
	flows := make([]db.Flow, 0)
	if err = json.Unmarshal(wfs.Flows, &flows); err != nil {
		return nil, err
	}
	for _, v := range flows {
		targetBranches := strings.Split(v.TargetBranch, ",")
		changeBranches := strings.Split(v.ChangeBranch, ",")
		if request.FlowType == v.FlowType &&
			diceworkspace.IsRefPatternMatch(request.TargetBranch, targetBranches) &&
			diceworkspace.IsRefPatternMatch(request.ChangeBranch, changeBranches) {
			return v.Convert(), nil
		}
	}
	return nil, nil
}
