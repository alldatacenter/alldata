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

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
)

func (i *IssueService) GetIssueStage(ctx context.Context, req *pb.IssueStageRequest) (*pb.GetIssueStageResponse, error) {
	res, err := i.query.GetIssueStage(req)
	if err != nil {
		return nil, err
	}
	return &pb.GetIssueStageResponse{Data: res}, nil
}

func (i *IssueService) UpdateIssueStage(ctx context.Context, req *pb.IssueStageRequest) (*pb.UpdateIssueStageResponse, error) {
	identityInfo := apis.GetIdentityInfo(ctx)
	if identityInfo == nil {
		return nil, apierrors.ErrCreateIssueProperty.NotLogin()
	}
	req.IdentityInfo = identityInfo

	err := i.db.DeleteIssuesStage(req.OrgID, req.IssueType)
	if err != nil {
		return nil, err
	}
	var stages []dao.IssueStage
	for _, v := range req.List {
		stage := dao.IssueStage{
			OrgID:     req.OrgID,
			IssueType: req.IssueType,
			Name:      v.Name,
			Value:     v.Value,
		}
		if stage.Value == "" {
			stage.Value = v.Name
		}
		stages = append(stages, stage)
	}
	return &pb.UpdateIssueStageResponse{}, i.db.CreateIssueStage(stages)
}
