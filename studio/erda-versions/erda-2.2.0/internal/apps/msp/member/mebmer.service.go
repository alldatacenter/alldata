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

package member

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-proto-go/msp/member/pb"
	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
)

type memberService struct {
	p *provider
}

func (m memberService) GetProjectIdByScopeId(scopeId string) (string, error) {
	projectId := ""
	var err error
	projectId, err = m.p.monitorDB.SelectProjectIdByTk(scopeId)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			instance, err := m.p.mspTenantDB.QueryTenant(scopeId)
			if err != nil {
				return "", err
			}
			if instance != nil {
				projectId = instance.RelatedProjectId
			}
		} else {
			return projectId, err
		}
	}
	return projectId, nil
}

func (m memberService) ListMemberRoles(ctx context.Context, request *pb.ListMemberRolesRequest) (*pb.ListMemberRolesResponse, error) {
	projectId, err := m.GetProjectIdByScopeId(request.ScopeId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if projectId == "" {
		return nil, errors.NewInternalServerError(fmt.Errorf("Query project record by scopeid is empty scopeId is %v", request.ScopeId))
	}
	project, err := m.p.ProjectServer.GetProject(ctx, &projectpb.GetProjectRequest{
		ProjectID: projectId,
	})
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.Atoi(orgIdStr)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	lang := apis.Language(ctx)
	if project.Data.Type == "DOP" {
		scopeId, err := strconv.Atoi(projectId)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		roleList, err := m.p.bdl.ListMemberRoles(apistructs.ListScopeManagersByScopeIDRequest{
			ScopeType: apistructs.ScopeType(request.ScopeType),
			ScopeID:   int64(scopeId),
		}, int64(orgId), lang[0].Code)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		roleListResult := &pb.ListMemberRolesResponse{
			Data: &pb.RoleList{
				List:  make([]*pb.RoleInfo, 0),
				Total: int64(roleList.Total),
			},
		}
		for _, v := range roleList.List {
			if v.Role == "" && v.Name == "" {
				roleListResult.Data.Total--
				continue
			}
			role := &pb.RoleInfo{
				Role: v.Role,
				Name: v.Name,
			}
			roleListResult.Data.List = append(roleListResult.Data.List, role)
		}
		return roleListResult, nil
	}
	return &pb.ListMemberRolesResponse{
		Data: &pb.RoleList{
			List: []*pb.RoleInfo{
				{
					Role: "Owner",
					Name: m.p.I18n.Text(lang, "project_owner_role"),
				},
				{
					Role: "Lead",
					Name: m.p.I18n.Text(lang, "project_lead_role"),
				},
				{
					Role: "Dev",
					Name: m.p.I18n.Text(lang, "project_dev_role"),
				},
			},
			Total: 3,
		},
	}, nil
}

func (m memberService) DeleteMember(ctx context.Context, request *pb.DeleteMemberRequest) (*pb.DeleteMemberResponse, error) {
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	deleteReq := apistructs.MemberRemoveRequest{}
	err = json.Unmarshal(data, &deleteReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	userId := apis.GetUserID(ctx)
	projectIdStr, err := m.GetProjectIdByScopeId(request.Scope.Id)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if projectIdStr == "" {
		return nil, errors.NewInternalServerError(fmt.Errorf("Query project record by scopeid is empty scopeId is %v", request.Scope.Id))
	}
	deleteReq.IdentityInfo.UserID = userId
	deleteReq.Scope.ID = projectIdStr
	err = m.p.bdl.DeleteMember(deleteReq)
	if err != nil {
		return nil, err
	}
	projectId, err := m.contextEntry(projectIdStr, request.UserIds, ctx)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.DeleteMemberResponse{Data: projectId}, nil
}

func (m memberService) contextEntry(projectIdStr string, userIds []string, ctx context.Context) (uint64, error) {
	projectName, projectId, err := m.auditContextInfo(projectIdStr)
	if err != nil {
		return 0, errors.NewInternalServerError(err)
	}
	userList, err := m.p.bdl.ListUsers(apistructs.UserListRequest{UserIDs: userIds})
	if err != nil {
		return 0, errors.NewInternalServerError(err)
	}
	auditContext := map[string]interface{}{
		"projectName": projectName,
		"users":       userList.Users,
	}
	audit.ContextEntryMap(ctx, auditContext)
	return projectId, nil
}

func (m memberService) auditContextInfo(projectIdStr string) (string, uint64, error) {
	projectId, err := strconv.Atoi(projectIdStr)
	if err != nil {
		return "", 0, err
	}
	project, err := m.p.bdl.GetProject(uint64(projectId))
	if err != nil {
		return "", 0, err
	}
	return project.Name, uint64(projectId), nil
}

func (m memberService) CreateOrUpdateMember(ctx context.Context, request *pb.CreateOrUpdateMemberRequest) (*pb.CreateOrUpdateMemberResponse, error) {
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	memberReq := apistructs.MemberAddRequest{}
	err = json.Unmarshal(data, &memberReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectIdStr, err := m.GetProjectIdByScopeId(request.Scope.Id)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if projectIdStr == "" {
		return nil, errors.NewInternalServerError(fmt.Errorf("Query project record by scopeid is empty scopeId is %v", request.Scope.Id))
	}
	memberReq.Scope.ID = projectIdStr
	userId := apis.GetUserID(ctx)
	err = m.p.bdl.AddMember(memberReq, userId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectId, err := m.contextEntry(projectIdStr, request.UserIds, ctx)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.CreateOrUpdateMemberResponse{Data: projectId}, nil
}

func (m memberService) ListMember(ctx context.Context, request *pb.ListMemberRequest) (*pb.ListMemberResponse, error) {
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectIdStr, err := m.GetProjectIdByScopeId(request.ScopeId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if projectIdStr == "" {
		return nil, errors.NewInternalServerError(fmt.Errorf("Query project record by scopeid is empty scopeId is %v", request.ScopeId))
	}
	projectId, err := strconv.Atoi(projectIdStr)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	listMemberReq := apistructs.MemberListRequest{
		ScopeType: apistructs.ScopeType(request.ScopeType),
		ScopeID:   int64(projectId),
		Roles:     request.Roles,
		Labels:    request.Label,
		Q:         request.Q,
		PageNo:    int(request.PageNo),
		PageSize:  int(request.PageSize),
	}
	listTotal, err := m.p.bdl.ListMembersAndTotal(listMemberReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.ListMemberResponse{
		Data: &pb.MemberList{
			List: make([]*pb.Member, 0),
		},
	}
	data, err = json.Marshal(listTotal)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, result.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return result, nil
}

func (m memberService) ScopeRoleAccess(ctx context.Context, request *pb.ScopeRoleAccessRequest) (*pb.ScopeRoleAccessResponse, error) {
	listReq := apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			ID:   request.Scope.Id,
			Type: apistructs.ScopeType(request.Scope.Type),
		},
	}
	userId := apis.GetUserID(ctx)
	list, err := m.p.bdl.ScopeRoleAccessList(userId, &listReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(list)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := pb.ScopeRoleAccessResponse{
		Data:    &pb.ScopeRoleData{},
		UserIDs: list.ContactsWhenNoPermission,
	}
	err = json.Unmarshal(data, &result.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &result, nil
}
