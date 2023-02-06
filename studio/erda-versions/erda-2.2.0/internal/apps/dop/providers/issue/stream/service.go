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

package stream

import (
	"context"
	"fmt"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/dop/issue/stream/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/core"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/strutil"
)

type CommentIssueStreamService struct {
	logger logs.Logger

	db     *dao.DBClient
	bdl    *bundle.Bundle
	stream core.Interface
	query  query.Interface
}

func (s *CommentIssueStreamService) BatchCreateIssueStream(ctx context.Context, req *pb.CommentIssueStreamBatchCreateRequest) (*pb.CommentIssueStreamBatchCreateResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, fmt.Errorf("not login")
	}
	if len(req.IssueStreams) == 0 {
		return &pb.CommentIssueStreamBatchCreateResponse{}, nil
	}
	issueStreams := make([]dao.IssueStream, 0, len(req.IssueStreams))
	issueAppRels := make([]dao.IssueAppRelation, 0, len(req.IssueStreams))
	issueAppRelIDs := make([]int64, 0, len(req.IssueStreams))
	for _, i := range req.IssueStreams {
		if i.Type == "" {
			i.Type = string(common.ISTComment)
		}

		var istParam common.ISTParam
		if i.Type == string(common.ISTComment) {
			istParam.Comment = i.Content
			istParam.CommentTime = time.Now().Format("2006-01-02 15:04:05")
		} else {
			istParam.MRInfo = pb.MRCommentInfo{
				AppID:   i.MrInfo.AppID,
				MrID:    i.MrInfo.MrID,
				MrTitle: i.MrInfo.MrTitle,
			}
			issueAppRel := dao.IssueAppRelation{
				IssueID: i.IssueID,
				AppID:   i.MrInfo.AppID,
				MRID:    i.MrInfo.MrID,
			}
			issueAppRels = append(issueAppRels, issueAppRel)
			issueAppRelIDs = append(issueAppRelIDs, i.IssueID)
		}
		is := dao.IssueStream{
			IssueID:      i.IssueID,
			Operator:     i.UserID,
			StreamType:   i.Type,
			StreamParams: istParam,
		}
		issueStreams = append(issueStreams, is)
	}
	if err := s.db.BatchCreateIssueStream(issueStreams); err != nil {
		return nil, err
	}
	if len(issueAppRels) > 0 {
		if err := s.db.BatchCreateIssueAppRelation(issueAppRels); err != nil {
			return nil, err
		}
		if err := s.query.AfterIssueAppRelationCreate(issueAppRelIDs); err != nil {
			return nil, err
		}
	}
	return &pb.CommentIssueStreamBatchCreateResponse{}, nil
}

func (s *CommentIssueStreamService) CreateIssueStream(ctx context.Context, req *pb.IssueStreamCreateRequest) (*pb.IssueStreamCreateResponse, error) {
	issueID, err := strutil.Atoi64(req.Id)
	if err != nil {
		return nil, apierrors.ErrCreateIssueStream.InvalidParameter(err)
	}

	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrCreateIssueStream.NotLogin()
	}
	req.IdentityInfo = identityInfo

	if req.Type == "" {
		req.Type = common.ISTComment // 兼容现有评论
	}

	var istParam common.ISTParam
	if req.Type == common.ISTComment {
		istParam.Comment = req.Content
		istParam.CommentTime = time.Now().Format("2006-01-02 15:04:05")
		user, err := s.bdl.GetCurrentUser(identityInfo.UserID)
		if err != nil {
			return nil, apierrors.ErrCreateIssueStream.InvalidParameter(err)
		}
		istParam.UserName = user.Nick
	} else { // mr 类型评论
		istParam.MRInfo = *req.MrInfo
	}
	commentReq := &common.IssueStreamCreateRequest{
		IssueID:      issueID,
		Operator:     req.IdentityInfo.UserID,
		StreamType:   req.Type,
		StreamParams: istParam,
	}
	commentID, err := s.stream.Create(commentReq)
	if err != nil {
		return nil, apierrors.ErrCreateIssueStream.InternalError(err)
	}
	go func() {
		if err := s.stream.CreateIssueEvent(commentReq); err != nil {
			logrus.Errorf("create issue %d event err: %v", commentReq.IssueID, err)
		}
	}()

	return &pb.IssueStreamCreateResponse{Data: commentID}, nil
}

func (s *CommentIssueStreamService) PagingIssueStreams(ctx context.Context, req *pb.PagingIssueStreamsRequest) (*pb.PagingIssueStreamsResponse, error) {
	id, err := strconv.ParseUint(req.Id, 10, 64)
	if err != nil {
		return nil, apierrors.ErrPagingIssueStream.InvalidParameter(err)
	}
	req.IssueID = id
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrPagingIssueStream.NotLogin()
	}
	issueModel, err := s.db.GetIssue(int64(req.IssueID))
	if err != nil {
		return nil, apierrors.ErrPagingIssueStream.InvalidParameter(err)
	}

	if !apis.IsInternalClient(ctx) {
		access, err := s.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  issueModel.ProjectID,
			Resource: apistructs.ProjectResource,
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, apierrors.ErrPagingIssueStream.InternalError(err)
		}
		if !access.Access {
			return nil, apierrors.ErrPagingIssueStream.AccessDenied()
		}
	}

	// 请求校验
	if req.IssueID == 0 {
		return nil, apierrors.ErrPagingIssueStream.MissingParameter("missing issueID")
	}
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 10
	}
	// 分页查询
	total, issueStreams, err := s.db.PagingIssueStream(req)
	if err != nil {
		return nil, err
	}
	iss := make([]*pb.IssueStream, 0, len(issueStreams))
	for i := range issueStreams {
		v := issueStreams[i]
		is := &pb.IssueStream{
			Id:         int64(v.ID),
			IssueID:    v.IssueID,
			Operator:   v.Operator,
			StreamType: v.StreamType,
			CreatedAt:  timestamppb.New(v.CreatedAt),
			UpdatedAt:  timestamppb.New(v.UpdatedAt),
			MrInfo:     &pb.MRCommentInfo{},
		}
		if v.StreamType == common.ISTRelateMR {
			is.MrInfo = &v.StreamParams.MRInfo
		} else {
			content, err := s.stream.GetDefaultContent(core.StreamTemplateRequest{
				StreamType:   v.StreamType,
				StreamParams: v.StreamParams,
				Locale:       apis.GetLang(ctx),
				Lang:         apis.Language(ctx),
			})
			if err != nil {
				return nil, err
			}
			is.Content = content
		}
		iss = append(iss, is)
	}

	var userIDs []string
	for _, stream := range iss {
		userIDs = append(userIDs, stream.Operator)
	}
	userIDs = strutil.DedupSlice(userIDs, true)

	return &pb.PagingIssueStreamsResponse{
		Data: &pb.IssueStreamPagingResponseData{
			Total: total,
			List:  iss,
		},
		UserIDs: userIDs,
	}, nil
}
