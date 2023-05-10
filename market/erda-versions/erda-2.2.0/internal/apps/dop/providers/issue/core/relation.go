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
	"fmt"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/strutil"
)

func (i *IssueService) AddIssueRelation(ctx context.Context, req *pb.AddIssueRelationRequest) (*pb.AddIssueRelationResponse, error) {
	issueID, err := strutil.Atoi64(req.Id)
	if err != nil {
		return nil, apierrors.ErrCreateIssueRelation.InvalidParameter(err)
	}

	req.IssueID = uint64(issueID)
	if err := Check(req); err != nil {
		return nil, apierrors.ErrCreateIssueRelation.InvalidParameter(err)
	}
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrCreateIssueRelation.NotLogin()
	}

	exist, err := i.db.IssueRelationsExist(&dao.IssueRelation{
		IssueID: req.IssueID,
		Type:    req.Type,
	}, req.RelatedIssues)
	if err != nil {
		return nil, apierrors.ErrCreateIssueRelation.InternalError(err)
	}
	if exist {
		return nil, apierrors.ErrCreateIssueRelation.AlreadyExists()
	}

	if req.Type == IssueRelationInclusion {
		if err := i.ValidIssueRelationType(req.IssueID, pb.IssueTypeEnum_REQUIREMENT.String()); err != nil {
			return nil, err
		}
		if err := i.ValidIssueRelationTypes(req.RelatedIssues, []string{pb.IssueTypeEnum_REQUIREMENT.String(), pb.IssueTypeEnum_BUG.String()}); err != nil {
			return nil, err
		}
	}
	issueRels := make([]dao.IssueRelation, 0, len(req.RelatedIssues))
	for _, i := range req.RelatedIssues {
		issueRels = append(issueRels, dao.IssueRelation{
			IssueID:      req.IssueID,
			RelatedIssue: i,
			Comment:      req.Comment,
			Type:         req.Type,
		})
	}

	if err := i.db.BatchCreateIssueRelations(issueRels); err != nil {
		return nil, err
	}

	if req.Type == IssueRelationInclusion {
		if err := i.query.AfterIssueInclusionRelationChange(req.IssueID); err != nil {
			return nil, err
		}
	}
	return &pb.AddIssueRelationResponse{}, nil
}

func (i *IssueService) DeleteIssueRelation(ctx context.Context, req *pb.DeleteIssueRelationRequest) (*pb.DeleteIssueRelationResponse, error) {
	issueID, err := strutil.Atoi64(req.Id)
	if err != nil {
		return nil, apierrors.ErrDeleteIssueRelation.InvalidParameter(err)
	}
	relatedIssueID, err := strutil.Atoi64(req.RelatedIssueID)
	if err != nil {
		return nil, apierrors.ErrDeleteIssueRelation.InvalidParameter(err)
	}
	if err := i.db.DeleteIssueRelation(uint64(issueID), uint64(relatedIssueID), req.RelationTypes); err != nil {
		return nil, err
	}

	if strutil.Exist(req.RelationTypes, apistructs.IssueRelationInclusion) {
		if err := i.query.AfterIssueInclusionRelationChange(uint64(issueID)); err != nil {
			return nil, err
		}
	}
	return &pb.DeleteIssueRelationResponse{}, nil
}

type issueRelationRetriever struct {
	issueIDs []uint64
	userIDs  []string
}

func (is *IssueService) GetIssueRelations(ctx context.Context, req *pb.GetIssueRelationsRequest) (*pb.GetIssueRelationsResponse, error) {
	issueID, err := strutil.Atoi64(req.Id)
	if err != nil {
		return nil, apierrors.ErrGetIssueRelations.InvalidParameter(err)
	}
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrCreateIssueRelation.NotLogin()
	}
	if len(req.RelationTypes) == 0 {
		req.RelationTypes = []string{IssueRelationInclusion, IssueRelationConnection}
	}

	var userIDs []string
	var relations pb.IssueRelations
	for _, i := range req.RelationTypes {
		relatingIssueIDs, relatedIssueIDs, err := is.query.GetIssueRelationsByIssueIDs(uint64(issueID), []string{i})
		if err != nil {
			return nil, apierrors.ErrGetIssueRelations.InternalError(err)
		}
		var users []string
		r := &issueRelationRetriever{
			relatingIssueIDs, users,
		}
		if i == apistructs.IssueRelationInclusion {
			relatingIssues, err := is.GetIssuesByRelation(r)
			if err != nil {
				return nil, apierrors.ErrGetIssueRelations.InternalError(err)
			}
			r.issueIDs = relatedIssueIDs
			relatedIssues, err := is.GetIssuesByRelation(r)
			if err != nil {
				return nil, apierrors.ErrGetIssueRelations.InternalError(err)
			}
			relations.Include = relatingIssues
			relations.BeIncluded = relatedIssues
		} else {
			r.issueIDs = append(relatingIssueIDs, relatedIssueIDs...)
			relatingIssues, err := is.GetIssuesByRelation(r)
			if err != nil {
				return nil, apierrors.ErrGetIssueRelations.InternalError(err)
			}
			relations.RelatedTo = relatingIssues
		}
		userIDs = append(userIDs, r.userIDs...)
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	return &pb.GetIssueRelationsResponse{Data: &relations, UserIDs: userIDs}, nil
}

func (i *IssueService) GetIssuesByRelation(r *issueRelationRetriever) ([]*pb.Issue, error) {
	issues, err := i.query.GetIssuesByIssueIDs(r.issueIDs)
	if err != nil {
		return issues, err
	}

	for _, issue := range issues {
		r.userIDs = append(r.userIDs, issue.Creator, issue.Assignee)
	}
	return issues, nil
}

const IssueRelationConnection = "connection"
const IssueRelationInclusion = "inclusion"

// Check 检查请求参数是否合法
func Check(irc *pb.AddIssueRelationRequest) error {
	if irc.IssueID == 0 {
		return errors.New("issueId is required")
	}

	if len(irc.RelatedIssues) == 0 {
		return errors.New("relatedIssue is required")
	}

	if irc.ProjectId == 0 {
		return errors.New("projectId is required")
	}

	if len(irc.Type) == 0 {
		return errors.New("type is required")
	}

	if irc.Type != IssueRelationConnection && irc.Type != IssueRelationInclusion {
		return errors.New("invalid issue relation type")
	}

	for _, i := range irc.RelatedIssues {
		if i == irc.IssueID {
			return errors.New("can not connect yourself")
		}
	}

	return nil
}

func (i *IssueService) ValidIssueRelationType(id uint64, issueType string) error {
	issue, err := i.db.GetIssue(int64(id))
	if err != nil {
		return err
	}
	if issue.Type != issueType {
		return fmt.Errorf("issue id %v type is %v, not %v", id, issue.Type, issueType)
	}
	return nil
}

func (i *IssueService) ValidIssueRelationTypes(ids []uint64, issueTypes []string) error {
	issueIDs := make([]int64, 0, len(ids))
	for _, i := range ids {
		issueIDs = append(issueIDs, int64(i))
	}
	issues, err := i.db.ListIssue(pb.IssueListRequest{IDs: issueIDs, Type: issueTypes})
	if err != nil {
		return err
	}
	if len(issues) > 0 {
		return fmt.Errorf("issue ids %v contains invalid type", ids)
	}
	return nil
}
