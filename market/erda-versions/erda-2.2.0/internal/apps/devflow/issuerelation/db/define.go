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

package db

import (
	"time"

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/apps/devflow/issuerelation/pb"
)

type IssueRelation struct {
	ID            string    `gorm:"id"`
	CreatedAt     time.Time `json:"created_at"`
	UpdatedAt     time.Time `json:"updated_at"`
	SoftDeletedAt int       `gorm:"soft_deleted_at"`
	Relation      string    `gorm:"relation"`
	IssueID       uint64    `gorm:"issue_id"`
	Type          string    `gorm:"type"`
	OrgID         uint64    `gorm:"org_id"`
	OrgName       string    `gorm:"org_name"`
	Extra         string
}

func (that IssueRelation) Covert() *pb.IssueRelation {
	return &pb.IssueRelation{
		ID:          that.ID,
		Type:        that.Type,
		Relation:    that.Relation,
		IssueID:     that.IssueID,
		TimeUpdated: timestamppb.New(that.UpdatedAt),
		TimeCreated: timestamppb.New(that.CreatedAt),
		Extra:       that.Extra,
	}
}

func (IssueRelation) TableName() string {
	return "erda_issue_relation"
}

func (db *Client) CreateIssueRelation(relation *IssueRelation) error {
	return db.Create(relation).Error
}

func (db *Client) DeleteIssueRelation(id string) error {

	var relation = IssueRelation{ID: id}
	return db.Model(&relation).Update("soft_deleted_at", int(time.Now().UnixNano()/1e6)).Debug().Error
}

func (db *Client) ListIssueRelation(req *pb.ListIssueRelationRequest, orgID string) ([]IssueRelation, error) {
	var issueRelations []IssueRelation
	client := db.Where("soft_deleted_at = 0 and type = ? and org_id = ?", req.Type, orgID)

	if len(req.IssueIDs) > 0 {
		client = client.Where("issue_id in (?)", req.IssueIDs)
	}

	if len(req.Relations) > 0 {
		client = client.Where("relation in (?)", req.Relations)
	}

	err := client.Find(&issueRelations).Debug().Error
	return issueRelations, err
}
