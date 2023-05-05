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

package issuerelation

import (
	"context"
	"fmt"
	"time"

	"github.com/erda-project/erda-proto-go/apps/devflow/issuerelation/pb"
	"github.com/erda-project/erda/internal/apps/devflow/issuerelation/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

type issueRelationService struct {
	p        *provider
	dbClient *db.Client
}

func (s *issueRelationService) Create(ctx context.Context, req *pb.CreateIssueRelationRequest) (*pb.CreateIssueRelationResponse, error) {
	if req.Relation == "" {
		return nil, fmt.Errorf("relation can not empty")
	}
	if req.Type == "" {
		return nil, fmt.Errorf("type can not empty")
	}
	if req.IssueID <= 0 {
		return nil, fmt.Errorf("issueID can not empty")
	}

	orgID := apis.GetOrgID(ctx)
	org, err := s.p.bdl.GetOrg(orgID)
	if err != nil {
		return nil, err
	}

	relations, err := s.dbClient.ListIssueRelation(&pb.ListIssueRelationRequest{
		Type:      req.Type,
		IssueIDs:  []uint64{req.IssueID},
		Relations: []string{req.Relation},
	}, orgID)
	if err != nil {
		return nil, err
	}

	var updateRelation *db.IssueRelation
	for _, relation := range relations {
		if relation.Relation == req.Relation && relation.IssueID == req.IssueID {
			updateRelation = &relation
			break
		}
	}

	if updateRelation != nil {
		return &pb.CreateIssueRelationResponse{
			IssueRelation: updateRelation.Covert(),
		}, nil
	}

	createRelation := db.IssueRelation{
		Relation:  req.Relation,
		Type:      req.Type,
		IssueID:   req.IssueID,
		ID:        uuid.New(),
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
		Extra:     req.Extra,
		OrgID:     org.ID,
		OrgName:   org.Name,
	}
	err = s.dbClient.CreateIssueRelation(&createRelation)
	if err != nil {
		return nil, err
	}
	return &pb.CreateIssueRelationResponse{
		IssueRelation: createRelation.Covert(),
	}, nil
}

func (s *issueRelationService) Delete(ctx context.Context, req *pb.DeleteIssueRelationRequest) (*pb.DeleteIssueRelationResponse, error) {
	if req.RelationID == "" {
		return nil, fmt.Errorf("relationID can not empty")
	}
	err := s.dbClient.DeleteIssueRelation(req.RelationID)
	if err != nil {
		return nil, err
	}
	return &pb.DeleteIssueRelationResponse{}, nil
}

func (s *issueRelationService) List(ctx context.Context, req *pb.ListIssueRelationRequest) (*pb.ListIssueRelationResponse, error) {
	if req.Type == "" {
		return nil, fmt.Errorf("type can not empty")
	}

	result, err := s.dbClient.ListIssueRelation(req, apis.GetOrgID(ctx))
	if err != nil {
		return nil, err
	}

	var data []*pb.IssueRelation
	for _, value := range result {
		data = append(data, value.Covert())
	}

	return &pb.ListIssueRelationResponse{
		Data: data,
	}, nil
}
