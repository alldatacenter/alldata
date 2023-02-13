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

package core

import (
	"context"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

func (i *IssueService) CreateIssueState(ctx context.Context, req *pb.CreateIssueStateRequest) (*pb.CreateIssueStateResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrCreateIssueState.NotLogin()
	}
	req.IdentityInfo = identityInfo

	states, err := i.db.GetIssuesStatesByProjectID(req.ProjectID, req.IssueType)
	var maxIndex int64 = -1
	for _, v := range states {
		if v.Index > maxIndex {
			maxIndex = v.Index
		}
	}
	if err != nil {
		return nil, err
	}
	createState := &dao.IssueState{
		ProjectID: req.ProjectID,
		IssueType: req.IssueType,
		Name:      req.StateName,
		Belong:    req.StateBelong,
		Index:     maxIndex + 1,
		Role:      "Ops,Dev,QA,Owner,Lead",
	}
	if err = i.db.CreateIssuesState(createState); err != nil {
		return nil, err
	}

	project, err := i.bdl.GetProject(req.ProjectID)
	if err != nil {
		return nil, apierrors.ErrCreateIssueState.InternalError(err)
	}
	now := strconv.FormatInt(time.Now().Unix(), 10)
	audit := apistructs.Audit{
		UserID:       identityInfo.UserID,
		ScopeType:    apistructs.ProjectScope,
		ScopeID:      req.ProjectID,
		OrgID:        project.OrgID,
		ProjectID:    req.ProjectID,
		Result:       "success",
		StartTime:    now,
		EndTime:      now,
		TemplateName: apistructs.CreateIssueStateTemplate,
		Context: map[string]interface{}{
			"projectName": project.Name,
			"issueType":   req.IssueType,
			"stateName":   createState.Name,
		},
	}
	if err := i.bdl.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: audit}); err != nil {
		return nil, apierrors.ErrCreateIssueState.InternalError(err)
	}

	return &pb.CreateIssueStateResponse{Data: createState.ID}, nil
}

func (i *IssueService) DeleteIssueState(ctx context.Context, req *pb.DeleteIssueStateRequest) (*pb.DeleteIssueStateResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrDeleteIssueState.NotLogin()
	}
	req.IdentityInfo = identityInfo

	state, err := i.db.GetIssueStateByID(req.Id)
	if err != nil {
		return nil, err
	}
	status := &pb.IssueStatus{
		ProjectID:   state.ProjectID,
		IssueType:   state.IssueType,
		StateID:     int64(state.ID),
		StateName:   state.Name,
		StateBelong: state.Belong,
		Index:       state.Index,
	}
	// 如果有事件是该状态则不可删除
	_, err = i.db.GetIssueByState(req.Id)
	if err != nil {
		if !gorm.IsRecordNotFoundError(err) {
			return nil, err
		}
	} else {
		return nil, apierrors.ErrDeleteIssueState.InvalidState("有事件处于该状态,不可删除")
	}
	// 删除该状态的关联
	if err := i.db.DeleteIssuesStateRelationByStartID(req.Id); err != nil {
		return nil, err
	}
	// 删除状态
	if err := i.db.DeleteIssuesState(req.Id); err != nil {
		return nil, err
	}
	return &pb.DeleteIssueStateResponse{Data: status}, nil
}

func (i *IssueService) UpdateIssueStateRelation(ctx context.Context, req *pb.UpdateIssueStateRelationRequest) (*pb.UpdateIssueStateRelationResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrUpdateIssueStateRelation.NotLogin()
	}
	req.IdentityInfo = identityInfo

	for _, v := range req.Data {
		sta := &dao.IssueState{
			BaseModel: dbengine.BaseModel{
				ID: uint64(v.StateID),
			},
			ProjectID: v.ProjectID,
			IssueType: v.IssueType,
			Name:      v.StateName,
			Belong:    v.StateBelong,
			Index:     v.Index,
			Role:      "Ops,Dev,QA,Owner,Lead",
		}
		if err := i.db.UpdateIssueState(sta); err != nil {
			return nil, err
		}
	}
	var updateIssueStateRelations []dao.IssueStateRelation
	for _, st := range req.Data {
		for _, endID := range st.StateRelation {
			if endID == st.StateID {
				continue
			}
			updateIssueStateRelations = append(updateIssueStateRelations, dao.IssueStateRelation{
				ProjectID:    req.ProjectID,
				IssueType:    st.IssueType,
				StartStateID: st.StateID,
				EndStateID:   endID,
			})
		}
	}
	if len(updateIssueStateRelations) == 0 {
		return nil, apierrors.ErrUpdateIssueState.MissingParameter("工作流不能为空")
	}
	if err := i.db.UpdateIssueStateRelations(req.ProjectID, req.Data[0].IssueType, updateIssueStateRelations); err != nil {
		return nil, err
	}

	issueStateRelations, err := i.GetIssueStatesRelations(&pb.GetIssueStateRelationRequest{ProjectID: uint64(req.ProjectID), IssueType: req.Data[0].IssueType})
	if err != nil {
		return nil, err
	}
	return &pb.UpdateIssueStateRelationResponse{Data: issueStateRelations}, nil
}

