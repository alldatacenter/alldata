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
	"errors"
	"strconv"
	"strings"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/strutil"
)

func (i *IssueService) SubscribeIssue(ctx context.Context, req *pb.SubscribeIssueRequest) (*pb.SubscribeIssueResponse, error) {
	id, err := strconv.ParseInt(req.Id, 10, 64)
	if err != nil {
		return nil, apierrors.ErrSubscribeIssue.InvalidParameter(err)
	}

	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrSubscribeIssue.NotLogin()
	}

	issue, err := i.db.GetIssue(id)
	if err != nil {
		return nil, err
	}

	if !apis.IsInternalClient(ctx) {
		access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  issue.ProjectID,
			Resource: "issue-" + strings.ToLower(issue.Type),
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, apierrors.ErrSubscribeIssue.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrSubscribeIssue.AccessDenied()
		}
	}

	is, err := i.db.GetIssueSubscriber(id, identityInfo.UserID)
	if err != nil {
		return nil, err
	}
	if is != nil {
		return nil, errors.New("already subscribed")
	}

	create := dao.IssueSubscriber{
		IssueID: int64(issue.ID),
		UserID:  identityInfo.UserID,
	}

	if err := i.db.CreateIssueSubscriber(&create); err != nil {
		return nil, err
	}
	return &pb.SubscribeIssueResponse{Data: id}, nil
}

func (i *IssueService) UnsubscribeIssue(ctx context.Context, req *pb.UnsubscribeIssueRequest) (*pb.UnsubscribeIssueResponse, error) {
	id, err := strconv.ParseInt(req.Id, 10, 64)
	if err != nil {
		return nil, apierrors.ErrSubscribeIssue.InvalidParameter(err)
	}

	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrSubscribeIssue.NotLogin()
	}

	issue, err := i.db.GetIssue(id)
	if err != nil {
		return nil, err
	}

	if !apis.IsInternalClient(ctx) {
		access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  issue.ProjectID,
			Resource: "issue-" + strings.ToLower(issue.Type),
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, apierrors.ErrSubscribeIssue.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrSubscribeIssue.AccessDenied()
		}
	}

	if err := i.db.DeleteIssueSubscriber(id, identityInfo.UserID); err != nil {
		return nil, err
	}
	return &pb.UnsubscribeIssueResponse{Data: id}, nil
}

func (i *IssueService) BatchUpdateIssueSubscriber(ctx context.Context, req *pb.BatchUpdateIssueSubscriberRequest) (*pb.BatchUpdateIssueSubscriberResponse, error) {
	id, err := strconv.ParseInt(req.Id, 10, 64)
	if err != nil {
		return nil, apierrors.ErrSubscribeIssue.InvalidParameter(err)
	}

	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrSubscribeIssue.NotLogin()
	}
	req.IdentityInfo = identityInfo
	req.IssueID = id

	issue, err := i.db.GetIssue(id)
	if err != nil {
		return nil, err
	}

	if !apis.IsInternalClient(ctx) {
		access, err := i.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  issue.ProjectID,
			Resource: "issue-" + strings.ToLower(issue.Type),
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, apierrors.ErrSubscribeIssue.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrSubscribeIssue.AccessDenied()
		}
	}

	oldSubscribers, err := i.db.GetIssueSubscribersByIssueID(req.IssueID)
	if err != nil {
		return nil, err
	}

	subscriberMap := make(map[string]struct{}, 0)
	req.Subscribers = strutil.DedupSlice(req.Subscribers)
	for _, v := range req.Subscribers {
		subscriberMap[v] = struct{}{}
	}

	var needDeletedSubscribers []string
	for _, v := range oldSubscribers {
		_, exist := subscriberMap[v.UserID]
		if exist {
			delete(subscriberMap, v.UserID)
		} else {
			needDeletedSubscribers = append(needDeletedSubscribers, v.UserID)
		}
	}

	if len(needDeletedSubscribers) != 0 {
		if err := i.db.BatchDeleteIssueSubscribers(req.IssueID, needDeletedSubscribers); err != nil {
			return nil, err
		}
	}

	var subscribers []dao.IssueSubscriber
	issueID := int64(req.IssueID)
	for k := range subscriberMap {
		subscribers = append(subscribers, dao.IssueSubscriber{IssueID: issueID, UserID: k})
	}
	if len(subscribers) != 0 {
		if err := i.db.BatchCreateIssueSubscribers(subscribers); err != nil {
			return nil, err
		}
	}

	return &pb.BatchUpdateIssueSubscriberResponse{}, nil
}
