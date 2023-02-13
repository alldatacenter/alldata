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

package sync

import (
	"context"
	"fmt"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/sync/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/common/apis"
)

type IssueSyncService struct {
	logger logs.Logger

	db    *dao.DBClient
	query query.Interface
}

func (s *IssueSyncService) IssueSync(ctx context.Context, req *pb.IssueSyncRequest) (*pb.IssueSyncResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, fmt.Errorf("not login")
	}
	if req.Id == 0 {
		return nil, nil
	}
	issue, err := s.query.GetIssue(req.Id, &commonpb.IdentityInfo{UserID: userID})
	if err != nil {
		return nil, err
	}
	for _, i := range req.UpdateFields {
		// TODO: define issue field as enum and add more available issue fields for sync
		switch i.Field {
		case "labels":
			relatingIssueIDs, err := s.db.GetRelatingIssues(uint64(req.Id), []string{apistructs.IssueRelationInclusion})
			if err != nil {
				return nil, err
			}
			var add, delete []*structpb.Value
			if i.Value != nil {
				add, delete = removeIntersectionInTwoNumberLists(i.Value.Addition, i.Value.Deletion)
			}
			if err := s.query.SyncLabels(&pb.Value{Addition: add, Deletion: delete}, relatingIssueIDs); err != nil {
				return nil, err
			}
		case "iterationID":
			iterationID := int64(i.Value.Content.GetNumberValue())
			if err := s.query.SyncIssueChildrenIteration(issue, iterationID); err != nil {
				return nil, err
			}
		}
	}

	return &pb.IssueSyncResponse{}, err
}

func removeIntersectionInTwoNumberLists(l1, l2 []*structpb.Value) ([]*structpb.Value, []*structpb.Value) {
	m := make(map[float64]bool)
	for _, v := range l1 {
		m[v.GetNumberValue()] = true
	}
	remove := make(map[float64]bool)
	for _, v := range l2 {
		if _, ok := m[v.GetNumberValue()]; ok {
			remove[v.GetNumberValue()] = true
		}
	}
	return removeElementsInFloatList(l1, remove), removeElementsInFloatList(l2, remove)
}

func removeElementsInFloatList(l []*structpb.Value, remove map[float64]bool) []*structpb.Value {
	res := make([]*structpb.Value, 0, len(l))
	for _, v := range l {
		if _, ok := remove[v.GetNumberValue()]; !ok {
			res = append(res, v)
		}
	}
	return res
}