func (i *IssueService) GetIssueStates(ctx context.Context, req *pb.GetIssueStatesRequest) (*pb.GetIssueStatesResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrGetIssueState.NotLogin()
	}
	req.IdentityInfo = identityInfo
	var states []*pb.IssueTypeState

	st, err := i.db.GetIssuesStatesByProjectID(req.ProjectID, "")
	if err != nil {
		return nil, err
	}
	states = append(states, &pb.IssueTypeState{
		IssueType: pb.IssueTypeEnum_TASK.String(),
	})
	states = append(states, &pb.IssueTypeState{
		IssueType: pb.IssueTypeEnum_REQUIREMENT.String(),
	})
	states = append(states, &pb.IssueTypeState{
		IssueType: pb.IssueTypeEnum_BUG.String(),
	})
	states = append(states, &pb.IssueTypeState{
		IssueType: pb.IssueTypeEnum_EPIC.String(),
	})
	states = append(states, &pb.IssueTypeState{
		IssueType: pb.IssueTypeEnum_TICKET.String(),
	})
	for _, v := range st {
		if v.IssueType == pb.IssueTypeEnum_TASK.String() {
			states[0].State = append(states[0].State, v.Name)
		} else if v.IssueType == pb.IssueTypeEnum_REQUIREMENT.String() {
			states[1].State = append(states[1].State, v.Name)
		} else if v.IssueType == pb.IssueTypeEnum_BUG.String() {
			states[2].State = append(states[2].State, v.Name)
		} else if v.IssueType == pb.IssueTypeEnum_EPIC.String() {
			states[3].State = append(states[3].State, v.Name)
		} else if v.IssueType == pb.IssueTypeEnum_TICKET.String() {
			states[4].State = append(states[4].State, v.Name)
		}
	}
	return &pb.GetIssueStatesResponse{Data: states}, nil
}

func (i *IssueService) GetIssueStateRelation(ctx context.Context, req *pb.GetIssueStateRelationRequest) (*pb.GetIssueStateRelationResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrGetIssueState.NotLogin()
	}
	issueStateRelations, err := i.GetIssueStatesRelations(req)
	if err != nil {
		return nil, apierrors.ErrGetIssueStateRelation.InternalError(err)
	}
	return &pb.GetIssueStateRelationResponse{Data: issueStateRelations}, nil
}

// GetIssueStatesRelations 获取工作流
func (i *IssueService) GetIssueStatesRelations(req *pb.GetIssueStateRelationRequest) ([]*pb.IssueStateRelation, error) {
	issueRelations, err := i.db.GetIssuesStateRelations(req.ProjectID, req.IssueType)
	if err != nil {
		return nil, err
	}
	var response []*pb.IssueStateRelation
	var index int = -1
	for _, v := range issueRelations {
		// 按照开始状态归类
		if index == -1 || v.ID != response[index].StateID {
			index++
			response = append(response, &pb.IssueStateRelation{
				ProjectID:   v.ProjectID,
				IssueType:   v.IssueType,
				StateID:     v.ID,
				StateName:   v.Name,
				StateBelong: v.Belong,
				Index:       v.Index,
			})
		}
		if v.EndStateID != 0 {
			response[index].StateRelation = append(response[index].StateRelation, v.EndStateID)
		}
	}
	return response, err
}
