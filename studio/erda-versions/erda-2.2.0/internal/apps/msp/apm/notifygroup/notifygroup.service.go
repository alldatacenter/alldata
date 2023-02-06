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

package notifygroup

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/jinzhu/gorm"

	notifygroup "github.com/erda-project/erda-proto-go/core/messenger/notifygroup/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/notifygroup/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/common/errors"
)

type notifyGroupService struct {
	p *provider
}

func (n *notifyGroupService) GetProjectIdByScopeId(scopeId string) (string, error) {
	projectId := ""
	var err error
	projectId, err = n.p.monitorDB.SelectProjectIdByTk(scopeId)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			instance, err := n.p.mspTenantDB.QueryTenant(scopeId)
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

func (n *notifyGroupService) CreateNotifyGroup(ctx context.Context, request *pb.CreateNotifyGroupRequest) (*pb.CreateNotifyGroupResponse, error) {
	projectId, err := n.GetProjectIdByScopeId(request.ScopeId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if projectId == "" {
		return nil, errors.NewInternalServerError(fmt.Errorf("Query project record by scopeid is empty scopeId is %v", request.ScopeId))
	}

	label := map[string]string{
		"member_scopeID":   projectId,
		"member_scopeType": "project",
	}
	data, err := json.Marshal(label)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	request.Label = string(data)
	data, err = json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	createReq := &notifygroup.CreateNotifyGroupRequest{}
	err = json.Unmarshal(data, createReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	c := utils.NewContextWithHeader(ctx)
	resp, err := n.p.NotifyGroup.CreateNotifyGroup(c, createReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.CreateNotifyGroupResponse{
		Data: &pb.GroupIdAndProjectId{},
	}
	projectName, auditProjectId, err := n.GetProjectInfo(request.ScopeId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	workResp, err := n.p.Tenant.GetTenantProject(context.Background(), &tenantpb.GetTenantProjectRequest{
		ScopeId: request.ScopeId,
	})
	result.Data.ProjectId = auditProjectId
	result.Data.GroupID = resp.Data
	auditContext := auditContextMap(projectName, workResp.Data.Workspace, request.Name)
	audit.ContextEntryMap(ctx, auditContext)
	return result, nil
}

func (n *notifyGroupService) auditContextInfo(groupId int64, ctx context.Context) (string, string, string, uint64, error) {
	c := utils.NewContextWithHeader(ctx)
	notifyGroup, err := n.p.NotifyGroup.GetNotifyGroup(c, &notifygroup.GetNotifyGroupRequest{
		GroupID: groupId,
	})
	if err != nil {
		return "", "", "", 0, err
	}
	resp, err := n.p.Tenant.GetTenantProject(context.Background(), &tenantpb.GetTenantProjectRequest{
		ScopeId: notifyGroup.Data.ScopeId,
	})
	if err != nil {
		return "", "", "", 0, err
	}
	projectName, auditProjectId, err := n.GetProjectInfo(notifyGroup.Data.ScopeId)
	if err != nil {
		return "", "", "", 0, err
	}
	return projectName, resp.Data.Workspace, notifyGroup.Data.Name, auditProjectId, nil
}

func (n *notifyGroupService) GetProjectInfo(scopeId string) (string, uint64, error) {
	projectIdStr, err := n.GetProjectIdByScopeId(scopeId)
	if err != nil {
		return "", 0, errors.NewInternalServerError(err)
	}
	if projectIdStr == "" {
		return "", 0, errors.NewInternalServerError(fmt.Errorf("Query project record by scopeid is empty scopeId is %v", scopeId))
	}
	projectId, err := strconv.Atoi(projectIdStr)
	if err != nil {
		return "", 0, errors.NewInternalServerError(err)
	}
	auditProjectId := uint64(projectId)
	project, err := n.p.bdl.GetProject(auditProjectId)
	if err != nil {
		{
			return "", 0, errors.NewInternalServerError(err)
		}
	}
	return project.Name, auditProjectId, nil
}

func (n *notifyGroupService) QueryNotifyGroup(ctx context.Context, request *pb.QueryNotifyGroupRequest) (*pb.QueryNotifyGroupResponse, error) {
	queryReq := &notifygroup.QueryNotifyGroupRequest{
		PageNo:      request.PageNo,
		PageSize:    request.PageSize,
		ScopeType:   request.ScopeType,
		ScopeId:     request.ScopeId,
		Label:       request.Label,
		ClusterName: request.ClusterName,
		Name:        request.Name,
	}
	c := utils.NewContextWithHeader(ctx)
	resp, err := n.p.NotifyGroup.QueryNotifyGroup(c, queryReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(resp.Data.List)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.QueryNotifyGroupResponse{
		Data: &pb.QueryNotifyGroupData{
			List:  make([]*pb.NotifyGroup, 0),
			Total: resp.Data.Total,
		},
		UserIDs: resp.UserIDs,
	}
	err = json.Unmarshal(data, &result.Data.List)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return result, nil
}

func (n *notifyGroupService) GetNotifyGroup(ctx context.Context, request *pb.GetNotifyGroupRequest) (*pb.GetNotifyGroupResponse, error) {
	c := utils.NewContextWithHeader(ctx)
	resp, err := n.p.NotifyGroup.GetNotifyGroup(c, &notifygroup.GetNotifyGroupRequest{
		GroupID: request.GroupID,
	})
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.GetNotifyGroupResponse{
		Data:    &pb.NotifyGroup{},
		UserIDs: resp.UserIDs,
	}
	data, err := json.Marshal(resp.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, result.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return result, nil
}

func (n *notifyGroupService) UpdateNotifyGroup(ctx context.Context, request *pb.UpdateNotifyGroupRequest) (*pb.UpdateNotifyGroupResponse, error) {
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	updateReq := &notifygroup.UpdateNotifyGroupRequest{}
	err = json.Unmarshal(data, updateReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	c := utils.NewContextWithHeader(ctx)
	resp, err := n.p.NotifyGroup.UpdateNotifyGroup(c, updateReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.UpdateNotifyGroupResponse{
		Data: &pb.GroupIdAndProjectId{},
	}
	projectName, workspace, notifyGroupName, auditProjectId, err := n.auditContextInfo(request.GroupID, ctx)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result.Data.ProjectId = auditProjectId
	result.Data.GroupID = resp.Data
	auditContext := auditContextMap(projectName, workspace, notifyGroupName)
	audit.ContextEntryMap(ctx, auditContext)
	return result, nil
}

func (n *notifyGroupService) GetNotifyGroupDetail(ctx context.Context, request *pb.GetNotifyGroupDetailRequest) (*pb.GetNotifyGroupDetailResponse, error) {
	c := utils.NewContextWithHeader(ctx)
	resp, err := n.p.NotifyGroup.GetNotifyGroupDetail(c, &notifygroup.GetNotifyGroupDetailRequest{
		GroupID: request.GroupID,
	})
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(resp)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.GetNotifyGroupDetailResponse{
		Data: &pb.NotifyGroupDetail{},
	}
	err = json.Unmarshal(data, result.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return result, nil
}

func (n *notifyGroupService) DeleteNotifyGroup(ctx context.Context, request *pb.DeleteNotifyGroupRequest) (*pb.DeleteNotifyGroupResponse, error) {
	projectName, workspace, notifyGroupName, auditProjectId, err := n.auditContextInfo(request.GroupID, ctx)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	c := utils.NewContextWithHeader(ctx)
	resp, err := n.p.NotifyGroup.DeleteNotifyGroup(c, &notifygroup.DeleteNotifyGroupRequest{
		GroupID: request.GroupID,
	})
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &pb.DeleteNotifyGroupResponse{
		Data: &pb.GroupIdAndProjectId{},
	}
	result.Data.ProjectId = auditProjectId
	result.Data.GroupID = resp.Data
	auditContext := auditContextMap(projectName, workspace, notifyGroupName)
	audit.ContextEntryMap(ctx, auditContext)
	return result, nil
}

func auditContextMap(projectName, workspace, notifyGroupName string) map[string]interface{} {
	return map[string]interface{}{
		"projectName":     projectName,
		"workspace":       workspace,
		"notifyGroupName": notifyGroupName,
	}
}
