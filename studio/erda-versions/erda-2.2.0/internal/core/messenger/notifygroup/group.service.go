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
	"strings"
	"unicode/utf8"

	"github.com/erda-project/erda-proto-go/core/messenger/notifygroup/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/services/notify"
	"github.com/erda-project/erda/internal/core/legacy/services/permission"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
)

type notifyGroupService struct {
	Permission  *permission.Permission
	NotifyGroup *notify.NotifyGroup
	bdl         *bundle.Bundle
}

func (n *notifyGroupService) BatchGetNotifyGroup(ctx context.Context, request *pb.BatchGetNotifyGroupRequest) (*pb.BatchGetNotifyGroupResponse, error) {
	var ids []int64
	for _, idStr := range strings.Split(request.Ids, ",") {
		id, err := strconv.ParseInt(idStr, 10, 64)
		if err == nil {
			ids = append(ids, id)
		}
	}
	result, err := n.NotifyGroup.BatchGet(ids)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(result)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	var groupData []*pb.NotifyGroup
	err = json.Unmarshal(data, &groupData)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.BatchGetNotifyGroupResponse{
		Data: groupData,
	}, nil
}

func (n *notifyGroupService) CreateNotifyGroup(ctx context.Context, request *pb.CreateNotifyGroupRequest) (*pb.CreateNotifyGroupResponse, error) {
	userIdStr := apis.GetUserID(ctx)
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.ParseInt(orgIdStr, 10, 64)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	org, err := n.bdl.GetOrg(orgIdStr)
	if err != nil {
		return nil, errors.NewInvalidParameterError("orgId", "orgId is invalidate")
	}
	if strings.TrimSpace(request.Name) == "" {
		return nil, errors.NewInvalidParameterError(request.Name, "name is empty")
	}
	if utf8.RuneCountInString(request.Name) > 50 {
		return nil, errors.NewInvalidParameterError(request.Name, "name is too long")
	}
	err = n.checkNotifyPermission(userIdStr, request.ScopeType, request.ScopeId, apistructs.CreateAction)
	if err != nil {
		return nil, errors.NewPermissionError(apistructs.NotifyResource, apistructs.CreateAction, err.Error())
	}
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	creatReq := &apistructs.CreateNotifyGroupRequest{}
	err = json.Unmarshal(data, creatReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	creatReq.OrgID = orgId
	creatReq.Creator = userIdStr
	lang := apis.GetLang(ctx)
	langCode := n.bdl.GetLocale(lang)
	notifyGroupID, err := n.NotifyGroup.Create(langCode, creatReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	var auditContext map[string]interface{}
	if request.ScopeType == apistructs.OrgResource {
		auditContext = map[string]interface{}{
			"notifyGroupName": request.Name,
			"orgName":         org.Name,
		}
	} else {
		auditContext = map[string]interface{}{
			"isSkip": true,
		}
	}
	audit.ContextEntryMap(ctx, auditContext)
	return &pb.CreateNotifyGroupResponse{
		Data: notifyGroupID,
	}, nil
}

func (n *notifyGroupService) QueryNotifyGroup(ctx context.Context, request *pb.QueryNotifyGroupRequest) (*pb.QueryNotifyGroupResponse, error) {
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.ParseInt(orgIdStr, 10, 64)
	userIdStr := apis.GetUserID(ctx)
	if request.PageNo < 1 {
		request.PageNo = 1
	}
	if request.PageSize < 1 {
		request.PageSize = 10
	}
	err = n.checkNotifyPermission(userIdStr, request.ScopeType, request.ScopeId, apistructs.ListAction)
	if err != nil {
		return nil, errors.NewPermissionError(apistructs.NotifyResource, apistructs.ListAction, err.Error())
	}
	queryReq := &apistructs.QueryNotifyGroupRequest{}
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, queryReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	//result, err := n.DB.QueryNotifyGroup(queryReq, orgId)
	result, err := n.NotifyGroup.Query(queryReq, orgId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	var userIDs []string
	for _, group := range result.List {
		userIDs = append(userIDs, group.Creator)
		for _, target := range group.Targets {
			if target.Type == apistructs.UserNotifyTarget {
				for _, t := range target.Values {
					userIDs = append(userIDs, t.Receiver)
				}
			}
		}
	}
	data, err = json.Marshal(result)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	groupResp := &pb.QueryNotifyGroupData{}
	err = json.Unmarshal(data, groupResp)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.QueryNotifyGroupResponse{
		Data:    groupResp,
		UserIDs: userIDs,
	}, nil
}

func (n *notifyGroupService) GetNotifyGroup(ctx context.Context, request *pb.GetNotifyGroupRequest) (*pb.GetNotifyGroupResponse, error) {
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.ParseInt(orgIdStr, 10, 64)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	//notifyGroup, err := n.DB.GetNotifyGroupByID(request.GroupID, orgId)
	notifyGroup, err := n.NotifyGroup.Get(request.GroupID, orgId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	userIdStr := apis.GetUserID(ctx)
	err = n.checkNotifyPermission(userIdStr, notifyGroup.ScopeType, notifyGroup.ScopeID, apistructs.GetAction)
	if err != nil {
		return nil, errors.NewPermissionError(apistructs.NotifyResource, apistructs.GetAction, err.Error())
	}
	var userIDs []string
	if notifyGroup.Creator != "" {
		userIDs = append(userIDs, notifyGroup.Creator)
	}
	for _, target := range notifyGroup.Targets {
		if target.Type == apistructs.UserNotifyTarget {
			for _, t := range target.Values {
				userIDs = append(userIDs, t.Receiver)
			}
		}
	}
	data, err := json.Marshal(notifyGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	groupData := &pb.NotifyGroup{}
	err = json.Unmarshal(data, groupData)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.GetNotifyGroupResponse{
		Data:    groupData,
		UserIDs: userIDs,
	}, nil
}

func (n *notifyGroupService) UpdateNotifyGroup(ctx context.Context, request *pb.UpdateNotifyGroupRequest) (*pb.UpdateNotifyGroupResponse, error) {
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.ParseInt(orgIdStr, 10, 64)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	org, err := n.bdl.GetOrg(orgIdStr)
	if err != nil {
		return nil, errors.NewInvalidParameterError("orgId", "orgId is invalidate")
	}
	//notifyGroup, err := n.DB.GetNotifyGroupByID(request.GroupID, orgId)
	notifyGroup, err := n.NotifyGroup.Get(request.GroupID, orgId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	userIdStr := apis.GetUserID(ctx)
	err = n.checkNotifyPermission(userIdStr, notifyGroup.ScopeType, notifyGroup.ScopeID, apistructs.UpdateAction)
	if err != nil {
		return nil, errors.NewPermissionError(apistructs.NotifyResource, apistructs.UpdateAction, err.Error())
	}
	notifyGroupUpdateReq := &apistructs.UpdateNotifyGroupRequest{
		ID:    request.GroupID,
		Name:  request.Name,
		OrgID: orgId,
	}

	data, err := json.Marshal(request.Targets)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, &notifyGroupUpdateReq.Targets)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = n.NotifyGroup.Update(notifyGroupUpdateReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	var auditContext map[string]interface{}
	if notifyGroup.ScopeType == apistructs.OrgResource {
		auditContext = map[string]interface{}{
			"notifyGroupName": request.Name,
			"orgName":         org.Name,
		}
	} else {
		auditContext = map[string]interface{}{
			"isSkip": true,
		}
	}
	audit.ContextEntryMap(ctx, auditContext)
	return &pb.UpdateNotifyGroupResponse{
		Data: notifyGroup.ID,
	}, nil
}

func (n *notifyGroupService) GetNotifyGroupDetail(ctx context.Context, request *pb.GetNotifyGroupDetailRequest) (*pb.GetNotifyGroupDetailResponse, error) {
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.ParseInt(orgIdStr, 10, 64)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	notifyGroup, err := n.NotifyGroup.GetDetail(request.GroupID, orgId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	userIdStr := apis.GetUserID(ctx)
	err = n.checkNotifyPermission(userIdStr, notifyGroup.ScopeType, notifyGroup.ScopeID, apistructs.GetAction)
	if err != nil {
		return nil, errors.NewPermissionError(apistructs.NotifyResource, apistructs.GetAction, err.Error())
	}
	data, err := json.Marshal(notifyGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	groupData := &pb.NotifyGroupDetail{}
	err = json.Unmarshal(data, groupData)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.GetNotifyGroupDetailResponse{
		Data: groupData,
	}, nil
}

func (n *notifyGroupService) DeleteNotifyGroup(ctx context.Context, request *pb.DeleteNotifyGroupRequest) (*pb.DeleteNotifyGroupResponse, error) {
	orgIdStr := apis.GetOrgID(ctx)
	orgId, err := strconv.ParseInt(orgIdStr, 10, 64)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	org, err := n.bdl.GetOrg(orgIdStr)
	if err != nil {
		return nil, errors.NewInvalidParameterError("orgId", "orgId is invalidate")
	}
	notifyGroup, err := n.NotifyGroup.Get(request.GroupID, orgId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	userIdStr := apis.GetUserID(ctx)
	err = n.checkNotifyPermission(userIdStr, notifyGroup.ScopeType, notifyGroup.ScopeID, apistructs.DeleteAction)
	if err != nil {
		return nil, errors.NewPermissionError(apistructs.NotifyResource, apistructs.DeleteAction, err.Error())
	}
	err = n.NotifyGroup.Delete(request.GroupID, orgId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	var auditContext map[string]interface{}
	if notifyGroup.ScopeType == apistructs.OrgResource {
		auditContext = map[string]interface{}{
			"notifyGroupName": notifyGroup.Name,
			"orgName":         org.Name,
		}
	} else {
		auditContext = map[string]interface{}{
			"isSkip": true,
		}
	}
	audit.ContextEntryMap(ctx, auditContext)
	return &pb.DeleteNotifyGroupResponse{
		Data: notifyGroup.ID,
	}, nil
}

func (n *notifyGroupService) checkNotifyPermission(userId, scopeType, scopeId, action string) error {
	if scopeType == apistructs.MSPScope {
		return nil
	}
	if userId == "" {
		return fmt.Errorf("failed to get permission(User-ID is empty)")
	}
	var scope apistructs.ScopeType
	if scopeType == "org" {
		scope = apistructs.OrgScope
	}
	if scopeType == "project" {
		scope = apistructs.ProjectScope
	}
	if scopeType == "app" {
		scope = apistructs.AppScope
	}
	id, err := strconv.ParseInt(scopeId, 10, 64)
	if err != nil {
		return err
	}
	access, err := n.Permission.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userId,
		Scope:    scope,
		ScopeID:  uint64(id),
		Action:   action,
		Resource: apistructs.NotifyResource,
	})
	if err != nil {
		return err
	}
	if !access {
		return fmt.Errorf("no permission")
	}
	return nil
}
